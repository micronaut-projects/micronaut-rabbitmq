/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.rabbitmq.connect

import com.rabbitmq.client.Connection
import io.micronaut.context.ApplicationContext
import io.micronaut.context.BeanLocator
import io.micronaut.context.annotation.Retain
import io.micronaut.context.event.ApplicationEventPublisher
import io.micronaut.core.value.PropertyResolver
import io.micronaut.inject.BeanDefinition
import io.micronaut.inject.qualifiers.Qualifiers
import io.micronaut.rabbitmq.connect.recovery.TemporarilyDownConnectionManager
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.executor.ExecutorFactory
import spock.lang.Specification

import java.util.concurrent.ExecutorService

/**
 * Development mode retains a bean across a restart only when what it received, transitively, holds nothing bound to
 * the context: the connection and the channel pool, the factory and the configuration, and the consumer executor
 * service the connection dispatches on.
 */
class ConnectionRetentionSpec extends Specification {

    void "the connection and the channel pool carry @Retain and receive nothing bound to the context"() {
        given:
        ApplicationContext context = ApplicationContext.run(['rabbitmq.uri': 'amqp://localhost:5672'])

        when:
        List<BeanDefinition<?>> retained = [Connection, ChannelPool].collect { context.getBeanDefinition(it) }
        List<BeanDefinition<?>> closure = retained + [
                context.getBeanDefinition(RabbitConnectionFactory),
                context.getBeanDefinition(RabbitConnectionFactoryConfig),
                context.getBeanDefinition(TemporarilyDownConnectionManager),
                context.getBeanDefinition(ExecutorService, Qualifiers.byName(TaskExecutors.MESSAGE_CONSUMER)),
                // the factory of the executor service, which the retained connection holds with it
                context.getBeanDefinition(ExecutorFactory),
        ]

        then:
        retained.every { it.getAnnotationMetadata().getDeclaredMetadata().stringValues(Retain, "invalidatedBy") as List == ['rabbitmq', 'micronaut.executors'] }
        closure.every { BeanDefinition<?> definition ->
            !definition.isProxy() && definition.requiredComponents.every { Class<?> type ->
                !BeanLocator.isAssignableFrom(type)
                        && !PropertyResolver.isAssignableFrom(type)
                        && !ApplicationEventPublisher.isAssignableFrom(type)
            }
        }

        cleanup:
        context.close()
    }
}
