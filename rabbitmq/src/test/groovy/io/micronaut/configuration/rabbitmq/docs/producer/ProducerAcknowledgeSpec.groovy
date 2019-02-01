package io.micronaut.configuration.rabbitmq.docs.producer

import io.micronaut.configuration.rabbitmq.AbstractRabbitMQTest
import io.micronaut.context.ApplicationContext
import io.reactivex.Completable
import spock.util.concurrent.PollingConditions

class ProducerAcknowledgeSpec extends AbstractRabbitMQTest {

    void "test producer acknowledgement"() {
        ApplicationContext applicationContext = ApplicationContext.run(
                ["rabbitmq.port": rabbitContainer.getMappedPort(5672),
                 "spec.name": getClass().simpleName], "test")
        PollingConditions conditions = new PollingConditions(timeout: 3)


        when:
        // tag::producer[]
        ProductBootstrap productBootstrap = applicationContext.getBean(ProductBootstrap)
        Completable completable = productBootstrap.sendProducts()
        // end::producer[]
        then:
        // tag::producer[]
        //The following method causes the messages to be published
        completable.subscribe()
        // end::producer[]

        cleanup:
        applicationContext.close()

    }
}
