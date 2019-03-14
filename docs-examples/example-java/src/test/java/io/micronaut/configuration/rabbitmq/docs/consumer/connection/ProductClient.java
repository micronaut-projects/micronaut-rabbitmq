package io.micronaut.configuration.rabbitmq.docs.consumer.connection;

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.Binding;
import io.micronaut.configuration.rabbitmq.annotation.RabbitClient;
import io.micronaut.context.annotation.Requires;
// end::imports[]

@Requires(property = "spec.name", value = "ConnectionSpec")
// tag::clazz[]
@RabbitClient // <1>
public interface ProductClient {

    @Binding(value = "product", connection = "product-cluster") // <2>
    void send(byte[] data); // <3>
}
// end::clazz[]