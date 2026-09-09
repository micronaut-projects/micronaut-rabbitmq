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

import com.rabbitmq.client.Channel;

import java.io.IOException;

/**
 * Initializes a channel before consumers or producers are created.
 *
 * @author James Kleeh
 * @since 5.10.0
 */
@FunctionalInterface
public interface ChannelPoolInitializer {

    /**
     * Do any work with a channel.
     *
     * @param channel The channel to use
     * @param name The name of the channel pool, like configured under {@code rabbitmq.servers}
     * @throws IOException If any error occurs
     */
    void initialize(Channel channel, String name) throws IOException;
}
