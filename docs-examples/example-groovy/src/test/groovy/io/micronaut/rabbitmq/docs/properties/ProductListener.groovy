package io.micronaut.rabbitmq.docs.properties

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.core.annotation.Nullable
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener
import io.micronaut.rabbitmq.annotation.RabbitProperty
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
