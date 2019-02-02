package io.micronaut.configuration.rabbitmq;

import io.micronaut.context.ApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractRabbitMQTest {

    protected static GenericContainer rabbitContainer = new GenericContainer("library/rabbitmq:3.7")
            .withExposedPorts(5672)
            .waitingFor(new LogMessageWaitStrategy().withRegEx("(?s).*Server startup complete.*"));

    static {
        rabbitContainer.start();
    }

    protected ApplicationContext startContext() {
        Map<String, Object> config = new HashMap<>();
        config.put("rabbitmq.port", rabbitContainer.getMappedPort(5672));
        config.put("spec.name", this.getClass().getSimpleName());
        return ApplicationContext.run(config, "test");
    }
}
