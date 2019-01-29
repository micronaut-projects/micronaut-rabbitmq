package io.micronaut.configuration.rabbitmq.connect;

import com.rabbitmq.client.Channel;

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
public interface ChannelPool {

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
