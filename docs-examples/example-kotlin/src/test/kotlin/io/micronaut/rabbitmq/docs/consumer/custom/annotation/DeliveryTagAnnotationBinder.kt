package io.micronaut.rabbitmq.docs.consumer.custom.annotation

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.core.bind.ArgumentBinder
import io.micronaut.core.convert.ArgumentConversionContext
import io.micronaut.core.convert.ConversionService
import io.micronaut.rabbitmq.bind.RabbitAnnotatedArgumentBinder
import io.micronaut.rabbitmq.bind.RabbitConsumerState
import jakarta.inject.Singleton
// end::imports[]

@Requires(property = "spec.name", value = "DeliveryTagSpec")
// tag::clazz[]
@Singleton // <1>
class DeliveryTagAnnotationBinder(private val conversionService: ConversionService)// <3>
    : RabbitAnnotatedArgumentBinder<DeliveryTag> { // <2>

    override fun getAnnotationType(): Class<DeliveryTag> {
        return DeliveryTag::class.java
    }

    override fun bind(context: ArgumentConversionContext<Any>, source: RabbitConsumerState): ArgumentBinder.BindingResult<Any> {
        val deliveryTag = source.envelope.deliveryTag // <4>
        return ArgumentBinder.BindingResult { conversionService.convert(deliveryTag, context) } // <5>
    }
}
// end::clazz[]
