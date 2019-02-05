package io.micronaut.configuration.rabbitmq.docs.consumer.custom.type;

// tag::clazz[]
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public class ProductInfo {

    private String size;
    private Long count;
    private Boolean sealed;

    public ProductInfo(@Nullable String size, // <1>
                       @Nonnull Long count, // <2>
                       @Nonnull Boolean sealed) { // <3>
        this.size = size;
        this.count = count;
        this.sealed = sealed;
    }

    public String getSize() {
        return size;
    }

    public Long getCount() {
        return count;
    }

    public Boolean getSealed() {
        return sealed;
    }
}
// end::clazz[]