package io.micronaut.rabbitmq.rpc

import io.micronaut.context.annotation.Requires
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.rabbitmq.annotation.RabbitProperty
import org.reactivestreams.Publisher

@Requires(property = "spec.name", value = "RpcSpec")
@RabbitClient
@RabbitProperty(name = "replyTo", value = "amq.rabbitmq.reply-to")
interface RpcPublisher {

    @Binding("rpc")
    Publisher<String> rpcCall(String data)

    @Binding("rpc")
    String rpcBlocking(String data)
}
