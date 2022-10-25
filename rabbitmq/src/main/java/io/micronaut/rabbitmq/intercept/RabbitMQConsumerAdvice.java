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
package io.micronaut.rabbitmq.intercept;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.RecoverableChannel;
import io.micronaut.context.BeanContext;
import io.micronaut.context.processor.ExecutableMethodProcessor;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.bind.BoundExecutable;
import io.micronaut.core.bind.DefaultExecutableBinder;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.messaging.Acknowledgement;
import io.micronaut.messaging.exceptions.MessageAcknowledgementException;
import io.micronaut.messaging.exceptions.MessageListenerException;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitConnection;
import io.micronaut.rabbitmq.annotation.RabbitProperty;
import io.micronaut.rabbitmq.bind.RabbitBinderRegistry;
import io.micronaut.rabbitmq.bind.RabbitConsumerState;
import io.micronaut.rabbitmq.bind.RabbitMessageCloseable;
import io.micronaut.rabbitmq.connect.ChannelPool;
import io.micronaut.rabbitmq.exception.RabbitListenerException;
import io.micronaut.rabbitmq.exception.RabbitListenerExceptionHandler;
import io.micronaut.rabbitmq.serdes.RabbitMessageSerDes;
import io.micronaut.rabbitmq.serdes.RabbitMessageSerDesRegistry;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An {@link ExecutableMethodProcessor} that will process all beans annotated
 * with {@link io.micronaut.rabbitmq.annotation.RabbitListener} and create and subscribe the relevant methods
 * as consumers to RabbitMQ queues.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Singleton
public class RabbitMQConsumerAdvice implements ExecutableMethodProcessor<Queue>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(RabbitMQConsumerAdvice.class);

    private final BeanContext beanContext;
    private final RabbitBinderRegistry binderRegistry;
    private final RabbitListenerExceptionHandler exceptionHandler;
    private final RabbitMessageSerDesRegistry serDesRegistry;
    private final ConversionService<?> conversionService;
    private final Map<String, ChannelPool> channelPools;
    private final List<RecoverableConsumerWrapper> consumers = new CopyOnWriteArrayList<>();

    /**
     * Default constructor.
     *
     * @param beanContext       The bean context
     * @param binderRegistry    The registry to bind arguments to the method
     * @param exceptionHandler  The exception handler to use if the consumer isn't a handler
     * @param serDesRegistry    The serialization/deserialization registry
     * @param conversionService The service to convert consume argument values
     * @param channelPools      The channel pools to retrieve channels
     */
    public RabbitMQConsumerAdvice(BeanContext beanContext,
                                  RabbitBinderRegistry binderRegistry,
                                  RabbitListenerExceptionHandler exceptionHandler,
                                  RabbitMessageSerDesRegistry serDesRegistry,
                                  ConversionService<?> conversionService,
                                  List<ChannelPool> channelPools) {
        this.beanContext = beanContext;
        this.binderRegistry = binderRegistry;
        this.exceptionHandler = exceptionHandler;
        this.serDesRegistry = serDesRegistry;
        this.conversionService = conversionService;
        this.channelPools = new HashMap<>(channelPools.size());
        for (ChannelPool cp: channelPools) {
            this.channelPools.put(cp.getName(), cp);
        }
    }

    @Override
    public void process(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {

        AnnotationValue<Queue> queueAnn = method.getAnnotation(Queue.class);

        if (queueAnn != null) {
            String queue = queueAnn.getRequiredValue(String.class);

            String methodTag = method.getDeclaringType().getSimpleName() + '#' + method;

            boolean reQueue = queueAnn.getRequiredValue("reQueue", boolean.class);
            boolean exclusive = queueAnn.getRequiredValue("exclusive", boolean.class);

            boolean hasAckArg = Arrays.stream(method.getArguments())
                    .anyMatch(arg -> Acknowledgement.class.isAssignableFrom(arg.getType()));

            Integer prefetch = queueAnn.get("prefetch", Integer.class).orElse(null);
            int numberOfConsumers = queueAnn.intValue("numberOfConsumers").orElse(1);

            ChannelPool channelPool = getChannelPool(method);
            ExecutorService executorService = getExecutorService(method);

            Map<String, Object> arguments = retrieveArguments(method);
            Object bean = getExecutableMethodBean(beanDefinition, method);
            boolean isVoid = method.getReturnType().isVoid();

            DefaultExecutableBinder<RabbitConsumerState> binder = new DefaultExecutableBinder<>();

            DeliverCallback deliverCallback = (channel, message) -> {
                final RabbitConsumerState state = new RabbitConsumerState(message.getEnvelope(),
                        message.getProperties(), message.getBody(), channel);

                BoundExecutable boundExecutable = null;
                try {
                    boundExecutable = binder.bind(method, binderRegistry, state);
                } catch (Throwable e) {
                    handleException(new RabbitListenerException("An error occurred binding the message to the method",
                            e, bean, state));
                }
                try {
                    if (boundExecutable != null) {
                        try (RabbitMessageCloseable closeable = new RabbitMessageCloseable(state, false, reQueue)
                                .withAcknowledge(hasAckArg ? null : false)) {
                            Object returnedValue = boundExecutable.invoke(bean);

                            String replyTo = message.getProperties().getReplyTo();
                            if (!isVoid && StringUtils.isNotEmpty(replyTo)) {
                                MutableBasicProperties replyProps = new MutableBasicProperties();
                                replyProps.setCorrelationId(message.getProperties().getCorrelationId());

                                byte[] converted = null;
                                if (returnedValue != null) {
                                    RabbitMessageSerDes serDes = serDesRegistry
                                            .findSerdes(method.getReturnType().asArgument())
                                            .map(RabbitMessageSerDes.class::cast)
                                            .orElseThrow(() -> new RabbitListenerException(String.format(
                                                    "Could not find a serializer for the body argument of type [%s]",
                                                    returnedValue.getClass().getName()), bean, state));

                                    converted = serDes.serialize(returnedValue, replyProps);
                                }

                                channel.basicPublish("", replyTo, replyProps.toBasicProperties(), converted);
                            }

                            if (!hasAckArg) {
                                closeable.withAcknowledge(true);
                            }
                        } catch (MessageAcknowledgementException e) {
                            throw e;
                        } catch (RabbitListenerException e) {
                            handleException(e);
                        } catch (Throwable e) {
                            handleException(new RabbitListenerException("An error occurred executing the listener",
                                    e, bean, state));
                        }
                    } else {
                        new RabbitMessageCloseable(state, false, reQueue).withAcknowledge(false).close();
                    }
                } catch (MessageAcknowledgementException e) {
                    handleException(new RabbitListenerException(e.getMessage(), e, bean, state));
                }
            };

            try {
                for (int idx = 0; idx < numberOfConsumers; idx++) {
                    String consumerTag = methodTag + "[" + idx + "]";
                    LOG.debug("Registering a consumer to queue [{}] with client tag [{}]", queue, consumerTag);
                    consumers.add(new RecoverableConsumerWrapper(queue, consumerTag, executorService,
                            exclusive, arguments, channelPool, prefetch, deliverCallback));
                }
            } catch (Throwable e) {
                handleException(new RabbitListenerException("An error occurred subscribing to a queue", e, bean, null));
            }
        }
    }

    private ChannelPool getChannelPool(ExecutableMethod<?, ?> method) {
        String connection = method.stringValue(RabbitConnection.class, "connection")
                .orElse(RabbitConnection.DEFAULT_CONNECTION);

        return Optional.ofNullable(channelPools.get(connection))
                .orElseThrow(() -> new MessageListenerException(
                        String.format("Failed to find a channel pool named [%s] to register a listener", connection)));
    }

    private static void setChannelPrefetch(Integer prefetch, Channel channel) {
        try {
            if (prefetch != null) {
                channel.basicQos(prefetch);
            }
        } catch (IOException e) {
            throw new MessageListenerException(String.format("Failed to set a prefetch count of [%s] on the channel",
                    prefetch), e);
        }
    }

    private Map<String, Object> retrieveArguments(ExecutableMethod<?, ?> method) {
        Map<String, Object> arguments = new HashMap<>();

        List<AnnotationValue<RabbitProperty>> propertyAnnotations = method.getAnnotationValuesByType(RabbitProperty.class);
        Collections.reverse(propertyAnnotations); //set the values in the class first so methods can override
        propertyAnnotations.forEach((prop) -> {
            String name = prop.getRequiredValue("name", String.class);
            String value = prop.getValue(String.class).orElse(null);
            Class<?> type = prop.get("type", Class.class).orElse(null);

            if (StringUtils.isNotEmpty(name) && StringUtils.isNotEmpty(value)) {
                if (type != null && type != Void.class) {
                    Optional<?> converted = conversionService.convert(value, type);
                    if (converted.isPresent()) {
                        arguments.put(name, converted.get());
                    } else {
                        throw new MessageListenerException(String.format("Could not convert the argument [%s] to the required type [%s]", name, type));
                    }
                } else {
                    arguments.put(name, value);
                }
            }
        });
        return arguments;
    }

    private Object getExecutableMethodBean(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        io.micronaut.context.Qualifier<Object> qualifier = beanDefinition
                .getAnnotationNameByStereotype("javax.inject.Qualifier")
                .map(type -> Qualifiers.byAnnotation(beanDefinition, type))
                .orElse(null);

        Class<Object> beanType = (Class<Object>) beanDefinition.getBeanType();
        Object bean = beanContext.findBean(beanType, qualifier).orElseThrow(() -> new MessageListenerException("Could not find the bean to execute the method " + method));
        return bean;
    }

    private ExecutorService getExecutorService(ExecutableMethod<?, ?> method) {
        String executor = method.stringValue(RabbitConnection.class, "executor").orElse(null);
        if (executor != null) {
            return beanContext.findBean(ExecutorService.class, Qualifiers.byName(executor))
                    .orElseThrow(() -> new MessageListenerException(String.format("Could not find the executor service [%s] specified for the method [%s]", executor, method)));
        }
        return null;
    }

    @PreDestroy
    @Override
    public void close() throws Exception {
        consumers.forEach(RecoverableConsumerWrapper::cancel);
    }

    private void handleException(RabbitListenerException exception) {
        Object bean = exception.getListener();
        if (bean instanceof RabbitListenerExceptionHandler) {
            ((RabbitListenerExceptionHandler) bean).handle(exception);
        } else {
            exceptionHandler.handle(exception);
        }
    }

    /**
     * Callback interface to be notified when a message is delivered.
     */
    @FunctionalInterface
    private interface DeliverCallback {
        /**
         * Inspired by {@link com.rabbitmq.client.DeliverCallback#handle(String, Delivery)}.
         *
         * @param channel that is used to register the consumer for this callback
         * @param message the delivered message
         */
        void handle(Channel channel, Delivery message);
    }

    /**
     * This wrapper around a {@link com.rabbitmq.client.DefaultConsumer} allows to handle the different signals from
     * the underlying channel and to react accordingly.
     * <p>
     * If the consumer is canceled due to an external event (like an unavailable queue) we will try to recover from it.
     * Exceptions that are caused by the consumer itself will not trigger the recovery process. In such a case the
     * consumer will no longer receive any messages.
     *
     * @see com.rabbitmq.client.Consumer#handleShutdownSignal(String, ShutdownSignalException)
     * @see com.rabbitmq.client.Consumer#handleCancel(String)
     */
    private class RecoverableConsumerWrapper {
        final String consumerTag;
        private final ExecutorService executorService;
        private final String queue;
        private final boolean exclusive;
        private final Map<String, Object> arguments;
        private final ChannelPool channelPool;
        private final Integer prefetch;
        private final DeliverCallback deliverCallback;
        private com.rabbitmq.client.DefaultConsumer consumer;
        private boolean canceled = false;
        private final AtomicInteger handlingDeliveryCount = new AtomicInteger();

        /**
         * Create the consumer and register ({@code Channel.basicConsume}) it with a dedicated channel from the
         * provided pool.
         *
         * @throws IOException in case no channel is available or the registration of the consumer fails
         */
        RecoverableConsumerWrapper(String queue, String consumerTag, ExecutorService executorService, boolean exclusive,
                Map<String, Object> arguments, ChannelPool channelPool, Integer prefetch, DeliverCallback deliverCallback)
                throws IOException {
            this.queue = queue;
            this.consumerTag = consumerTag;
            this.executorService = executorService;
            this.exclusive = exclusive;
            this.arguments = arguments;
            this.channelPool = channelPool;
            this.prefetch = prefetch;
            this.deliverCallback = deliverCallback;

            Channel channel = null;
            try {
                channel = channelPool.getChannel();
                consumer = createConsumer(channel);
            } catch (IOException e) {
                if (channel != null) {
                    channelPool.returnChannel(channel);
                }
                throw e;
            }
        }

        /**
         * Cancel the consumer and return the associated channel to the pool.
         */
        public synchronized void cancel() {
            canceled = true;
            if (consumer == null) {
                return;
            }

            Channel channel = consumer.getChannel();
            try {
                channel.basicCancel(consumerTag);
            } catch (IOException | AlreadyClosedException e) {
                // ignore
            }
            try {
                while (handlingDeliveryCount.get() > 0) {
                    this.wait(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                consumer = null;
                consumers.remove(this);
                channelPool.returnChannel(channel);
            }
        }

        private void performConsumerRecovery() {
            com.rabbitmq.client.DefaultConsumer recoveredConsumer = null;
            int recoveryAttempts = 0;
            while (recoveredConsumer == null) {
                Channel channel = null;
                try {
                    synchronized (this) {
                        if (canceled) {
                            return;
                        }
                        LOG.debug("consumer [{}] recovery attempt: {}", consumerTag, recoveryAttempts + 1);
                        channel = channelPool.getChannelWithRecoveringDelay(recoveryAttempts++);
                        recoveredConsumer = createConsumer(channel);
                        consumer = recoveredConsumer;
                    }
                } catch (IOException e) {
                    if (channel != null) {
                        channelPool.returnChannel(channel);
                    }
                    LOG.warn("Recovery attempt {} for consumer [{}] failed, will retry.",
                            recoveryAttempts, consumerTag, e);
                } catch (InterruptedException e) {
                    LOG.warn("The consumer [{}] recovery was interrupted. The consumer will not recover.",
                            consumerTag, e);
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            LOG.info("consumer [{}] recovered", consumerTag);
        }

        private com.rabbitmq.client.DefaultConsumer createConsumer(Channel channel) throws IOException {
            setChannelPrefetch(prefetch, channel);

            com.rabbitmq.client.DefaultConsumer consumer = new com.rabbitmq.client.DefaultConsumer(channel) {
                /**
                 * The consumer was irregular terminated. This may be caused by a deleted or temporary unavailable
                 * queue.
                 * <p>
                 * This kind of infrastructure failure may happen due to RabbitMQ cluster node restarts or other
                 * external actions. The client application will most likely be unable to restore the infrastructure,
                 * but it should return to normal operation as soon as the external infrastructure problem is solved.
                 * e.g. the RabbitMQ node restart is complete and the queue is available again.
                 */
                @Override
                public void handleCancel(String consumerTag) throws IOException {
                    synchronized (RecoverableConsumerWrapper.this) {
                        RecoverableConsumerWrapper.this.consumer = null;
                        channelPool.returnChannel(getChannel());
                    }

                    if (channelPool.isTopologyRecoveryEnabled() && getChannel() instanceof RecoverableChannel) {
                        LOG.warn("The consumer [{}] subscription was canceled, a recovery will be tried.",
                                consumerTag);
                        performConsumerRecovery();
                    } else {
                        LOG.warn("The RabbitMQ consumer [{}] was canceled. Recovery is not enabled. It will no longer receive messages",
                                consumerTag);
                        cancel();
                    }
                }

                /**
                 * A shutdown signal from the channel or the underlying connection does not always imply that the
                 * consumer is no longer usable. If the automatic topology recovery is active and the shutdown
                 * was not initiated by the application it will be recovered by the RabbitMQ client.
                 * <p>
                 * If the topology recovery is enabled we will also try to recover the consumer if (only) its channel
                 * fails. These events are not handled by the RabbitMQ client itself as they are most likely application
                 * specific. Also some edge cases like a delivery acknowledgement timeout may cause a channel shutdown.
                 * The registered exception handler of the consumer may handle these cases and it is possible to
                 * resume message handling by re-registering the consumer on a new channel.
                 */
                @Override
                public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
                    if (getChannel() instanceof RecoverableChannel && sig.isHardError()) {
                        // The RabbitMQ client automatic recovery is only triggered by connection errors.
                        // The consumer will be recovered by the client, so no additional handling here.
                        LOG.info("The underlying connection was terminated. Automatic recovery attempt is underway for consumer [{}]",
                                consumerTag);
                    } else if (channelPool.isTopologyRecoveryEnabled() && getChannel() instanceof RecoverableChannel) {
                        LOG.info("The channel of this consumer was terminated. Automatic recovery attempt is underway for consumer [{}]",
                                consumerTag, sig);
                        synchronized (RecoverableConsumerWrapper.this) {
                            RecoverableConsumerWrapper.this.consumer = null;
                            channelPool.returnChannel(getChannel());
                        }
                        performConsumerRecovery();
                    } else {
                        LOG.error("The channel was closed. Recovery is not enabled. The consumer [{}] will no longer receive messages",
                                consumerTag, sig);
                        cancel();
                    }
                }

                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                        throws IOException {
                    // If the broker forces the channel to close, the client may already have prefetched messages in
                    // memory and will call handleDelivery for these messages, even if they are re-queued by the broker.
                    // The client will be unable to acknowledge these messages. So it is safe to silently discard
                    // them, without bothering the callback handler.
                    // In addition, consuming of queued messages is stopped when the consumer is canceled.
                    if (canceled || !getChannel().isOpen()) {
                        return;
                    }
                    handlingDeliveryCount.incrementAndGet();
                    if (executorService != null) {
                        executorService.submit(() -> callbackHandle(envelope, properties, body));
                    } else {
                        callbackHandle(envelope, properties, body);
                    }
                }

                private void callbackHandle(Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                    try {
                        deliverCallback.handle(getChannel(), new Delivery(envelope, properties, body));
                    } finally {
                        handlingDeliveryCount.decrementAndGet();
                    }
                }
            };

            channel.basicConsume(queue, false, consumerTag, false, exclusive, arguments, consumer);
            return consumer;
        }
    }
}
