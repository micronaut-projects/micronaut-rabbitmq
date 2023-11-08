package io.micronaut.rabbitmq.listener

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Consumer
import com.rabbitmq.client.ExceptionHandler
import com.rabbitmq.client.TopologyRecoveryException
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Requires
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import io.micronaut.messaging.annotation.MessageBody
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener
import spock.lang.Issue

class ShutdownExecutorsSpec extends AbstractRabbitMQTest {

    void "server can shut down cleanly without exceptions thrown when the default executor is used"() {
        given:
        startContext(['rabbitmq.automatic-recovery-enabled':false])

        when:
        MyDefaultConsumer consumer = applicationContext.getBean(MyDefaultConsumer.class)
        ConnectionFactoryInterceptor interceptor = applicationContext.getBean(ConnectionFactoryInterceptor.class)

        then:
        consumer
        interceptor

        when:
        applicationContext.stop()

        then:
        waitFor {
            assert !applicationContext.isRunning()
        }
        !interceptor.exceptionThrown
    }

    @Issue('https://github.com/micronaut-projects/micronaut-rabbitmq/issues/524')
    void "server can shut down cleanly without exceptions thrown when custom executor is used"() {
        given:
        startContext(['spec.configuration':'custom',
                      'micronaut.executors.test.type':'fixed',
                      'micronaut.executors.test.number-of-threads':15,
                      'rabbitmq.automatic-recovery-enabled':false])

        when:
        MyConsumer consumer = applicationContext.getBean(MyConsumer.class)
        ConnectionFactoryInterceptor interceptor = applicationContext.getBean(ConnectionFactoryInterceptor.class)

        then:
        consumer
        interceptor

        when:
        applicationContext.stop()

        then:
        waitFor {
            assert !applicationContext.isRunning()
        }
        !interceptor.exceptionThrown
    }

    @Requires(property = "spec.name", value = "ShutdownExecutorsSpec")
    @RabbitListener
    static class MyDefaultConsumer {

        @Queue(value = "shutdown-default")
        void listenOne(@MessageBody String body) {
            sleep 500
        }
    }

    @Requires(property = "spec.name", value = "ShutdownExecutorsSpec")
    @Requires(property = "spec.configuration", value = "custom")
    @RabbitListener
    static class MyConsumer {

        @Queue(value = "shutdown-custom", executor = "test")
        void listenOne(@MessageBody String body) {
            sleep 500
        }
    }
}

@Requires(property = "spec.name", value = "ShutdownExecutorsSpec")
@Context
class ConnectionFactoryInterceptor implements BeanCreatedEventListener<ConnectionFactory> {

    boolean exceptionThrown = false

    @Override
    ConnectionFactory onCreated(BeanCreatedEvent<ConnectionFactory> event) {
        ConnectionFactory connectionFactory = event.getBean();
        connectionFactory.setExceptionHandler(new ShutdownFailureExceptionHandler())
        return connectionFactory;
    }

    class ShutdownFailureExceptionHandler implements ExceptionHandler {
        @Override
        void handleUnexpectedConnectionDriverException(Connection conn, Throwable exception) {
            exceptionThrown = true
        }

        @Override
        void handleReturnListenerException(Channel channel, Throwable exception) {
            exceptionThrown = true
        }

        @Override
        void handleConfirmListenerException(Channel channel, Throwable exception) {
            exceptionThrown = true
        }

        @Override
        void handleBlockedListenerException(Connection connection, Throwable exception) {
            exceptionThrown = true
        }

        @Override
        void handleConsumerException(Channel channel, Throwable exception, Consumer consumer, String consumerTag, String methodName) {
            exceptionThrown = true
        }

        @Override
        void handleConnectionRecoveryException(Connection conn, Throwable exception) {
            exceptionThrown = true
        }

        @Override
        void handleChannelRecoveryException(Channel ch, Throwable exception) {
            exceptionThrown = true
        }

        @Override
        void handleTopologyRecoveryException(Connection conn, Channel ch, TopologyRecoveryException exception) {
            exceptionThrown = true
        }
    }
}
