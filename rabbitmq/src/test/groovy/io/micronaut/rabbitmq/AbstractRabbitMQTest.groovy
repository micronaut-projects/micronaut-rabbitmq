package io.micronaut.rabbitmq

import io.micronaut.context.ApplicationContext
import org.testcontainers.containers.RabbitMQContainer
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

abstract class AbstractRabbitMQTest extends Specification {

    static String RABBIT_CONTAINER_VERSION = "3.13.1"

    @Shared
    @AutoCleanup
    RabbitMQContainer rabbitContainer = new RabbitMQContainer("rabbitmq:" + RABBIT_CONTAINER_VERSION)

    def setupSpec() {
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
