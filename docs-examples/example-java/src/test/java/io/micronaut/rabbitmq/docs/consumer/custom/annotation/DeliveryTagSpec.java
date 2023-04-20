package io.micronaut.rabbitmq.docs.consumer.custom.annotation;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@MicronautTest
@Property(name = "spec.name", value = "DeliveryTagSpec")
class DeliveryTagSpec {

    @Test
    void testUsingACustomAnnotationBinder(ProductClient productClient, ProductListener productListener) {
// tag::producer[]
        productClient.send("body".getBytes());
        productClient.send("body2".getBytes());
        productClient.send("body3".getBytes());
// end::producer[]


        await().atMost(60, SECONDS).until(() -> productListener.messages.size() == 3);
    }
}
