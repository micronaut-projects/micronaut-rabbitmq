package io.micronaut.rabbitmq

import io.kotlintest.specs.AbstractBehaviorSpec
import io.kotlintest.specs.BehaviorSpec
import io.micronaut.context.ApplicationContext
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy

abstract class AbstractRabbitMQTest(body: AbstractBehaviorSpec.() -> Unit = {}): BehaviorSpec(body) {

    companion object {
        val rabbitContainer = KGenericContainer("library/rabbitmq:3.7")
                .withExposedPorts(5672)
                .waitingFor(LogMessageWaitStrategy().withRegEx("(?s).*Server startup complete.*"))!!

        init {
            rabbitContainer.start()
        }

        fun startContext(specName: String): ApplicationContext {
            return ApplicationContext.run(getDefaultConfig(specName), "test")
        }

        fun startContext(configuration: Map<String, Any>): ApplicationContext {
            return ApplicationContext.run(configuration, "test")
        }

        fun getDefaultConfig(specName: String): MutableMap<String, Any> {
            return mutableMapOf(
                    "rabbitmq.port" to rabbitContainer.getMappedPort(5672),
                    "spec.name" to specName)
        }

    }

}