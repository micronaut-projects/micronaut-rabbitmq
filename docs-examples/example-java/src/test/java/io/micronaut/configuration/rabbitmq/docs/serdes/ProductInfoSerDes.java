package io.micronaut.configuration.rabbitmq.docs.serdes;

// tag::imports[]
import io.micronaut.configuration.rabbitmq.bind.RabbitMessageState;
import io.micronaut.configuration.rabbitmq.serdes.RabbitMessageSerDes;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.convert.ConversionService;

import javax.inject.Singleton;
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
    public ProductInfo deserialize(RabbitMessageState messageState, Class<ProductInfo> type) { // <4>
        String body = new String(messageState.getBody(), CHARSET);
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
    public byte[] serialize(ProductInfo data) { // <5>
        return (data.getSize() + "|" + data.getCount() + "|" + data.getSealed()).getBytes(CHARSET);
    }

    @Override
    public boolean supports(Class<ProductInfo> type) { // <6>
        return type.isAssignableFrom(ProductInfo.class);
    }
}
// end::clazz[]