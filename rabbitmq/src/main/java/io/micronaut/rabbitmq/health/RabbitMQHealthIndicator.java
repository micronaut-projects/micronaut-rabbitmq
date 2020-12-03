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
package io.micronaut.rabbitmq.health;

import com.rabbitmq.client.Connection;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.health.HealthStatus;
import io.micronaut.management.endpoint.health.HealthEndpoint;
import io.micronaut.management.health.indicator.AbstractHealthIndicator;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
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

    private final List<Connection> connections;

    /**
     * Default constructor.
     *
     * @param connection The connection to query for details
     */
    public RabbitMQHealthIndicator(Connection connection) {
        this.connections = Collections.singletonList(connection);
    }

    /**
     * Default constructor.
     *
     * @param connections The connections to query for details
     */
    @Inject
    public RabbitMQHealthIndicator(List<Connection> connections) {
        this.connections = connections;
    }

    @Override
    protected Map<String, Object> getHealthInformation() {
        if (connections.stream().allMatch(Connection::isOpen)) {
            healthStatus = HealthStatus.UP;
        } else {
            throw new RuntimeException("RabbitMQ connection is not open");
        }
        if (connections.size() == 1) {
            Connection connection = connections.get(0);
            return getDetails(connection);
        } else {
            Map<String, Object> healthInfo = new HashMap<>();
            List<Map<String, Object>> connectionDetails = new ArrayList<>(connections.size());
            healthInfo.put("connections", connectionDetails);
            for (Connection connection: connections) {
                connectionDetails.add(getDetails(connection));
            }
            return healthInfo;
        }
    }

    /**
     * @param connection The connection
     * @return The health details for the connection
     */
    protected Map<String, Object> getDetails(Connection connection) {
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
    }

    @Override
    protected String getName() {
        return "rabbitmq";
    }

}
