package io.micronaut.rabbitmq.docs.consumer.custom.type

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.core.convert.ArgumentConversionContext
import io.micronaut.core.convert.ConversionError
import io.micronaut.core.convert.ConversionService
import io.micronaut.core.type.Argument
import io.micronaut.rabbitmq.bind.RabbitConsumerState
import io.micronaut.rabbitmq.bind.RabbitHeaderConvertibleValues
import io.micronaut.rabbitmq.bind.RabbitTypeArgumentBinder

import javax.inject.Singleton
// end::imports[]

@Requires(property = "spec.name", value = "ProductInfoSpec")
// tag::clazz[]
@Singleton // <1>
class ProductInfoTypeBinder implements RabbitTypeArgumentBinder<ProductInfo> { //<2>

    private final ConversionService conversionService

    ProductInfoTypeBinder(ConversionService conversionService) { //<3>
        this.conversionService = conversionService
    }

    @Override
    Argument<ProductInfo> argumentType() {
        return Argument.of(ProductInfo)
    }

    @Override
    BindingResult<ProductInfo> bind(ArgumentConversionContext<ProductInfo> context, RabbitConsumerState source) {
        Map<String, Object> rawHeaders = source.properties.headers //<4>

        if (rawHeaders == null) {
            return BindingResult.EMPTY
        }

        def headers = new RabbitHeaderConvertibleValues(rawHeaders, conversionService)

        String size = headers.get("productSize", String).orElse(null)  //<5>
        Optional<Long> count = headers.get("x-product-count", Long) //<6>
        Optional<Boolean> sealed = headers.get("x-product-sealed", Boolean) // <7>

        if (headers.getConversionErrors().isEmpty() && count.isPresent() && sealed.isPresent()) {
            { -> Optional.of(new ProductInfo(size, count.get(), sealed.get())) } //<8>
        } else {
            new BindingResult<ProductInfo>() {
                @Override
                Optional<ProductInfo> getValue() {
                    Optional.empty()
                }

                @Override
                List<ConversionError> getConversionErrors() {
                    headers.conversionErrors //<9>
                }
            }
        }
    }
}
// end::clazz[]
