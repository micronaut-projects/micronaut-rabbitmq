package io.micronaut.rabbitmq.docs.rpc

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.rabbitmq.annotation.RabbitProperty
import org.reactivestreams.Publisher
// end::imports[]

@Requires(property = "spec.name", value = "RpcUppercaseSpec")
// tag::clazz[]
@RabbitClient
@RabbitProperty(name = "replyTo", value = "amq.rabbitmq.reply-to") // <1>
interface ProductClient {

    @Binding("product")
    fun send(data: String): String  // <2>

    @Binding("product")
    fun sendReactive(data: String): Publisher<String>  // <3>
}
// end::clazz[]
