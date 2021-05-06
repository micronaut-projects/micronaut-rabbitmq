package io.micronaut.rabbitmq.docs.publisher.acknowledge

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.reactivex.Completable
import io.reactivex.Maybe
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
// end::imports[]

@Requires(property = "spec.name", value = "PublisherAcknowledgeSpec")
// tag::clazz[]
@RabbitClient
interface ProductClient {

    @Binding("product")
    fun send(data: ByteArray): Completable  // <1>

    @Binding("product")
    fun sendMaybe(data: ByteArray): Maybe<Void>  // <2>

    @Binding("product")
    fun sendMono(data: ByteArray): Mono<Void>  // <3>

    @Binding("product")
    fun sendPublisher(data: ByteArray): Publisher<Void>  // <4>
}
// end::clazz[]
