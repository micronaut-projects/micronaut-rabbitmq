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

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties.Builder;
import com.rabbitmq.client.Channel;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.configuration.rabbitmq.annotation.RabbitClient;
import io.micronaut.configuration.rabbitmq.annotation.RabbitProperty;
import io.micronaut.configuration.rabbitmq.annotation.Binding;
import io.micronaut.configuration.rabbitmq.connect.ChannelPool;
import io.micronaut.configuration.rabbitmq.reactivex.ReactiveChannel;
import io.micronaut.configuration.rabbitmq.serdes.RabbitMessageSerDesRegistry;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.bind.annotation.Bindable;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.ReturnType;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.messaging.annotation.Body;
import io.micronaut.messaging.annotation.Header;
import io.micronaut.messaging.exceptions.MessagingClientException;
import io.reactivex.Completable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Implementation of the {@link RabbitClient} advice annotation.
 *
 * @author James Kleeh
 * @see RabbitClient
 * @since 1.1.0
 */
@Singleton
public class RabbitMQIntroductionAdvice implements MethodInterceptor<Object, Object> {

    private static final Logger LOG = LoggerFactory.getLogger(RabbitMQIntroductionAdvice.class);

    private final ChannelPool channelPool;
    private final ConversionService<?> conversionService;
    private final RabbitMessageSerDesRegistry serDesRegistry;
    private final Map<String, BiConsumer<Object, Builder>> properties = new HashMap<>();

    /**
     * Default constructor.
     *
     * @param channelPool The pool to retrieve a channel from
     * @param conversionService The conversion service
     * @param serDesRegistry The registry to find a serDes to serialize the body
     */
    public RabbitMQIntroductionAdvice(ChannelPool channelPool,
                                      ConversionService<?> conversionService,
                                      RabbitMessageSerDesRegistry serDesRegistry) {
        this.channelPool = channelPool;
        this.conversionService = conversionService;
        this.serDesRegistry = serDesRegistry;


        properties.put("contentType", (prop, builder) ->
                convert("contentType", prop, String.class, builder::contentType));
        properties.put("contentEncoding", (prop, builder) ->
                convert("contentEncoding", prop, String.class, builder::contentEncoding));
        properties.put("deliveryMode", (prop, builder) ->
                convert("deliveryMode", prop, Integer.class, builder::deliveryMode));
        properties.put("priority", (prop, builder) ->
                convert("priority", prop, Integer.class, builder::priority));
        properties.put("correlationId", (prop, builder) ->
                convert("correlationId", prop, String.class, builder::correlationId));
        properties.put("replyTo", (prop, builder) ->
                convert("replyTo", prop, String.class, builder::replyTo));
        properties.put("expiration", (prop, builder) ->
                convert("expiration", prop, String.class, builder::expiration));
        properties.put("messageId", (prop, builder) ->
                convert("messageId", prop, String.class, builder::messageId));
        properties.put("timestamp", (prop, builder) ->
                convert("timestamp", prop, Date.class, builder::timestamp));
        properties.put("type", (prop, builder) ->
                convert("type", prop, String.class, builder::type));
        properties.put("userId", (prop, builder) ->
                convert("userId", prop, String.class, builder::userId));
        properties.put("appId", (prop, builder) ->
                convert("appId", prop, String.class, builder::appId));
        properties.put("clusterId", (prop, builder) ->
                convert("clusterId", prop, String.class, builder::clusterId));
    }

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {

        if (context.hasAnnotation(RabbitClient.class)) {
            AnnotationValue<RabbitClient> client = context.findAnnotation(RabbitClient.class).orElseThrow(() -> new IllegalStateException("No @RabbitClient annotation present on method: " + context));

            String exchange = client.getValue(String.class).orElse("");

            String routingKey = findRoutingKey(context).orElse("");

            Argument bodyArgument = findBodyArgument(context).orElseThrow(() -> new MessagingClientException("No valid message body argument found for method: " + context));

            Builder builder = new Builder();

            Map<String, Object> headers = new HashMap<>();

            context.getAnnotationValuesByType(Header.class).forEach((header) -> {
                String name = header.get("name", String.class).orElse(null);
                String value = header.getValue(String.class).orElse(null);

                if (StringUtils.isNotEmpty(name) && StringUtils.isNotEmpty(value)) {
                    headers.put(name, value);
                }
            });

            context.getAnnotationValuesByType(RabbitProperty.class).forEach((prop) -> {
                String name = prop.get("name", String.class).orElse(null);
                String value = prop.getValue(String.class).orElse(null);

                if (StringUtils.isNotEmpty(name) && StringUtils.isNotEmpty(value)) {
                    setBasicProperty(builder, name, value);
                }
            });

            Argument[] arguments = context.getArguments();
            Map<String, Object> parameterValues = context.getParameterValueMap();
            for (Argument argument : arguments) {
                AnnotationValue<Header> headerAnn = argument.getAnnotation(Header.class);
                AnnotationValue<RabbitProperty> propertyAnn = argument.getAnnotation(RabbitProperty.class);
                if (headerAnn != null) {
                    Map.Entry<String, Object> entry = getNameAndValue(argument, headerAnn, parameterValues);
                    if (entry.getValue() != null) {
                        headers.put(entry.getKey(), entry.getValue());
                    }
                } else if (propertyAnn != null) {
                    Map.Entry<String, Object> entry = getNameAndValue(argument, propertyAnn, parameterValues);
                    setBasicProperty(builder, entry.getKey(), entry.getValue());
                } else if (argument != bodyArgument) {
                    String argumentName = argument.getName();
                    if (properties.containsKey(argumentName)) {
                        properties.get(argumentName).accept(parameterValues.get(argumentName), builder);
                    }
                }
            }

            if (!headers.isEmpty()) {
                builder.headers(headers);
            }

            AMQP.BasicProperties properties = builder.build();

            if (LOG.isDebugEnabled()) {
                LOG.debug("Sending a message to exchange [{}] with binding [{}] and properties [{}]", exchange, routingKey, properties);
            }

            ReturnType<Object> returnType = context.getReturnType();
            Class javaReturnType = returnType.getType();
            boolean isReactiveReturnType = Publishers.isConvertibleToPublisher(javaReturnType);

            Channel channel = getChannel();

            Object body = parameterValues.get(bodyArgument.getName());
            byte[] converted = serDesRegistry.findSerdes((Class<Object>) bodyArgument.getType())
                    .map(serDes -> serDes.serialize(body))
                    .orElseThrow(() -> new MessagingClientException(String.format("Could not serialize the body argument of type [%s] to a byte[] for publishing", bodyArgument.getType())));

            try {
                if (isReactiveReturnType) {

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Determined the method [{}] returns a reactive type. Sending the message with confirms.", context);
                    }

                    ReactiveChannel reactiveChannel = new ReactiveChannel(channel);
                    Completable completable = reactiveChannel.publish(exchange, routingKey, properties, converted)
                            .doFinally(() -> {
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("The publish has terminated. Returning the channel to the pool");
                                }
                                channelPool.returnChannel(channel);
                            });
                    return conversionService.convert(completable, javaReturnType)
                            .orElseThrow(() -> new MessagingClientException("Could not convert the publisher acknowledgement response to the return type of the method"));
                } else {

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Determined the method [{}] does not return a reactive type. Sending the message without confirms and returning null.", context);
                    }

                    try {
                        channel.basicPublish(exchange, routingKey, properties, converted);
                    } finally {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Returning the channel to the pool");
                        }
                        channelPool.returnChannel(channel);
                    }
                }
            } catch (Throwable e) {
                throw new MessagingClientException(String.format("Failed to publish a message with exchange: [%s] and routing key [%s]", exchange, routingKey), e);
            }

            return null;
        } else {
            // can't be implemented so proceed
            return context.proceed();
        }
    }

    private Map.Entry<String, Object> getNameAndValue(Argument argument, AnnotationValue<?> annotationValue, Map<String, Object> parameterValues) {
        String argumentName = argument.getName();
        String name = annotationValue.get("name", String.class).orElse(annotationValue.getValue(String.class).orElse(argumentName));
        Object value = parameterValues.get(argumentName);

        return new AbstractMap.SimpleEntry<>(name, value);
    }

    private void setBasicProperty(Builder builder, String name, Object value) {
        if (value != null) {
            BiConsumer<Object, Builder> consumer = properties.get(name);
            if (consumer != null) {
                consumer.accept(value, builder);
            } else {
                throw new MessagingClientException(String.format("Attempted to set property [%s], but could not match the name to any of the com.rabbitmq.client.BasicProperties", name));
            }
        }
    }

    private <T> void convert(String name, Object value, Class<T> type, Consumer<? super T> consumer) {
        consumer.accept(conversionService.convert(value, type)
                .orElseThrow(() -> new MessagingClientException(String.format("Attempted to set property [%s], but could not convert the value to the required type [%s]", name, type.getName()))));
    }

    private Optional<String> findRoutingKey(MethodInvocationContext<Object, Object> method) {
        if (method.hasAnnotation(Binding.class)) {
            return Optional.ofNullable(method.getAnnotation(Binding.class).getRequiredValue(String.class));
        } else {
            Map<String, Object> argumentValues = method.getParameterValueMap();
            return Arrays.stream(method.getArguments())
                    .filter(arg -> arg.getAnnotationMetadata().hasAnnotation(Binding.class))
                    .map(Argument::getName)
                    .map(argumentValues::get)
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .findFirst();
        }
    }

    private Optional<Argument> findBodyArgument(ExecutableMethod<?, ?> method) {
        return Optional.ofNullable(Arrays.stream(method.getArguments())
                .filter(arg -> arg.getAnnotationMetadata().hasAnnotation(Body.class))
                .findFirst()
                .orElseGet(() ->
                        Arrays.stream(method.getArguments())
                                .filter(arg -> !arg.getAnnotationMetadata().hasStereotype(Bindable.class))
                                .filter(arg -> !properties.containsKey(arg.getName()))
                                .findFirst()
                                .orElse(null)
                ));
    }

    private Channel getChannel() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Retrieving a channel from the pool");
        }

        try {
            return channelPool.getChannel();
        } catch (IOException e) {
            throw new MessagingClientException("Could not retrieve a channel from the pool", e);
        }
    }
}
