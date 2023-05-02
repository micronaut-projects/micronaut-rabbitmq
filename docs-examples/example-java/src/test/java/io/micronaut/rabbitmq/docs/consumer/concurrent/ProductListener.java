package io.micronaut.rabbitmq.docs.consumer.concurrent;

// tag::imports[]
import io.micronaut.context.annotation.Requires;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;

import java.util.concurrent.CopyOnWriteArraySet;
// end::imports[]

@Requires(property = "spec.name", value = "ConcurrentSpec")
// tag::clazz[]
@RabbitListener
public class ProductListener {

    CopyOnWriteArraySet<String> threads = new CopyOnWriteArraySet<>();

    @Queue(value = "product", numberOfConsumers = "5") // <1>
    public void receive(byte[] data) {
        threads.add(Thread.currentThread().getName()); // <2>
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) { }
    }
}
// end::clazz[]
