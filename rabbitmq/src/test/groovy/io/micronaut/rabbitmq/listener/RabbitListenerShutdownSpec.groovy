package io.micronaut.rabbitmq.listener

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import io.micronaut.context.annotation.Requires
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.context.event.ShutdownEvent
import io.micronaut.core.order.Ordered
import io.micronaut.messaging.annotation.MessageBody
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener
import io.micronaut.rabbitmq.intercept.RabbitMQConsumerAdvice
import jakarta.annotation.PreDestroy
import jakarta.inject.Singleton

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch

import static java.util.concurrent.TimeUnit.SECONDS

class RabbitListenerShutdownSpec extends AbstractRabbitMQTest {

    static final String QUEUE_A = "shutdown-order-a"
    static final String QUEUE_B = "shutdown-order-b"
    static final List<String> HANDLED_WITH_OPEN_RESOURCE = ["$QUEUE_A: resource closed=false".toString(),
                                                            "$QUEUE_B: resource closed=false".toString()]

    private Connection connection
    private Channel channel

    void setup() {
        connection = new ConnectionFactory(host: rabbitContainer.host, port: rabbitContainer.getMappedPort(5672)).newConnection()
        channel = connection.createChannel()
        [QUEUE_A, QUEUE_B].each {
            channel.queueDeclare(it, false, false, false, [:])
            channel.queuePurge(it)
        }
    }

    void cleanup() {
        connection?.close()
    }

    void "consumers are cancelled at once and in-flight deliveries finish before any bean is destroyed"() {
        given:
        startContext()
        SlowListener listener = startDeliveries()
        ListenerResource resource = applicationContext.getBean(ListenerResource)
        BeforeDestructionProbe beforeDestruction = applicationContext.getBean(BeforeDestructionProbe)

        when:
        Thread stopping = Thread.start { applicationContext.stop() }

        then: 'every consumer is cancelled while both deliveries are still being handled'
        waitFor {
            assert channel.consumerCount(QUEUE_A) == 0
            assert channel.consumerCount(QUEUE_B) == 0
        }
        beforeDestruction.reached.count == 1
        !resource.closed

        when:
        listener.release.countDown()
        stopping.join(20_000)

        then: 'the deliveries completed before singleton destruction started and were acknowledged'
        !stopping.alive
        beforeDestruction.handledWhenReached == 2
        listener.handled as Set == HANDLED_WITH_OPEN_RESOURCE as Set
        resource.closed
        channel.messageCount(QUEUE_A) == 0
        channel.messageCount(QUEUE_B) == 0
    }

    void "graceful shutdown cancels all consumers at once and drains in-flight deliveries within the grace period"() {
        given:
        startContext(['micronaut.lifecycle.graceful-shutdown.enabled': true,
                      'micronaut.lifecycle.graceful-shutdown.grace-period': '15s'])
        SlowListener listener = startDeliveries()
        ListenerResource resource = applicationContext.getBean(ListenerResource)
        RabbitMQConsumerAdvice advice = applicationContext.getBean(RabbitMQConsumerAdvice)
        AfterGracefulShutdownProbe afterGracefulShutdown = applicationContext.getBean(AfterGracefulShutdownProbe)

        when:
        Thread stopping = Thread.start { applicationContext.stop() }

        then: 'every consumer is cancelled while graceful shutdown waits for both deliveries'
        waitFor {
            assert channel.consumerCount(QUEUE_A) == 0
            assert channel.consumerCount(QUEUE_B) == 0
        }
        advice.reportActiveTasks() == OptionalLong.of(2)
        afterGracefulShutdown.reached.count == 1
        !resource.closed

        when:
        listener.release.countDown()
        stopping.join(20_000)

        then: 'graceful shutdown returned only after both deliveries completed and were acknowledged'
        !stopping.alive
        afterGracefulShutdown.handledWhenReached == 2
        listener.handled as Set == HANDLED_WITH_OPEN_RESOURCE as Set
        resource.closed
        channel.messageCount(QUEUE_A) == 0
        channel.messageCount(QUEUE_B) == 0
    }

    void "deliveries still in flight when the grace period expires finish before any bean is destroyed"() {
        given:
        startContext(['micronaut.lifecycle.graceful-shutdown.enabled': true,
                      'micronaut.lifecycle.graceful-shutdown.grace-period': '1s'])
        SlowListener listener = startDeliveries()
        ListenerResource resource = applicationContext.getBean(ListenerResource)
        AfterGracefulShutdownProbe afterGracefulShutdown = applicationContext.getBean(AfterGracefulShutdownProbe)
        BeforeDestructionProbe beforeDestruction = applicationContext.getBean(BeforeDestructionProbe)

        when:
        Thread stopping = Thread.start { applicationContext.stop() }

        then: 'the grace period expired with both deliveries in flight'
        afterGracefulShutdown.reached.await(10, SECONDS)
        afterGracefulShutdown.handledWhenReached == 0

        when:
        listener.release.countDown()
        stopping.join(20_000)

        then: 'the remaining deliveries were still awaited before singleton destruction started'
        !stopping.alive
        beforeDestruction.handledWhenReached == 2
        listener.handled as Set == HANDLED_WITH_OPEN_RESOURCE as Set
        resource.closed
        channel.messageCount(QUEUE_A) == 0
        channel.messageCount(QUEUE_B) == 0
    }

    private SlowListener startDeliveries() {
        SlowListener listener = applicationContext.getBean(SlowListener)
        [QUEUE_A, QUEUE_B].each { channel.basicPublish("", it, null, "body".bytes) }
        assert listener.started.await(10, SECONDS)
        listener
    }

    /**
     * A resource the listener uses but the advice does not depend on, like a DataSource.
     */
    @Requires(property = "spec.name", value = "RabbitListenerShutdownSpec")
    @Singleton
    static class ListenerResource {

        volatile boolean closed

        @PreDestroy
        void close() {
            closed = true
        }
    }

    /**
     * Records how many deliveries had completed when the {@link ShutdownEvent} reached it.
     */
    static abstract class ShutdownProbe implements ApplicationEventListener<ShutdownEvent>, Ordered {

        final CountDownLatch reached = new CountDownLatch(1)
        volatile int handledWhenReached = -1
        private final SlowListener listener

        ShutdownProbe(SlowListener listener) {
            this.listener = listener
        }

        @Override
        void onApplicationEvent(ShutdownEvent event) {
            handledWhenReached = listener.handled.size()
            reached.countDown()
        }
    }

    /**
     * Runs right after graceful shutdown.
     */
    @Requires(property = "spec.name", value = "RabbitListenerShutdownSpec")
    @Singleton
    static class AfterGracefulShutdownProbe extends ShutdownProbe {

        AfterGracefulShutdownProbe(SlowListener listener) {
            super(listener)
        }

        @Override
        int getOrder() {
            HIGHEST_PRECEDENCE + 1
        }
    }

    /**
     * Runs after every other shutdown listener, right before singletons are destroyed.
     */
    @Requires(property = "spec.name", value = "RabbitListenerShutdownSpec")
    @Singleton
    static class BeforeDestructionProbe extends ShutdownProbe {

        BeforeDestructionProbe(SlowListener listener) {
            super(listener)
        }

        @Override
        int getOrder() {
            LOWEST_PRECEDENCE
        }
    }

    @Requires(property = "spec.name", value = "RabbitListenerShutdownSpec")
    @RabbitListener
    static class SlowListener {

        final CountDownLatch started = new CountDownLatch(2)
        final CountDownLatch release = new CountDownLatch(1)
        final List<String> handled = new CopyOnWriteArrayList<>()
        private final ListenerResource resource

        SlowListener(ListenerResource resource) {
            this.resource = resource
        }

        @Queue(QUEUE_A)
        void onA(@MessageBody String body) {
            handle(QUEUE_A)
        }

        @Queue(QUEUE_B)
        void onB(@MessageBody String body) {
            handle(QUEUE_B)
        }

        private void handle(String queue) {
            started.countDown()
            release.await(15, SECONDS)
            handled << "$queue: resource closed=${resource.closed}".toString()
        }
    }
}
