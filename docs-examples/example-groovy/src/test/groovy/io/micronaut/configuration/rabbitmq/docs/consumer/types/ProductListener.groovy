package io.micronaut.configuration.rabbitmq.docs.consumer.types

// tag::imports[]
import com.rabbitmq.client.BasicProperties
import com.rabbitmq.client.Envelope
import io.micronaut.configuration.rabbitmq.annotation.Queue
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener
import io.micronaut.context.annotation.Requires
// end::imports[]

@Requires(property = "spec.name", value = "TypeBindingSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    List<String> messages = Collections.synchronizedList([])

    @Queue("product")
    void receive(byte[] data,
                 Envelope envelope, // <1>
                 BasicProperties basicProperties) { // <2>
        messages.add("exchange: [${envelope.exchange}], routingKey: [${envelope.routingKey}], contentType: [${basicProperties.contentType}]".toString())
    }
}
// end::clazz[]
