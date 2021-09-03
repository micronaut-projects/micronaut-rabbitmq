package io.micronaut.rabbitmq.connect

import com.rabbitmq.client.ConnectionFactory
import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import spock.lang.Specification

class RabbitConfigurationSpec extends Specification {

    void "default rabbit configuration"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test").start()

        expect: "connection factory bean is available"
        applicationContext.containsBean(ConnectionFactory)

        when: "when the connection factory is returned"
        ConnectionFactory cf = applicationContext.getBean(ConnectionFactory)

        then: "default configuration is available"
        cf.username == "guest"
        cf.password == "guest"
        cf.virtualHost == "/"
        cf.host == "localhost"
        cf.port == 5672
        cf.requestedChannelMax == 2047
        cf.requestedFrameMax == 0
        cf.requestedHeartbeat == 60
        cf.connectionTimeout == 60_000
        cf.handshakeTimeout == 10_000
        cf.shutdownTimeout == 10_000

        cleanup:
        applicationContext.close()
    }

    void "default rabbit configuration is overridden when configuration properties are passed in"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
                ["rabbitmq.username"           : "guest1",
                 "rabbitmq.password"           : "guest1",
                 "rabbitmq.virtualHost"        : "/guest1",
                 "rabbitmq.host"               : "guesthost",
                 "rabbitmq.port"               : 9999,
                 "rabbitmq.requestedChannelMax": 50,
                 "rabbitmq.requestedFrameMax"  : 50,
                 "rabbitmq.requestedHeartbeat" : 50,
                 "rabbitmq.connectionTimeout"  : 50,
                 "rabbitmq.handshakeTimeout"   : 50,
                 "rabbitmq.shutdownTimeout"    : 50],
                "test"
        )

        expect: "connection factory bean is available"
        applicationContext.containsBean(ConnectionFactory)
        applicationContext.containsBean(RabbitConnectionFactoryConfig)

        when: "when the connection factory is returned and values are overridden"
        ConnectionFactory cf = applicationContext.getBean(RabbitConnectionFactoryConfig)

        then: "default configuration is available"
        cf.username == "guest1"
        cf.password == "guest1"
        cf.virtualHost == "/guest1"
        cf.host == "guesthost"
        cf.port == 9999
        cf.requestedChannelMax == 50
        cf.requestedFrameMax == 50
        cf.requestedHeartbeat == 50
        cf.connectionTimeout == 50
        cf.handshakeTimeout == 50
        cf.shutdownTimeout == 50

        cleanup:
        applicationContext.close()
    }
}
