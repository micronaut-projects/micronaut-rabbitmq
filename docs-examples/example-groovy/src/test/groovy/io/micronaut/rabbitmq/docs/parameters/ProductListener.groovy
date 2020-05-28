package io.micronaut.rabbitmq.docs.parameters

// tag::imports[]
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener
import io.micronaut.context.annotation.Requires
// end::imports[]

@Requires(property = "spec.name", value = "BindingSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    List<Integer> messageLengths = Collections.synchronizedList([])

    @Queue("product") // <1>
    void receive(byte[] data) {
        messageLengths.add(data.length)
    }
}
// end::clazz[]