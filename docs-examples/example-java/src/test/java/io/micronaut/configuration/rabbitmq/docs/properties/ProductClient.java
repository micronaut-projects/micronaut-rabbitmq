package io.micronaut.configuration.rabbitmq.docs.properties;

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.Binding;
import io.micronaut.configuration.rabbitmq.annotation.RabbitClient;
import io.micronaut.configuration.rabbitmq.annotation.RabbitProperty;
import io.micronaut.context.annotation.Requires;
// end::imports[]

@Requires(property = "spec.name", value = "PropertiesSpec")
// tag::clazz[]
@RabbitClient
@RabbitProperty(name = "appId", value = "myApp") // <1>
public interface ProductClient {

    @Binding("product")
    @RabbitProperty(name = "contentType", value = "application/json") // <2>
    @RabbitProperty(name = "userId", value = "guest")
    void send(byte[] data);

    @Binding("product")
    void send(@RabbitProperty("userId") String user, @RabbitProperty String contentType, byte[] data); // <3>
}
// end::clazz[]