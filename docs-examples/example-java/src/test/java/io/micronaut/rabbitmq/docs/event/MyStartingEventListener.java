package io.micronaut.rabbitmq.docs.event;

import io.micronaut.context.annotation.Requires;
// tag::imports[]
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.rabbitmq.event.RabbitConsumerStarting;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// end::imports[]

@Requires(property = "spec.name", value = "RabbitListenerEventsSpec")
// tag::clazz[]
@Singleton
public class MyStartingEventListener implements ApplicationEventListener<RabbitConsumerStarting> {
  private static final Logger LOG = LoggerFactory.getLogger(MyStartingEventListener.class);
  @Override
  public void onApplicationEvent(RabbitConsumerStarting event) {
    LOG.info("RabbitMQ consumer: {} (method: {}) is subscribing to: {}",
      event.getSource(), event.getMethod(), event.getQueue());
  }
}
// end::clazz[]
