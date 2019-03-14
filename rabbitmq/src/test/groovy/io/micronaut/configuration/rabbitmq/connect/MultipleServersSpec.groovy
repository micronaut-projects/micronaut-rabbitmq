package io.micronaut.configuration.rabbitmq.connect

import com.rabbitmq.client.Connection
import io.micronaut.context.ApplicationContext
import io.micronaut.inject.qualifiers.Qualifiers
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import spock.lang.Shared
import spock.lang.Specification

import java.time.Duration

class MultipleServersSpec extends Specification {

    @Shared GenericContainer rabbit1 = new GenericContainer("library/rabbitmq:3.7")
        .withExposedPorts(5672)
        .waitingFor(new LogMessageWaitStrategy().withRegEx("(?s).*Server startup complete.*"))
    @Shared GenericContainer rabbit2 = new GenericContainer("library/rabbitmq:3.7")
            .withExposedPorts(5672)
            .waitingFor(new LogMessageWaitStrategy().withRegEx("(?s).*Server startup complete.*"))

    void "test multiple server configuration"() {
        given:
        rabbit1.start()
        rabbit2.start()
        ApplicationContext context = ApplicationContext.run(
                ["spec.name": getClass().simpleName,
                 "rabbitmq.servers.one.uri": "amqp://localhost:${rabbit1.getMappedPort(5672)}",
                 "rabbitmq.servers.one.channel-pool.max-idle-channels": "10",
                 "rabbitmq.servers.one.rpc.timeout": "10s",
                 "rabbitmq.servers.two.uri": "amqp://localhost:${rabbit2.getMappedPort(5672)}",
                 "rabbitmq.servers.two.channel-pool.max-idle-channels": "20",
                 "rabbitmq.servers.two.rpc.timeout": "20s"])

        expect:
        context.getBean(RabbitConnectionFactoryConfig, Qualifiers.byName("one")).getPort() == rabbit1.getMappedPort(5672)
        context.getBean(RabbitConnectionFactoryConfig, Qualifiers.byName("one")).getRpc().timeout.get() == Duration.ofSeconds(10)
        context.getBean(RabbitConnectionFactoryConfig, Qualifiers.byName("two")).getRpc().timeout.get() == Duration.ofSeconds(20)
        context.getBean(RabbitConnectionFactoryConfig, Qualifiers.byName("two")).getPort() == rabbit2.getMappedPort(5672)
        context.getBean(Connection, Qualifiers.byName("one")).getPort() == rabbit1.getMappedPort(5672)
        context.getBean(Connection, Qualifiers.byName("two")).getPort() == rabbit2.getMappedPort(5672)
        context.getBean(DefaultChannelPool, Qualifiers.byName("one")).connection == context.getBean(Connection, Qualifiers.byName("one"))
        context.getBean(DefaultChannelPool, Qualifiers.byName("two")).connection == context.getBean(Connection, Qualifiers.byName("two"))
        context.getBean(DefaultChannelPool, Qualifiers.byName("one")).channels.remainingCapacity() == 9
        context.getBean(DefaultChannelPool, Qualifiers.byName("two")).channels.remainingCapacity() == 19
        context.getBean(DefaultChannelPool, Qualifiers.byName("two")).getName() == "two"
        context.getBean(DefaultChannelPool, Qualifiers.byName("one")).getName() == "one"

        cleanup:
        context.stop()
        rabbit1.stop()
        rabbit2.stop()
    }
}
