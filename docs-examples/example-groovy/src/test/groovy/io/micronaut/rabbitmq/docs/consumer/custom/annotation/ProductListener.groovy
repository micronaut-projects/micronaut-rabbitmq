package io.micronaut.rabbitmq.docs.consumer.custom.annotation

// tag::imports[]
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener
import io.micronaut.context.annotation.Requires
// end::imports[]

@Requires(property = "spec.name", value = "DeliveryTagSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    Set<Long> messages = Collections.synchronizedSet(new HashSet<>())

    @Queue("product")
    void receive(byte[] data, @DeliveryTag Long tag) { // <1>
        messages.add(tag)
    }
}
// end::clazz[]
