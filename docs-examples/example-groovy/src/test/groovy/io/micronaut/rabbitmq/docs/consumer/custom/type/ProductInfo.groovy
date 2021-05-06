package io.micronaut.rabbitmq.docs.consumer.custom.type

// tag::clazz[]
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable

import javax.annotation.concurrent.Immutable

@Immutable
class ProductInfo {

    private String size
    private Long count
    private Boolean sealed

    ProductInfo(@Nullable String size, // <1>
                @NonNull Long count, // <2>
                @NonNull Boolean sealed) { // <3>
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
