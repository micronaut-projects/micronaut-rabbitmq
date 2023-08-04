package io.micronaut.rabbitmq.docs.event

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.rabbitmq.event.RabbitConsumerStarted
import jakarta.inject.Singleton
import org.slf4j.Logger
import org.slf4j.LoggerFactory
// end::imports[]

@Requires(property = "spec.name", value = "RabbitListenerEventsSpec")
// tag::clazz[]
@Singleton
class MyStartedEventListener implements ApplicationEventListener<RabbitConsumerStarted> {
  private static final Logger LOG = LoggerFactory.getLogger(MyStartingEventListener.class)
  @Override
  void onApplicationEvent(RabbitConsumerStarted event) {
    LOG.info("RabbitMQ consumer: {} (method: {}) just subscribed to: {}",
      event.source, event.method, event.queue);
  }
}
// end::clazz[]
