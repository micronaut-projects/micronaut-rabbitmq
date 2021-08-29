package io.micronaut.rabbitmq.health

import io.micronaut.context.ApplicationContext
import io.micronaut.management.health.indicator.HealthResult
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import reactor.core.publisher.Mono

import static io.micronaut.health.HealthStatus.DOWN
import static io.micronaut.health.HealthStatus.UP

class RabbitHealthIndicatorSpec extends AbstractRabbitMQTest {

    void "test rabbitmq health indicator"() {
        given:
        startContext()

        when:
        RabbitMQHealthIndicator healthIndicator = applicationContext.getBean(RabbitMQHealthIndicator)
        HealthResult result = Mono.from(healthIndicator.result).block()

        then:
        result.status == UP
        ((Map<String, Object>) result.details).version.toString().startsWith("3.7")
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

        then:
        result.status == UP
        Map<String, List> details = result.details
        details.get("connections")[0].get("version").toString().startsWith("3.7")
        details.get("connections")[1].get("version").toString().startsWith("3.7")
    }

    void "test rabbitmq health indicator shows down"() {
        given:
        startContext()

        when:
        RabbitMQHealthIndicator healthIndicator = applicationContext.getBean(RabbitMQHealthIndicator)
        rabbitContainer.stop()
        HealthResult result = Mono.from(healthIndicator.result).block()

        then:
        result.status == DOWN
        ((Map) result.details).get("error").toString().contains("RabbitMQ connection is not open")

        cleanup:
        rabbitContainer.start()
    }
}
