package io.micronaut.configuration.rabbitmq.docs.consumer.custom.annotation

import io.micronaut.configuration.rabbitmq.bind.RabbitConsumerState

// tag::imports[]
import io.micronaut.configuration.rabbitmq.bind.RabbitAnnotatedArgumentBinder
import io.micronaut.context.annotation.Requires
import io.micronaut.core.convert.ArgumentConversionContext
import io.micronaut.core.convert.ConversionService

import javax.inject.Singleton
// end::imports[]

@Requires(property = "spec.name", value = "DeliveryTagSpec")
// tag::clazz[]
@Singleton // <1>
class DeliveryTagAnnotationBinder implements RabbitAnnotatedArgumentBinder<DeliveryTag> { // <2>

    private final ConversionService conversionService;

    DeliveryTagAnnotationBinder(ConversionService conversionService) { // <3>
        this.conversionService = conversionService
    }

    @Override
    Class<DeliveryTag> getAnnotationType() {
        DeliveryTag
    }

    @Override
    BindingResult<Object> bind(ArgumentConversionContext<Object> context, RabbitConsumerState source) {
        Long deliveryTag = source.envelope.deliveryTag // <4>
        return { -> conversionService.convert(deliveryTag, context) } // <5>
    }
}
// end::clazz[]