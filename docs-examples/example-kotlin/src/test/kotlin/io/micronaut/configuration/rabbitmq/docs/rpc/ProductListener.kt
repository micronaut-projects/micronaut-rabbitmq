package io.micronaut.configuration.rabbitmq.docs.rpc

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.Queue
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener
import io.micronaut.context.annotation.Requires

import java.io.IOException
// end::imports[]

@Requires(property = "spec.name", value = "RpcUppercaseSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    @Queue("product")
    @Throws(IOException::class)
    fun toUpperCase(data: String): String { // <1>
        return data.toUpperCase() // <2>
    }
}
// end::clazz[]
