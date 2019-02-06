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

import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default implementation of {@link ChannelPool}. There is no limit
 * to the number of channels that can be created. If the channel list is empty
 * a new channel will be created and returned. The maximum number of channels
 * should be the maximum number of concurrent operations.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Singleton
public class DefaultChannelPool implements AutoCloseable, ChannelPool {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultChannelPool.class);

    private final LinkedList<Channel> channels = new LinkedList<>();
    private final Connection connection;
    private final AtomicLong totalChannels = new AtomicLong(0);

    /**
     * Default constructor.
     *
     * @param connection The connection to create channels with
     */
    public DefaultChannelPool(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Channel getChannel() throws IOException {
        Channel channel = null;
        synchronized (channels) {
            while (!channels.isEmpty()) {
                channel = channels.removeFirst();
                if (channel.isOpen()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Retrieved channel [{}] from the pool", channel.toString());
                    }
                    break;
                } else {
                    channel = null;
                    totalChannels.decrementAndGet();
                }
            }
        }
        if (channel == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No channels are in the pool. Creating a new channel");
            }
            channel = createChannel();
        }

        return channel;
    }

    @Override
    public void returnChannel(Channel channel) {
        if (channel.isOpen()) {
            synchronized (channels) {
                if (!channels.contains(channel)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Returning channel [{}] to the pool", channel.toString());
                    }
                    channels.addLast(channel);
                }
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Attempted to return a closed channel to the pool [{}]. Channel has been ignored", channel.toString());
            }
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

    @PreDestroy
    @Override
    public void close() {
        synchronized (channels) {
            if (totalChannels.get() > channels.size()) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Channel pool is being closed without all channels being returned! Any channels not returned are the responsibility of the owner to close. Total channels [{}] - Returned Channels [{}]", totalChannels.get(), channels.size());
                }
            }
            while (!channels.isEmpty()) {
                Channel channel = channels.removeFirst();
                if (channel.isOpen()) {
                    try {
                        channel.close();
                    } catch (AlreadyClosedException e) {
                        //no-op
                    } catch (IOException | TimeoutException e) {
                        if (LOG.isWarnEnabled()) {
                            LOG.warn(String.format("Failed to close the channel [%s]", channel.toString()), e);
                        }
                    }
                }
            }
        }
    }
}
