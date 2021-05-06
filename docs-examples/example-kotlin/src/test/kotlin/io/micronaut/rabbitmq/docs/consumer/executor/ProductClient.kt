package io.micronaut.rabbitmq.docs.consumer.executor

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.RabbitClient
// end::imports[]

@Requires(property = "spec.name", value = "CustomExecutorSpec")
// tag::clazz[]
@RabbitClient // <1>
interface ProductClient {

    @Binding(value = "product") // <2>
    fun send(data: ByteArray)  // <3>
}
// end::clazz[]
