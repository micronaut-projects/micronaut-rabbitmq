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

package io.micronaut.configuration.rabbitmq.health;

import com.rabbitmq.client.Connection;
import io.micronaut.configuration.rabbitmq.exception.RabbitClientException;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.health.HealthStatus;
import io.micronaut.management.endpoint.health.HealthEndpoint;
import io.micronaut.management.health.indicator.AbstractHealthIndicator;

import javax.inject.Singleton;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A {@link io.micronaut.management.health.indicator.HealthIndicator} for RabbitMQ.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Requires(property = HealthEndpoint.PREFIX + ".rabbitmq.enabled", notEquals = StringUtils.FALSE)
@Requires(beans = HealthEndpoint.class)
@Singleton
public class RabbitMQHealthIndicator extends AbstractHealthIndicator<Map<String, Object>> {

    private final Connection connection;

    /**
     * Default constructor.
     *
     * @param connection The connection to query for details
     */
    public RabbitMQHealthIndicator(Connection connection) {
        this.connection = connection;
    }

    @Override
    protected Map<String, Object> getHealthInformation() {
        if (connection.isOpen()) {
            healthStatus = HealthStatus.UP;
            return connection.getServerProperties()
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, (entry) -> {
                        Object value = entry.getValue();
                        if (value instanceof Map) {
                            return value;
                        } else {
                            return value.toString();
                        }
                    }));
        } else {
            throw new RuntimeException("RabbitMQ connection is not open");
        }
    }

    @Override
    protected String getName() {
        return "rabbitmq";
    }

}
