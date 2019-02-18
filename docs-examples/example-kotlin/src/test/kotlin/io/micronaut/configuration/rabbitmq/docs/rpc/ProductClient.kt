package io.micronaut.configuration.rabbitmq.docs.rpc

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.Binding
import io.micronaut.configuration.rabbitmq.annotation.RabbitClient
import io.micronaut.configuration.rabbitmq.annotation.RabbitProperty
import io.micronaut.context.annotation.Requires
import io.reactivex.Single
// end::imports[]

@Requires(property = "spec.name", value = "RpcUppercaseSpec")
// tag::clazz[]
@RabbitClient
@RabbitProperty(name = "replyTo", value = "amq.rabbitmq.reply-to") // <1>
interface ProductClient {

    @Binding("product")
    fun send(data: String): String  // <2>

    @Binding("product")
    fun sendReactive(data: String): Single<String>  // <3>
}
// end::clazz[]