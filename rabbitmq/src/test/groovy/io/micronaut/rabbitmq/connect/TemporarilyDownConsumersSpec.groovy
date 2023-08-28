package io.micronaut.rabbitmq.connect

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.core.io.socket.SocketUtils
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.rabbitmq.annotation.RabbitListener
import io.micronaut.rabbitmq.exception.RabbitListenerException
import io.micronaut.rabbitmq.exception.RabbitListenerExceptionHandler
import org.testcontainers.containers.FixedHostPortGenericContainer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class TemporarilyDownConsumersSpec extends Specification {

    static PollingConditions conditions = new PollingConditions(timeout: 10)

    void "test temporarily down consumer"() {
        int port = SocketUtils.findAvailableTcpPort()
        FixedHostPortGenericContainer rabbitmq = new FixedHostPortGenericContainer("library/rabbitmq:3.7")
                .withFixedExposedPort(port,5672)
                .waitingFor(new LogMessageWaitStrategy().withRegEx("(?s).*Server startup complete.*"))
        ApplicationContext applicationContext = ApplicationContext.run(["rabbitmq.port": port, "spec.name": "TemporarilyDownConsumersSpec"], "test")

        when: "consumer is instantiated when the server is down"
        MyConsumer consumer = applicationContext.getBean(MyConsumer)

        then: "consumer cannot subscribe to a queue"
        conditions.eventually {
            assert consumer.error?.message == "An error occurred subscribing to a queue"
        }

        when: "the server is eventually up"
        rabbitmq.start()

        and: "a message is published to the queue"
        applicationContext.getBean(MyProducer).send("hello")

        then: "the consumer will receive the published message"
        conditions.eventually {
            assert consumer.message == 'hello'
        }

        cleanup:
        rabbitmq.close()
        applicationContext?.close()
    }

    @Requires(property = "spec.name", value = "TemporarilyDownConsumersSpec")
    @RabbitClient
    static interface MyProducer {
        @Binding("eventually-up")
        void send(String message)
    }

    @Requires(property = "spec.name", value = "TemporarilyDownConsumersSpec")
    @RabbitListener
    static class MyConsumer implements RabbitListenerExceptionHandler {

        RabbitListenerException error
        String message

        @Queue("eventually-up")
        void receive(String message) {
            this.message = message
        }

        @Override
        void handle(RabbitListenerException error) {
            this.error = error
        }
    }
}
