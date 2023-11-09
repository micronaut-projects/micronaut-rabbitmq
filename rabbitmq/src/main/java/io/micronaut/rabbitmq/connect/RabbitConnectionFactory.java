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

import com.rabbitmq.client.Address;
import com.rabbitmq.client.Connection;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.event.BeanPreDestroyEvent;
import io.micronaut.context.event.BeanPreDestroyEventListener;
import io.micronaut.context.exceptions.BeanInstantiationException;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.rabbitmq.connect.recovery.TemporarilyDownConnectionManager;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

/**
 * A factory for creating a connection to RabbitMQ.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Factory
public class RabbitConnectionFactory implements BeanPreDestroyEventListener<ExecutorService> {
    private static final Logger LOG = LoggerFactory.getLogger(RabbitConnectionFactory.class);
    private final ConcurrentLinkedQueue<ActiveConnection> activeConnections = new ConcurrentLinkedQueue<>();

    /**
     * @param connectionFactory The factory to create the connection
     * @param beanContext The bean context to dynamically retrieve the executor service
     * @return The connection
     * @deprecated Use {@link RabbitConnectionFactory#connection(RabbitConnectionFactoryConfig, TemporarilyDownConnectionManager, BeanContext)}
     */
    @Deprecated(since = "4.2.0", forRemoval = true)
    Connection connection(RabbitConnectionFactoryConfig connectionFactory,
                          BeanContext beanContext) {
        return connection(connectionFactory, beanContext.getBean(TemporarilyDownConnectionManager.class), beanContext);
    }

    /**
     * @param connectionFactory The factory to create the connection
     * @param temporarilyDownConnectionManager The temporarily down connection manager
     * @param beanContext The bean context to dynamically retrieve the executor service
     * @return The connection
     * @since 4.2.0
     */
    @Singleton
    @EachBean(RabbitConnectionFactoryConfig.class)
    Connection connection(RabbitConnectionFactoryConfig connectionFactory,
                          TemporarilyDownConnectionManager temporarilyDownConnectionManager,
                          BeanContext beanContext) {
        try {
            ExecutorService executorService = beanContext.getBean(ExecutorService.class, Qualifiers.byName(connectionFactory.getConsumerExecutor()));
            Connection connection = newConnection(connectionFactory, temporarilyDownConnectionManager, executorService);
            activeConnections.add(new ActiveConnection(connection, connectionFactory, executorService));
            return connection;
        } catch (IOException | TimeoutException e) {
            throw new BeanInstantiationException("Error creating connection to RabbitMQ", e);
        }
    }

    private Connection newConnection(RabbitConnectionFactoryConfig factory, TemporarilyDownConnectionManager temporarilyDownConnectionManager, ExecutorService executorService) throws IOException, TimeoutException {
        Optional<List<Address>> addresses = factory.getAddresses();
        try {
            if (addresses.isPresent()) {
                return factory.newConnection(executorService, addresses.get());
            }
            return factory.newConnection(executorService);
        } catch (ConnectException e) {
            // Check if automatic recovery is enabled
            if (factory.isAutomaticRecoveryEnabled()) {
                // Create a "temporarily down" connection that may be up eventually
                return temporarilyDownConnectionManager.newConnection(factory, executorService);
            }
            throw e;
        }
    }

    /**
     * Closes active connections.
     */
    @PreDestroy
    void shutdownConnections() {
        try {
            activeConnections.forEach(ActiveConnection::tryClose);
        } finally {
            this.activeConnections.clear();
        }
    }

    /**
     * Closes active connections associated with the {@link ExecutorService} prior to its removal.
     *
     * @param event The bean created event
     * @return The unmodified {@link ExecutorService} bean
     */
    @Override
    @NonNull
    public ExecutorService onPreDestroy(@NonNull BeanPreDestroyEvent<ExecutorService> event) {
        activeConnections.stream().filter(activeConnection ->
            activeConnection.executorService() == event.getBean()).forEach(ActiveConnection::tryClose);
        activeConnections.removeIf(activeConnection -> !activeConnection.connection().isOpen());
        return event.getBean();
    }

    private record ActiveConnection(Connection connection,
                                    RabbitConnectionFactoryConfig connectionFactory, ExecutorService executorService) {

        private void tryClose() {
            Connection connection = connection();
            if (connection.isOpen()) {
                try {
                    connection.close(connectionFactory().getShutdownTimeout());
                } catch (Exception e) {
                    LOG.warn("Error closing RabbitMQ connection: " + e.getMessage(), e);
                }
            }
        }
    }
}
