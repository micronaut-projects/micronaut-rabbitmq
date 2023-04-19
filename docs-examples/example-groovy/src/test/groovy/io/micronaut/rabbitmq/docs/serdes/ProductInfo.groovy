package io.micronaut.rabbitmq.docs.serdes

// tag::clazz[]
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable

class ProductInfo {

    private String size
    private Long count
    private Boolean productSealed

    ProductInfo(@Nullable String size, // <1>
                @NonNull Long count, // <2>
                @NonNull Boolean productSealed) { // <3>
        this.size = size
        this.count = count
        this.productSealed = productSealed
    }

    String getSize() {
        size
    }

    Long getCount() {
        count
    }

    Boolean getSealed() {
        productSealed
    }
}
// end::clazz[]
