package io.micronaut.configuration.rabbitmq.docs.consumer.custom.annotation

// tag::imports[]
import io.micronaut.configuration.rabbitmq.bind.RabbitAnnotatedArgumentBinder
import io.micronaut.configuration.rabbitmq.bind.RabbitMessageState
import io.micronaut.context.annotation.Requires
import io.micronaut.core.bind.ArgumentBinder
import io.micronaut.core.convert.ArgumentConversionContext
import io.micronaut.core.convert.ConversionService

import javax.inject.Singleton
// end::imports[]

@Requires(property = "spec.name", value = "DeliveryTagSpec")
// tag::clazz[]
@Singleton // <1>
class DeliveryTagAnnotationBinder(private val conversionService: ConversionService<*>)// <3>
    : RabbitAnnotatedArgumentBinder<DeliveryTag> { // <2>

    override fun getAnnotationType(): Class<DeliveryTag> {
        return DeliveryTag::class.java
    }

    override fun bind(context: ArgumentConversionContext<Any>, source: RabbitMessageState): ArgumentBinder.BindingResult<Any> {
        val deliveryTag = source.envelope.deliveryTag // <4>
        return ArgumentBinder.BindingResult { conversionService.convert(deliveryTag, context) } // <5>
    }
}
// end::clazz[]