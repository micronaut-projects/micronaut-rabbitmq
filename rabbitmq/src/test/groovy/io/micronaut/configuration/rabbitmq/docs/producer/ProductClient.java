package io.micronaut.configuration.rabbitmq.docs.producer;

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.Binding;
import io.micronaut.configuration.rabbitmq.annotation.RabbitClient;
import io.micronaut.context.annotation.Requires;
import io.reactivex.Completable;
// end::imports[]

@Requires(property = "spec.name", value = "ProducerAcknowledgeSpec")
// tag::clazz[]
@RabbitClient // <1>
public interface ProductClient {

    @Binding("product") // <2>
    Completable send(byte[] data); // <3>
}
// end::clazz[]