package io.micronaut.rabbitmq.docs.consumer.acknowledge.type

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener
import io.micronaut.rabbitmq.bind.RabbitAcknowledgement

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
