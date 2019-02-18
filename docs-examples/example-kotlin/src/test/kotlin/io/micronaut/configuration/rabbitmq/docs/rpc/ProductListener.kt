package io.micronaut.configuration.rabbitmq.docs.rpc

// tag::imports[]
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import io.micronaut.configuration.rabbitmq.annotation.Queue
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener
import io.micronaut.configuration.rabbitmq.annotation.RabbitProperty
import io.micronaut.context.annotation.Requires

import java.io.IOException
// end::imports[]

@Requires(property = "spec.name", value = "RpcUppercaseSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    @Queue("product")
    @Throws(IOException::class)
    fun toUpperCase(data: String,
                    @RabbitProperty replyTo: String, // <1>
                    channel: Channel) { // <2>
        val replyProps = AMQP.BasicProperties.Builder().build()
        channel.basicPublish("", replyTo, replyProps, data.toUpperCase().toByteArray()) // <3>
    }
}
// end::clazz[]
