package io.micronaut.rabbitmq.docs.parameters;

import io.micronaut.context.annotation.Requires;
// tag::imports[]
import io.micronaut.rabbitmq.annotation.Binding;
import io.micronaut.rabbitmq.annotation.Mandatory;
import io.micronaut.rabbitmq.annotation.RabbitClient;
// end::imports[]

@Requires(property = "spec.name", value = "MandatorySpec")
// tag::clazz[]
@RabbitClient
public interface MandatoryProductClient {

    @Binding("product")
    @Mandatory // <1>
    void send(byte[] data);

    @Binding("product")
    void send(@Mandatory boolean mandatory, byte[] data); // <2>
}
// end::clazz[]
