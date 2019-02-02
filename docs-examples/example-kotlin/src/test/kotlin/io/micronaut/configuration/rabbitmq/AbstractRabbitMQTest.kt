package io.micronaut.configuration.rabbitmq

import io.kotlintest.specs.AbstractBehaviorSpec
import io.kotlintest.specs.BehaviorSpec
import io.micronaut.configuration.KGenericContainer
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
    }

    val specName = javaClass.simpleName

    fun startContext(): ApplicationContext {
        return ApplicationContext.run(
                mapOf("rabbitmq.port" to rabbitContainer.getMappedPort(5672),
                        "spec.name" to specName))
    }
}