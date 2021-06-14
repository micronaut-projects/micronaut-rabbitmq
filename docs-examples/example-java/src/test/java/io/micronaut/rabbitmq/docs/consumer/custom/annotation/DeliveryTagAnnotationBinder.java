package io.micronaut.rabbitmq.docs.consumer.custom.annotation;

import io.micronaut.context.annotation.Requires;
// tag::imports[]
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.rabbitmq.bind.RabbitAnnotatedArgumentBinder;
import io.micronaut.rabbitmq.bind.RabbitConsumerState;
import jakarta.inject.Singleton;
// end::imports[]

@Requires(property = "spec.name", value = "DeliveryTagSpec")
// tag::clazz[]
@Singleton // <1>
public class DeliveryTagAnnotationBinder implements RabbitAnnotatedArgumentBinder<DeliveryTag> { // <2>

    private final ConversionService<?> conversionService;

    public DeliveryTagAnnotationBinder(ConversionService<?> conversionService) { // <3>
        this.conversionService = conversionService;
    }

    @Override
    public Class<DeliveryTag> getAnnotationType() {
        return DeliveryTag.class;
    }

    @Override
    public BindingResult<Object> bind(ArgumentConversionContext<Object> context, RabbitConsumerState source) {
        Long deliveryTag = source.getEnvelope().getDeliveryTag(); // <4>
        return () -> conversionService.convert(deliveryTag, context); // <5>
    }
}
// end::clazz[]
