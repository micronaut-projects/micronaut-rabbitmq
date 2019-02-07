package io.micronaut.configuration.rabbitmq.docs.serdes

// tag::clazz[]
import javax.annotation.concurrent.Immutable

@Immutable
class ProductInfo(val size: String?, // <1>
                  val count: Long, // <2>
                  val sealed: Boolean)// <3>
// end::clazz[]