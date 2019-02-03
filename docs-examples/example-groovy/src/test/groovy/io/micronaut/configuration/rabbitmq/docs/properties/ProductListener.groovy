package io.micronaut.configuration.rabbitmq.docs.properties

import io.micronaut.configuration.rabbitmq.annotation.Queue
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener
import io.micronaut.configuration.rabbitmq.annotation.RabbitProperty
import io.micronaut.context.annotation.Requires

import javax.annotation.Nullable

@Requires(property = "spec.name", value = "PropertiesSpec")
@RabbitListener
class ProductListener {

    List<String> messageProperties = Collections.synchronizedList([])

    @Queue("product")
    void receive(byte[] data,
                 @RabbitProperty("userId") String user,
                 @Nullable @RabbitProperty String contentType,
                 String appId) {
        messageProperties.add(user + "|" + contentType + "|" + appId)
    }
}
