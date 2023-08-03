package io.micronaut.rabbitmq.docs.event

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.rabbitmq.event.RabbitConsumerStarting
import jakarta.inject.Singleton
// end::imports[]

@Requires(property = "spec.name", value = "RabbitListenerEventsSpec")
// tag::clazz[]
@Singleton
class MyStartingEventListener implements ApplicationEventListener<RabbitConsumerStarting> {
  @Override
  void onApplicationEvent(RabbitConsumerStarting event) {
    System.out.println("RabbitMQ consumer: ${event.source} (method: ${event.method}) is subscribing to: ${event.queue}")
  }
}
// end::clazz[]
