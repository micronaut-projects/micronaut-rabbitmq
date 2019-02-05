package io.micronaut.configuration.rabbitmq.docs.publisher.acknowledge;

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.Binding;
import io.micronaut.configuration.rabbitmq.annotation.RabbitClient;
import io.micronaut.context.annotation.Requires;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
// end::imports[]

@Requires(property = "spec.name", value = "PublisherAcknowledgeSpec")
// tag::clazz[]
@RabbitClient
public interface ProductClient {

    @Binding("product")
    Completable send(byte[] data); // <1>

    @Binding("product")
    Maybe<Void> sendMaybe(byte[] data); // <2>

    @Binding("product")
    Mono<Void> sendMono(byte[] data); // <3>

    @Binding("product")
    Publisher<Void> sendPublisher(byte[] data); // <4>
}
// end::clazz[]