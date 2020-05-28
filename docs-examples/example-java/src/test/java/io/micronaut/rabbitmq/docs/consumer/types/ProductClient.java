package io.micronaut.rabbitmq.docs.consumer.types;

// tag::imports[]
import io.micronaut.rabbitmq.annotation.Binding;
import io.micronaut.rabbitmq.annotation.RabbitClient;
import io.micronaut.context.annotation.Requires;
// end::imports[]

@Requires(property = "spec.name", value = "TypeBindingSpec")
// tag::clazz[]
@RabbitClient // <1>
public interface ProductClient {

    @Binding("product") // <2>
    void send(byte[] data, String contentType); // <3>
}
// end::clazz[]