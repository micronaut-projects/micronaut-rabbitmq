package io.micronaut.rabbitmq.docs.serdes;

import io.micronaut.context.annotation.Requires;
// tag::imports[]
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;
import io.micronaut.rabbitmq.bind.RabbitConsumerState;
import io.micronaut.rabbitmq.intercept.MutableBasicProperties;
import io.micronaut.rabbitmq.serdes.RabbitMessageSerDes;
import jakarta.inject.Singleton;

import java.nio.charset.Charset;
import java.util.Optional;
// end::imports[]

@Requires(property = "spec.name", value = "ProductInfoSerDesSpec")
// tag::clazz[]
@Singleton // <1>
public class ProductInfoSerDes implements RabbitMessageSerDes<ProductInfo> { // <2>

    private static final Charset CHARSET = Charset.forName("UTF-8");

    private final ConversionService<?> conversionService;

    public ProductInfoSerDes(ConversionService<?> conversionService) { // <3>
        this.conversionService = conversionService;
    }

    @Override
    public ProductInfo deserialize(RabbitConsumerState consumerState, Argument<ProductInfo> argument) { // <4>
        String body = new String(consumerState.getBody(), CHARSET);
        String[] parts = body.split("\\|");
        if (parts.length == 3) {
            String size = parts[0];
            if (size.equals("null")) {
                size = null;
            }

            Optional<Long> count = conversionService.convert(parts[1], Long.class);
            Optional<Boolean> sealed = conversionService.convert(parts[2], Boolean.class);

            if (count.isPresent() && sealed.isPresent()) {
                return new ProductInfo(size, count.get(), sealed.get());
            }
        }
        return null;
    }

    @Override
    public byte[] serialize(ProductInfo data, MutableBasicProperties properties) { // <5>
        if (data == null) {
            return null;
        }
        return (data.getSize() + "|" + data.getCount() + "|" + data.getSealed()).getBytes(CHARSET);
    }

    @Override
    public boolean supports(Argument<ProductInfo> argument) { // <6>
        return argument.getType().isAssignableFrom(ProductInfo.class);
    }
}
// end::clazz[]
