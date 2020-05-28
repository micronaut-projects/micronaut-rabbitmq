package io.micronaut.rabbitmq.docs.properties

// tag::imports[]
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.rabbitmq.annotation.RabbitProperties
import io.micronaut.rabbitmq.annotation.RabbitProperty
import io.micronaut.context.annotation.Requires
// end::imports[]

@Requires(property = "spec.name", value = "PropertiesSpec")
// tag::clazz[]
@RabbitClient
@RabbitProperty(name = "appId", value = "myApp") // <1>
interface ProductClient {

    @Binding("product")
    @RabbitProperties( // <2>
            RabbitProperty(name = "contentType", value = "application/json"),
            RabbitProperty(name = "userId", value = "guest")
    )
    fun send(data: ByteArray)

    @Binding("product")
    fun send(@RabbitProperty("userId") user: String, @RabbitProperty contentType: String?, data: ByteArray)  // <3>
}
// end::clazz[]