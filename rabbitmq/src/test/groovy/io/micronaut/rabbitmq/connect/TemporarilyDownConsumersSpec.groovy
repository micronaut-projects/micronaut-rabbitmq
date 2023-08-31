package io.micronaut.rabbitmq.connect

import com.rabbitmq.client.BlockedListener
import com.rabbitmq.client.ShutdownListener
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.core.io.socket.SocketUtils
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.rabbitmq.annotation.RabbitListener
import io.micronaut.rabbitmq.connect.recovery.TemporarilyDownException
import io.micronaut.rabbitmq.connect.recovery.TemporarilyDownIOException
import io.micronaut.rabbitmq.connect.recovery.TemporarilyDownRuntimeException
import io.micronaut.rabbitmq.exception.RabbitClientException
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

    void "test temporarily down producer"() {
        int port = SocketUtils.findAvailableTcpPort()
        FixedHostPortGenericContainer rabbitmq = new FixedHostPortGenericContainer("library/rabbitmq:3.7")
                .withFixedExposedPort(port,5672)
                .waitingFor(new LogMessageWaitStrategy().withRegEx("(?s).*Server startup complete.*"))
        ApplicationContext applicationContext = ApplicationContext.run(["rabbitmq.port": port, "spec.name": "TemporarilyDownConsumersSpec"], "test")

        when: "producer publishes a message when the server is down"
        applicationContext.getBean(MyProducer).send("hello")

        then: "a temporarily down exception is thrown"
        var exception = thrown(RabbitClientException)
        exception.message == 'Failed to publish a message with exchange: [] routing key [eventually-up] and mandatory flag [false]'
        exception.cause.message == 'Failed to retrieve a channel from the pool'
        exception.cause.cause.message == 'Connection is not ready yet'

        when: "the temporarily down connection is retrieved from the exception"
        var connection = ((TemporarilyDownException) exception.cause.cause).connection
        then: "the connection is still down"
        connection.stillDown

        when: "check if the connection is up now"
        connection.check()
        then:
        noExceptionThrown()

        when: "retrieve connection address"
        connection.address
        then:
        exception = thrown(TemporarilyDownRuntimeException)
        exception.connection == connection

        when: "retrieve connection port"
        connection.port
        then:
        thrown(TemporarilyDownRuntimeException)

        when: "retrieve connection max channel"
        connection.channelMax
        then:
        thrown(TemporarilyDownRuntimeException)

        when: "retrieve connection max frame"
        connection.frameMax
        then:
        thrown(TemporarilyDownRuntimeException)

        when: "retrieve connection heartbeat"
        connection.heartbeat
        then:
        thrown(TemporarilyDownRuntimeException)

        when: "retrieve connection client properties"
        connection.clientProperties
        then:
        thrown(TemporarilyDownRuntimeException)

        when: "retrieve connection client provided name"
        connection.clientProvidedName
        then:
        thrown(TemporarilyDownRuntimeException)

        when: "retrieve connection server properties"
        connection.serverProperties
        then:
        thrown(TemporarilyDownRuntimeException)

        when: "create channel"
        connection.createChannel()
        then:
        thrown(TemporarilyDownIOException)

        when: "create channel with number"
        connection.createChannel(1)
        then:
        thrown(TemporarilyDownIOException)

        when: "close connection"
        connection.close()
        then:
        thrown(TemporarilyDownIOException)

        when: "close connection with code and message"
        connection.close(-1, "message")
        then:
        thrown(TemporarilyDownIOException)

        when: "close connection with timeout"
        connection.close(10)
        then:
        thrown(TemporarilyDownIOException)

        when: "close connection with code, message and timeout"
        connection.close(-1, "message", 10)
        then:
        thrown(TemporarilyDownIOException)

        when: "abort connection"
        connection.abort()
        then:
        noExceptionThrown()

        when: "abort connection with code and message"
        connection.abort(-1, "message")
        then:
        noExceptionThrown()

        when: "abort connection with timeout"
        connection.abort(10)
        then:
        noExceptionThrown()

        when: "abort connection with code, message and timeout"
        connection.abort(-1, "message", 10)
        then:
        noExceptionThrown()

        when: "add blocked listener"
        connection.addBlockedListener(null)
        then:
        thrown(TemporarilyDownRuntimeException)

        when: "add blocked listener callbacks"
        connection.addBlockedListener(null, null)
        then:
        thrown(TemporarilyDownRuntimeException)

        when: "remove blocked listener"
        connection.removeBlockedListener(null)
        then:
        thrown(TemporarilyDownRuntimeException)

        when: "clear blocked listener"
        connection.clearBlockedListeners()
        then:
        thrown(TemporarilyDownRuntimeException)

        when: "retrieve connection exception handler"
        connection.exceptionHandler
        then:
        thrown(TemporarilyDownRuntimeException)

        when: "retrieve connection id"
        connection.id
        then:
        thrown(TemporarilyDownRuntimeException)

        when: "set connection id"
        connection.id = null
        then:
        thrown(TemporarilyDownRuntimeException)

        when: "add shutdown listener"
        connection.addShutdownListener(null)
        then:
        thrown(TemporarilyDownRuntimeException)

        when: "remove shutdown listener"
        connection.removeShutdownListener(null)
        then:
        thrown(TemporarilyDownRuntimeException)

        when: "retrieve connection close reason"
        connection.closeReason
        then:
        thrown(TemporarilyDownRuntimeException)

        when: "notify listeners"
        connection.notifyListeners()
        then:
        thrown(TemporarilyDownRuntimeException)

        when: "check if connection is open"
        connection.open
        then:
        thrown(TemporarilyDownRuntimeException)

        when: "the server is eventually up"
        boolean wasNotified = false
        connection.addEventuallyUpListener {throw new RuntimeException('testing') }
        connection.addEventuallyUpListener {wasNotified = true }
        rabbitmq.start()
        then: "details can be retrieved from connection"
        connection.check()
        wasNotified
        connection.stillDown == false
        connection.address.hostName == 'localhost'
        connection.port == port
        connection.channelMax == 2047
        connection.frameMax == 131072
        connection.heartbeat == 60
        connection.clientProperties.product.toString() == 'RabbitMQ'
        connection.clientProvidedName == null
        connection.serverProperties.product.toString() == 'RabbitMQ'
        connection.exceptionHandler != null
        connection.id != null
        connection.closeReason == null
        connection.open

        when: "set connection id"
        connection.id = 'test'
        then: "retrieved connection id is as expected"
        connection.id == 'test'

        when: "listeners are added and removed"
        BlockedListener blockedListener = new BlockedListener() {
            @Override void handleBlocked(String reason) { }
            @Override void handleUnblocked() throws IOException { }
        }
        connection.addBlockedListener(blockedListener)
        connection.removeBlockedListener(blockedListener)
        connection.addBlockedListener(r -> {}, () -> {})
        connection.clearBlockedListeners()
        ShutdownListener shutdownListener = x -> {}
        connection.addShutdownListener(shutdownListener)
        connection.removeShutdownListener(shutdownListener)
        connection.notifyListeners()
        then:
        noExceptionThrown()

        when: "close connection"
        connection.close(-1, 'testing')
        then: "we can retrieve details"
        connection.open == false
        connection.closeReason.reason.replyText == 'testing'

        when: "abort connection"
        connection.abort()
        connection.abort(-1, "message")
        connection.abort(10)
        connection.abort(-1, "message", 10)
        then:
        noExceptionThrown()

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
