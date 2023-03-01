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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rabbitmq.client.AMQP;
import io.micronaut.aop.InterceptedMethod;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.bind.annotation.Bindable;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.messaging.annotation.MessageBody;
import io.micronaut.messaging.annotation.MessageHeader;
import io.micronaut.rabbitmq.annotation.Binding;
import io.micronaut.rabbitmq.annotation.RabbitClient;
import io.micronaut.rabbitmq.annotation.RabbitConnection;
import io.micronaut.rabbitmq.annotation.RabbitHeaders;
import io.micronaut.rabbitmq.annotation.RabbitProperty;
import io.micronaut.rabbitmq.bind.RabbitConsumerState;
import io.micronaut.rabbitmq.exception.RabbitClientException;
import io.micronaut.rabbitmq.reactive.RabbitPublishState;
import io.micronaut.rabbitmq.reactive.ReactivePublisher;
import io.micronaut.rabbitmq.serdes.RabbitMessageSerDes;
import io.micronaut.rabbitmq.serdes.RabbitMessageSerDesRegistry;
import io.micronaut.scheduling.TaskExecutors;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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

    private final BeanContext beanContext;
    private final ConversionService conversionService;
    private final RabbitMessageSerDesRegistry serDesRegistry;
    private final Scheduler scheduler;
    private final Map<String, BiConsumer<Object, MutableBasicProperties>> properties = new HashMap<>();
    private final Cache<ExecutableMethod, StaticPublisherState> publisherCache = Caffeine.newBuilder().build();

    /**
     * Default constructor.
     *
     * @param beanContext The bean context
     * @param conversionService The conversion service
     * @param serDesRegistry The registry to find a serDes to serialize the body
     * @param executorService The executor to execute reactive operations on
     */
    public RabbitMQIntroductionAdvice(BeanContext beanContext,
                                      ConversionService conversionService,
                                      RabbitMessageSerDesRegistry serDesRegistry,
                                      @Named(TaskExecutors.IO) ExecutorService executorService) {
        this.beanContext = beanContext;
        this.conversionService = conversionService;
        this.serDesRegistry = serDesRegistry;
        this.scheduler = Schedulers.fromExecutor(executorService);


        properties.put("contentType", (prop, builder) ->
                convert("contentType", prop, String.class, builder::setContentType));
        properties.put("contentEncoding", (prop, builder) ->
                convert("contentEncoding", prop, String.class, builder::setContentEncoding));
        properties.put("deliveryMode", (prop, builder) ->
                convert("deliveryMode", prop, Integer.class, builder::setDeliveryMode));
        properties.put("priority", (prop, builder) ->
                convert("priority", prop, Integer.class, builder::setPriority));
        properties.put("correlationId", (prop, builder) ->
                convert("correlationId", prop, String.class, builder::setCorrelationId));
        properties.put("replyTo", (prop, builder) ->
                convert("replyTo", prop, String.class, builder::setReplyTo));
        properties.put("expiration", (prop, builder) ->
                convert("expiration", prop, String.class, builder::setExpiration));
        properties.put("messageId", (prop, builder) ->
                convert("messageId", prop, String.class, builder::setMessageId));
        properties.put("timestamp", (prop, builder) ->
                convert("timestamp", prop, Date.class, builder::setTimestamp));
        properties.put("type", (prop, builder) ->
                convert("type", prop, String.class, builder::setType));
        properties.put("userId", (prop, builder) ->
                convert("userId", prop, String.class, builder::setUserId));
        properties.put("appId", (prop, builder) ->
                convert("appId", prop, String.class, builder::setAppId));
        properties.put("clusterId", (prop, builder) ->
                convert("clusterId", prop, String.class, builder::setClusterId));
    }

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {

        if (context.hasAnnotation(RabbitClient.class)) {

            StaticPublisherState publisherState = getPublisherState(context);

            String exchange = publisherState.getExchange();
            String routingKey = publisherState.getRoutingKey().orElse(findRoutingKey(context).orElse(""));
            Argument bodyArgument = publisherState.getBodyArgument();

            MutableBasicProperties mutableProperties = new MutableBasicProperties();

            Map<String, Object> headers = publisherState.getHeaders()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            publisherState.getProperties().forEach((name, value) -> {
                setBasicProperty(mutableProperties, name, value);
            });

            Argument[] arguments = context.getArguments();
            Map<String, Object> parameterValues = context.getParameterValueMap();
            for (Argument argument : arguments) {
                AnnotationValue<MessageHeader> headerAnn = argument.getAnnotation(MessageHeader.class);
                AnnotationValue<RabbitProperty> propertyAnn = argument.getAnnotation(RabbitProperty.class);
                boolean headersMap = argument.getAnnotationMetadata().hasAnnotation(RabbitHeaders.class);
                if (headerAnn != null) {
                    Map.Entry<String, Object> entry = getNameAndValue(argument, headerAnn, parameterValues);
                    String name = entry.getKey();
                    Object value = entry.getValue();
                    headers.put(name, value);
                } else if (propertyAnn != null) {
                    Map.Entry<String, Object> entry = getNameAndValue(argument, propertyAnn, parameterValues);
                    String name = entry.getKey();
                    Object value = entry.getValue();
                    setBasicProperty(mutableProperties, name, value);
                } else if (headersMap) {
                    if (!argument.equalsType(Argument.mapOf(String.class, Object.class))) {
                        throw new RabbitClientException("The @RabbitHeaders annotation is applied to an argument that is not java.util.Map<String, ?>.");
                    }
                    Object value = parameterValues.get(argument.getName());
                    headers.putAll((Map) value);
                } else if (argument != bodyArgument) {
                    String argumentName = argument.getName();
                    if (properties.containsKey(argumentName)) {
                        properties.get(argumentName).accept(parameterValues.get(argumentName), mutableProperties);
                    }
                }
            }

            if (!headers.isEmpty()) {
                mutableProperties.setHeaders(headers);
            }

            Object body = parameterValues.get(bodyArgument.getName());
            byte[] converted = publisherState.getSerDes().serialize(body, mutableProperties);

            AMQP.BasicProperties properties = mutableProperties.toBasicProperties();

            if (LOG.isDebugEnabled()) {
                LOG.debug("Sending a message to exchange [{}] with binding [{}] and properties [{}]", exchange, routingKey, properties);
            }

            RabbitPublishState publishState = new RabbitPublishState(exchange, routingKey, properties, converted);
            ReactivePublisher reactivePublisher = publisherState.getReactivePublisher();
            InterceptedMethod interceptedMethod = InterceptedMethod.of(context);

            try {
                boolean replyToSet = StringUtils.isNotEmpty(properties.getReplyTo());
                boolean rpc = replyToSet && !interceptedMethod.returnTypeValue().isVoid();

                Mono<?> reactive;
                if (rpc) {
                    reactive = Mono.from(reactivePublisher.publishAndReply(publishState))
                            .flatMap(consumerState -> {
                                Object deserialized = deserialize(consumerState, publisherState.getDataType(), publisherState.getDataType());
                                if (deserialized == null) {
                                    return Mono.empty();
                                } else {
                                    return Mono.just(deserialized);
                                }
                            });

                    if (interceptedMethod.resultType() == InterceptedMethod.ResultType.SYNCHRONOUS) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Publish is an RPC call. Blocking until a response is received.", context);
                        }
                    } else {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Publish is an RPC call. Publisher will complete when a response is received.", context);
                        }
                        reactive = reactive.subscribeOn(scheduler);
                    }
                } else {
                    if (interceptedMethod.resultType() == InterceptedMethod.ResultType.SYNCHRONOUS) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Sending the message without publisher confirms.", context);
                        }
                        reactive = Mono.from(reactivePublisher.publish(publishState));
                    } else {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Sending the message with publisher confirms.", context);
                        }
                        reactive = Mono.from(reactivePublisher.publishAndConfirm(publishState))
                                .subscribeOn(scheduler);
                    }
                }

                switch (interceptedMethod.resultType()) {
                    case PUBLISHER:
                        return interceptedMethod.handleResult(reactive);
                    case COMPLETION_STAGE:
                        CompletableFuture<Object> future = new CompletableFuture<>();
                        reactive.subscribe(new Subscriber<Object>() {
                            Object value = null;
                            @Override
                            public void onSubscribe(Subscription s) {
                                s.request(1);
                            }

                            @Override
                            public void onNext(Object o) {
                                value = o;
                            }

                            @Override
                            public void onError(Throwable t) {
                                future.completeExceptionally(t);
                            }

                            @Override
                            public void onComplete() {
                                future.complete(value);
                            }
                        });
                        return interceptedMethod.handleResult(future);
                    case SYNCHRONOUS:
                        try {
                            return interceptedMethod.handleResult(reactive.block());
                        } catch (Throwable throwable) {
                            throw new RabbitClientException(String.format("Failed to publish a message with exchange: [%s] and routing key [%s]", exchange, routingKey), throwable, Collections.singletonList(publishState));
                        }
                    default:
                        return interceptedMethod.unsupported();
                }
            } catch (Exception e) {
                return interceptedMethod.handleException(e);
            }
        } else {
            // can't be implemented so proceed
            return context.proceed();
        }
    }

    private StaticPublisherState getPublisherState(MethodInvocationContext<?, ?> context) {
        return publisherCache.get(context.getExecutableMethod(), (method) -> {
            AnnotationValue<RabbitClient> client = method.findAnnotation(RabbitClient.class).orElseThrow(() -> new IllegalStateException("No @RabbitClient annotation present on method: " + method));


            String exchange = client.getValue(String.class).orElse("");
            Optional<AnnotationValue<Binding>> bindingAnn = method.findAnnotation(Binding.class);

            Optional<String> routingKey = bindingAnn.flatMap(b -> b.getValue(String.class));

            String connection = method.findAnnotation(RabbitConnection.class)
                    .flatMap(conn -> conn.get("connection", String.class))
                    .orElse(RabbitConnection.DEFAULT_CONNECTION);

            Argument<?> bodyArgument = findBodyArgument(method)
                    .orElseThrow(() -> new RabbitClientException("No valid message body argument found for method: " + method));

            Map<String, Object> methodHeaders = new HashMap<>();

            List<AnnotationValue<MessageHeader>> headerAnnotations = method.getAnnotationValuesByType(MessageHeader.class);
            Collections.reverse(headerAnnotations); //set the values in the class first so methods can override
            headerAnnotations.forEach((header) -> {
                String name = header.get("name", String.class).orElse(null);
                String value = header.getValue(String.class).orElse(null);

                if (StringUtils.isNotEmpty(name) && StringUtils.isNotEmpty(value)) {
                    methodHeaders.put(name, value);
                }
            });

            Map<String, String> methodProperties = new HashMap<>();

            List<AnnotationValue<RabbitProperty>> propertyAnnotations = method.getAnnotationValuesByType(RabbitProperty.class);
            Collections.reverse(propertyAnnotations); //set the values in the class first so methods can override
            propertyAnnotations.forEach((prop) -> {
                String name = prop.get("name", String.class).orElse(null);
                String value = prop.getValue(String.class).orElse(null);

                if (StringUtils.isNotEmpty(name) && StringUtils.isNotEmpty(value)) {
                    if (this.properties.containsKey(name)) {
                        methodProperties.put(name, value);
                    } else {
                        throw new RabbitClientException(String.format("Attempted to set property [%s], but could not match the name to any of the com.rabbitmq.client.BasicProperties", name));
                    }
                }
            });

            RabbitMessageSerDes<?> serDes = serDesRegistry.findSerdes(bodyArgument).orElseThrow(() -> new RabbitClientException(String.format("Could not find a serializer for the body argument of type [%s]", bodyArgument.getType().getName())));

            ReactivePublisher reactivePublisher;
            try {
                reactivePublisher = beanContext.getBean(ReactivePublisher.class, Qualifiers.byName(connection));
            } catch (Throwable e) {
                throw new RabbitClientException(String.format("Failed to retrieve a publisher named [%s] to publish messages", connection), e);
            }

            return new StaticPublisherState(exchange,
                    routingKey.orElse(null),
                    bodyArgument,
                    methodHeaders,
                    methodProperties,
                    method.getReturnType(),
                    serDes,
                    reactivePublisher);

        });
    }

    private Object deserialize(RabbitConsumerState consumerState, Argument dataType, Argument returnType) {
        Optional<RabbitMessageSerDes<Object>> serDes = serDesRegistry.findSerdes(dataType);
        if (serDes.isPresent()) {
            return serDes.get().deserialize(consumerState, returnType);
        } else {
            throw new RabbitClientException(String.format("Could not find a deserializer for [%s]", dataType.getName()));
        }
    }

    private Map.Entry<String, Object> getNameAndValue(Argument argument, AnnotationValue<?> annotationValue, Map<String, Object> parameterValues) {
        String argumentName = argument.getName();
        String name = annotationValue.get("name", String.class).orElse(annotationValue.getValue(String.class).orElse(argumentName));

        // FIXME? AnnotationValue.get(CharSequence member, ArgumentConversionContext<T> conversionContext)
        //  changed with a recent core 4.0.0-SNAPSHOT and now maps "" to keys where it didn't previously
        //  so this is a defense against that, but is it correct, or is it a bug in core?
        name = "".equals(name) ? argumentName : name;

        Object value = parameterValues.get(argumentName);

        return new AbstractMap.SimpleEntry<>(name, value);
    }

    private void setBasicProperty(MutableBasicProperties mutableProperties, String name, Object value) {
        BiConsumer<Object, MutableBasicProperties> consumer = properties.get(name);
        if (consumer != null) {
            consumer.accept(value, mutableProperties);
        } else {
            throw new RabbitClientException(String.format("Attempted to set property [%s], but could not match the name to any of the com.rabbitmq.client.BasicProperties", name));
        }
    }

    private <T> void convert(String name, Object value, Class<T> type, Consumer<? super T> consumer) {
        if (value == null) {
            consumer.accept(null);
        } else {
            consumer.accept(conversionService.convert(value, type)
                    .orElseThrow(() -> new RabbitClientException(String.format("Attempted to set property [%s], but could not convert the value to the required type [%s]", name, type.getName()))));
        }

    }

    private Optional<String> findRoutingKey(MethodInvocationContext<Object, Object> method) {
        Map<String, Object> argumentValues = method.getParameterValueMap();
        return Arrays.stream(method.getArguments())
                .filter(arg -> arg.getAnnotationMetadata().hasAnnotation(Binding.class))
                .map(Argument::getName)
                .map(argumentValues::get)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .findFirst();
    }

    private Optional<Argument<?>> findBodyArgument(ExecutableMethod<?, ?> method) {
        return Optional.ofNullable(Arrays.stream(method.getArguments())
                .filter(arg -> arg.getAnnotationMetadata().hasAnnotation(MessageBody.class))
                .findFirst()
                .orElseGet(() ->
                        Arrays.stream(method.getArguments())
                                .filter(arg -> !arg.getAnnotationMetadata().hasStereotype(Bindable.class))
                                .filter(arg -> !properties.containsKey(arg.getName()))
                                .findFirst()
                                .orElse(null)
                ));
    }
}
