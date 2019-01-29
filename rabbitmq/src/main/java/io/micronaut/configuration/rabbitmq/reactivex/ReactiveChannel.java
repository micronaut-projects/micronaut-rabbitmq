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

package io.micronaut.configuration.rabbitmq.reactivex;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmListener;
import io.micronaut.messaging.exceptions.MessagingClientException;
import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.CompletableSource;
import io.reactivex.Single;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class provides a wrapper around a {@link Channel} to provide
 * reactive implementations of the common actions that can be performed
 * on a channel.
 *
 * After all operations on the channel have been executed, subscribe to
 * the {@link #finish()} method to get back the original channel.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
public class ReactiveChannel {

    private final ConcurrentHashMap<Long, CompletableEmitter> unconfirmed = new ConcurrentHashMap<>();
    private final List<CompletableSource> allCompletables = new ArrayList<>();
    private final Channel channel;
    private final ConfirmListener listener;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * Default constructor.
     *
     * @param channel The channel to use
     */
    public ReactiveChannel(Channel channel) {
        this.channel = channel;
        listener = new ConfirmListener() {
            @Override
            public void handleAck(long deliveryTag, boolean multiple) {
                handleAckNack(deliveryTag, multiple, true);
            }

            @Override
            public void handleNack(long deliveryTag, boolean multiple) {
                handleAckNack(deliveryTag, multiple, false);
            }

            private void handleAckNack(long deliveryTag, boolean multiple, boolean ack) {
                List<CompletableEmitter> completables = new ArrayList<>();
                synchronized (unconfirmed) {
                    if (unconfirmed.containsKey(deliveryTag)) {
                        if (multiple) {
                            final Iterator<Map.Entry<Long, CompletableEmitter>> iterator = unconfirmed.entrySet().iterator();
                            while (iterator.hasNext()) {
                                Map.Entry<Long, CompletableEmitter> entry = iterator.next();
                                if (entry.getKey() <= deliveryTag) {
                                    completables.add(entry.getValue());
                                    iterator.remove();
                                }
                            }
                        } else {
                            completables.add(unconfirmed.remove(deliveryTag));
                        }
                    }
                }

                for (CompletableEmitter completable: completables) {
                    if (ack) {
                        completable.onComplete();
                    } else {
                        completable.onError(new MessagingClientException("Message could not be delivered to the broker"));
                    }
                }

            }
        };
    }

    /**
     * Publishes the message and returns a {@link Completable}.
     *
     * @param exchange The exchange
     * @param routingKey The routing key
     * @param properties The properties
     * @param body The body
     * @return A completable that will complete if the message is confirmed
     */
    public Completable publish(String exchange, String routingKey, AMQP.BasicProperties properties, byte[] body) {
        /*The result must be cached because the finish method will subscribe to the
         completables and thus 2 subscribers (this and downstream) will cause the
         message to be published twice
         */
        Completable publish = initializePublish()
                .andThen(Completable.create((emitter) ->
                        publishInternal(exchange, routingKey, properties, body, emitter))
                .cache());
        allCompletables.add(publish);
        return publish;
    }

    private void publishInternal(String exchange, String routingKey, AMQP.BasicProperties props, byte[] body, CompletableEmitter emitter) {
        long nextPublishSeqNo = channel.getNextPublishSeqNo();
        try {
            unconfirmed.put(nextPublishSeqNo, emitter);
            channel.basicPublish(
                    exchange,
                    routingKey,
                    props,
                    body
            );
        } catch (IOException e) {
            unconfirmed.remove(nextPublishSeqNo);
            emitter.onError(e);
        }
    }

    private Completable initializePublish() {
        if (initialized.compareAndSet(false, true)) {
            try {
                channel.confirmSelect();
                channel.addConfirmListener(listener);
                return Completable.complete();
            } catch (IOException e) {
                return Completable.error(new MessagingClientException("Failed to enable publisher confirms on the channel", e));
            }
        } else {
            return Completable.complete();
        }
    }

    /**
     * @return A Single that will never error that returns the channel
     * after all actions are terminated.
     */
    public Single<Channel> finish() {
        return Completable.mergeDelayError(allCompletables)
                .onErrorComplete()
                .doOnTerminate(() -> channel.removeConfirmListener(listener))
                .toSingle(() -> channel);
    }
}
