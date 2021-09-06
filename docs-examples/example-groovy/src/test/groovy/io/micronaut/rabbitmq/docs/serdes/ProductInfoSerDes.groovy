package io.micronaut.rabbitmq.docs.serdes

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.core.convert.ConversionService
import io.micronaut.core.type.Argument
import io.micronaut.rabbitmq.bind.RabbitConsumerState
import io.micronaut.rabbitmq.intercept.MutableBasicProperties
import io.micronaut.rabbitmq.serdes.RabbitMessageSerDes
import jakarta.inject.Singleton
import java.nio.charset.Charset
// end::imports[]

@Requires(property = "spec.name", value = "ProductInfoSerDesSpec")
// tag::clazz[]
@Singleton // <1>
class ProductInfoSerDes implements RabbitMessageSerDes<ProductInfo> { // <2>

    private static final Charset UTF8 = Charset.forName("UTF-8")

    private final ConversionService conversionService

    ProductInfoSerDes(ConversionService conversionService) { // <3>
        this.conversionService = conversionService
    }

    @Override
    ProductInfo deserialize(RabbitConsumerState consumerState, Argument<ProductInfo> argument) { // <4>
        String body = new String(consumerState.body, UTF8)
        String[] parts = body.split("\\|")
        if (parts.length == 3) {
            String size = parts[0]
            if (size == "null") {
                size = null
            }

            Optional<Long> count = conversionService.convert(parts[1], Long)
            Optional<Boolean> sealed = conversionService.convert(parts[2], Boolean)

            if (count.isPresent() && sealed.isPresent()) {
                return new ProductInfo(size, count.get(), sealed.get())
            }
        }
        null
    }

    @Override
    byte[] serialize(ProductInfo data, MutableBasicProperties properties) { // <5>
        if (data == null) {
            return null
        }
        (data.size + "|" + data.count + "|" + data.sealed).getBytes(UTF8)
    }

    @Override
    boolean supports(Argument<ProductInfo> argument) { // <6>
        argument.type.isAssignableFrom(ProductInfo)
    }
}
// end::clazz[]
