package io.micronaut.rabbitmq.docs.consumer.executor;

import io.micronaut.context.annotation.Requires;
// tag::imports[]
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
// end::imports[]

@Requires(property = "spec.name", value = "CustomExecutorSpec")
// tag::clazz[]
@RabbitListener
public class ProductListener {

    List<String> messageLengths = Collections.synchronizedList(new ArrayList<>());

    @Queue(value = "product", executor = "product-listener") // <1>
    public void receive(byte[] data) {
        messageLengths.add(new String(data));
        System.out.println("Java received " + data.length + " bytes from RabbitMQ");
    }
}
// end::clazz[]
