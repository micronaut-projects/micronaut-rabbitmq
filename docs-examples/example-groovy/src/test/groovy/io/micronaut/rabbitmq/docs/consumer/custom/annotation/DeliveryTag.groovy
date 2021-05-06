package io.micronaut.rabbitmq.docs.consumer.custom.annotation

// tag::imports[]
import io.micronaut.core.bind.annotation.Bindable

import java.lang.annotation.Documented
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target
// end::imports[]

// tag::clazz[]
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.PARAMETER])
@Bindable // <1>
@interface DeliveryTag {
}
// end::clazz[]
