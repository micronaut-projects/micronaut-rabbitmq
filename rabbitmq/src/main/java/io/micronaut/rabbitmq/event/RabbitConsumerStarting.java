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

/**
 * An event fired before a RabbitMQ consumer subscribes to a queue.
 *
 * @since 4.1.0
 */
public class RabbitConsumerStarting extends AbstractRabbitConsumerEvent {

    /**
     * Default constructor.
     *
     * @param bean   The bean annotated as {@link io.micronaut.rabbitmq.annotation.RabbitListener}
     * @param method The consumer method
     * @param queue  The name of the queue the consumer subscribes to
     */
    public RabbitConsumerStarting(Object bean, String method, String queue) {
        super(bean, method, queue);
    }
}
