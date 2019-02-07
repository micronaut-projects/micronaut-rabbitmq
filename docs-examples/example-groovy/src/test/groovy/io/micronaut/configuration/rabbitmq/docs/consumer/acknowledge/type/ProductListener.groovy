package io.micronaut.configuration.rabbitmq.docs.consumer.acknowledge.type

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.Queue
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener
import io.micronaut.configuration.rabbitmq.bind.RabbitAcknowledgement
import io.micronaut.context.annotation.Requires

import java.util.concurrent.atomic.AtomicInteger
// end::imports[]

@Requires(property = "spec.name", value = "AcknowledgeSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    AtomicInteger messageCount = new AtomicInteger()

    @Queue(value = "product") // <1>
    void receive(byte[] data, RabbitAcknowledgement acknowledgement) { // <2>
        int count = messageCount.getAndUpdate({ intValue -> ++intValue })
        println new String(data)
        if (count  == 0) {
            acknowledgement.nack(false, true) // <3>
        } else if (count > 3) {
            acknowledgement.ack(true) // <4>
        }
    }
}
// end::clazz[]
