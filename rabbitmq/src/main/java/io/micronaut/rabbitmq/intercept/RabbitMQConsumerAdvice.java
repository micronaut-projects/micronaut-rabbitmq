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

import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import io.micronaut.context.BeanContext;
import io.micronaut.context.processor.ExecutableMethodProcessor;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.bind.DefaultExecutableBinder;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.messaging.Acknowledgement;
import io.micronaut.messaging.exceptions.MessageListenerException;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitConnection;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import io.micronaut.rabbitmq.annotation.RabbitProperty;
import io.micronaut.rabbitmq.bind.RabbitBinderRegistry;
import io.micronaut.rabbitmq.bind.RabbitConsumerState;
import io.micronaut.rabbitmq.connect.ChannelPool;
import io.micronaut.rabbitmq.connect.RabbitConnectionFactoryConfig;
import io.micronaut.rabbitmq.exception.RabbitListenerException;
import io.micronaut.rabbitmq.exception.RabbitListenerExceptionHandler;
import io.micronaut.rabbitmq.serdes.RabbitMessageSerDesRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.inject.Qualifier;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * An {@link ExecutableMethodProcessor} that will process all beans annotated
 * with {@link RabbitListener} and create and subscribe the relevant methods
 * as consumers to RabbitMQ queues.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Singleton
public class RabbitMQConsumerAdvice implements ExecutableMethodProcessor<RabbitListener>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(RabbitMQConsumerAdvice.class);

    private final BeanContext beanContext;
    private final RabbitBinderRegistry binderRegistry;
    private final RabbitListenerExceptionHandler exceptionHandler;
    private final RabbitMessageSerDesRegistry serDesRegistry;
    private final ConversionService conversionService;
    private final Map<Channel, ConsumerState> consumerChannels = new ConcurrentHashMap<>();
    private final RabbitConnectionFactoryConfig connectionFactoryConfig;

    /**
     * Default constructor.
     *
     * @param beanContext             The bean context
     * @param binderRegistry          The registry to bind arguments to the method
     * @param exceptionHandler        The exception handler to use if the consumer isn't a handler
     * @param serDesRegistry          The serialization/deserialization registry
     * @param conversionService       The service to convert consume argument values
     * @param connectionFactoryConfig The configuration of the rabbit connection
     */
    public RabbitMQConsumerAdvice(BeanContext beanContext,
                                  RabbitBinderRegistry binderRegistry,
                                  RabbitListenerExceptionHandler exceptionHandler,
                                  RabbitMessageSerDesRegistry serDesRegistry,
                                  ConversionService conversionService,
                                  RabbitConnectionFactoryConfig connectionFactoryConfig) {
        this.beanContext = beanContext;
        this.binderRegistry = binderRegistry;
        this.exceptionHandler = exceptionHandler;
        this.serDesRegistry = serDesRegistry;
        this.conversionService = conversionService;
        this.connectionFactoryConfig = connectionFactoryConfig;
    }

    @Override
    public void process(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {

        AnnotationValue<io.micronaut.rabbitmq.annotation.Queue> queueAnn = method.getAnnotation(Queue.class);

        if (queueAnn != null) {
            String queue = queueAnn.getRequiredValue(String.class);

            String clientTag = method.getDeclaringType().getSimpleName() + '#' + method.toString();

            boolean reQueue = queueAnn.getRequiredValue("reQueue", boolean.class);
            boolean exclusive = queueAnn.getRequiredValue("exclusive", boolean.class);

            boolean hasAckArg = Arrays.stream(method.getArguments())
                    .anyMatch(arg -> Acknowledgement.class.isAssignableFrom(arg.getType()));

            String connection = method.findAnnotation(RabbitConnection.class)
                    .flatMap(conn -> conn.get("connection", String.class))
                    .orElse(RabbitConnection.DEFAULT_CONNECTION);

            ChannelPool channelPool;
            try {
                channelPool = beanContext.getBean(ChannelPool.class, Qualifiers.byName(connection));
            } catch (Throwable e) {
                throw new MessageListenerException(String.format("Failed to retrieve a channel pool named [%s] to register a listener", connection), e);
            }

            Channel channel = getChannel(channelPool);

            Integer prefetch = queueAnn.get("prefetch", Integer.class).orElse(null);
            try {
                if (prefetch != null) {
                    channel.basicQos(prefetch);
                }
            } catch (IOException e) {
                throw new MessageListenerException(String.format("Failed to set a prefetch count of [%s] on the channel", prefetch), e);
            }

            ConsumerState state = new ConsumerState();
            state.channelPool = channelPool;
            state.consumerTag = clientTag;
            consumerChannels.put(channel, state);

            Map<String, Object> arguments = new HashMap<>();

            List<AnnotationValue<RabbitProperty>> propertyAnnotations = method.getAnnotationValuesByType(RabbitProperty.class);
            Collections.reverse(propertyAnnotations); //set the values in the class first so methods can override
            propertyAnnotations.forEach((prop) -> {
                String name = prop.getRequiredValue("name", String.class);
                String value = prop.getValue(String.class).orElse(null);
                Class type = prop.get("type", Class.class).orElse(null);

                if (StringUtils.isNotEmpty(name) && StringUtils.isNotEmpty(value)) {
                    if (type != null && type != Void.class) {
                        Optional<Object> converted = conversionService.convert(value, type);
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

            io.micronaut.context.Qualifier<Object> qualifer = beanDefinition
                    .getAnnotationTypeByStereotype(Qualifier.class)
                    .map(type -> Qualifiers.byAnnotation(beanDefinition, type))
                    .orElse(null);

            Class<Object> beanType = (Class<Object>) beanDefinition.getBeanType();

            Class<?> returnTypeClass = method.getReturnType().getType();
            boolean isVoid = returnTypeClass == Void.class || returnTypeClass == void.class;

            Object bean = beanContext.findBean(beanType, qualifer).orElseThrow(() -> new MessageListenerException("Could not find the bean to execute the method " + method));

            try {
                DefaultExecutableBinder<RabbitConsumerState> binder = new DefaultExecutableBinder<>();

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Registering a consumer to queue [{}] with client tag [{}]", queue, clientTag);
                }

                Optional<String> executor = method.findAnnotation(RabbitConnection.class)
                        .flatMap(conn -> conn.get("executor", String.class));

                ExecutorService executorService = executor
                        .flatMap(exec -> beanContext.findBean(ExecutorService.class, Qualifiers.byName(exec)))
                        .orElse(null);

                if (executor.isPresent() && executorService == null) {
                    throw new MessageListenerException(String.format("Could not find the executor service [%s] specified for the method [%s]", executor.get(), method));
                }

                ConnectionAwareConsumer.Builder.SharedWithRabbitMQConsumerAdvice sharedWithRabbitMQConsumerAdvice = new ConnectionAwareConsumer.Builder.SharedWithRabbitMQConsumerAdvice(bean, binder, executorService, consumerChannels, beanContext, connectionFactoryConfig, binderRegistry, serDesRegistry, exceptionHandler);

                DefaultConsumer consumer = new ConnectionAwareConsumer.Builder(method, clientTag, connection, queue, channel, sharedWithRabbitMQConsumerAdvice)
                        .withExclusive(exclusive)
                        .withReQueue(reQueue)
                        .withHasAckArg(hasAckArg)
                        .withIsVoid(isVoid)
                        .withArguments(arguments)
                        .build();
                channel.basicConsume(queue, false, clientTag, false, exclusive, arguments, consumer);
            } catch (Throwable e) {
                if (!channel.isOpen()) {
                    channelPool.returnChannel(channel);
                    consumerChannels.remove(channel);
                    if (LOG.isErrorEnabled()) {
                        LOG.error("The channel was closed due to an exception. The consumer [{}] will no longer receive messages", clientTag);
                    }
                }
                handleException(new RabbitListenerException("An error occurred subscribing to a queue", e, bean, null));
            }
        }

    }

    @PreDestroy
    @Override
    public void close() throws Exception {
        while (!consumerChannels.entrySet().isEmpty()) {
            Iterator<Map.Entry<Channel, ConsumerState>> it = consumerChannels.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Channel, ConsumerState> entry = it.next();
                Channel channel = entry.getKey();
                ConsumerState state = entry.getValue();
                try {
                    channel.basicCancel(state.consumerTag);
                } catch (IOException | AlreadyClosedException e) {
                    //ignore
                }
                if (!state.inProgress) {
                    state.channelPool.returnChannel(channel);
                    it.remove();
                }
            }
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
     * Consumer state.
     */
    static class ConsumerState {
        String consumerTag;
        ChannelPool channelPool;
        volatile boolean inProgress;
    }
}
