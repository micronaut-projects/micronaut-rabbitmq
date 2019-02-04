package io.micronaut.configuration.rabbitmq.docs.headers;

import io.micronaut.configuration.rabbitmq.annotation.Queue;
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener;
import io.micronaut.configuration.rabbitmq.annotation.RabbitProperty;
import io.micronaut.context.annotation.Requires;
import io.micronaut.messaging.annotation.Header;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Requires(property = "spec.name", value = "HeadersSpec")
@RabbitListener
public class ProductListener {

    List<String> messageProperties = Collections.synchronizedList(new ArrayList<>());

    @Queue("product")
    public void receive(byte[] data,
                        @Header("x-product-sealed") Boolean sealed,
                        @Header("x-product-count") Long count,
                        @Nullable @Header("x-product-size") String size) {
        messageProperties.add(sealed + "|" + count + "|" + size);
    }
}
