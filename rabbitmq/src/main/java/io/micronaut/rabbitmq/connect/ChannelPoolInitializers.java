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
package io.micronaut.rabbitmq.connect;

import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * Applies {@link ChannelPoolInitializer} beans that do not use the legacy
 * {@link ChannelInitializer} base class.
 *
 * @author James Kleeh
 * @since 5.10.0
 */
@Singleton
final class ChannelPoolInitializers implements BeanCreatedEventListener<ChannelPool> {

    private final List<ChannelPoolInitializer> initializers;

    ChannelPoolInitializers(List<ChannelPoolInitializer> initializers) {
        this.initializers = initializers;
    }

    @Override
    public ChannelPool onCreated(BeanCreatedEvent<ChannelPool> event) {
        for (ChannelPoolInitializer initializer : initializers) {
            if (!(initializer instanceof ChannelInitializer)) {
                ChannelInitializer.initialize(event, initializer);
            }
        }
        return event.getBean();
    }
}
