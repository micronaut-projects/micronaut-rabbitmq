package io.micronaut.configuration.rabbitmq.docs.rpc

// tag::imports[]
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import io.micronaut.configuration.rabbitmq.annotation.Queue
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener
import io.micronaut.configuration.rabbitmq.annotation.RabbitProperty
import io.micronaut.context.annotation.Requires
// end::imports[]

@Requires(property = "spec.name", value = "RpcUppercaseSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    @Queue("product")
    void toUpperCase(String data,
                            @RabbitProperty String replyTo, // <1>
                            Channel channel) throws IOException { // <2>
        AMQP.BasicProperties replyProps = new AMQP.BasicProperties.Builder().build()
        channel.basicPublish("", replyTo, replyProps, data.toUpperCase().getBytes()) // <3>
    }
}
// end::clazz[]
