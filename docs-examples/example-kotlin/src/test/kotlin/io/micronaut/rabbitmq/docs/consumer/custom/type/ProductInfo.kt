package io.micronaut.rabbitmq.docs.consumer.custom.type

// tag::clazz[]
import javax.annotation.concurrent.Immutable

@Immutable
class ProductInfo(val size: String?, // <1>
                  val count: Long, // <2>
                  val sealed: Boolean)// <3>
// end::clazz[]