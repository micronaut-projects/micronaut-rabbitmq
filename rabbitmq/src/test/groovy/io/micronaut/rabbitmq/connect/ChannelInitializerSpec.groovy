package io.micronaut.rabbitmq.connect

import com.rabbitmq.client.Channel
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.exceptions.BeanInstantiationException
import io.micronaut.rabbitmq.connect.recovery.TemporarilyDownConnection
import io.micronaut.rabbitmq.connect.recovery.TemporarilyDownException
import spock.lang.Specification

class ChannelInitializerSpec extends Specification {

    void "resource locked initializer failure is not swallowed"() {
        given:
        Channel channel = Mock()
        ChannelPool pool = Mock() {
            getName() >> "default"
            getChannel() >> channel
        }
        BeanCreatedEvent<ChannelPool> event = Stub() {
            getBean() >> pool
        }
        ChannelInitializer initializer = new ChannelInitializer() {
            @Override
            void initialize(Channel ch, String name) throws IOException {
                throw new IOException("RESOURCE_LOCKED - cannot obtain exclusive access to locked queue")
            }
        }

        when:
        initializer.onCreated(event)

        then:
        BeanInstantiationException e = thrown()
        e.message == "Initialization of the channel has failed"
        e.cause instanceof IOException
        e.cause.message == "RESOURCE_LOCKED - cannot obtain exclusive access to locked queue"
        1 * pool.returnChannel(channel)
    }

    void "temporarily down initializer failure registers eventual up retry"() {
        given:
        Channel channel = Mock()
        TemporarilyDownConnection connection = Mock()
        ChannelPool pool = Mock() {
            getName() >> "default"
            getChannel() >> channel
        }
        BeanCreatedEvent<ChannelPool> event = Stub() {
            getBean() >> pool
        }
        ChannelInitializer initializer = new ChannelInitializer() {
            @Override
            void initialize(Channel ch, String name) throws IOException {
                throw new TemporarilyDownInitializationException(connection)
            }
        }

        when:
        ChannelPool created = initializer.onCreated(event)

        then:
        created.is(pool)
        1 * connection.addEventuallyUpListener(_)
        1 * pool.returnChannel(channel)
    }

    private static class TemporarilyDownInitializationException extends IOException implements TemporarilyDownException {

        private final TemporarilyDownConnection connection

        TemporarilyDownInitializationException(TemporarilyDownConnection connection) {
            super(ERROR_MESSAGE)
            this.connection = connection
        }

        @Override
        TemporarilyDownConnection getConnection() {
            connection
        }
    }
}
