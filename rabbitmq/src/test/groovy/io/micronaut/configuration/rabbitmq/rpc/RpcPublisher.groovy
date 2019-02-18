package io.micronaut.configuration.rabbitmq.rpc;

import io.micronaut.configuration.rabbitmq.annotation.RabbitClient
import io.micronaut.configuration.rabbitmq.annotation.RabbitProperty
import io.micronaut.context.annotation.Requires
import io.reactivex.Flowable
import io.micronaut.configuration.rabbitmq.annotation.Binding

@Requires(property = "spec.name", value = "RpcSpec")
@RabbitClient
@RabbitProperty(name = "replyTo", value = "amq.rabbitmq.reply-to")
interface RpcPublisher {

    @Binding("rpc")
    Flowable<String> rpcCall(String data)

    @Binding("rpc")
    String rpcBlocking(String data)
}
