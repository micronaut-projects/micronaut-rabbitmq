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

package io.micronaut.configuration.rabbitmq.intercept;

import com.rabbitmq.client.*;
import io.micronaut.configuration.rabbitmq.annotation.Queue;
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener;
import io.micronaut.configuration.rabbitmq.annotation.RabbitProperty;
import io.micronaut.configuration.rabbitmq.bind.RabbitBinderRegistry;
import io.micronaut.configuration.rabbitmq.bind.RabbitMessageCloseable;
import io.micronaut.configuration.rabbitmq.bind.RabbitMessageState;
import io.micronaut.configuration.rabbitmq.connect.ChannelPool;
import io.micronaut.configuration.rabbitmq.exception.RabbitListenerException;
import io.micronaut.configuration.rabbitmq.exception.RabbitListenerExceptionHandler;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.inject.Qualifier;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;

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
    private final ChannelPool channelPool;
    private final RabbitBinderRegistry binderRegistry;
    private final RabbitListenerExceptionHandler exceptionHandler;
    private final ConversionService conversionService;
    private final List<Channel> consumerChannels = new ArrayList<>();

    /**
     * Default constructor.
     *
     * @param beanContext       The bean context
     * @param channelPool       The pool to retrieve channels from
     * @param binderRegistry    The registry to bind arguments to the method
     * @param exceptionHandler  The exception handler to use if the consumer isn't a handler
     * @param conversionService The service to convert consume argument values
     */
    public RabbitMQConsumerAdvice(BeanContext beanContext,
                                  ChannelPool channelPool,
                                  RabbitBinderRegistry binderRegistry,
                                  RabbitListenerExceptionHandler exceptionHandler,
                                  ConversionService conversionService) {
        this.beanContext = beanContext;
        this.channelPool = channelPool;
        this.binderRegistry = binderRegistry;
        this.exceptionHandler = exceptionHandler;
        this.conversionService = conversionService;
    }

    @Override
    public void process(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {

        AnnotationValue<Queue> queueAnn = method.getAnnotation(Queue.class);

        if (queueAnn != null) {
            String queue = queueAnn.getRequiredValue(String.class);

            String clientTag = method.getDeclaringType().getSimpleName() + '#' + method.toString();

            boolean reQueue = queueAnn.getRequiredValue("reQueue", boolean.class);
            boolean exclusive = queueAnn.getRequiredValue("exclusive", boolean.class);

            boolean hasAckArg = Arrays.stream(method.getArguments())
                    .anyMatch(arg -> Acknowledgement.class.isAssignableFrom(arg.getType()));

            Channel channel = getChannel();

            consumerChannels.add(channel);

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

            Object bean = beanContext.findBean(beanType, qualifer).orElseThrow(() -> new MessageListenerException("Could not find the bean to execute the method " + method));

            try {
                DefaultExecutableBinder<RabbitMessageState> binder = new DefaultExecutableBinder<>();

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Registering a consumer to queue [{}] with client tag [{}]", queue, clientTag);
                }

                channel.basicConsume(queue, false, clientTag, false, exclusive, arguments, new DefaultConsumer() {

                    @Override
                    public void handleTerminate(String consumerTag) {
                        if (consumerChannels.contains(channel)) {
                            channelPool.returnChannel(channel);
                            consumerChannels.remove(channel);
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("The channel was terminated. The consumer [{}] will no longer receive messages", clientTag);
                            }
                        }
                    }

                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                        RabbitMessageState state = new RabbitMessageState(envelope, properties, body, channel);

                        BoundExecutable boundExecutable = null;
                        try {
                            boundExecutable = binder.bind(method, binderRegistry, state);
                        } catch (Throwable e) {
                            handleException(new RabbitListenerException("An error occurred binding the message to the method", e, bean, state));
                        }

                        if (boundExecutable != null) {
                            try (RabbitMessageCloseable closeable = new RabbitMessageCloseable(state, false, reQueue)) {
                                Object returnedValue = boundExecutable.invoke(bean);

                                if (!hasAckArg) {
                                    if (method.getReturnType().getType().equals(Boolean.class)) {
                                        Boolean ack = (Boolean) returnedValue;
                                        closeable.withAcknowledge(ack == null ? false : ack);
                                    } else {
                                        closeable.withAcknowledge(true);
                                    }
                                }
                            } catch (MessageAcknowledgementException e) {
                                throw e;
                            } catch (Throwable e) {
                                handleException(new RabbitListenerException("An error occurred executing the listener", e, bean, state));
                            }
                        } else {
                            new RabbitMessageCloseable(state, false, reQueue).withAcknowledge(false).close();
                        }
                    }
                });
            } catch (MessageAcknowledgementException e) {
                if (!channel.isOpen()) {
                    channelPool.returnChannel(channel);
                    consumerChannels.remove(channel);
                    if (LOG.isErrorEnabled()) {
                        LOG.error("The channel was closed due to an exception. The consumer [{}] will no longer receive messages", clientTag);
                    }
                }
                handleException(new RabbitListenerException(e.getMessage(), e, bean, null));
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
        for (Channel channel : consumerChannels) {
            channelPool.returnChannel(channel);
        }
        consumerChannels.clear();
    }

    /**
     * @return A channel to publish with
     */
    protected Channel getChannel() {
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
}
