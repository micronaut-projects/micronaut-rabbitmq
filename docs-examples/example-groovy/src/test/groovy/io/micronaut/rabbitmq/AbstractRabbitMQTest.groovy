package io.micronaut.rabbitmq

import io.micronaut.context.ApplicationContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

abstract class AbstractRabbitMQTest extends Specification {

    static GenericContainer rabbitContainer =
            new GenericContainer("library/rabbitmq:3.7")
                    .withExposedPorts(5672)
                    .waitingFor(new LogMessageWaitStrategy().withRegEx("(?s).*Server startup complete.*"))

    protected ApplicationContext applicationContext
    protected PollingConditions conditions = new PollingConditions(timeout: 5)

    protected void startContext() {
        applicationContext = ApplicationContext.run(configuration, "test")
    }

    protected Map<String, Object> getConfiguration() {
        rabbitContainer.start()

        ["rabbitmq.port": rabbitContainer.getMappedPort(5672),
         "spec.name"    : getClass().simpleName] as Map
    }

    protected void waitFor(Closure<?> conditionEvaluator) {
        conditions.eventually conditionEvaluator
    }

    void cleanup() {
        rabbitContainer.stop()

        applicationContext?.close()
    }
}
