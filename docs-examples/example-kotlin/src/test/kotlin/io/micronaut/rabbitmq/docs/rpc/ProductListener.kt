package io.micronaut.rabbitmq.docs.rpc

// tag::imports[]
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener
import io.micronaut.context.annotation.Requires

// end::imports[]

@Requires(property = "spec.name", value = "RpcUppercaseSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    @Queue("product")
    fun toUpperCase(data: String): String { // <1>
        return data.toUpperCase() // <2>
    }
}
// end::clazz[]
