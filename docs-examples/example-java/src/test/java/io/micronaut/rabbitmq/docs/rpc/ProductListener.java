package io.micronaut.rabbitmq.docs.rpc;

// tag::imports[]
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import io.micronaut.context.annotation.Requires;
// end::imports[]

@Requires(property = "spec.name", value = "RpcUppercaseSpec")
// tag::clazz[]
@RabbitListener
public class ProductListener {

    @Queue("product")
    public String toUpperCase(String data) { // <1>
        return data.toUpperCase(); // <2>
    }
}
// end::clazz[]
