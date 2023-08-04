package io.micronaut.rabbitmq.docs.event

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.rabbitmq.event.RabbitConsumerStarted
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
// end::imports[]

@Requires(property = "spec.name", value = "RabbitListenerEventsSpec")
// tag::clazz[]
@Singleton
class MyStartedEventListener : ApplicationEventListener<RabbitConsumerStarted> {
  private val LOG = LoggerFactory.getLogger(javaClass)
  override fun onApplicationEvent(event: RabbitConsumerStarted) {
    LOG.info("RabbitMQ consumer: {} (method: {}) just subscribed to: {}",
      event.source, event.method, event.queue)
  }
}
// end::clazz[]
