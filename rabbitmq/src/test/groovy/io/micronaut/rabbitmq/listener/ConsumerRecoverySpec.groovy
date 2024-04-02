package io.micronaut.rabbitmq.listener

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.AlreadyClosedException
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Return
import com.rabbitmq.client.ReturnCallback
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
import org.testcontainers.containers.GenericContainer
import spock.lang.Shared
import spock.util.concurrent.PollingConditions

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeoutException

import static java.util.concurrent.TimeUnit.MILLISECONDS
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo

class ConsumerRecoverySpec extends AbstractRabbitMQClusterTest {

    private static final Logger log = LoggerFactory.getLogger(ConsumerRecoverySpec)

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
        ConnectionFactory connectionFactory = new ConnectionFactory(port: node3Port)
        connectionFactory.newConnection().openChannel().map(ch -> {
                try {
                    ch.confirmSelect()
                    // returned messages must not count as published
                    ch.addReturnListener(new ReturnCallback() {
                        @Override
                        void handle(Return r) {
                            String returned = new String(r.body)
                            publishedMessages.remove(returned)
                            log.warn("{} publish message returned: {}", publishedMessages.size(), returned)
                        }
                    })
                } catch (IOException e) {
                    log.error("failed to set confirmSelect", e)
                }
                return ch
            })
            .ifPresent(ch -> executorService.scheduleWithFixedDelay(() -> {
                if (!enablePublisher) {
                    return
                }
                String msg = UUID.randomUUID()
                try {
                    publishedMessages << msg
                    ch.basicPublish(EXCHANGE, "", true,
                            new AMQP.BasicProperties.Builder().deliveryMode(2).build(),
                            msg.bytes)
                    if (ch.waitForConfirms(1000)) {
                        log.info("publish ack")
                    }
                } catch (IOException | RuntimeException | InterruptedException | TimeoutException e) {
                    publishedMessages.remove(msg)
                    log.error("failed to publish: {}", e.message)
                }
            }, 500, 500, MILLISECONDS))
    }

    void cleanupSpec() {
        executorService.shutdown()
    }

    void setup() {
        publishedMessages.clear()
        enablePublisher = true
    }

    void "test restart of Node1 with consumer connected to Node1"() {
        given:
        startContext(connectToNode: "node1")
        TestConsumer consumer = applicationContext.getBean(TestConsumer)
        awaitPublishConsumeOfMessages(consumer)

        when:
        restartContainer(node1)

        then:
        awaitPublishConsumeOfMessages(consumer)
        stopPublishAndAssertAllConsumed(consumer)
    }

    void "test restart of Node1 with consumer connected to Node2"() {
        given:
        startContext(connectToNode: "node2")
        TestConsumer consumer = applicationContext.getBean(TestConsumer)
        awaitPublishConsumeOfMessages(consumer)

        when:
        restartContainer(node1)

        then:
        awaitPublishConsumeOfMessages(consumer)
        stopPublishAndAssertAllConsumed(consumer)
    }

    void "test restart of Node2 with consumer connected to Node1"() {
        given:
        startContext(connectToNode: "node1")
        TestConsumer consumer = applicationContext.getBean(TestConsumer)
        awaitPublishConsumeOfMessages(consumer)

        when:
        restartContainer(node2)

        then:
        awaitPublishConsumeOfMessages(consumer)
        stopPublishAndAssertAllConsumed(consumer)
    }

    void "test restart of Node2 with consumer connected to Node2"() {
        given:
        startContext(connectToNode: "node2")
        TestConsumer consumer = applicationContext.getBean(TestConsumer)
        awaitPublishConsumeOfMessages(consumer)

        when:
        restartContainer(node2)

        then:
        awaitPublishConsumeOfMessages(consumer)
        stopPublishAndAssertAllConsumed(consumer)
    }

    void "test consumer recovery after delivery acknowledgement timeout"() {
        given:
        startContext(connectToNode: "node3")
        SlowTestConsumer slowConsumer = applicationContext.getBean(SlowTestConsumer)

        when:
        new PollingConditions(timeout: 120).eventually {
            RabbitListenerException e = slowConsumer.lastException
            e && e.cause instanceof AlreadyClosedException
        }
        slowConsumer.doSlowdown = false

        then:
        stopPublishAndAssertAllConsumed(slowConsumer)
    }

    @Requires(property = "spec.name", value = "ConsumerRecoverySpec")
    @Requires(property = "connectToNode", value = "node1")
    @RabbitListener(connection = "node1")
    static class Node1Consumer extends TestConsumer {}

    @Requires(property = "spec.name", value = "ConsumerRecoverySpec")
    @Requires(property = "connectToNode", value = "node2")
    @RabbitListener(connection = "node2")
    static class Node2Consumer extends TestConsumer {}

    @Requires(property = "spec.name", value = "ConsumerRecoverySpec")
    @Requires(property = "connectToNode", value = "node3")
    @RabbitListener(connection = "node3")
    static class SlowTestConsumer extends TestConsumer {

        boolean doSlowdown = true

        @Override
        @Queue(value = QUEUE, prefetch = 5)
        void handleMessage(@MessageBody String body) {
            super.handleMessage(body)

            if (doSlowdown && consumedMessages.size() % 10 == 0) {
                sleep 20_000 // simulate slow processing
                log.info("slow message processing complete")
            }
        }
    }

    private void awaitPublishConsumeOfMessages(TestConsumer consumer) {
        int targetPubCount = publishedMessages.size() + 10
        int targetConCount = consumer.consumedMessages.size() + 10

        new PollingConditions(timeout: 60).eventually {
            assert publishedMessages.size() > targetPubCount
            assert consumer.consumedMessages.size() > targetConCount
        }
    }

    private void stopPublishAndAssertAllConsumed(TestConsumer consumer) {
        enablePublisher = false

        new PollingConditions(timeout: 60).eventually {
            assertThat "all published messages must be consumed",
                    publishedMessages, equalTo(consumer.consumedMessages)
        }
    }

    private static restartContainer(GenericContainer container) throws InterruptedException {
        log.info("stopping container: {}", container.containerId)
        container.execInContainer("rabbitmqctl", "stop_app")
        container.execInContainer("rabbitmqctl", "start_app")

        new PollingConditions(timeout: 60).eventually {
            container.isHealthy()
        }
        log.info("started container: {}", container.containerId)
    }

    static abstract class TestConsumer implements RabbitListenerExceptionHandler {

        static final Logger log = LoggerFactory.getLogger(TestConsumer)

        final Set<String> consumedMessages = new LinkedHashSet<>()

        RabbitListenerException lastException

        @Queue(AbstractRabbitMQClusterTest.QUEUE)
        void handleMessage(@MessageBody String body) {
            consumedMessages << body
            log.info("{} received: {}", consumedMessages.size(), body)
        }

        @Override
        void handle(RabbitListenerException e) {
            lastException = e
            String msg = e.messageState
                    .map(RabbitConsumerState::getBody)
                    .map(String::new)
                    .orElse("<<no message>>")
            consumedMessages.remove(msg)
            log.warn("{} failed to consume: {}", consumedMessages.size(), msg, e)
        }
    }
}
