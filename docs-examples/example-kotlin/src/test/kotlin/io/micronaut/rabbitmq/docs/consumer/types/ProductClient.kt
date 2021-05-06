package io.micronaut.rabbitmq.docs.consumer.types

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.RabbitClient
// end::imports[]

@Requires(property = "spec.name", value = "TypeBindingSpec")
// tag::clazz[]
@RabbitClient // <1>
interface ProductClient {

    @Binding("product") // <2>
    fun send(data: ByteArray, contentType: String)  // <3>
}
// end::clazz[]
