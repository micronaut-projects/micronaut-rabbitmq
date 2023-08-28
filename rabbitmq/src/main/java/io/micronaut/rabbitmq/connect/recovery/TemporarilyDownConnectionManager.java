/*
 * Copyright 2017-2023 original authors
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
package io.micronaut.rabbitmq.connect.recovery;

import io.micronaut.rabbitmq.connect.RabbitConnectionFactoryConfig;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static java.util.Collections.synchronizedList;

/**
 * Creates temporarily down RabbitMQ connections and checks periodically if they are eventually up.
 *
 * @author Guillermo Calvo
 * @since 4.1.0
 */
@Singleton
public class TemporarilyDownConnectionManager {

    private static final Logger LOG = LoggerFactory.getLogger(TemporarilyDownConnectionManager.class);

    private final RabbitConnectionFactoryConfig factory;
    private final List<TemporarilyDownAutorecoveringConnection> connections = synchronizedList(new ArrayList<>());

    TemporarilyDownConnectionManager(RabbitConnectionFactoryConfig factory) {
        this.factory = factory;
    }

    /**
     * Creates a new temporarily down RabbitMQ connection.
     *
     * @param executor The executor service.
     * @return a new {@link TemporarilyDownConnection}
     */
    public TemporarilyDownConnection newConnection(ExecutorService executor) {
        LOG.info("Creating a new temporarily down connection");
        final TemporarilyDownAutorecoveringConnection connection = new TemporarilyDownAutorecoveringConnection(factory, executor);
        this.connections.add(connection);
        return connection;
    }

    /**
     * Tries to initialize connections that are still down.
     */
    @Scheduled(initialDelay = "PT10S", fixedRate = "PT60S")
    void checkConnections() {
        LOG.debug("Checking if temporarily down connections are up now");
        this.connections.parallelStream()
            .filter(TemporarilyDownAutorecoveringConnection::isStillDown)
            .forEach(TemporarilyDownAutorecoveringConnection::check);
    }
}
