package io.micronaut.rabbitmq.docs.consumer.connection


import io.micronaut.context.ApplicationContext
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import spock.util.concurrent.PollingConditions

class ConnectionSpec extends AbstractRabbitMQTest {

    void "test product client and listener"() {
        ApplicationContext applicationContext = startContext()
        PollingConditions conditions = new PollingConditions(timeout: 5)

        when:
// tag::producer[]
        def productClient = applicationContext.getBean(ProductClient)
        productClient.send("quickstart".getBytes())
// end::producer[]

        ProductListener productListener = applicationContext.getBean(ProductListener)

        then:
        conditions.eventually {
            productListener.messageLengths.size() == 1
            productListener.messageLengths[0] == "quickstart"
        }

        cleanup:
        // Finding that the context is closing the channel before ack is sent
        Thread.sleep(200)
        applicationContext.close()
    }


    protected Map<String, Object> getConfiguration() {
        Map<String, Object> config = super.getConfiguration()
        config.put("rabbitmq.servers.product-cluster.port", config.remove("rabbitmq.port"))
        return config
    }
}
