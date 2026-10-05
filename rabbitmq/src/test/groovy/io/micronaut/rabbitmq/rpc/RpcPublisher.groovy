package io.micronaut.rabbitmq.rpc

import io.micronaut.context.annotation.Requires
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.rabbitmq.annotation.RabbitProperty
import org.reactivestreams.Publisher

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

@Requires(property = "spec.name", value = "RpcSpec")
@RabbitClient
@RabbitProperty(name = "replyTo", value = "amq.rabbitmq.reply-to")
interface RpcPublisher {

    @Binding("rpc")
    Publisher<String> rpcCall(String data)

    @Binding("rpc")
    String rpcBlocking(String data)

    @Binding("rpc-list")
    Publisher<List<RpcSpec.Person>> rpcList(String data)

    @Binding("rpc-list")
    List<RpcSpec.Person> rpcListBlocking(String data)

    @Binding("rpc-list")
    CompletionStage<List<RpcSpec.Person>> rpcListAsync(String data)

    @Binding("rpc-list")
    CompletableFuture<List<RpcSpec.Person>> rpcListFuture(String data)
}
