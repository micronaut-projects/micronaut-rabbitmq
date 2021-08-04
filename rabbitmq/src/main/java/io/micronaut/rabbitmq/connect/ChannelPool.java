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
     * The default delay to apply for recovery channel getter.
     */
    int DEFAULT_RECOVERY_DELAY = 5000;

    /**
     * Retrieves a channel from the pool. The channel must be returned to the
     * pool after it is no longer being used.
     *
     * @return The channel
     * @throws IOException If a channel needed to be created and encountered an error
     */
    Channel getChannel() throws IOException;

    /**
     * Retrieves a channel from the pool after blocking the thread for a delay period defined by the
     * {@link com.rabbitmq.client.ConnectionFactory#getRecoveryDelayHandler() RecoveryDelayHandler}
     * of the connection for this pool.
     *
     * @param recoveryAttempts the number of recovery attempts so far
     * @return a channel from the pool
     * @throws IOException if a channel needed to be created and encountered an error
     * @throws InterruptedException if the thread was interrupted during the delay period
     */
    default Channel getChannelWithRecoveringDelay(int recoveryAttempts) throws IOException, InterruptedException {
        Thread.sleep(DEFAULT_RECOVERY_DELAY);
        return getChannel();
    }

    /**
     * Returns whether {@link com.rabbitmq.client.ConnectionFactory#isTopologyRecoveryEnabled() topology recovery}
     * is enabled for the connection of this pool.
     *
     * @return true by default
     */
    default boolean isTopologyRecoveryEnabled() {
        return true;
    }

    /**
     * Returns a channel to the pool. No further use of the channel
     * is allowed by the returner.
     *
     * @param channel The channel
     */
    void returnChannel(Channel channel);
}
