package io.micronaut.rabbitmq.docs.quickstart;

import io.micronaut.context.annotation.Requires;
// tag::imports[]
import io.micronaut.rabbitmq.annotation.Binding;
import io.micronaut.rabbitmq.annotation.RabbitClient;
// end::imports[]

@Requires(property = "spec.name", value = "QuickstartSpec")
// tag::clazz[]
@RabbitClient // <1>
public interface ProductClient {

    @Binding("product") // <2>
    void send(byte[] data); // <3>
}
// end::clazz[]
