/*
 * Copyright 2017-2021 original authors
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

import com.rabbitmq.client.*;
import com.rabbitmq.client.impl.AMQConnection;
import io.micronaut.context.BeanContext;
import io.micronaut.core.bind.BoundExecutable;
import io.micronaut.core.bind.DefaultExecutableBinder;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.messaging.exceptions.MessageAcknowledgementException;
import io.micronaut.messaging.exceptions.MessageListenerException;
import io.micronaut.rabbitmq.bind.RabbitBinderRegistry;
import io.micronaut.rabbitmq.bind.RabbitConsumerState;
import io.micronaut.rabbitmq.bind.RabbitMessageCloseable;
import io.micronaut.rabbitmq.connect.ChannelPool;
import io.micronaut.rabbitmq.connect.RabbitConnectionFactoryConfig;
import io.micronaut.rabbitmq.exception.RabbitListenerException;
import io.micronaut.rabbitmq.exception.RabbitListenerExceptionHandler;
import io.micronaut.rabbitmq.serdes.RabbitMessageSerDes;
import io.micronaut.rabbitmq.serdes.RabbitMessageSerDesRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * A consumer that will consume messages from rabbit and reconnect in case of a server initiated connection close.
 *
 * @author Oscar Albrecht
 * @since 2.3.3
 */
public final class ConnectionAwareConsumer implements DefaultConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectionAwareConsumer.class);

    private ExecutableMethod<?, ?> method;
    private String clientTag;
    private String connection;
    private boolean exclusive;
    private String queue;
    private boolean reQueue;
    private boolean hasAckArg;
    private Channel channel;
    private boolean isVoid;
    private Object bean;
    private DefaultExecutableBinder<RabbitConsumerState> binder;
    private ExecutorService executorService;
    private Map<String, Object> arguments;
    private Map<Channel, RabbitMQConsumerAdvice.ConsumerState> consumerChannels;
    private BeanContext beanContext;
    private RabbitConnectionFactoryConfig connectionFactoryConfig;
    private RabbitBinderRegistry binderRegistry;
    private RabbitMessageSerDesRegistry serDesRegistry;
    private RabbitListenerExceptionHandler exceptionHandler;

    @Override
    public void handleTerminate(String consumerTag) {
        if (channel instanceof RecoverableChannel) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(
                        "The channel was terminated.  Automatic recovery attempt is underway for consumer [{}]",
                        clientTag);
            }
            try {
                channel.basicConsume(queue, false, clientTag, false, exclusive, arguments, this);
            } catch (IOException | AlreadyClosedException e) {
                retryRecovery(e);
            }
        } else {
            RabbitMQConsumerAdvice.ConsumerState state = consumerChannels.remove(channel);
            if (state != null) {
                state.channelPool.returnChannel(channel);
                if (LOG.isDebugEnabled()) {
                    LOG.debug(
                            "The channel was terminated. The consumer [{}] will no longer receive messages",
                            clientTag);
                }
            }
        }
    }

    private void retryRecovery(Exception e) {
        retryRecovery(e, 0);
        LOG.info("Connection restablished");
    }

    private void retryRecovery(Exception e, int recoveryAttempts) {
        LOG.debug("Retrying network recovery");
        try {
            long delay = getDelay(recoveryAttempts);
            Thread.sleep(delay);
        } catch (InterruptedException interruptedException) {
            interruptedException.printStackTrace();
        }
        ShutdownSignalException cause;
        if (e.getCause() instanceof ShutdownSignalException) {
            cause = (ShutdownSignalException) e.getCause();
        } else {
            cause = new ShutdownSignalException(false, false, null, null);
        }
        AMQConnection amqConnection = (AMQConnection) channel.getConnection();
        amqConnection.handleIoError(cause);

        Channel newChannel = createNewChannelAndReturnOldOne();
        Builder.SharedWithRabbitMQConsumerAdvice sharedWithRabbitMQConsumerAdvice = new Builder.SharedWithRabbitMQConsumerAdvice(bean, binder, executorService, consumerChannels, beanContext, connectionFactoryConfig, binderRegistry, serDesRegistry, exceptionHandler);
        DefaultConsumer consumer = new ConnectionAwareConsumer.Builder(method, clientTag, connection, queue, newChannel, sharedWithRabbitMQConsumerAdvice)
                .withExclusive(exclusive)
                .withReQueue(reQueue)
                .withHasAckArg(hasAckArg)
                .withIsVoid(isVoid)
                .withArguments(arguments)
                .build();
        try {
            newChannel.basicConsume(queue, false, clientTag, false, exclusive, arguments, consumer);
        } catch (IOException | AlreadyClosedException e2) {
            retryRecovery(e2, recoveryAttempts + 1);
        }
    }

    private Channel createNewChannelAndReturnOldOne() {
        ChannelPool channelPool = beanContext.getBean(ChannelPool.class, Qualifiers.byName(connection));
        Channel newChannel = getChannel(channelPool);

        RabbitMQConsumerAdvice.ConsumerState oldChannelState = consumerChannels.remove(channel);
        if (oldChannelState != null) {
            channelPool.returnChannel(channel);
        }

        RabbitMQConsumerAdvice.ConsumerState state = new RabbitMQConsumerAdvice.ConsumerState();
        state.channelPool = channelPool;
        state.consumerTag = clientTag;
        consumerChannels.put(newChannel, state);

        return newChannel;
    }

    private long getDelay(int attempts) {
        RecoveryDelayHandler configuredDelayHandler = connectionFactoryConfig.getRecoveryDelayHandler();
        long networkRecoveryInterval = connectionFactoryConfig.getNetworkRecoveryInterval();
        RecoveryDelayHandler delayHandler = configuredDelayHandler == null ? new RecoveryDelayHandler.DefaultRecoveryDelayHandler(networkRecoveryInterval) : configuredDelayHandler;
        return delayHandler.getDelay(attempts);
    }

    public void doHandleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
        final RabbitConsumerState state = new RabbitConsumerState(envelope, properties, body, channel);

        BoundExecutable boundExecutable = null;
        try {
            boundExecutable = binder.bind(method, binderRegistry, state);
        } catch (Throwable e) {
            handleException(new RabbitListenerException(
                    "An error occurred binding the message to the method",
                    e,
                    bean,
                    state));
        }

        try {
            if (boundExecutable != null) {
                try (RabbitMessageCloseable closeable = new RabbitMessageCloseable(state,
                        false,
                        reQueue).withAcknowledge(hasAckArg ? null : false)) {
                    Object returnedValue = boundExecutable.invoke(bean);

                    String replyTo = properties.getReplyTo();
                    if (!isVoid && StringUtils.isNotEmpty(replyTo)) {
                        MutableBasicProperties replyProps = new MutableBasicProperties();
                        replyProps.setCorrelationId(properties.getCorrelationId());

                        byte[] converted = null;
                        if (returnedValue != null) {
                            RabbitMessageSerDes serDes = serDesRegistry.findSerdes(method.getReturnType()
                                    .asArgument())
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
                } catch (Throwable e) {
                    if (e instanceof RabbitListenerException) {
                        handleException((RabbitListenerException) e);
                    } else {
                        handleException(new RabbitListenerException(
                                "An error occurred executing the listener",
                                e,
                                bean,
                                state));
                    }
                }
            } else {
                new RabbitMessageCloseable(state, false, reQueue).withAcknowledge(false).close();
            }
        } catch (MessageAcknowledgementException e) {
            if (!channel.isOpen()) {
                RabbitMQConsumerAdvice.ConsumerState consumerState = consumerChannels.remove(channel);
                if (consumerState != null) {
                    consumerState.channelPool.returnChannel(channel);
                }
                if (LOG.isErrorEnabled()) {
                    LOG.error(
                            "The channel was closed due to an exception. The consumer [{}] will no longer receive messages",
                            clientTag);
                }
            }
            handleException(new RabbitListenerException(e.getMessage(), e, bean, null));
        } finally {
            consumerChannels.get(channel).inProgress = false;
        }
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
            throws IOException {
        consumerChannels.get(channel).inProgress = true;

        if (executorService != null) {
            executorService.submit(() -> doHandleDelivery(consumerTag, envelope, properties, body));
        } else {
            doHandleDelivery(consumerTag, envelope, properties, body);
        }
    }

    /**
     * Retrieves a channel to use for consuming messages.
     *
     * @param channelPool The channel pool to retrieve the channel from
     * @return A channel to publish with
     */
    protected Channel getChannel(ChannelPool channelPool) {
        try {
            return channelPool.getChannel();
        } catch (IOException e) {
            throw new MessageListenerException("Could not retrieve a channel", e);
        }
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
     * Builder for the connection aware consumer.
     *
     * @author Oscar Albrecht
     * @since 2.3.3
     */
    public static final class Builder {
        private ExecutableMethod<?, ?> method;
        private String clientTag;
        private String connection;
        private boolean exclusive;
        private String queue;
        private boolean reQueue;
        private boolean hasAckArg;
        private Channel channel;
        private boolean isVoid;
        private Object bean;
        private DefaultExecutableBinder<RabbitConsumerState> binder;
        private ExecutorService executorService;
        private Map<String, Object> arguments;
        private Map<Channel, RabbitMQConsumerAdvice.ConsumerState> consumerChannels;
        private BeanContext beanContext;
        private RabbitConnectionFactoryConfig connectionFactoryConfig;
        private RabbitBinderRegistry binderRegistry;
        private RabbitMessageSerDesRegistry serDesRegistry;
        private RabbitListenerExceptionHandler exceptionHandler;

        public Builder(ExecutableMethod<?, ?> method, String clientTag, String connection, String queue, Channel channel, SharedWithRabbitMQConsumerAdvice sharedWithRabbitMQConsumerAdvice) {
            this.method = method;
            this.clientTag = clientTag;
            this.connection = connection;
            this.queue = queue;
            this.channel = channel;
            this.bean = sharedWithRabbitMQConsumerAdvice.bean;
            this.binder = sharedWithRabbitMQConsumerAdvice.binder;
            this.executorService = sharedWithRabbitMQConsumerAdvice.executorService;
            this.consumerChannels = sharedWithRabbitMQConsumerAdvice.consumerChannels;
            this.beanContext = sharedWithRabbitMQConsumerAdvice.beanContext;
            this.connectionFactoryConfig = sharedWithRabbitMQConsumerAdvice.connectionFactoryConfig;
            this.binderRegistry = sharedWithRabbitMQConsumerAdvice.binderRegistry;
            this.serDesRegistry = sharedWithRabbitMQConsumerAdvice.serDesRegistry;
            this.exceptionHandler = sharedWithRabbitMQConsumerAdvice.exceptionHandler;
        }

        public Builder withMethod(ExecutableMethod<?, ?> method) {
            this.method = method;
            return this;
        }

        public Builder withClientTag(String clientTag) {
            this.clientTag = clientTag;
            return this;
        }

        public Builder withConnection(String connection) {
            this.connection = connection;
            return this;
        }

        public Builder withExclusive(boolean exclusive) {
            this.exclusive = exclusive;
            return this;
        }

        public Builder withQueue(String queue) {
            this.queue = queue;
            return this;
        }

        public Builder withReQueue(boolean reQueue) {
            this.reQueue = reQueue;
            return this;
        }

        public Builder withHasAckArg(boolean hasAckArg) {
            this.hasAckArg = hasAckArg;
            return this;
        }

        public Builder withBinder(DefaultExecutableBinder<RabbitConsumerState> binder) {
            this.binder = binder;
            return this;
        }

        public Builder withIsVoid(boolean isVoid) {
            this.isVoid = isVoid;
            return this;
        }

        public Builder withBean(Object bean) {
            this.bean = bean;
            return this;
        }

        public Builder withChannel(Channel channel) {
            this.channel = channel;
            return this;
        }

        public Builder withExecutorService(ExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        public Builder withArguments(Map<String, Object> arguments) {
            this.arguments = arguments;
            return this;
        }

        public ConnectionAwareConsumer build() {
            ConnectionAwareConsumer connectionAwareConsumer = new ConnectionAwareConsumer();
            connectionAwareConsumer.method = this.method;
            connectionAwareConsumer.clientTag = this.clientTag;
            connectionAwareConsumer.connection = this.connection;
            connectionAwareConsumer.exclusive = this.exclusive;
            connectionAwareConsumer.queue = this.queue;
            connectionAwareConsumer.reQueue = this.reQueue;
            connectionAwareConsumer.hasAckArg = this.hasAckArg;
            connectionAwareConsumer.channel = this.channel;
            connectionAwareConsumer.isVoid = this.isVoid;
            connectionAwareConsumer.bean = this.bean;
            connectionAwareConsumer.binder = this.binder;
            connectionAwareConsumer.executorService = this.executorService;
            connectionAwareConsumer.arguments = this.arguments;
            connectionAwareConsumer.consumerChannels = this.consumerChannels;
            connectionAwareConsumer.beanContext = this.beanContext;
            connectionAwareConsumer.connectionFactoryConfig = this.connectionFactoryConfig;
            connectionAwareConsumer.binderRegistry = this.binderRegistry;
            connectionAwareConsumer.serDesRegistry = this.serDesRegistry;
            connectionAwareConsumer.exceptionHandler = this.exceptionHandler;

            return connectionAwareConsumer;
        }

        /**
         * This nested class encapsulates the objects that are shared by the {@link RabbitMQConsumerAdvice}, which
         * should not be. Unfortunately I cannot separate them correctly right now, so I am encapsulating the shared
         * objects, so they are explicitly defined and can be refactored in the near future.
         */
        static class SharedWithRabbitMQConsumerAdvice {
            private Object bean;
            private DefaultExecutableBinder<RabbitConsumerState> binder;
            private ExecutorService executorService;
            private Map<String, Object> arguments;
            private Map<Channel, RabbitMQConsumerAdvice.ConsumerState> consumerChannels;
            private BeanContext beanContext;
            private RabbitConnectionFactoryConfig connectionFactoryConfig;
            private RabbitBinderRegistry binderRegistry;
            private RabbitMessageSerDesRegistry serDesRegistry;
            private RabbitListenerExceptionHandler exceptionHandler;

            public SharedWithRabbitMQConsumerAdvice(Object bean, DefaultExecutableBinder<RabbitConsumerState> binder, ExecutorService executorService, Map<Channel, RabbitMQConsumerAdvice.ConsumerState> consumerChannels, BeanContext beanContext, RabbitConnectionFactoryConfig connectionFactoryConfig, RabbitBinderRegistry binderRegistry, RabbitMessageSerDesRegistry serDesRegistry, RabbitListenerExceptionHandler exceptionHandler) {
                this.bean = bean;
                this.binder = binder;
                this.executorService = executorService;
                this.consumerChannels = consumerChannels;
                this.beanContext = beanContext;
                this.connectionFactoryConfig = connectionFactoryConfig;
                this.binderRegistry = binderRegistry;
                this.serDesRegistry = serDesRegistry;
                this.exceptionHandler = exceptionHandler;
            }
        }
    }
}
