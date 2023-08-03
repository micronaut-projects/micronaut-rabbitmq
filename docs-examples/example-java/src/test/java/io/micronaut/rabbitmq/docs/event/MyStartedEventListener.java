package io.micronaut.rabbitmq.docs.event;

import io.micronaut.context.annotation.Requires;
// tag::imports[]
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.rabbitmq.event.RabbitConsumerStarted;
import jakarta.inject.Singleton;
// end::imports[]

@Requires(property = "spec.name", value = "RabbitListenerEventsSpec")
// tag::clazz[]
@Singleton
public class MyStartedEventListener implements ApplicationEventListener<RabbitConsumerStarted> {
  @Override
  public void onApplicationEvent(RabbitConsumerStarted event) {
    System.out.println(new StringBuilder()
        .append("RabbitMQ consumer: " + event.getSource())
        .append(" (method: " + event.getMethod() + ")")
        .append(" just subscribed to: " + event.getQueue()));
  }
}
// end::clazz[]
