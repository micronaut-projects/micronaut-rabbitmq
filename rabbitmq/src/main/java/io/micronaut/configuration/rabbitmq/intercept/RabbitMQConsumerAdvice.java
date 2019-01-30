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
import io.micronaut.configuration.rabbitmq.bind.RabbitMessageState;
import io.micronaut.configuration.rabbitmq.connect.ChannelPool;
import io.micronaut.context.BeanContext;
import io.micronaut.context.processor.ExecutableMethodProcessor;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.bind.BoundExecutable;
import io.micronaut.core.bind.DefaultExecutableBinder;
import io.micronaut.core.bind.exceptions.UnsatisfiedArgumentException;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.messaging.exceptions.MessageListenerException;

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

    private final BeanContext beanContext;
    private final ChannelPool channelPool;
    private final RabbitBinderRegistry binderRegistry;
    private final List<Channel> consumerChannels = new ArrayList<>();

    /**
     * Default constructor.
     *
     * @param beanContext                  The bean context
     * @param channelPool                  The pool to retrieve channels from
     * @param binderRegistry               The registry to bind arguments to the method
     */
    public RabbitMQConsumerAdvice(BeanContext beanContext,
                                  ChannelPool channelPool,
                                  RabbitBinderRegistry binderRegistry) {
        this.beanContext = beanContext;
        this.channelPool = channelPool;
        this.binderRegistry = binderRegistry;
    }

    @Override
    public void process(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {

        AnnotationValue<Queue> queueAnn = method.findAnnotation(Queue.class).orElseThrow(() -> new MessageListenerException("No queue specified for method " + method));
        String queue = queueAnn.getRequiredValue(String.class);

        String clientTag = method.getDeclaringType().getSimpleName() + '#' + method.toString();

        boolean reQueue = queueAnn.getRequiredValue("reQueue", boolean.class);
        boolean exclusive = queueAnn.getRequiredValue("exclusive", boolean.class);

        Channel channel = getChannel();

        consumerChannels.add(channel);

        Map<String, Object> arguments = new HashMap<>();

        method.getAnnotationValuesByType(RabbitProperty.class).forEach((prop) -> {
            String name = prop.getRequiredValue("name", String.class);
            String value = prop.getValue(String.class).orElse(null);

            if (StringUtils.isNotEmpty(name) && StringUtils.isNotEmpty(value)) {
                arguments.put(name, value);
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

            channel.basicConsume(queue, false, clientTag, false, exclusive, arguments, new DefaultConsumer() {

                @Override
                public void handleTerminate(String consumerTag) {
                    channelPool.returnChannel(channel);
                    consumerChannels.remove(channel);
                }

                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    long deliveryTag = envelope.getDeliveryTag();
                    RabbitMessageState state = new RabbitMessageState(envelope, properties, body);
                    boolean ack = false;

                    try {
                        Object returnedValue = ((BoundExecutable) binder.bind(method, binderRegistry, state)).invoke(bean);

                        if (returnedValue instanceof Boolean) {
                            ack = ((Boolean) returnedValue);
                        } else {
                            ack = true;
                        }
                    } catch (UnsatisfiedArgumentException e) {
                        throw e;
                    } catch (Throwable e) {
                        throw new MessageListenerException("An error occurred executing the listener", e);
                    } finally {
                        if (ack) {
                            channel.basicAck(deliveryTag, false);
                        } else {
                            channel.basicNack(deliveryTag, false, reQueue);
                        }
                    }
                }
            });
        } catch (IOException e) {
            throw new MessageListenerException("An error occurred subscribing to a queue", e);
        } finally {
            channelPool.returnChannel(channel);
            consumerChannels.remove(channel);
        }

    }

    @PreDestroy
    @Override
    public void close() throws Exception {
        for (Channel channel : consumerChannels) {
            channelPool.returnChannel(channel);
        }
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
}
