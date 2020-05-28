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
package io.micronaut.configuration.rabbitmq.connect;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.ConnectionFactory;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.scheduling.TaskExecutors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Base class for RabbitMQ to be configured.
 *
 * @author James Kleeh
 * @since 1.0.0
 */
public abstract class RabbitConnectionFactoryConfig extends ConnectionFactory {

    private static final String DEFAULT_CONSUMER_EXECUTOR = TaskExecutors.MESSAGE_CONSUMER;

    private final String name;
    private RpcConfiguration rpc = new RpcConfiguration();
    private ChannelPoolConfiguration channelPool = new ChannelPoolConfiguration();
    private List<Address> addresses = null;
    private String consumerExecutor = DEFAULT_CONSUMER_EXECUTOR;

    /**
     * Default constructor.
     *
     * @param name The name of the configuration
     */
    public RabbitConnectionFactoryConfig(@Parameter String name) {
        this.name = name;
    }

    /**
     * @return The name qualifier
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return The RPC configuration
     */
    public RpcConfiguration getRpc() {
        return rpc;
    }

    /**
     * Sets the RPC configuration.
     *
     * @param rpc The RPC configuration
     */
    public void setRpc(@Nonnull RpcConfiguration rpc) {
        this.rpc = rpc;
    }

    /**
     * @return The channel pool configuration
     */
    public ChannelPoolConfiguration getChannelPool() {
        return channelPool;
    }

    /**
     * Sets the channel pool configuration.
     *
     * @param channelPool The channel pool configuration
     */
    public void setChannelPool(@Nonnull ChannelPoolConfiguration channelPool) {
        this.channelPool = channelPool;
    }

    /**
     * @return An optional list of addresses
     */
    public Optional<List<Address>> getAddresses() {
        return Optional.ofNullable(addresses);
    }

    /**
     * Sets the addresses to be passed to {@link ConnectionFactory#newConnection(List)}.
     *
     * @param addresses The list of addresses
     */
    public void setAddresses(@Nullable List<Address> addresses) {
        this.addresses = addresses;
    }

    /**
     * @return The executor service name that consumers should be executed on
     */
    public String getConsumerExecutor() {
        return consumerExecutor;
    }

    /**
     * Sets the name of which executor service consumers should be executed on. Default {@value #DEFAULT_CONSUMER_EXECUTOR}.
     *
     * @param consumerExecutor The consumer executor service name.
     */
    public void setConsumerExecutor(@Nonnull String consumerExecutor) {
        this.consumerExecutor = consumerExecutor;
    }


    /**
     * Configuration for RPC.
     */
    public static class RpcConfiguration {

        /**
         * The default timeout before cancelling an RPC call.
         */
        public static final long DEFAULT_TIMEOUT_SECONDS = 10;

        private Duration timeout = Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS);

        /**
         * @return The timeout duration before cancelling an RPC call.
         */
        public Optional<Duration> getTimeout() {
            return Optional.ofNullable(timeout);
        }

        /**
         * Sets the timeout duration before cancelling an RPC call. Default {@value #DEFAULT_TIMEOUT_SECONDS} seconds.
         *
         * @param timeout The timeout
         */
        public void setTimeout(@Nullable Duration timeout) {
            this.timeout = timeout;
        }
    }

    /**
     * Configuration for the channel pool.
     */
    public static class ChannelPoolConfiguration {

        private Integer maxIdleChannels = null;

        /**
         * @return The number of idle channels to keep open.
         */
        public Optional<Integer> getMaxIdleChannels() {
            return Optional.ofNullable(maxIdleChannels);
        }

        /**
         * Sets the maximum number of idle channels that will be kept open.
         *
         * @param maxIdleChannels The maximum idle channels
         */
        public void setMaxIdleChannels(@Nullable Integer maxIdleChannels) {
            this.maxIdleChannels = maxIdleChannels;
        }
    }
}
