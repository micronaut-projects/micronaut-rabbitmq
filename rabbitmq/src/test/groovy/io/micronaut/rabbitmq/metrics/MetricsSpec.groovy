package io.micronaut.rabbitmq.metrics

import io.micronaut.configuration.metrics.management.endpoint.MetricsEndpoint
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener

class MetricsSpec extends AbstractRabbitMQTest {

    void 'test metrics information is added to metrics endpoint'() {
        given:
        ApplicationContext ctx = startContext()

        when:
        MetricsEndpoint endpoint = ctx.getBean(MetricsEndpoint)

        then:
        endpoint.listNames().any { metricNames -> metricNames.names.any { it.startsWith('rabbitmq.') } }

        cleanup:
        ctx.close()
    }

    @Requires(property = 'spec.name', value = 'MetricsSpec')
    @RabbitListener
    static class MyConsumer {
        @Queue('simple')
        void listen(String data) {
        }
    }

}
