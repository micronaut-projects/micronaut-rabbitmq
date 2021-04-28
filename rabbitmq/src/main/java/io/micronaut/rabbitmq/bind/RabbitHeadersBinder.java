package io.micronaut.rabbitmq.bind;

import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.rabbitmq.annotation.RabbitHeaders;

import javax.inject.Singleton;

@Singleton
public class RabbitHeadersBinder implements RabbitAnnotatedArgumentBinder<RabbitHeaders> {

    private final ConversionService<?> conversionService;

    /**
     * Default constructor.
     *
     * @param conversionService The conversion service to convert the body
     */
    public RabbitHeadersBinder(ConversionService<?> conversionService) {
        this.conversionService = conversionService;
    }


    @Override
    public Class<RabbitHeaders> getAnnotationType() {
        return RabbitHeaders.class;
    }

    @Override
    public BindingResult<Object> bind(ArgumentConversionContext<Object> context, RabbitConsumerState source) {
        return () -> conversionService.convert(source.getProperties().getHeaders(), context);
    }
}
