package io.micronaut.rabbitmq

import io.kotest.core.spec.style.BehaviorSpec
import io.micronaut.context.ApplicationContext
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy

abstract class AbstractRabbitMQTest(body: BehaviorSpec.() -> Unit): BehaviorSpec(body) {

    companion object {
        val rabbitContainer = KGenericContainer("library/rabbitmq:3.11.9-management")
                .withExposedPorts(5672)
                .waitingFor(LogMessageWaitStrategy().withRegEx("(?s).*Server startup complete.*"))!!

        init {
            rabbitContainer.start()
        }

        fun startContext(specName: String): ApplicationContext =
            ApplicationContext.run(getDefaultConfig(specName), "test")

        fun startContext(configuration: Map<String, Any>): ApplicationContext =
            ApplicationContext.run(configuration, "test")

        fun getDefaultConfig(specName: String): MutableMap<String, Any> =
            mutableMapOf(
                    "rabbitmq.port" to rabbitContainer.getMappedPort(5672),
                    "spec.name" to specName)
    }
}
