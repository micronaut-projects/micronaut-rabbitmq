package io.micronaut.rabbitmq.docs.consumer.custom.annotation;

import io.micronaut.context.annotation.Requires;
// tag::imports[]
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
// end::imports[]

@Requires(property = "spec.name", value = "DeliveryTagSpec")
// tag::clazz[]
@RabbitListener
public class ProductListener {

    Set<Long> messages = Collections.synchronizedSet(new HashSet<>());

    @Queue("product")
    public void receive(byte[] data, @DeliveryTag Long tag) { // <1>
        messages.add(tag);
    }
}
// end::clazz[]
