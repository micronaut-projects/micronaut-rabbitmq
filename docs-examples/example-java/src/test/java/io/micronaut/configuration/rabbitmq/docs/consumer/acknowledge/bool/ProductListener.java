package io.micronaut.configuration.rabbitmq.docs.consumer.acknowledge.bool;

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.Queue;
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener;
import io.micronaut.context.annotation.Requires;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
// end::imports[]

@Requires(property = "spec.name", value = "BooleanAckSpec")
// tag::clazz[]
@RabbitListener
public class ProductListener {

    AtomicInteger messageCount = new AtomicInteger();

    @Queue(value = "product", reQueue = true) // <1>
    public Boolean receive(byte[] data) { // <2>
        return messageCount.getAndUpdate((intValue) -> ++intValue) > 0; // <3>
    }
}
// end::clazz[]
