package io.micronaut.rabbitmq.bind

import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.MessageHeader
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.rabbitmq.annotation.RabbitHeaders
import io.micronaut.rabbitmq.annotation.RabbitListener

class RabbitMessageHeaderSpec extends AbstractRabbitMQTest {

    void "test publishing and consuming with  MessageHeaders and RabbitHeaders"() {
        startContext()

        AnimalProducer producer = applicationContext.getBean(AnimalProducer)
        AnimalListener consumer = applicationContext.getBean(AnimalListener)

        when:
        producer.goWithRabbitHeaders("test", [weight: "2lbs", color: "black"])

        then:
        waitFor {
            assert consumer.receivedBody == 'test'
            assert consumer.receivedWeight == '2lbs'
            assert consumer.receivedColor == 'black'
        }
    }

    @Requires(property = "spec.name", value = "RabbitMessageHeaderSpec")
    @RabbitClient
    static interface AnimalProducer {
        @Binding("header")
        void goWithRabbitHeaders(String body, @RabbitHeaders Map<String, Object> rabbitHeaders)
    }

    @Requires(property = "spec.name", value = "RabbitMessageHeaderSpec")
    @RabbitListener
    static class AnimalListener {

        String receivedBody
        String receivedWeight
        String receivedColor

        @Queue("header")
        void listen(String body, @MessageHeader String weight, @MessageHeader String color) {
            this.receivedBody = body
            this.receivedWeight = weight
            this.receivedColor = color
        }
    }
}
