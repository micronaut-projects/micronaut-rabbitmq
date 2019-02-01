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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class provides a wrapper around a {@link Channel} to provide
 * reactive implementations of the common actions that can be performed
 * on a channel.
 *
 *
 * @author James Kleeh
 * @since 1.1.0
 */
public class ReactiveChannel {

    private final ConcurrentHashMap<Long, CompletableEmitter> unconfirmed = new ConcurrentHashMap<>();
    private final Channel channel;
    private final ConfirmListener listener;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicLong publishCount = new AtomicLong(0);

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
                    System.out.println("received delivery tag " + deliveryTag);
                    if (unconfirmed.containsKey(deliveryTag)) {
                        System.out.println("delivery tag is in unconfirmed list");
                        if (multiple) {
                            System.out.println("multiple ack");
                            final Iterator<Map.Entry<Long, CompletableEmitter>> iterator = unconfirmed.entrySet().iterator();
                            while (iterator.hasNext()) {
                                Map.Entry<Long, CompletableEmitter> entry = iterator.next();
                                if (entry.getKey() <= deliveryTag) {
                                    System.out.println("adding " + entry.getKey() + " to the to be completed list");
                                    completables.add(entry.getValue());
                                    iterator.remove();
                                }
                            }
                        } else {
                            System.out.println("adding " + deliveryTag + " to the to be completed list");
                            completables.add(unconfirmed.remove(deliveryTag));
                        }
                    }
                }

                for (CompletableEmitter completable: completables) {
                    if (ack) {
                        System.out.println("completable complete");
                        completable.onComplete();
                    } else {
                        System.out.println("completable error");
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
        return initializePublish()
                .andThen(Completable.create((emitter) ->
                        publishInternal(exchange, routingKey, properties, body, emitter)))
                .andThen(Completable.fromAction(this::cleanupChannel));
    }

    private void publishInternal(String exchange, String routingKey, AMQP.BasicProperties props, byte[] body, CompletableEmitter emitter) {
        long nextPublishSeqNo = channel.getNextPublishSeqNo();
        try {
            unconfirmed.put(nextPublishSeqNo, emitter);
            System.out.println("sending delivery tag " + nextPublishSeqNo);
            channel.basicPublish(
                    exchange,
                    routingKey,
                    props,
                    body
            );
            System.out.println("incrementing publish count");
            publishCount.incrementAndGet();
        } catch (IOException e) {
            System.out.println("error publishing");
            unconfirmed.remove(nextPublishSeqNo);
            emitter.onError(e);
        }
    }

    private Completable initializePublish() {
        if (initialized.compareAndSet(false, true)) {
            try {
                channel.confirmSelect();
                channel.addConfirmListener(listener);
                System.out.println("initialize successful");
                return Completable.complete();
            } catch (IOException e) {
                System.out.println("initialize error");
                return Completable.error(new MessagingClientException("Failed to enable publisher confirms on the channel", e));
            }
        } else {
            System.out.println("already initialized");
            return Completable.complete();
        }
    }

    private void cleanupChannel() {
        if (publishCount.decrementAndGet() == 0 &&
                initialized.compareAndSet(true, false)) {
            System.out.println("removing confirm listener");
            channel.removeConfirmListener(listener);
        }
    }

}
