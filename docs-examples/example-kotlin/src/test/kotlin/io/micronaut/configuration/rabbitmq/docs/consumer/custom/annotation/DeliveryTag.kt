package io.micronaut.configuration.rabbitmq.docs.consumer.custom.annotation

// tag::imports[]
import io.micronaut.core.bind.annotation.Bindable
import java.lang.annotation.Documented
// end::imports[]

// tag::clazz[]
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
@Bindable // <1>
annotation class DeliveryTag
// end::clazz[]