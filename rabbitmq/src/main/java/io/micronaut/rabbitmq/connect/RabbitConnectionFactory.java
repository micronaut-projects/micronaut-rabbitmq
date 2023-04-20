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
package io.micronaut.rabbitmq.connect;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.Connection;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.exceptions.BeanInstantiationException;
import io.micronaut.inject.qualifiers.Qualifiers;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory for creating a connection to RabbitMQ.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Factory
public class RabbitConnectionFactory {
    private static final Logger LOG = LoggerFactory.getLogger(RabbitConnectionFactory.class);
    private final ConcurrentLinkedQueue<ActiveConnection> activeConnections = new ConcurrentLinkedQueue<>();

    /**
     * @param connectionFactory The factory to create the connection
     * @param beanContext The bean context to dynamically retrieve the executor service
     * @return The connection
     */
    @Singleton
    @EachBean(RabbitConnectionFactoryConfig.class)
    Connection connection(RabbitConnectionFactoryConfig connectionFactory,
                          BeanContext beanContext) {
        try {
            ExecutorService executorService = beanContext.getBean(ExecutorService.class, Qualifiers.byName(connectionFactory.getConsumerExecutor()));
            Optional<List<Address>> addresses = connectionFactory.getAddresses();
            Connection connection;
            if (addresses.isPresent()) {
                connection = connectionFactory.newConnection(executorService, addresses.get());
            } else {
                connection = connectionFactory.newConnection(executorService);
            }
            activeConnections.add(new ActiveConnection(connection, connectionFactory));
            return connection;
        } catch (IOException | TimeoutException e) {
            throw new BeanInstantiationException("Error creating connection to RabbitMQ", e);
        }
    }

    /**
     * Closes active connections.
     */
    @PreDestroy
    void shutdownConnections() {
        try {
            for (ActiveConnection activeConnection : activeConnections) {
                Connection connection = activeConnection.connection();
                if (connection.isOpen()) {
                    try {
                        connection.close(activeConnection.connectionFactory().getShutdownTimeout());
                    } catch (Exception e) {
                        LOG.warn("Error closing RabbitMQ connection: " + e.getMessage(), e);
                    }
                }
            }
        } finally {
            this.activeConnections.clear();
        }
    }

    private record ActiveConnection(Connection connection,
                                    RabbitConnectionFactoryConfig connectionFactory) {
    }
}
