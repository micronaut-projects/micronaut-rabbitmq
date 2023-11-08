package io.micronaut.rabbitmq.docs.parameters

import io.micronaut.context.annotation.Requires
// tag::imports[]
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.ReturnListener
import io.micronaut.rabbitmq.connect.ChannelInitializer
import jakarta.inject.Singleton
import java.io.IOException
// end::imports[]

@Requires(property = "spec.name", value = "MandatorySpec")
// tag::clazz[]
@Singleton
internal class MyReturnListener : ChannelInitializer(), ReturnListener {

    @Throws(IOException::class)
    override fun initialize(channel: Channel, name: String) {
        channel.addReturnListener(this) // <1>
    }

    @Throws(IOException::class)
    override fun handleReturn(
        replyCode: Int,
        replyText: String,
        exchange: String,
        routingKey: String,
        properties: AMQP.BasicProperties,
        body: ByteArray
    ) {
        // <2>
    }
}
// end::clazz[]
