package io.micronaut.configuration.rabbitmq.docs.rpc;

// tag::imports[]
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import io.micronaut.configuration.rabbitmq.annotation.Queue;
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener;
import io.micronaut.configuration.rabbitmq.annotation.RabbitProperty;
import io.micronaut.context.annotation.Requires;

import java.io.IOException;
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
