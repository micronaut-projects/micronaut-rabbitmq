package io.micronaut.rabbitmq.docs.consumer.custom.type;

// tag::imports[]
import io.micronaut.rabbitmq.bind.RabbitHeaderConvertibleValues;
import io.micronaut.rabbitmq.bind.RabbitConsumerState;
import io.micronaut.rabbitmq.bind.RabbitTypeArgumentBinder;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionError;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;

import javax.inject.Singleton;
import java.util.*;
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
    public BindingResult<ProductInfo> bind(ArgumentConversionContext<ProductInfo> context, RabbitConsumerState source) {
        Map<String, Object> rawHeaders = source.getProperties().getHeaders(); //<4>

        if (rawHeaders == null) {
            return BindingResult.EMPTY;
        }

        RabbitHeaderConvertibleValues headers = new RabbitHeaderConvertibleValues(rawHeaders, conversionService);

        String size = headers.get("productSize", String.class).orElse(null);  //<5>
        Optional<Long> count = headers.get("x-product-count", Long.class); //<6>
        Optional<Boolean> sealed = headers.get("x-product-sealed", Boolean.class); // <7>

        if (headers.getConversionErrors().isEmpty() && count.isPresent() && sealed.isPresent()) {
            return () -> Optional.of(new ProductInfo(size, count.get(), sealed.get())); //<8>
        } else {
            return new BindingResult<ProductInfo>() {
                @Override
                public Optional<ProductInfo> getValue() {
                    return Optional.empty();
                }

                @Override
                public List<ConversionError> getConversionErrors() {
                    return headers.getConversionErrors(); //<9>
                }
            };
        }
    }
}
// end::clazz[]