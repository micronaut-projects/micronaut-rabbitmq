package io.micronaut.rabbitmq.docs.headers;

import io.micronaut.context.annotation.Requires;
// tag::imports[]
import io.micronaut.core.annotation.Nullable;
import io.micronaut.messaging.annotation.Header;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitHeaders;
import io.micronaut.rabbitmq.annotation.RabbitListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
// end::imports[]

@Requires(property = "spec.name", value = "HeadersSpec")
// tag::clazz[]
@RabbitListener
public class ProductListener {

    List<String> messageProperties = Collections.synchronizedList(new ArrayList<>());

    @Queue("product")
    public void receive(byte[] data,
                        @Header("x-product-sealed") Boolean sealed, // <1>
                        @Header("x-product-count") Long count, // <2>
                        @Nullable @Header String productSize) { // <3>
        messageProperties.add(sealed + "|" + count + "|" + productSize);
    }

    @Queue("product")
    public void receive(byte[] data,
                        @RabbitHeaders Map<String, Object> headers) { // <4>
        Object productSize = headers.get("productSize");
        messageProperties.add(
                headers.get("x-product-sealed").toString() + "|" +
                headers.get("x-product-count").toString() + "|" +
                (productSize != null ? productSize.toString() : null));
    }
}
// end::clazz[]
