package io.micronaut.rabbitmq.docs.consumer.concurrent;

import io.micronaut.context.annotation.Requires;
import io.micronaut.rabbitmq.annotation.Binding;
import io.micronaut.rabbitmq.annotation.RabbitClient;
// end::imports[]

@Requires(property = "spec.name", value = "ConcurrentSpec")
// tag::clazz[]
@RabbitClient // <1>
public interface ProductClient {

    @Binding("product") // <2>
    void send(byte[] data); // <3>
}
// end::clazz[]
