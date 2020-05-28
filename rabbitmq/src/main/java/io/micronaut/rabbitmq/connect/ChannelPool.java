/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.rabbitmq.connect;

import com.rabbitmq.client.Channel;
import io.micronaut.core.naming.Named;

import java.io.IOException;

/**
 * A pool of {@link Channel}s to allow for channels to be shared across
 * threads but not at the same time. A channel retrieved from the pool should
 * not be accessible to any other threads until the channel is returned from
 * the pool.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
public interface ChannelPool extends Named {

    /**
     * Retrieves a channel from the pool. The channel must be returned to the
     * pool after it is no longer being used.
     *
     * @return The channel
     * @throws IOException If a channel needed to be created and encountered an error
     */
    Channel getChannel() throws IOException;

    /**
     * Returns a channel to the pool. No further use of the channel
     * is allowed by the returner.
     *
     * @param channel The channel
     */
    void returnChannel(Channel channel);
}
