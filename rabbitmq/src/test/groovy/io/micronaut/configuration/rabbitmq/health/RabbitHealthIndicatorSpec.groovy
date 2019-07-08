package io.micronaut.configuration.rabbitmq.health

import io.micronaut.configuration.rabbitmq.AbstractRabbitMQTest
import io.micronaut.context.ApplicationContext
import io.micronaut.health.HealthStatus
import io.micronaut.management.health.indicator.HealthResult
import io.reactivex.Single
import spock.lang.Ignore

class RabbitHealthIndicatorSpec extends AbstractRabbitMQTest {

    void "test rabbitmq health indicator"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(["rabbitmq.port": rabbitContainer.getMappedPort(5672)], "test")

        when:
        RabbitMQHealthIndicator healthIndicator = applicationContext.getBean(RabbitMQHealthIndicator)
        HealthResult result = Single.fromPublisher(healthIndicator.result).blockingGet()

        then:
        result.status == HealthStatus.UP
        ((Map<String, Object>) result.details).get("version").toString().startsWith("3.7")

        cleanup:
        applicationContext.close()
    }

    @Ignore
    void "test rabbitmq health indicator shows down"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(["rabbitmq.port": rabbitContainer.getMappedPort(5672)], "test")

        when:
        RabbitMQHealthIndicator healthIndicator = applicationContext.getBean(RabbitMQHealthIndicator)
        rabbitContainer.stop()
        HealthResult result = Single.fromPublisher(healthIndicator.result).blockingGet()

        then:
        result.status == HealthStatus.DOWN
        ((Map) result.details).get("error").toString().contains("RabbitMQ connection is not open")

        cleanup:
        rabbitContainer.start()
        applicationContext.close()
    }
}
