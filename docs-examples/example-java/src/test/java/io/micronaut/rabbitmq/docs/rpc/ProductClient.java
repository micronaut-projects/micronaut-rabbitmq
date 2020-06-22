package io.micronaut.rabbitmq.docs.rpc;

// tag::imports[]
import io.micronaut.rabbitmq.annotation.Binding;
import io.micronaut.rabbitmq.annotation.RabbitClient;
import io.micronaut.rabbitmq.annotation.RabbitProperty;
import io.micronaut.context.annotation.Requires;
import io.reactivex.Single;
// end::imports[]

@Requires(property = "spec.name", value = "RpcUppercaseSpec")
// tag::clazz[]
@RabbitClient
@RabbitProperty(name = "replyTo", value = "amq.rabbitmq.reply-to") // <1>
public interface ProductClient {

    @Binding("product")
    String send(String data); // <2>

    @Binding("product")
    Single<String> sendReactive(String data); // <3>
}
// end::clazz[]