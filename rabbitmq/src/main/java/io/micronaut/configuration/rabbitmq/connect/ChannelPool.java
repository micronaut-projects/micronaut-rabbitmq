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

package io.micronaut.configuration.rabbitmq.connect;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A simple channel pool that prevents concurrent usage
 * of any given channel because they are not thread safe.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Singleton
public class ChannelPool implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ChannelPool.class);

    private final LinkedList<Channel> channels = new LinkedList<>();
    private final Connection connection;
    private final AtomicLong totalChannels = new AtomicLong(0);


    /**
     * Default constructor.
     *
     * @param connection The connection to create channels with
     */
    public ChannelPool(Connection connection) {
        this.connection = connection;
    }

    /**
     * Retrieves an available channel or creates one if none
     * are available.
     *
     * @return The channel
     * @throws IOException If a channel needed to be created and encountered an error
     */
    public Channel getChannel() throws IOException {
        Channel channel = null;
        synchronized (channels) {
            while (!channels.isEmpty()) {
                channel = channels.removeFirst();
                if (channel.isOpen()) {
                    break;
                } else {
                    channel = null;
                    totalChannels.decrementAndGet();
                }
            }
        }
        if (channel == null) {
            channel = createChannel();
        }

        return channel;
    }

    /**
     * Returns a channel to the pool. No further use of the channel
     * is allowed.
     *
     * @param channel The channel
     */
    public void returnChannel(Channel channel) {
        if (channel.isOpen()) {
            synchronized (channels) {
                if (!channels.contains(channel)) {
                    channels.addLast(channel);
                }
            }
        } else {
            totalChannels.decrementAndGet();
        }
    }

    /**
     * Creates a channel.
     *
     * @return The newly created channel
     * @throws IOException If an error occurred creating the channel
     */
    protected Channel createChannel() throws IOException {
        Channel channel = connection.createChannel();
        totalChannels.incrementAndGet();
        return channel;
    }

    @Override
    public void close() {
        synchronized (channels) {
            if (totalChannels.get() > channels.size()) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Channel pool is being closed without all channels being returned! Any channels not returned are the responsibility of the owner to close.");
                }
            }
            while (!channels.isEmpty()) {
                Channel channel = channels.removeFirst();
                if (channel.isOpen()) {
                    try {
                        channel.close();
                    } catch (IOException | TimeoutException e) {
                        if (LOG.isWarnEnabled()) {
                            LOG.warn("Failed to close channel", e);
                        }
                    }
                }
            }
        }
    }
}
