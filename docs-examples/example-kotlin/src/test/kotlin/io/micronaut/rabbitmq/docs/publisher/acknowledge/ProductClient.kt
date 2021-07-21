package io.micronaut.rabbitmq.docs.publisher.acknowledge

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.RabbitClient
import org.reactivestreams.Publisher
// end::imports[]

@Requires(property = "spec.name", value = "PublisherAcknowledgeSpec")
// tag::clazz[]
@RabbitClient
interface ProductClient {

    @Binding("product")
    fun sendPublisher(data: ByteArray): Publisher<Void>  // <1>
}
// end::clazz[]
