package io.micronaut.rabbitmq.docs.parameters;

import io.micronaut.context.annotation.Requires;
// tag::imports[]
import io.micronaut.rabbitmq.annotation.Binding;
import io.micronaut.rabbitmq.annotation.RabbitClient;
// end::imports[]

@Requires(property = "spec.name", value = "BindingSpec")
// tag::clazz[]
@RabbitClient
public interface ProductClient {

    @Binding("product") // <1>
    void send(byte[] data);

    void send(@Binding String binding, byte[] data); // <2>
}
// end::clazz[]
