package io.micronaut.rabbitmq.docs.publisher.acknowledge;

// tag::imports[]
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import io.micronaut.context.annotation.Requires;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
// end::imports[]

@Requires(property = "spec.name", value = "PublisherAcknowledgeSpec")
// tag::clazz[]
@RabbitListener // <1>
public class ProductListener {

    List<Integer> messageLengths = Collections.synchronizedList(new ArrayList<>());

    @Queue("product") // <2>
    public void receive(byte[] data) { // <3>
        Integer length = data.length;
        messageLengths.add(length);
        System.out.println("Groovy received " + length + " bytes from RabbitMQ");
    }
}
// end::clazz[]
