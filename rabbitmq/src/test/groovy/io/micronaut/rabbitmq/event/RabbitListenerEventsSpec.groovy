package io.micronaut.rabbitmq.event

import io.micronaut.context.annotation.Requires
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener
import jakarta.inject.Singleton

class RabbitListenerEventsSpec extends AbstractRabbitMQTest {

    void "test rabbit listener events"() {
        given: "started application context"
        startContext()

        expect: "received all expected rabbit events"
        MyStartingEventListener starting = applicationContext.getBean(MyStartingEventListener)
        MyStartedEventListener started = applicationContext.getBean(MyStartedEventListener)
        starting.events.size() == 4
        started.events.size() == 3

        and: "received one event consumer1/starting"
        MyConsumer1 consumer1 = applicationContext.getBean(MyConsumer1)
        starting.events.grep { it.source == consumer1 }.size() == 1
        starting.events.any { it.source == consumer1 && it.method == 'consumeMessage' && it.queue == 'abc' }

        and: "received one event consumer2/starting"
        MyConsumer2 consumer2 = applicationContext.getBean(MyConsumer2)
        starting.events.grep { it.source == consumer2 }.size() == 1
        starting.events.any { it.source == consumer2 && it.method == 'receiveMessage' && it.queue == 'my-queue' }

        and: "received two events consumer3/starting, one per listener method"
        MyConsumer3 consumer3 = applicationContext.getBean(MyConsumer3)
        starting.events.grep { it.source == consumer3 }.size() == 2
        starting.events.any { it.source == consumer3 && it.method == 'method1' && it.queue == 'simple' }
        starting.events.any { it.source == consumer3 && it.method == 'method2' && it.queue == 'simple' }

        and: "received one event consumer1/started"
        started.events.grep { it.source == consumer1 }.size() == 1
        started.events.any { it.source == consumer1 && it.method == 'consumeMessage' && it.queue == 'abc' }

        and: "received no event consumer2/started, because 'my-queue' does not exist"
        started.events.grep { it.source == consumer2 }.size() == 0

        and: "received two events consumer3/started, one per listener method"
        started.events.grep { it.source == consumer3 }.size() == 2
        started.events.any { it.source == consumer3 && it.method == 'method1' && it.queue == 'simple' }
        started.events.any { it.source == consumer3 && it.method == 'method2' && it.queue == 'simple' }
    }

    static abstract class MyRabbitEventListener<T extends AbstractRabbitEvent> implements ApplicationEventListener<T> {
        List<T> events = []

        @Override
        void onApplicationEvent(T event) {
            events << event
        }
    }

    @Requires(property = "spec.name", value = "RabbitListenerEventsSpec")
    @Singleton
    static class MyStartingEventListener extends MyRabbitEventListener<RabbitConsumerStarting> {}

    @Requires(property = "spec.name", value = "RabbitListenerEventsSpec")
    @Singleton
    static class MyStartedEventListener extends MyRabbitEventListener<RabbitConsumerStarted> {}

    @Requires(property = "spec.name", value = "RabbitListenerEventsSpec")
    @RabbitListener
    static class MyConsumer1 {
        @Queue("abc")
        void consumeMessage(byte[] data) {}
    }

    @Requires(property = "spec.name", value = "RabbitListenerEventsSpec")
    @RabbitListener
    static class MyConsumer2 {
        @Queue("my-queue")
        void receiveMessage(byte[] data) {}
    }

    @Requires(property = "spec.name", value = "RabbitListenerEventsSpec")
    @RabbitListener
    static class MyConsumer3 {
        @Queue("simple")
        void method1(byte[] data) {}

        @Queue("simple")
        void method2(byte[] data) {}
    }
}
