package io.micronaut.rabbitmq.docs.event;

import io.micronaut.context.annotation.Requires;
// tag::imports[]
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.rabbitmq.event.RabbitConsumerStarting;
import jakarta.inject.Singleton;
// end::imports[]

@Requires(property = "spec.name", value = "RabbitListenerEventsSpec")
// tag::clazz[]
@Singleton
public class MyStartingEventListener implements ApplicationEventListener<RabbitConsumerStarting> {
  @Override
  public void onApplicationEvent(RabbitConsumerStarting event) {
    System.out.println(new StringBuilder()
        .append("RabbitMQ consumer: " + event.getSource())
        .append(" (method: " + event.getMethod() + ")")
        .append(" is subscribing to: " + event.getQueue()));
  }
}
// end::clazz[]
