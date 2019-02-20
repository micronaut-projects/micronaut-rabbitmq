package io.micronaut.configuration.rabbitmq.rpc;

import io.micronaut.configuration.rabbitmq.annotation.RabbitClient
import io.micronaut.configuration.rabbitmq.annotation.RabbitProperty
import io.micronaut.context.annotation.Requires
import io.reactivex.Flowable
import io.micronaut.configuration.rabbitmq.annotation.Binding
import io.reactivex.Maybe
import io.reactivex.Single

@Requires(property = "spec.name", value = "RpcSpec")
@RabbitClient
@RabbitProperty(name = "replyTo", value = "amq.rabbitmq.reply-to")
interface RpcPublisher {

    @Binding("rpc")
    Flowable<String> rpcCall(String data)

    @Binding("rpc")
    Maybe<String> rpcCallMaybe(String data)

    @Binding("rpc")
    Single<String> rpcCallSingle(String data)

    @Binding("rpc")
    String rpcBlocking(String data)
}
