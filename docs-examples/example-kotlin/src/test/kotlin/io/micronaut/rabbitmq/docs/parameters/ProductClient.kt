package io.micronaut.rabbitmq.docs.parameters

// tag::imports[]
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.context.annotation.Requires
// end::imports[]

@Requires(property = "spec.name", value = "BindingSpec")
// tag::clazz[]
@RabbitClient
interface ProductClient {

    @Binding("product") // <1>
    fun send(data: ByteArray)

    fun send(@Binding binding: String, data: ByteArray)  // <2>
}
// end::clazz[]