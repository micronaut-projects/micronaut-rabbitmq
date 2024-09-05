package io.micronaut.rabbitmq.ssl

import io.micronaut.context.ApplicationContext
import io.micronaut.health.HealthStatus
import io.micronaut.management.health.indicator.HealthResult
import io.micronaut.rabbitmq.health.RabbitMQHealthIndicator
import reactor.core.publisher.Mono

class RabbitSSLHealthIndicatorSpec extends AbstractRabbitMQSSLTest {

    void "test rabbitmq health indicator"() {
        given:
        startContext()

        when:
        RabbitMQHealthIndicator healthIndicator = applicationContext.getBean(RabbitMQHealthIndicator)
        HealthResult result = Mono.from(healthIndicator.result).block()
        def details = result.details

        then:
        result.status == HealthStatus.UP
        details.version.toString() == RABBIT_CONTAINER_VERSION
    }

    void "test rabbitmq health indicator with 2 connections"() {
        given:
        applicationContext = ApplicationContext.run([
                "rabbitmq.servers.one.port": rabbitContainer.getMappedPort(5672),
                "rabbitmq.servers.two.port": rabbitContainer.getMappedPort(5672)
        ], "test")

        when:
        RabbitMQHealthIndicator healthIndicator = applicationContext.getBean(RabbitMQHealthIndicator)
        HealthResult result = Mono.from(healthIndicator.result).block()
        def details = result.details

        then:
        result.status == HealthStatus.UP
        details.get("connections")[0].get("version").toString() == RABBIT_CONTAINER_VERSION
        details.get("connections")[1].get("version").toString() == RABBIT_CONTAINER_VERSION
    }

    void "test rabbitmq health indicator shows down"() {
        given:
        startContext()

        when:
        RabbitMQHealthIndicator healthIndicator = applicationContext.getBean(RabbitMQHealthIndicator)
        rabbitContainer.stop()
        HealthResult result = Mono.from(healthIndicator.result).block()
        def details = result.details

        then:
        result.status == HealthStatus.DOWN
        details.get("error").toString().contains("RabbitMQ connection is not open")

        cleanup:
        rabbitContainer.start()
    }
}
