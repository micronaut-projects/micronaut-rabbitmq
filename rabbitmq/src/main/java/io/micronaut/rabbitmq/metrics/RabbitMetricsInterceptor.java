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
package io.micronaut.rabbitmq.metrics;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.impl.MicrometerMetricsCollector;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micronaut.configuration.metrics.annotation.RequiresMetrics;
import io.micronaut.context.BeanProvider;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.convert.format.MapFormat;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Singleton;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.micronaut.configuration.metrics.micrometer.MeterRegistryFactory.MICRONAUT_METRICS_BINDERS;

/**
 * Interceptor of the connection factory to set the metrics collector.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Singleton
@RequiresMetrics
@Requires(property = MICRONAUT_METRICS_BINDERS + ".rabbitmq.enabled", notEquals = StringUtils.FALSE)
public class RabbitMetricsInterceptor implements BeanCreatedEventListener<ConnectionFactory> {

    private final BeanProvider<MeterRegistry> meterRegistryBeanProvider;
    private final String prefix;
    private final List<Tag> tags;

     /**
     * Default constructor.
     *
     * @param meterRegistryBeanProvider The meter registry bean provider
     * @param prefix                    The prefix
     * @param tags                      The tags
     */
    public RabbitMetricsInterceptor(
            BeanProvider<MeterRegistry> meterRegistryBeanProvider,
            @Nullable @Property(name = MICRONAUT_METRICS_BINDERS + ".rabbitmq.prefix") String prefix,
            @Property(name = MICRONAUT_METRICS_BINDERS + ".rabbitmq.tags")
            @MapFormat(transformation = MapFormat.MapTransformation.FLAT)
                    Map<String, String> tags) {
        this.meterRegistryBeanProvider = meterRegistryBeanProvider;
        this.prefix = prefix == null ? "rabbitmq" : prefix;
        if (CollectionUtils.isNotEmpty(tags)) {
            this.tags = tags.entrySet().stream().map(entry -> Tag.of(entry.getKey(), entry.getValue())).collect(Collectors.toList());
        } else {
            this.tags = Collections.emptyList();
        }

    }

    @Override
    public ConnectionFactory onCreated(BeanCreatedEvent<ConnectionFactory> event) {
        ConnectionFactory connectionFactory = event.getBean();
        connectionFactory.setMetricsCollector(new MicrometerMetricsCollector(meterRegistryBeanProvider.get(), prefix, tags));
        return connectionFactory;
    }
}
