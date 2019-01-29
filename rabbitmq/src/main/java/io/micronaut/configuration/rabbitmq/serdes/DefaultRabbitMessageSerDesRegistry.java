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

package io.micronaut.configuration.rabbitmq.serdes;

import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Optional;

/**
 * Default implementation of {@link RabbitMessageSerDesRegistry}.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Singleton
public class DefaultRabbitMessageSerDesRegistry implements RabbitMessageSerDesRegistry {

    private final RabbitMessageSerDes<?>[] serDes;

    /**
     * Default constructor.
     *
     * @param serDes The serdes to be registered.
     */
    public DefaultRabbitMessageSerDesRegistry(RabbitMessageSerDes<?>... serDes) {
        this.serDes = serDes;
    }

    @Override
    public <T> Optional<RabbitMessageSerDes<T>> findSerdes(Class<T> type) {
        return Arrays.stream(serDes)
                .filter(serDes -> serDes.supports((Class) type))
                .map(serDes -> (RabbitMessageSerDes<T>) serDes)
                .findFirst();
    }
}
