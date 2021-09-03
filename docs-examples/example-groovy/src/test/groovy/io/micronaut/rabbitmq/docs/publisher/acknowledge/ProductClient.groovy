package io.micronaut.rabbitmq.docs.publisher.acknowledge

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.RabbitClient
import org.reactivestreams.Publisher

import java.util.concurrent.CompletableFuture
// end::imports[]

@Requires(property = "spec.name", value = "PublisherAcknowledgeSpec")
// tag::clazz[]
@RabbitClient
interface ProductClient {

    @Binding("product")
    Publisher<Void> sendPublisher(byte[] data) // <1>

    @Binding("product")
    CompletableFuture<Void> sendFuture(byte[] data) // <2>
}
// end::clazz[]
