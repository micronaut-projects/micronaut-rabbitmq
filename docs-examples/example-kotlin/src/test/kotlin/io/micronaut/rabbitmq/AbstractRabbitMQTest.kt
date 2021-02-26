package io.micronaut.rabbitmq

import io.kotlintest.specs.AbstractAnnotationSpec
import io.kotlintest.specs.AbstractBehaviorSpec
import io.kotlintest.specs.BehaviorSpec
import io.micronaut.context.ApplicationContext
import org.junit.Before
import org.slf4j.LoggerFactory
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy

abstract class AbstractRabbitMQTest(body: AbstractBehaviorSpec.() -> Unit = {}): BehaviorSpec(body) {
    companion object {
        private val logger = LoggerFactory.getLogger(javaClass)
        val rabbitContainer = KGenericContainer("library/rabbitmq:3.7")
                .withExposedPorts(5672)
                .waitingFor(LogMessageWaitStrategy().withRegEx("(?s).*Server startup complete.*"))!!

        init {
            rabbitContainer.start()
        }

        fun startContext(specName: String): ApplicationContext {
            logger.info("Running " + specName)
            return ApplicationContext.run(getDefaultConfig(specName), "test")
        }

        fun startContext(configuration: Map<String, Any>): ApplicationContext {
            logger.info("Running from map " + configuration["spec.name"])
            return ApplicationContext.run(configuration, "test")
        }

        fun getDefaultConfig(specName: String): MutableMap<String, Any> {
            return mutableMapOf(
                    "rabbitmq.port" to rabbitContainer.getMappedPort(5672),
                    "spec.name" to specName)
        }
    }
}