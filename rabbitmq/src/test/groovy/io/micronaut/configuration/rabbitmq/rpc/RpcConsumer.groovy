package io.micronaut.configuration.rabbitmq.rpc

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener
import io.micronaut.configuration.rabbitmq.annotation.Queue
import io.micronaut.context.annotation.Requires

@Requires(property = "spec.name", value = "RpcSpec")
@RabbitListener
class RpcConsumer {

    @Queue("rpc")
    void echo(String data, Channel channel, String replyTo) {
        AMQP.BasicProperties replyProps = new AMQP.BasicProperties.Builder().build()
        channel.basicPublish("", replyTo, replyProps, data.toUpperCase().bytes)
    }
}
