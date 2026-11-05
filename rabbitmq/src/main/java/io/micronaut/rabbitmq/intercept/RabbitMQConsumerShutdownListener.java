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
package io.micronaut.rabbitmq.intercept;

import io.micronaut.context.BeanContext;
import io.micronaut.context.BeanRegistration;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.ShutdownEvent;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.order.Ordered;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stops the {@link io.micronaut.rabbitmq.annotation.RabbitListener} consumers when the {@link ShutdownEvent} is
 * published, before any singleton is destroyed.
 *
 * <p>Without this, the consumers are only cancelled when {@link RabbitMQConsumerAdvice} is destroyed, and a bean
 * that a listener uses (for example a {@code DataSource}) may be destroyed first, since the advice has no dependency
 * on it. Stopping here waits for the deliveries being handled, so they finish against live dependencies.</p>
 *
 * <p>This runs after graceful shutdown (when it is enabled), which has already drained the consumers within its
 * grace period. In that case this only waits for what graceful shutdown did not.</p>
 *
 * <p>Only an advice that has already been created is stopped: listening to the event must not create one during
 * shutdown.</p>
 *
 * @since 5.1.1
 */
@Singleton
@Requires(bean = RabbitMQConsumerAdvice.class)
@Internal
final class RabbitMQConsumerShutdownListener implements ApplicationEventListener<ShutdownEvent>, Ordered {

    /**
     * Runs right after {@code io.micronaut.runtime.graceful.GracefulShutdownListener}, which has
     * {@link Ordered#HIGHEST_PRECEDENCE}, and before other shutdown listeners.
     */
    static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 100;

    private static final Logger LOG = LoggerFactory.getLogger(RabbitMQConsumerShutdownListener.class);

    private final BeanContext beanContext;

    RabbitMQConsumerShutdownListener(BeanContext beanContext) {
        this.beanContext = beanContext;
    }

    @Override
    public void onApplicationEvent(ShutdownEvent event) {
        for (BeanRegistration<RabbitMQConsumerAdvice> registration : beanContext.getActiveBeanRegistrations(RabbitMQConsumerAdvice.class)) {
            try {
                registration.bean().stopConsumers();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                LOG.error("Error stopping RabbitMQ consumers on shutdown", e);
            }
        }
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
