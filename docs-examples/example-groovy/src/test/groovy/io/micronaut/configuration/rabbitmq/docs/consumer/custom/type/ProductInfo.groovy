package io.micronaut.configuration.rabbitmq.docs.consumer.custom.type;

// tag::clazz[]
import javax.annotation.Nonnull
import javax.annotation.Nullable
import javax.annotation.concurrent.Immutable

@Immutable
class ProductInfo {

    private String size
    private Long count
    private Boolean sealed

    ProductInfo(@Nullable String size, // <1>
                @Nonnull Long count, // <2>
                @Nonnull Boolean sealed) { // <3>
        this.size = size
        this.count = count
        this.sealed = sealed
    }

    String getSize() {
        size
    }

    Long getCount() {
        count
    }

    Boolean getSealed() {
        sealed
    }
}
// end::clazz[]