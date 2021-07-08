/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.rabbitmq.reactive;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmListener;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.annotation.Internal;
import io.micronaut.messaging.exceptions.MessagingClientException;
import io.micronaut.rabbitmq.bind.RabbitConsumerState;
import io.micronaut.rabbitmq.connect.ChannelPool;
import io.micronaut.rabbitmq.connect.RabbitConnectionFactoryConfig;
import io.micronaut.rabbitmq.exception.RabbitClientException;
import io.micronaut.rabbitmq.intercept.DefaultConsumer;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * A reactive publisher implementation that uses a single channel per publish
 * operation and returns a Reactor {@link Mono}.
 * <p>
 * This is an internal API and may change at any time
 *
 * @author James Kleeh
 * @author Iván López
 * @since 3.0.0
 */
@Internal
@EachBean(ChannelPool.class)
public class ReactorReactivePublisher implements ReactivePublisher {

    private final ChannelPool channelPool;
    private final RabbitConnectionFactoryConfig config;

    /**
     * Default constructor.
     *
     * @param channelPool The channel pool to retrieve channels
     * @param config      Any configuration used in building the publishers
     */
    public ReactorReactivePublisher(@Parameter ChannelPool channelPool,
                                    @Parameter RabbitConnectionFactoryConfig config) {
        this.channelPool = channelPool;
        this.config = config;
    }

    @Override
    public Mono<Void> publishAndConfirm(RabbitPublishState publishState) {
        return Mono.from(getChannel()
                .flatMap(this::initializePublish)
                .flatMap(channel -> publishInternal(channel, publishState))
                .timeout(config.getConfirmTimeout(),
                        Mono.error(new RabbitClientException(String.format("Timed out waiting for publisher confirm for exchange: [%s] and routing key: [%s]", publishState.getExchange(), publishState.getRoutingKey())))
                )
                .then());
//                .toFlowable();
    }

    @Override
    public Mono<Void> publish(RabbitPublishState publishState) {
        return getChannel()
                .flatMap(channel -> publishInternalNoConfirm(channel, publishState))
                .then();
    }

    @Override
    public Flux<RabbitConsumerState> publishAndReply(RabbitPublishState publishState) {
        Flux<RabbitConsumerState> flowable = getChannel()
                .flatMap(channel -> publishRpcInternal(channel, publishState))
                .flux();

        config.getRpc()
                .getTimeout()
                .ifPresent(flowable::timeout);

        return flowable;
    }

    /**
     * Creates a {@link Mono} from a channel, emitting an error
     * if the channel could not be retrieved.
     *
     * @return A {@link Mono} that emits the channel on success
     */
    protected Mono<Channel> getChannel() {
        return Mono.create(emitter -> {
            try {
                Channel channel = channelPool.getChannel();
                emitter.success(channel);
            } catch (IOException e) {
                emitter.error(new RabbitClientException("Failed to retrieve a channel from the pool", e));
            }
        });
    }

    /**
     * Publishes the message to the channel. The {@link Mono} returned from this
     * method should ensure the {@link ConfirmListener} is added to the channel prior
     * to publishing and the {@link ConfirmListener} is removed from the channel after
     * the publish has been acknowledged.
     *
     * @param channel      The channel to publish the message to
     * @param publishState The publishing state
     * @return A completable that terminates when the publish has been acknowledged
     * @see Channel#basicPublish(String, String, AMQP.BasicProperties, byte[])
     */
    protected Mono<Object> publishInternal(Channel channel, RabbitPublishState publishState) {
        return Mono.create(subscriber -> {
            Disposable listener = createListener(channel, subscriber, publishState);
            try {
                channel.basicPublish(
                        publishState.getExchange(),
                        publishState.getRoutingKey(),
                        publishState.getProperties(),
                        publishState.getBody()
                );
            } catch (IOException e) {
                listener.dispose();
                subscriber.error(e);
            }
        }).doFinally(signalType -> returnChannel(channel));
    }

    /**
     * Publishes the message to the channel. The {@link Mono} returned from this
     * method should ensure the {@link ConfirmListener} is added to the channel prior
     * to publishing and the {@link ConfirmListener} is removed from the channel after
     * the publish has been acknowledged.
     *
     * @param channel      The channel to publish the message to
     * @param publishState The publishing state
     * @return A completable that terminates when the publish has been acknowledged
     * @see Channel#basicPublish(String, String, AMQP.BasicProperties, byte[])
     */
    protected Mono<RabbitConsumerState> publishRpcInternal(Channel channel, RabbitPublishState publishState) {
        return Mono.<RabbitConsumerState>create(subscriber -> {
            Disposable listener = null;
            try {
                String correlationId = UUID.randomUUID().toString();
                AMQP.BasicProperties properties = publishState.getProperties().builder().correlationId(correlationId).build();
                listener = createConsumer(channel, publishState, correlationId, subscriber);

                channel.basicPublish(
                        publishState.getExchange(),
                        publishState.getRoutingKey(),
                        properties,
                        publishState.getBody()
                );
            } catch (IOException e) {
                if (listener != null) {
                    listener.dispose();
                }
                subscriber.error(new MessagingClientException("Failed to publish the message", e));
            }
        }).doFinally(signalType -> returnChannel(channel));
    }

    /**
     * Publishes the message to the channel. The {@link Mono} returned from this
     * method should ensure the {@link ConfirmListener} is added to the channel prior
     * to publishing and the {@link ConfirmListener} is removed from the channel after
     * the publish has been acknowledged.
     *
     * @param channel      The channel to publish the message to
     * @param publishState The publishing state
     * @return A completable that terminates when the publish has been acknowledged
     * @see Channel#basicPublish(String, String, AMQP.BasicProperties, byte[])
     */
    protected Mono<Object> publishInternalNoConfirm(Channel channel, RabbitPublishState publishState) {
        return Mono.create(subscriber -> {
            try {
                channel.basicPublish(
                        publishState.getExchange(),
                        publishState.getRoutingKey(),
                        publishState.getProperties(),
                        publishState.getBody()
                );

                subscriber.success();
            } catch (IOException e) {
                subscriber.error(new MessagingClientException("Failed to publish the message", e));
            }
        }).doFinally(signalType -> returnChannel(channel));
    }

    /**
     * Initializes the channel to allow publisher acknowledgements.
     *
     * @param channel The channel to enable acknowledgements.
     * @return A {@link Mono} that will complete according to the
     * success of the operation.
     */
    protected Mono<Channel> initializePublish(Channel channel) {
        return Mono.create(emitter -> {
            try {
                channel.confirmSelect();
                emitter.success(channel);
            } catch (IOException e) {
                emitter.error(new MessagingClientException("Failed to enable publisher confirms on the channel", e));
            }
        });
    }

    /**
     * Removes confirm listeners from the channel and returns the
     * channel to the pool.
     *
     * @param channel The channel to clean and return
     */
    protected void returnChannel(Channel channel) {
        channelPool.returnChannel(channel);
    }

    /**
     * Listens for ack/nack from the broker. The listener auto disposes
     * itself after receiving a response from the broker. If no response is
     * received, the caller is responsible for disposing the listener.
     *
     * @param channel      The channel to listen for confirms
     * @param emitter      The emitter to send the event
     * @param publishState The publishing state
     * @return A disposable to allow cleanup of the listener
     */
    protected Disposable createListener(Channel channel, MonoSink<Object> emitter, RabbitPublishState publishState) {
        AtomicBoolean disposed = new AtomicBoolean();
        Consumer<ConfirmListener> dispose = (listener) -> {
            if (disposed.compareAndSet(false, true)) {
                channel.removeConfirmListener(listener);
            }
        };

        ConfirmListener confirmListener = new ConfirmListener() {
            @Override
            public void handleAck(long deliveryTag, boolean multiple) {
                ackNack(deliveryTag, multiple, true);
            }

            @Override
            public void handleNack(long deliveryTag, boolean multiple) {
                ackNack(deliveryTag, multiple, false);
            }

            private void ackNack(long deliveryTag, boolean multiple, boolean ack) {
                if (ack) {
                    emitter.success();
                } else {
                    emitter.error(new RabbitClientException("Message could not be delivered to the broker", Collections.singletonList(publishState)));
                }
                dispose.accept(this);
            }
        };

        channel.addConfirmListener(confirmListener);

        return new Disposable() {
            @Override
            public void dispose() {
                dispose.accept(confirmListener);
            }

            @Override
            public boolean isDisposed() {
                return disposed.get();
            }
        };
    }

    /**
     * Listens for ack/nack from the broker. The listener auto disposes
     * itself after receiving a response from the broker. If no response is
     * received, the caller is responsible for disposing the listener.
     *
     * @param channel       The channel to listen for confirms
     * @param publishState  The publish state
     * @param correlationId The correlation id
     * @param emitter       The emitter to send the response
     * @return A disposable to allow cleanup of the listener
     * @throws IOException If an error occurred subscribing
     */
    protected Disposable createConsumer(Channel channel, RabbitPublishState publishState, String correlationId, MonoSink<RabbitConsumerState> emitter) throws IOException {
        String replyTo = publishState.getProperties().getReplyTo();
        AtomicBoolean disposed = new AtomicBoolean();

        Consumer<String> dispose = (consumerTag) -> {
            if (disposed.compareAndSet(false, true)) {
                try {
                    channel.basicCancel(consumerTag);
                } catch (IOException e) {
                    //no-op
                }
            }
        };

        DefaultConsumer consumer = new DefaultConsumer() {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                if (replyTo.equals("amq.rabbitmq.reply-to") || correlationId.equals(properties.getCorrelationId())) {
                    dispose.accept(consumerTag);
                    emitter.success(new RabbitConsumerState(envelope, properties, body, channel));
                }
            }

            @Override
            public void handleCancel(String consumerTag) throws IOException {
                handleError(consumerTag);
            }

            @Override
            public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
                handleError(consumerTag);
            }

            private void handleError(String consumerTag) {
                dispose.accept(consumerTag);
                emitter.error(new RabbitClientException("Message was not able to be received from the reply to queue. The consumer was cancelled", Collections.singletonList(publishState)));
            }
        };

        final String consumerTag = channel.basicConsume(replyTo, true, consumer);

        return new Disposable() {
            @Override
            public void dispose() {
                dispose.accept(consumerTag);
            }

            @Override
            public boolean isDisposed() {
                return disposed.get();
            }
        };
    }
}
