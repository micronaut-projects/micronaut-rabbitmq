package io.micronaut.rabbitmq.bind

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.rabbitmq.annotation.RabbitHeaders
import io.micronaut.rabbitmq.annotation.RabbitListener
import spock.util.concurrent.PollingConditions

class RabbitMessageHeadersSpec extends AbstractRabbitMQTest {

    void "test publishing and consuming with  MessageHeaders and RabbitHeaders"() {

        ApplicationContext applicationContext = ApplicationContext.run(
                ["rabbitmq.port": rabbitContainer.getMappedPort(5672),
                 "spec.name": getClass().simpleName], "test")
        PollingConditions conditions = new PollingConditions(timeout: 5, initialDelay: 1)
        AnimalProducer producer = applicationContext.getBean(AnimalProducer)
        AnimalListener consumer = applicationContext.getBean(AnimalListener)

        when:
        producer.goWithRabbitHeaders("test", ["weight": "2lbs", "color": "black"])

        then:
        conditions.eventually {
            consumer.headers == ["weight": "2lbs", "color": "black"]
        }

        cleanup:
        applicationContext.close()
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

        public Map<String, Object> headers = [:]

        @Queue("header")
        void listen(@RabbitHeaders Map<String, Object> rabbitHeaders) {
            for (Map.Entry<String, Object> entry: rabbitHeaders) {
                headers.put(entry.getKey(), entry.getValue().toString())
            }
        }

    }
}
