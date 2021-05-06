package io.micronaut.rabbitmq.docs.consumer.custom.annotation

// tag::imports[]
import io.micronaut.core.bind.annotation.Bindable
// end::imports[]

// tag::clazz[]
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
@Bindable // <1>
annotation class DeliveryTag
// end::clazz[]
