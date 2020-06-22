package io.micronaut.rabbitmq.docs.properties

// tag::imports[]
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.rabbitmq.annotation.RabbitProperty
import io.micronaut.context.annotation.Requires
// end::imports[]

@Requires(property = "spec.name", value = "PropertiesSpec")
// tag::clazz[]
@RabbitClient
@RabbitProperty(name = "appId", value = "myApp") // <1>
interface ProductClient {

    @Binding("product")
    @RabbitProperty(name = "contentType", value = "application/json") // <2>
    @RabbitProperty(name = "userId", value = "guest")
    void send(byte[] data)

    @Binding("product")
    void send(@RabbitProperty("userId") String user, @RabbitProperty String contentType, byte[] data) // <3>
}
// end::clazz[]