package io.micronaut.rabbitmq.docs.consumer.custom.type;

// tag::clazz[]
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;

public class ProductInfo {

    private String size;
    private Long count;
    private Boolean sealed;

    public ProductInfo(@Nullable String size, // <1>
                       @NonNull Long count, // <2>
                       @NonNull Boolean sealed) { // <3>
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
