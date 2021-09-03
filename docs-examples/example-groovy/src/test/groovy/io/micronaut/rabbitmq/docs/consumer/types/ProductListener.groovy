package io.micronaut.rabbitmq.docs.consumer.types

import io.micronaut.context.annotation.Requires
// tag::imports[]
import com.rabbitmq.client.BasicProperties
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener

import java.util.concurrent.CopyOnWriteArrayList
// end::imports[]

@Requires(property = "spec.name", value = "TypeBindingSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    CopyOnWriteArrayList<String> messages = []

    @Queue("product")
    void receive(byte[] data,
                 Envelope envelope, // <1>
                 BasicProperties basicProperties, // <2>
                 Channel channel) { // <3>
        messages << "exchange: [$envelope.exchange], routingKey: [$envelope.routingKey], contentType: [$basicProperties.contentType]".toString()
    }
}
// end::clazz[]
