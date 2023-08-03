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
package io.micronaut.rabbitmq.event;

import io.micronaut.inject.MethodReference;

/**
 * Abstract class for RabbitMQ consumer events.
 *
 * @since 4.1.0
 */
public abstract class AbstractRabbitConsumerEvent extends AbstractRabbitEvent<Object> {

    private final MethodReference<?, ?> method;
    private final String queue;

    /**
     * Default constructor.
     *
     * @param bean   The bean annotated as {@link io.micronaut.rabbitmq.annotation.RabbitListener}
     * @param method The consumer method
     * @param queue  The name of the queue the consumer subscribes to
     */
    protected AbstractRabbitConsumerEvent(Object bean, MethodReference<?, ?> method, String queue) {
        super(bean);
        this.method = method;
        this.queue = queue;
    }

    /**
     * @return The name of the queue the consumer subscribes to.
     */
    public String getQueue() {
        return queue;
    }

    /**
     * @return The consumer method.
     */
    public MethodReference<?, ?> getMethod() {
        return method;
    }
}
