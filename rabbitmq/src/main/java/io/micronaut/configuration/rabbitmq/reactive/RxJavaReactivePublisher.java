/*
 * Copyright 2017-2018 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.micronaut.configuration.rabbitmq.reactive;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmListener;
import io.micronaut.configuration.rabbitmq.connect.ChannelPool;
import io.micronaut.configuration.rabbitmq.exception.RabbitClientException;
import io.micronaut.core.annotation.Internal;
import io.micronaut.messaging.exceptions.MessagingClientException;
import io.reactivex.*;
import io.reactivex.disposables.Disposable;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * A reactive publisher implementation that uses a single channel per publish
 * operation and returns an RxJava2 {@link Completable}.
 *
 * This is an internal API and may change at any time
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Singleton
@Internal
public class RxJavaReactivePublisher implements ReactivePublisher<Completable> {

    private final ChannelPool channelPool;

    /**
     * Default constructor.
     *
     * @param channelPool The channel pool to retrieve channels
     */
    public RxJavaReactivePublisher(ChannelPool channelPool) {
        this.channelPool = channelPool;
    }

    @Override
    public Completable publish(String exchange, String routingKey, AMQP.BasicProperties properties, byte[] body) {
        return getChannel()
            .flatMap(this::initializePublish)
            .flatMapCompletable(channel -> {
                RabbitPublishState publishState = new RabbitPublishState(exchange, routingKey, properties, body);
                return publishInternal(channel, publishState);
            });
    }

    /**
     * Creates a {@link Single} from a channel, emitting an error
     * if the channel could not be retrieved.
     *
     * @return A {@link Single} that emits the channel on success
     */
    protected Single<Channel> getChannel() {
        return Single.create(emitter -> {
            try {
                Channel channel = channelPool.getChannel();
                emitter.onSuccess(channel);
            } catch (IOException e) {
                emitter.onError(new RabbitClientException("Failed to retrieve a channel from the pool", e));
            }
        });
    }

    /**
     * Publishes the message to the channel. The {@link Completable} returned from this
     * method should ensure the {@link ConfirmListener} is added to the channel prior
     * to publishing and the {@link ConfirmListener} is removed from the channel after
     * the publish has been acknowledged.
     *
     * @see Channel#basicPublish(String, String, AMQP.BasicProperties, byte[])
     *
     * @param channel The channel to publish the message to
     * @param publishState The publishing state
     *
     * @return A completable that terminates when the publish has been acknowledged
     */
    protected Completable publishInternal(Channel channel, RabbitPublishState publishState) {
        return Completable.create(subscriber -> {
            AtomicReference<BrokerResponse> acknowledgement = new AtomicReference<>(BrokerResponse.NONE);
            Disposable listener = createListener(channel, acknowledgement);
            try {
                channel.basicPublish(
                        publishState.getExchange(),
                        publishState.getRoutingKey(),
                        publishState.getProperties(),
                        publishState.getBody()
                );

                synchronized (acknowledgement) {
                    if (subscriber.isDisposed()) {
                        listener.dispose();
                    } else {
                        while (acknowledgement.get() == BrokerResponse.NONE) {
                            acknowledgement.wait();
                        }
                        if (acknowledgement.get() == BrokerResponse.ACK) {
                            subscriber.onComplete();
                        } else {
                            subscriber.onError(new RabbitClientException("Message could not be delivered to the broker", Collections.singletonList(publishState)));
                        }
                    }
                }
            } catch (IOException | InterruptedException e) {
                listener.dispose();
                subscriber.onError(e);
            }
        }).doFinally(() -> returnChannel(channel));
    }

    /**
     * Initializes the channel to allow publisher acknowledgements.
     *
     * @param channel The channel to enable acknowledgements.
     * @return A {@link Single} that will complete according to the
     * success of the operation.
     */
    protected Single<Channel> initializePublish(Channel channel) {
        return Single.create(emitter -> {
            try {
                channel.confirmSelect();
                emitter.onSuccess(channel);
            } catch (IOException e) {
                emitter.onError(new MessagingClientException("Failed to enable publisher confirms on the channel", e));
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
     * @param channel The channel to listen for confirms
     * @param acknowledgement The acknowledgement object to update on response
     * @return A disposable to allow cleanup of the listener
     */
    protected Disposable createListener(Channel channel, AtomicReference<BrokerResponse> acknowledgement) {
        AtomicBoolean disposed = new AtomicBoolean();
        Consumer<ConfirmListener> dispose = (listener) -> {
            if (disposed.compareAndSet(false, true)) {
                channel.removeConfirmListener(listener);
            }
        };

        ConfirmListener confirmListener = new ConfirmListener() {
            @Override
            public void handleAck(long deliveryTag, boolean multiple) {
                ackNack(deliveryTag, multiple, BrokerResponse.ACK);
            }

            @Override
            public void handleNack(long deliveryTag, boolean multiple) {
                ackNack(deliveryTag, multiple, BrokerResponse.NACK);
            }

            private void ackNack(long deliveryTag, boolean multiple, BrokerResponse response) {
                synchronized (acknowledgement) {
                    acknowledgement.set(response);
                    dispose.accept(this);
                    acknowledgement.notify();
                }
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
}
