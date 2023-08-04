package io.micronaut.rabbitmq.connect

import com.rabbitmq.client.Address
import com.rabbitmq.client.DefaultSocketConfigurator
import com.rabbitmq.client.impl.AMQConnection
import com.rabbitmq.client.impl.ConnectionParams
import com.rabbitmq.client.impl.SocketFrameHandlerFactory
import io.micronaut.core.io.socket.SocketUtils
import spock.lang.Specification

import javax.net.SocketFactory

class DefaultChannelPoolSpec extends Specification {

    void "test infinite loop when channels leak"() {
        given: "dummy server listening to random port"
        int port = SocketUtils.findAvailableTcpPort()
        Thread server = new Thread(() -> new ServerSocket(port).accept())
        server.start()

        when: "new default channel pool"
        AMQConnection connection = new AMQConnection(
                (ConnectionParams) [clientProperties: [:]],
                new SocketFrameHandlerFactory(60, SocketFactory.default, new DefaultSocketConfigurator(), false)
                        .create(new Address("localhost", port), "test"))
        DefaultChannelPool pool = new DefaultChannelPool("pool-name", connection, new SingleRabbitConnectionFactoryConfig())

        and: "try to obtain a channel from the pool"
        pool.getChannel()

        then: "IO exception is thrown (no infinite loop)"
        IOException e = thrown()
        e.message == "Failed to create a new channel"

        cleanup: "stop the dummy server"
        server.interrupt()
    }
}
