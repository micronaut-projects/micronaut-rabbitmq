package io.micronaut.rabbitmq.rpc;

import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.context.annotation.Requires
import io.micronaut.rabbitmq.annotation.RabbitProperty
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single

@Requires(property = "spec.name", value = "RpcSpec")
@RabbitClient
@RabbitProperty(name = "replyTo", value = "amq.rabbitmq.reply-to")
interface RpcPublisher {

    @io.micronaut.rabbitmq.annotation.Binding("rpc")
    Flowable<String> rpcCall(String data)

    @io.micronaut.rabbitmq.annotation.Binding("rpc")
    Maybe<String> rpcCallMaybe(String data)

    @io.micronaut.rabbitmq.annotation.Binding("rpc")
    Single<String> rpcCallSingle(String data)

    @io.micronaut.rabbitmq.annotation.Binding("rpc")
    String rpcBlocking(String data)
}
