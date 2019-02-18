package io.micronaut.configuration.rabbitmq.annotation

import io.micronaut.configuration.rabbitmq.AbstractRabbitMQTest
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.Header
import io.micronaut.messaging.exceptions.MessagingClientException
import io.reactivex.Completable
import spock.lang.Ignore
import spock.util.concurrent.PollingConditions

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@Ignore
class BooleanReturnTest extends AbstractRabbitMQTest {

    void "test simple producing and consuming with the ack/nack"() {
        ApplicationContext applicationContext = ApplicationContext.run(
                ["rabbitmq.port": rabbitContainer.getMappedPort(5672),
                 "spec.name": getClass().simpleName], "test")
        MyProducer producer = applicationContext.getBean(MyProducer)
        MyConsumer consumer = applicationContext.getBean(MyConsumer)
        PollingConditions conditions = new PollingConditions(timeout: 3)

        when:
        producer.go(new Person(name: "abc"), "false").blockingGet()
        producer.go(new Person(name: "abc"), "true").blockingGet()

        then:
        //2 for the 2 produces above, 1 for the requeued nack that is ack'd
        conditions.eventually {
            consumer.messages.get() == 3
        }

        cleanup:
        applicationContext.close()
    }

    static class Person {
        String name
    }

    @Requires(property = "spec.name", value = "BooleanReturnTest")
    @RabbitClient
    static interface MyProducer {

        @Binding("boolean")
        Completable go(Person data, @Header String ack)

    }

    @Requires(property = "spec.name", value = "BooleanReturnTest")
    @RabbitListener
    static class MyConsumer {

        public static AtomicInteger messages = new AtomicInteger()
        private AtomicBoolean nack = new AtomicBoolean(false)

        @Queue(value = "boolean", reQueue = true)
        Boolean listen(Person data, @Header String ack) {
            Boolean acknowledge = Boolean.valueOf(ack)
            //only nack and requeue once
            if (!acknowledge && !nack.compareAndSet(false, true)) {
                acknowledge = true
            }
            messages.incrementAndGet()
            return acknowledge
        }
    }
}

