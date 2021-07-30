package io.micronaut.rabbitmq.listener

import com.github.dockerjava.api.DockerClient
import com.rabbitmq.client.*
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.MessageBody
import io.micronaut.rabbitmq.AbstractRabbitMQClusterTest
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener
import io.micronaut.rabbitmq.bind.RabbitConsumerState
import io.micronaut.rabbitmq.exception.RabbitListenerException
import io.micronaut.rabbitmq.exception.RabbitListenerExceptionHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.GenericContainer
import spock.lang.Shared
import spock.util.concurrent.PollingConditions

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo

class ConsumerRecoverySpec extends AbstractRabbitMQClusterTest {
    private static final Logger log = LoggerFactory.getLogger(ConsumerRecoverySpec.class)
    private static final DockerClient DOCKER_CLIENT = DockerClientFactory.lazyClient()

    @Shared
    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor()
    @Shared
    private Set<String> publishedMessages = new LinkedHashSet<>()
    @Shared
    private boolean enablePublisher = false

    def setupSpec() {
        /*
         * The current Micronaut publisher implementation has a flaw in detecting unroutable drop/return messages
         * in a Rabbit cluster setup. It considers the messages as published even if the broker did not enqueue it.
         * So for this test a simple custom publisher is used that detects unpublished messages.
         */
        ConnectionFactory connectionFactory = new ConnectionFactory()
        connectionFactory.setPort(node3Port)
        connectionFactory.newConnection().openChannel()
                .map(ch -> {
                    try {
                        ch.confirmSelect()
                        // returned messages must not count as published
                        ch.addReturnListener(new ReturnCallback() {
                            @Override
                            void handle(Return r) {
                                String returned = new String(r.getBody())
                                publishedMessages.remove(returned)
                                log.warn("{} publish message returned: {}", publishedMessages.size(), returned)
                            }
                        })
                    } catch (IOException e) {
                        log.error("failed to set confirmSelect", e)
                    }
                    return ch
                })
                .ifPresent(
                        ch -> executorService.scheduleWithFixedDelay(() -> {
                            if (!enablePublisher) {
                                return
                            }
                            String msg = UUID.randomUUID().toString()
                            try {
                                publishedMessages.add(msg)
                                ch.basicPublish(EXCHANGE, "", true,
                                        new AMQP.BasicProperties.Builder().deliveryMode(2).build(),
                                        msg.getBytes())
                                if (ch.waitForConfirms(1000)) {
                                    log.info("publish ack")
                                }
                            } catch (IOException | RuntimeException | InterruptedException | TimeoutException e) {
                                publishedMessages.remove(msg)
                                log.error("failed to publish: {}", e.getMessage())
                            }
                        }, 500, 500, TimeUnit.MILLISECONDS))
    }

    def cleanupSpec() {
        executorService.shutdown()
    }

    def setup() {
        publishedMessages.clear()
        enablePublisher = true
    }

    def "test restart of Node1 with consumer connected to Node1"() {
        given:
        ApplicationContext ctx = startContext(["connectToNode": "node1"])
        TestConsumer consumer = ctx.getBean(TestConsumer)
        awaitPublishConsumeOfMessages(consumer)

        when:
        restartContainer(NODE1_CONT)

        then:
        awaitPublishConsumeOfMessages(consumer)
        stopPublishAndAssertAllConsumed(consumer)

        cleanup:
        ctx.close()
    }

    def "test restart of Node1 with consumer connected to Node2"() {
        given:
        ApplicationContext ctx = startContext(["connectToNode": "node2"])
        TestConsumer consumer = ctx.getBean(TestConsumer)
        awaitPublishConsumeOfMessages(consumer)

        when:
        restartContainer(NODE1_CONT)

        then:
        awaitPublishConsumeOfMessages(consumer)
        stopPublishAndAssertAllConsumed(consumer)

        cleanup:
        ctx.close()
    }

    def "test restart of Node2 with consumer connected to Node1"() {
        given:
        ApplicationContext ctx = startContext(["connectToNode": "node1"])
        TestConsumer consumer = ctx.getBean(TestConsumer)
        awaitPublishConsumeOfMessages(consumer)

        when:
        restartContainer(NODE2_CONT)

        then:
        awaitPublishConsumeOfMessages(consumer)
        stopPublishAndAssertAllConsumed(consumer)

        cleanup:
        ctx.close()
    }

    def "test restart of Node2 with consumer connected to Node2"() {
        given:
        ApplicationContext ctx = startContext(["connectToNode": "node2"])
        TestConsumer consumer = ctx.getBean(TestConsumer)
        awaitPublishConsumeOfMessages(consumer)

        when:
        restartContainer(NODE2_CONT)

        then:
        awaitPublishConsumeOfMessages(consumer)
        stopPublishAndAssertAllConsumed(consumer)

        cleanup:
        ctx.close()
    }

    def "test consumer recovery after delivery acknowledgement timeout"() {
        given:
        ApplicationContext ctx = startContext(["connectToNode": "node3"])
        SlowTestConsumer slowConsumer = ctx.getBean(SlowTestConsumer)
        PollingConditions until = new PollingConditions(timeout: 120)

        when:
        until.eventually {
            RabbitListenerException e = slowConsumer.lastException
            assert e != null && e.getCause() instanceof AlreadyClosedException
        }
        slowConsumer.doSlowdown = false;

        then:
        stopPublishAndAssertAllConsumed(slowConsumer)

        cleanup:
        ctx.close()
    }


    @Requires(property = "spec.name", value = "ConsumerRecoverySpec")
    @Requires(property = "connectToNode", value = "node1")
    @RabbitListener(connection = "node1")
    static class Node1Consumer extends TestConsumer {
    }

    @Requires(property = "spec.name", value = "ConsumerRecoverySpec")
    @Requires(property = "connectToNode", value = "node2")
    @RabbitListener(connection = "node2")
    static class Node2Consumer extends TestConsumer {
    }

    @Requires(property = "spec.name", value = "ConsumerRecoverySpec")
    @Requires(property = "connectToNode", value = "node3")
    @RabbitListener(connection = "node3")
    static class SlowTestConsumer extends TestConsumer {
        public boolean doSlowdown = true

        @Override
        @Queue(value = QUEUE, prefetch = 5)
        void handleMessage(@MessageBody String body) {
            super.handleMessage(body)

            if (doSlowdown && consumedMessages.size() % 10 == 0) {
                Thread.sleep(20000) // simulate slow processing
                log.info("slow message processing complete")
            }
        }
    }

    private void awaitPublishConsumeOfMessages(TestConsumer consumer) {
        PollingConditions until = new PollingConditions(timeout: 60)
        int targetPubCount = publishedMessages.size() + 10
        int targetConCount = consumer.consumedMessages.size() + 10

        until.eventually {
            assert publishedMessages.size() > targetPubCount
            assert consumer.consumedMessages.size() > targetConCount
        }
    }

    private void stopPublishAndAssertAllConsumed(TestConsumer consumer) {
        PollingConditions until = new PollingConditions(timeout: 60)
        enablePublisher = false

        until.eventually {
            assertThat "all published messages must be consumed",
                    publishedMessages, equalTo(consumer.consumedMessages)
        }
    }

    private static restartContainer(GenericContainer container) throws InterruptedException {
        PollingConditions until = new PollingConditions(timeout: 60)

        log.info("stopping container: {}", container.getContainerId())
        DOCKER_CLIENT.stopContainerCmd(container.getContainerId()).exec()
        log.info("re-starting container: {}", container.getContainerId())
        DOCKER_CLIENT.startContainerCmd(container.getContainerId()).exec()
        until.eventually {
            assert container.isHealthy()
        }
        log.info("started container: {}", container.getContainerId())
    }
}

abstract class TestConsumer implements RabbitListenerExceptionHandler {
    static final Logger log = LoggerFactory.getLogger(TestConsumer.class)
    public final Set<String> consumedMessages = new LinkedHashSet<>()
    public RabbitListenerException lastException

    @Queue(AbstractRabbitMQClusterTest.QUEUE)
    void handleMessage(@MessageBody String body) {
        consumedMessages.add(body)
        log.info("{} received: {}", consumedMessages.size(), body)
    }

    @Override
    void handle(RabbitListenerException e) {
        lastException = e
        String msg = e.getMessageState()
                .map(RabbitConsumerState::getBody)
                .map(String::new)
                .orElse("<<no message>>")
        consumedMessages.remove(msg)
        log.warn("{} failed to consume: {}", consumedMessages.size(), msg, e)
    }
}
