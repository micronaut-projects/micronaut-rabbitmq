package io.micronaut.rabbitmq.docs.event

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.rabbitmq.event.RabbitConsumerStarted
import jakarta.inject.Singleton
// end::imports[]

@Requires(property = "spec.name", value = "RabbitListenerEventsSpec")
// tag::clazz[]
@Singleton
class MyStartedEventListener : ApplicationEventListener<RabbitConsumerStarted> {
  override fun onApplicationEvent(event: RabbitConsumerStarted) {
    println("RabbitMQ consumer: ${event.source} (method: ${event.method}) just subscribed to: ${event.queue}")
  }
}
// end::clazz[]
