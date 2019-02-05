package io.micronaut.configuration.rabbitmq.docs.properties

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.Queue
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener
import io.micronaut.configuration.rabbitmq.annotation.RabbitProperty
import io.micronaut.context.annotation.Requires

import javax.annotation.Nullable
// end::imports[]

@Requires(property = "spec.name", value = "PropertiesSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    List<String> messageProperties = Collections.synchronizedList([])

    @Queue("product")
    @RabbitProperty(name = "x-priority", value = "10", type = Integer.class) // <1>
    void receive(byte[] data,
                 @RabbitProperty("userId") String user, // <2>
                 @Nullable @RabbitProperty String contentType, // <3>
                 String appId) { // <4>
        messageProperties.add(user + "|" + contentType + "|" + appId)
    }
}
// end::clazz[]
