package io.micronaut.rabbitmq.rpc

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import io.micronaut.context.annotation.Requires
import io.micronaut.core.annotation.Nullable
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener

@Requires(property = "spec.name", value = "RpcSpec")
@RabbitListener
class RpcConsumer {

    @Queue("rpc")
    void echo(@Nullable String data, Channel channel, String replyTo) {
        AMQP.BasicProperties replyProps = new AMQP.BasicProperties.Builder().build()
        channel.basicPublish("", replyTo, replyProps, data?.toUpperCase()?.bytes)
    }
}
