package io.micronaut.rabbitmq.docs.serdes

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

import static java.util.concurrent.TimeUnit.SECONDS
import static org.awaitility.Awaitility.await

@MicronautTest
@Property(name = "spec.name", value = "ProductInfoSerDesSpec")
class ProductInfoSerDesSpec extends Specification {
    @Inject
    ProductClient productClient
    @Inject
    ProductListener listener

    void "test using a custom serdes"() {

        when:
// tag::producer[]
        productClient.send(new ProductInfo("small", 10L, true))
        productClient.send(new ProductInfo("medium", 20L, true))
        productClient.send(new ProductInfo(null, 30L, false))
// end::producer[]

        await().atMost(10, SECONDS).until {
            listener.messages.size() == 3
        }

        then:
        listener.messages.size() == 3
        listener.messages.find({ p -> p.size == "small" && p.count == 10L && p.sealed }) != null
        listener.messages.find({ p -> p.size == "medium" && p.count == 20L && p.sealed }) != null
        listener.messages.find({ p -> p.size == null && p.count == 30L && !p.sealed }) != null
    }
}
