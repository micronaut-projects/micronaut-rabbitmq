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
import io.micronaut.messaging.exceptions.MessagingClientException;
import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.Single;
import org.reactivestreams.Publisher;

import javax.inject.Singleton;
import java.io.IOException;

/**
 * A reactive publisher implementation that uses a single channel per publish
 * operation and returns an RxJava2 {@link Completable}.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Singleton
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
            .flatMapCompletable(channel -> publishInternal(channel, exchange, routingKey, properties, body));
    }

    @Override
    public Completable publish(Publisher<RabbitPublisherState> messagePublisher) {
        throw new UnsupportedOperationException("Not implemented yet");
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
                emitter.onError(e);
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
     * @param exchange The exchange
     * @param routingKey The routing key
     * @param properties The properties
     * @param body The message body
     *
     * @return A completable that terminates when the publish has been acknowledged
     */
    protected Completable publishInternal(Channel channel, String exchange, String routingKey, AMQP.BasicProperties properties, byte[] body) {
        return Completable.create((emitter) -> {
            channel.addConfirmListener(createListener(emitter));
            try {
                channel.basicPublish(
                        exchange,
                        routingKey,
                        properties,
                        body
                );
            } catch (IOException e) {
                emitter.onError(e);
            }
        }).doFinally(() -> cleanupChannel(channel));
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
    protected void cleanupChannel(Channel channel) {
        channel.clearConfirmListeners();
        channelPool.returnChannel(channel);
    }

    /**
     * Returns the listener that will emit the appropriate event.
     *
     * @param emitter The emitter to emit completion or error
     * @return The listener to add to pass to {@link Channel#addConfirmListener(ConfirmListener)}
     */
    protected ConfirmListener createListener(CompletableEmitter emitter) {
        return new ConfirmListener() {
            @Override
            public void handleAck(long deliveryTag, boolean multiple) {
                emitter.onComplete();
            }

            @Override
            public void handleNack(long deliveryTag, boolean multiple) {
                emitter.onError(new MessagingClientException("Message could not be delivered to the broker"));
            }
        };
    }
}
