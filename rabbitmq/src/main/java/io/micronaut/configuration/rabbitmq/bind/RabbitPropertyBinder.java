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

package io.micronaut.configuration.rabbitmq.bind;

import com.rabbitmq.client.AMQP;
import io.micronaut.configuration.rabbitmq.annotation.RabbitProperty;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionService;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Binds an argument of with the {@link RabbitProperty} annotation from the {@link RabbitMessageState}.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Singleton
public class RabbitPropertyBinder implements RabbitAnnotatedArgumentBinder<RabbitProperty> {

    private final Map<String, Function<AMQP.BasicProperties, Object>> properties = new HashMap<>();
    private final ConversionService conversionService;

    /**
     * Default constructor.
     *
     * @param conversionService The conversion service to convert the body
     */
    public RabbitPropertyBinder(ConversionService conversionService) {
        this.conversionService = conversionService;
        properties.put("contentType", AMQP.BasicProperties::getContentType);
        properties.put("contentEncoding", AMQP.BasicProperties::getContentEncoding);
        properties.put("deliveryMode", AMQP.BasicProperties::getDeliveryMode);
        properties.put("priority", AMQP.BasicProperties::getPriority);
        properties.put("correlationId", AMQP.BasicProperties::getCorrelationId);
        properties.put("replyTo", AMQP.BasicProperties::getReplyTo);
        properties.put("expiration", AMQP.BasicProperties::getExpiration);
        properties.put("messageId", AMQP.BasicProperties::getMessageId);
        properties.put("timestamp", AMQP.BasicProperties::getTimestamp);
        properties.put("type", AMQP.BasicProperties::getType);
        properties.put("userId", AMQP.BasicProperties::getUserId);
        properties.put("appId", AMQP.BasicProperties::getAppId);
        properties.put("clusterId", AMQP.BasicProperties::getClusterId);
    }

    @Override
    public Class<RabbitProperty> getAnnotationType() {
        return RabbitProperty.class;
    }

    @Override
    public BindingResult<Object> bind(ArgumentConversionContext<Object> context, RabbitMessageState source) {
        String parameterName = context.getAnnotationMetadata().getValue(RabbitProperty.class, String.class).orElse(context.getArgument().getName());

        return () -> Optional.ofNullable(properties.get(parameterName))
                .map(f -> f.apply(source.getProperties()))
                .flatMap(prop -> conversionService.convert(prop, context));
    }
}
