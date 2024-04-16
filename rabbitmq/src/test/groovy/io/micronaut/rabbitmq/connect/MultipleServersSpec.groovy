package io.micronaut.rabbitmq.connect

import com.rabbitmq.client.Connection
import io.micronaut.context.ApplicationContext
import io.micronaut.inject.qualifiers.Qualifiers
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import org.testcontainers.containers.RabbitMQContainer
import spock.lang.Specification

import java.time.Duration

class MultipleServersSpec extends Specification {

    void "test multiple server configuration"() {
        given:
        RabbitMQContainer rabbit1 = new RabbitMQContainer("rabbitmq:" + AbstractRabbitMQTest.RABBIT_CONTAINER_VERSION)
        RabbitMQContainer rabbit2 = new RabbitMQContainer("rabbitmq:" + AbstractRabbitMQTest.RABBIT_CONTAINER_VERSION)

        when:
        rabbit1.start()
        rabbit2.start()
        ApplicationContext context = ApplicationContext.run([
                "spec.name"                                          : getClass().simpleName,
                "rabbitmq.servers.one.uri"                           : "amqp://localhost:${rabbit1.getMappedPort(5672)}",
                "rabbitmq.servers.one.channel-pool.max-idle-channels": "10",
                "rabbitmq.servers.one.rpc.timeout"                   : "10s",
                "rabbitmq.servers.two.uri"                           : "amqp://localhost:${rabbit2.getMappedPort(5672)}",
                "rabbitmq.servers.two.channel-pool.max-idle-channels": "20",
                "rabbitmq.servers.two.rpc.timeout"                   : "20s"
        ])

        then:
        context.getBean(RabbitConnectionFactoryConfig, Qualifiers.byName("one")).port == rabbit1.getMappedPort(5672)
        context.getBean(RabbitConnectionFactoryConfig, Qualifiers.byName("one")).rpc.timeout.get() == Duration.ofSeconds(10)
        context.getBean(RabbitConnectionFactoryConfig, Qualifiers.byName("two")).rpc.timeout.get() == Duration.ofSeconds(20)
        context.getBean(RabbitConnectionFactoryConfig, Qualifiers.byName("two")).port == rabbit2.getMappedPort(5672)
        context.getBean(Connection, Qualifiers.byName("one")).port == rabbit1.getMappedPort(5672)
        context.getBean(Connection, Qualifiers.byName("two")).port == rabbit2.getMappedPort(5672)
        context.getBean(DefaultChannelPool, Qualifiers.byName("one")).connection == context.getBean(Connection, Qualifiers.byName("one"))
        context.getBean(DefaultChannelPool, Qualifiers.byName("two")).connection == context.getBean(Connection, Qualifiers.byName("two"))
        context.getBean(DefaultChannelPool, Qualifiers.byName("one")).channels.remainingCapacity() == 9
        context.getBean(DefaultChannelPool, Qualifiers.byName("two")).channels.remainingCapacity() == 19
        context.getBean(DefaultChannelPool, Qualifiers.byName("two")).name == "two"
        context.getBean(DefaultChannelPool, Qualifiers.byName("one")).name == "one"

        cleanup:
        context.stop()
        rabbit1.stop()
        rabbit2.stop()
    }
}
