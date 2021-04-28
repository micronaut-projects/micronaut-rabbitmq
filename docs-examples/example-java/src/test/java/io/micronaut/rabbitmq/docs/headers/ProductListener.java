package io.micronaut.rabbitmq.docs.headers;

// tag::imports[]
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitHeaders;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import io.micronaut.context.annotation.Requires;
import io.micronaut.messaging.annotation.Header;

import javax.annotation.Nullable;
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
        messageProperties.add(
                headers.get("x-product-sealed") + "|" +
                headers.get("x-product-count") + "|" +
                headers.get("productSize"));
    }
}
// end::clazz[]
