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

import io.micronaut.context.event.ApplicationEvent;

/**
 * Abstract class for RabbitMQ events.
 *
 * @param <T> The source type
 * @since 4.1.0
 */
public abstract class AbstractRabbitEvent<T> extends ApplicationEvent {

    /**
     * Default constructor.
     *
     * @param source The source
     */
    protected AbstractRabbitEvent(T source) {
        super(source);
    }

    @Override
    public T getSource() {
        return (T) super.getSource();
    }
}
