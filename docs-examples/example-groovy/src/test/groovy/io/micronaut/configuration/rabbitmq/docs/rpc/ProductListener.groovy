package io.micronaut.configuration.rabbitmq.docs.rpc

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.Queue
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener
import io.micronaut.context.annotation.Requires
// end::imports[]

@Requires(property = "spec.name", value = "RpcUppercaseSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    @Queue("product")
    String toUpperCase(String data) throws IOException { // <1>
        data.toUpperCase() // <2>
    }
}
// end::clazz[]
