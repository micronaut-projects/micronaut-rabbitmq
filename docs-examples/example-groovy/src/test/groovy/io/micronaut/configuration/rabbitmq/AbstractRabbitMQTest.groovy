package io.micronaut.configuration.rabbitmq

import io.micronaut.context.ApplicationContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import spock.lang.Specification

abstract class AbstractRabbitMQTest extends Specification {

    static GenericContainer rabbitContainer =
            new GenericContainer("library/rabbitmq:3.7")
                    .withExposedPorts(5672)
                    .waitingFor(new LogMessageWaitStrategy().withRegEx("(?s).*Server startup complete.*"))

    static {
        rabbitContainer.start()
    }

    protected ApplicationContext startContext() {
        return ApplicationContext.run(getConfiguration(), "test")
    }

    protected Map<String, Object> getConfiguration() {
        Map<String, Object> config = new HashMap<>()
        config.put("rabbitmq.port", rabbitContainer.getMappedPort(5672))
        config.put("spec.name", this.getClass().getSimpleName())
        return config
    }
}
