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

package io.micronaut.configuration.rabbitmq.connect;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.exceptions.BeanInstantiationException;
import io.micronaut.scheduling.TaskExecutors;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

/**
 * A factory for creating a connection to RabbitMQ.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Factory
public class RabbitConnectionFactory {

    /**
     * @param connectionFactory The factory to create the connection
     * @param executorService The executor service consumers will be executed on
     * @return The connection
     */
    @Bean(preDestroy = "close")
    @Singleton
    Connection connection(ConnectionFactory connectionFactory,
                          @Named(TaskExecutors.MESSAGE_CONSUMER) ExecutorService executorService) {
        try {
            return connectionFactory.newConnection(executorService);
        } catch (IOException | TimeoutException e) {
            throw new BeanInstantiationException("Error creating connection to RabbitMQ", e);
        }
    }
}
