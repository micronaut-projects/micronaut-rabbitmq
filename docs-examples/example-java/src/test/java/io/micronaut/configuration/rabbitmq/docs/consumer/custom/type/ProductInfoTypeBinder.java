package io.micronaut.configuration.rabbitmq.docs.consumer.custom.type;

// tag::imports[]
import io.micronaut.configuration.rabbitmq.bind.RabbitMessageState;
import io.micronaut.configuration.rabbitmq.bind.RabbitTypeArgumentBinder;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.ConversionError;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;

import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
// end::imports[]

@Requires(property = "spec.name", value = "ProductInfoSpec")
// tag::clazz[]
@Singleton // <1>
public class ProductInfoTypeBinder implements RabbitTypeArgumentBinder<ProductInfo> { //<2>

    private final ConversionService<?> conversionService;

    ProductInfoTypeBinder(ConversionService<?> conversionService) { //<3>
        this.conversionService = conversionService;
    }

    @Override
    public Argument<ProductInfo> argumentType() {
        return Argument.of(ProductInfo.class);
    }

    @Override
    public BindingResult<ProductInfo> bind(ArgumentConversionContext<ProductInfo> context, RabbitMessageState source) {
        Map<String, Object> headers = source.getProperties().getHeaders(); //<4>

        String size = Optional.ofNullable(headers.get("productSize"))
                .map(Object::toString)
                .flatMap((header) -> conversionService.convert(header, String.class))
                .orElse(null); //<5>

        ArgumentConversionContext<Long> countContext = ConversionContext.of(Long.class);
        Optional<Long> count = Optional.ofNullable(headers.get("x-product-count"))
                .map(Object::toString)
                .flatMap((header) -> conversionService.convert(header, countContext)); //<6>

        ArgumentConversionContext<Boolean> sealedContext = ConversionContext.of(Boolean.class);
        Optional<Boolean> sealed = Optional.ofNullable(headers.get("x-product-sealed"))
                .map(Object::toString)
                .flatMap((header) -> conversionService.convert(header, sealedContext)); //<7>

        List<ConversionError> conversionErrors = Stream.of(countContext.getLastError(), sealedContext.getLastError())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        if (conversionErrors.isEmpty() && count.isPresent() && sealed.isPresent()) {
            return () -> Optional.of(new ProductInfo(size, count.get(), sealed.get())); //<8>
        } else {
            return new BindingResult<ProductInfo>() {
                @Override
                public Optional<ProductInfo> getValue() {
                    return Optional.empty();
                }

                @Override
                public List<ConversionError> getConversionErrors() {
                    return conversionErrors; //<9>
                }
            };
        }
    }
}
// end::clazz[]