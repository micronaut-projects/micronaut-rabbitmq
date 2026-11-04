package io.micronaut.rabbitmq.rpc

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import io.micronaut.context.annotation.Requires
import io.micronaut.json.JsonMapper
import org.jspecify.annotations.Nullable
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener

@Requires(property = "spec.name", value = "RpcSpec")
@RabbitListener
class RpcConsumer {

    private final JsonMapper jsonMapper

    RpcConsumer(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper
    }

    @Queue("rpc")
    void echo(@Nullable String data, Channel channel, String replyTo) {
        AMQP.BasicProperties replyProps = new AMQP.BasicProperties.Builder().build()
        channel.basicPublish("", replyTo, replyProps, data?.toUpperCase()?.bytes)
    }

    @Queue("rpc-list")
    void list(String data, Channel channel, String replyTo) {
        List<RpcSpec.Person> people = [new RpcSpec.Person(name: data), new RpcSpec.Person(name: "second")]
        AMQP.BasicProperties replyProps = new AMQP.BasicProperties.Builder().contentType("application/json").build()
        channel.basicPublish("", replyTo, replyProps, jsonMapper.writeValueAsBytes(people))
    }
}
