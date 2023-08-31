package io.micronaut.rabbitmq.docs.parameters

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.Mandatory
import io.micronaut.rabbitmq.annotation.RabbitClient
// end::imports[]

@Requires(property = "spec.name", value = "MandatorySpec")
// tag::clazz[]
@RabbitClient
interface MandatoryProductClient {

    @Binding("product")
    @Mandatory // <1>
    fun send(data: ByteArray)

    @Binding("product")
    fun send(@Mandatory mandatory: Boolean, data: ByteArray) // <2>
}
// end::clazz[]
