package io.micronaut.configuration.rabbitmq.docs.properties;

import io.micronaut.configuration.rabbitmq.annotation.Queue;
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener;
import io.micronaut.configuration.rabbitmq.annotation.RabbitProperty;
import io.micronaut.context.annotation.Requires;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Requires(property = "spec.name", value = "PropertiesSpec")
@RabbitListener
public class ProductListener {

    List<String> messageProperties = Collections.synchronizedList(new ArrayList<>());

    @Queue("product")
    public void receive(byte[] data,
                        @RabbitProperty("userId") String user,
                        @Nullable @RabbitProperty String contentType,
                        String appId) {
        messageProperties.add(user + "|" + contentType + "|" + appId);
    }
}
