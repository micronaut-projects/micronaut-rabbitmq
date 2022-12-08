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

import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.RecoveryDelayHandler;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Parameter;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
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
@EachBean(Connection.class)
public class DefaultChannelPool implements AutoCloseable, ChannelPool {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultChannelPool.class);

    private final LinkedBlockingQueue<Channel> channels;
    private final Connection connection;
    private final AtomicLong totalChannels = new AtomicLong(0);
    private final String name;
    private final RecoveryDelayHandler recoveryDelayHandler;
    private final boolean topologyRecoveryEnabled;

    /**
     * Default constructor.
     *
     * @param name The pool name
     * @param connection The connection
     * @param config The connection factory config
     */
    public DefaultChannelPool(@Parameter String name,
                              @Parameter Connection connection,
                              @Parameter RabbitConnectionFactoryConfig config) {
        this.name = name;
        this.connection = connection;
        Integer maxIdleChannels = config.getChannelPool().getMaxIdleChannels().orElse(null);
        this.recoveryDelayHandler = config.params(null).getRecoveryDelayHandler();
        topologyRecoveryEnabled = config.isTopologyRecoveryEnabled();
        this.channels = new LinkedBlockingQueue<>(maxIdleChannels == null ? Integer.MAX_VALUE : maxIdleChannels);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Channel getChannel() throws IOException {
        Channel channel = null;
        while (channel == null) {
            channel = channels.poll();
            if (channel == null) {
                channel = createChannel();
            } else if (!channel.isOpen()) {
                channel = null;
                totalChannels.decrementAndGet();
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Retrieved channel [{}] from the pool", channel.toString());
        }
        return channel;
    }

    @Override
    public Channel getChannelWithRecoveringDelay(int recoveryAttempts) throws IOException, InterruptedException {
        Thread.sleep(recoveryDelayHandler.getDelay(recoveryAttempts));
        return getChannel();
    }

    @Override
    public boolean isTopologyRecoveryEnabled() {
        return topologyRecoveryEnabled;
    }

    @Override
    public void returnChannel(Channel channel) {
        if (channel.isOpen()) {
            if (channels.offer(channel)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Returned channel [{}] to the pool", channel.toString());
                }
            } else {
                closeChannel(channel);
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
        if (totalChannels.get() > channels.size()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Channel pool is being closed without all channels being returned! Any channels not returned are the responsibility of the owner to close. Total channels [{}] - Returned Channels [{}]", totalChannels.get(), channels.size());
            }
        }
        final Iterator<Channel> iterator = channels.iterator();
        while (iterator.hasNext()) {
            closeChannel(iterator.next());
            iterator.remove();
        }
    }

    private void closeChannel(Channel channel) {
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
        totalChannels.decrementAndGet();
    }
}
