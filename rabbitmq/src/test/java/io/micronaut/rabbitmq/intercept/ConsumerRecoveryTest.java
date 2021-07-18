package io.micronaut.rabbitmq.intercept;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.github.dockerjava.api.DockerClient;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.ConnectionFactory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.messaging.annotation.MessageBody;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import io.micronaut.rabbitmq.exception.RabbitListenerException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(rebuildContext = true, propertySources = "classpath:rabbit/recovery-test.yaml")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Property(name = "node-name", value = EnvironmentLoader.NODE1)
public class ConsumerRecoveryTest {
    private static final Logger log = LoggerFactory.getLogger(ConsumerRecoveryTest.class);
    private static final Set<String> publishedMessages = new LinkedHashSet<>();
    private static final DockerClient DOCKER_CLIENT = DockerClientFactory.lazyClient();
    private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private static boolean enablePublisher = false;
    @Inject
    TestConsumer consumer;
    @Property(name = EnvironmentLoader.NODE1_CONT)
    GenericContainer<?> rabbit1;
    @Property(name = EnvironmentLoader.NODE2_CONT)
    GenericContainer<?> rabbit2;

    @BeforeAll
    static void setupPublisher() throws Exception {
        /*
         * The current Micronaut publisher implementation has a flaw in detecting unroutable drop/return messages
         * in a Rabbit cluster setup. It considers the messages as published even if the broker did not enqueue it.
         * So for this test a simple custom publisher is used that detects unpublished messages.
         */
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setPort(EnvironmentLoader.NODE3_PORT);
        connectionFactory.newConnection().openChannel()
                .map(ch -> {
                    try {
                        ch.confirmSelect();
                        // returned messages must not count as published
                        ch.addReturnListener((r) -> {
                            String returned = new String(r.getBody());
                            publishedMessages.remove(returned);
                            log.warn("{} publish message returned: {}", publishedMessages.size(), returned);
                        });
                    } catch (IOException e) {
                        log.error("failed to set confirmSelect", e);
                    }
                    return ch;
                })
                .ifPresent(
                        ch -> executorService.scheduleWithFixedDelay(() -> {
                            if (!enablePublisher) {
                                return;
                            }
                            String msg = UUID.randomUUID().toString();
                            try {
                                publishedMessages.add(msg);
                                ch.basicPublish(EnvironmentLoader.EXCHANGE, "", true,
                                        new AMQP.BasicProperties.Builder().deliveryMode(2).build(),
                                        msg.getBytes());
                                if (ch.waitForConfirms(1000)) {
                                    log.info("publish ack");
                                }
                            } catch (IOException | RuntimeException | InterruptedException | TimeoutException e) {
                                publishedMessages.remove(msg);
                                log.error("failed to publish: {}", e.getMessage());
                            }
                        }, 500, 500, TimeUnit.MILLISECONDS));
    }

    @AfterAll
    static void shutdownPublisher() {
        executorService.shutdown();
    }

    @BeforeEach
    void prepareTest() {
        consumer.consumedMessages.clear();
        publishedMessages.clear();
        enablePublisher = true;
    }

    @Test
    @Order(1)
    public void testConsumerOnNode1AndRestartNode1() throws InterruptedException {
        awaitPublishConsumeOfMessages();

        restartContainer(rabbit1);

        awaitPublishConsumeOfMessages();
        stopPublishAndAssertAllConsumed();
    }

    @Test
    @Order(2)
    public void testConsumerOnNode1AndRestartNode2() throws InterruptedException {
        awaitPublishConsumeOfMessages();

        restartContainer(rabbit2);

        awaitPublishConsumeOfMessages();
        stopPublishAndAssertAllConsumed();
    }

    /**
     * This situation is not covered by the Rabbit client automatic recovery, as the connection is never closed.
     * Just the consumer gets canceled because the queue is temporary unavailable. Recovery attempts will fail
     * with {@code reply-code=404, reply-text=NOT_FOUND} until the node 1 container is restarted.
     */
    @Test
    @Order(3)
    @Property(name = "node-name", value = EnvironmentLoader.NODE2)
    public void testConsumerOnNode2AndRestartNode1() throws InterruptedException {
        awaitPublishConsumeOfMessages();

        restartContainer(rabbit1);

        awaitPublishConsumeOfMessages();
        stopPublishAndAssertAllConsumed();
    }

    @Test
    @Order(4)
    @Property(name = "node-name", value = EnvironmentLoader.NODE2)
    public void testConsumerOnNode2AndRestartNode2() throws InterruptedException {
        awaitPublishConsumeOfMessages();

        restartContainer(rabbit2);

        awaitPublishConsumeOfMessages();
        stopPublishAndAssertAllConsumed();
    }

    /**
     * This test provokes a forced channel close by the broker. The "consumer_timeout" is set to 5000 milliseconds,
     * but it takes more time (about a minute) for the broker to actually close the channel.
     * <p>
     * The max wait durations are set to such high values to ensure that a) the timeout really triggers and
     * b) that the consumer has time to recover and process all messages.
     */
    @Test
    @Order(5)
    @Property(name = "node-name", value = EnvironmentLoader.NODE3)
    void testConsumerRecoveryAfterDeliveryAcknowledgementTimeout() throws Exception {
        SlowTestConsumer slowConsumer = (SlowTestConsumer) consumer;

        Awaitility.await("consumer timeout exception")
                .atMost(Duration.ofMinutes(2)).pollInterval(Duration.ofSeconds(1))
                .until(()-> {
                    RabbitListenerException e = slowConsumer.lastException;
                    return e != null && e.getCause() instanceof AlreadyClosedException;
                });

        slowConsumer.doSlowdown = false;
        stopPublishAndAssertAllConsumed();
    }

    private void awaitPublishConsumeOfMessages() {
        int targetPubCount = publishedMessages.size() + 10;
        int targetConCount = consumer.consumedMessages.size() + 10;
        Awaitility.await("some messages to be published and consumed")
                .atMost(Duration.ofSeconds(30))
                .until(() -> {
                    return publishedMessages.size() > targetPubCount && consumer.consumedMessages.size() > targetConCount;
                });
    }

    private void stopPublishAndAssertAllConsumed() {
        enablePublisher = false;
        Awaitility.await("all published messages are consumed")
                .atMost(Duration.ofMinutes(1))
                .untilAsserted(() -> assertEquals(publishedMessages, consumer.consumedMessages,
                        "not all published messages are consumed"));
    }

    private void restartContainer(GenericContainer<?> cont) throws InterruptedException {
        log.info("stopping container: {}", cont.getContainerId());
        DOCKER_CLIENT.stopContainerCmd(cont.getContainerId()).exec();
        log.info("re-starting container: {}", cont.getContainerId());
        DOCKER_CLIENT.startContainerCmd(cont.getContainerId()).exec();
        Awaitility.await("other cluster nodes start")
                .atMost(Duration.ofMinutes(1)).pollInterval(Duration.ofSeconds(1))
                .until(() -> cont.isHealthy());
        log.info("started container: {}", cont.getContainerId());
    }
}

@RabbitListener(connection = EnvironmentLoader.NODE1)
@Requires(property = "node-name", value = EnvironmentLoader.NODE1)
class Node1Consumer extends TestConsumer {
}

@RabbitListener(connection = EnvironmentLoader.NODE2)
@Requires(property = "node-name", value = EnvironmentLoader.NODE2)
class Node2Consumer extends TestConsumer {
}

@RabbitListener(connection = EnvironmentLoader.NODE3)
@Requires(property = "node-name", value = EnvironmentLoader.NODE3)
class SlowTestConsumer extends TestConsumer {
    private static final Logger log = LoggerFactory.getLogger(SlowTestConsumer.class);

    public boolean doSlowdown = true;

    @Override
    @Queue(value = EnvironmentLoader.QUEUE, prefetch = 5)
    public void handleMessage(@MessageBody String body) {
        super.handleMessage(body);

        if (doSlowdown && consumedMessages.size() % 10 == 0) {
            try {
                Thread.sleep(20000); // simulate slow processing
                log.info("slow message processing complete");
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }
}
