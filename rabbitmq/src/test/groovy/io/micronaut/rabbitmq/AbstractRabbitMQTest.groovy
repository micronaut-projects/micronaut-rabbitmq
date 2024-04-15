package io.micronaut.rabbitmq

import io.micronaut.context.ApplicationContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

abstract class AbstractRabbitMQTest extends Specification {

    public static String RABBIT_CONTAINER_VERSION = "3.13.1"

    static GenericContainer rabbitContainer =
            new GenericContainer("rabbitmq:" + RABBIT_CONTAINER_VERSION)
                    .withExposedPorts(5672)
                    .waitingFor(new LogMessageWaitStrategy().withRegEx("(?s).*Server startup complete.*"))

    static {
        rabbitContainer.start()
    }

    protected ApplicationContext applicationContext
    protected PollingConditions conditions = new PollingConditions(timeout: 5)

    protected void startContext(Map additionalConfig = [:]) {
        applicationContext = ApplicationContext.run(
                ["rabbitmq.port": rabbitContainer.getMappedPort(5672),
                 "spec.name": getClass().simpleName] << additionalConfig, "test")
    }

    protected void waitFor(Closure<?> conditionEvaluator) {
        conditions.eventually conditionEvaluator
    }

    void cleanup() {
        applicationContext?.close()
    }
}
