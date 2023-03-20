package io.micronaut.rabbitmq;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.AfterEach;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

public abstract class AbstractRabbitMQTest {

    protected static GenericContainer<?> rabbitContainer = new GenericContainer<>("library/rabbitmq:3.7")
            .withExposedPorts(5672)
            .waitingFor(new LogMessageWaitStrategy().withRegEx("(?s).*Server startup complete.*"));

    static {
        rabbitContainer.start();
    }

    protected ApplicationContext applicationContext;

    protected void startContext() {
        applicationContext = ApplicationContext.run(getConfiguration(), "test");
    }

    protected Map<String, Object> getConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("rabbitmq.port", rabbitContainer.getMappedPort(5672));
        config.put("spec.name", getClass().getSimpleName());
        return config;
    }

    protected void waitFor(Callable<Boolean> conditionEvaluator) {
        await().atMost(5, SECONDS).until(conditionEvaluator);
    }

    @AfterEach
    void cleanup() {
        if (applicationContext != null) {
            applicationContext.close();
        }
    }
}
