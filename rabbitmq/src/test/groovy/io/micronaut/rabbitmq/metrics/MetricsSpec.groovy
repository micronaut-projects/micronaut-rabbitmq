package io.micronaut.rabbitmq.metrics

import io.micronaut.configuration.metrics.management.endpoint.MetricsEndpoint
import io.micronaut.context.annotation.Requires
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener

class MetricsSpec extends AbstractRabbitMQTest {

    void 'test metrics information is added to metrics endpoint'() {
        given:
        startContext()

        when:
        MetricsEndpoint endpoint = applicationContext.getBean(MetricsEndpoint)

        then:
        endpoint.listNames().any { metricNames -> metricNames.names.any { it.startsWith('rabbitmq.') } }
    }

    @Requires(property = 'spec.name', value = 'MetricsSpec')
    @RabbitListener
    static class MyConsumer {
        @Queue('simple')
        void listen(String data) {}
    }
}
