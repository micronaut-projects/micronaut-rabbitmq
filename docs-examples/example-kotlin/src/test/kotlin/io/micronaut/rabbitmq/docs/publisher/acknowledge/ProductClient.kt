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
    fun sendPublisher(data: ByteArray): Publisher<Void>  // <1>

    @Binding("product")
    fun sendFuture(data: ByteArray): CompletableFuture<Void>  // <2>

    @Binding("product")
    suspend fun sendSuspend(data: ByteArray) //suspend methods work too!
}
// end::clazz[]
