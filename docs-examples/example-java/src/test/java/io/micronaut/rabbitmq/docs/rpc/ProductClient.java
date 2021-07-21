package io.micronaut.rabbitmq.docs.rpc;

import io.micronaut.context.annotation.Requires;
// tag::imports[]
import io.micronaut.rabbitmq.annotation.Binding;
import io.micronaut.rabbitmq.annotation.RabbitClient;
import io.micronaut.rabbitmq.annotation.RabbitProperty;
import org.reactivestreams.Publisher;
// end::imports[]

@Requires(property = "spec.name", value = "RpcUppercaseSpec")
// tag::clazz[]
@RabbitClient
@RabbitProperty(name = "replyTo", value = "amq.rabbitmq.reply-to") // <1>
public interface ProductClient {

    @Binding("product")
    String send(String data); // <2>

    @Binding("product")
    Publisher<String> sendReactive(String data); // <3>
}
// end::clazz[]
