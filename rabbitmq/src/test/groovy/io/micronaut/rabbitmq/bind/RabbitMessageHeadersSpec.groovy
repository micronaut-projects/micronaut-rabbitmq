package io.micronaut.rabbitmq.bind

import io.micronaut.context.annotation.Requires
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.rabbitmq.annotation.RabbitHeaders
import io.micronaut.rabbitmq.annotation.RabbitListener

class RabbitMessageHeadersSpec extends AbstractRabbitMQTest {

    void "test publishing and consuming with  MessageHeaders and RabbitHeaders"() {

        startContext()

        AnimalProducer producer = applicationContext.getBean(AnimalProducer)
        AnimalListener consumer = applicationContext.getBean(AnimalListener)

        when:
        producer.goWithRabbitHeaders("test", [weight: "2lbs", color: "black"])

        then:
        waitFor {
            consumer.headers == [weight: "2lbs", color: "black"]
        }
    }

    @Requires(property = "spec.name", value = "RabbitMessageHeadersSpec")
    @RabbitClient
    static interface AnimalProducer {
        @Binding("header")
        void goWithRabbitHeaders(String body, @RabbitHeaders Map<String, Object> rabbitHeaders)
    }

    @Requires(property = "spec.name", value = "RabbitMessageHeadersSpec")
    @RabbitListener
    static class AnimalListener {

        Map<String, Object> headers = [:]

        @Queue("header")
        void listen(@RabbitHeaders Map<String, Object> rabbitHeaders) {
            rabbitHeaders.each { k, v ->
                headers[k] = v.toString()
            }
        }
    }
}
