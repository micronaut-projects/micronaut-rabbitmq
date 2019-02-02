package io.micronaut.configuration.rabbitmq.docs.parameters;

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.Binding
import io.micronaut.configuration.rabbitmq.annotation.RabbitClient
import io.micronaut.context.annotation.Requires
// end::imports[]

@Requires(property = "spec.name", value = "BindingSpec")
// tag::clazz[]
@RabbitClient
interface ProductClient {

    @Binding("product") // <1>
    void send(byte[] data)

    void send(@Binding String binding, byte[] data) // <2>
}
// end::clazz[]