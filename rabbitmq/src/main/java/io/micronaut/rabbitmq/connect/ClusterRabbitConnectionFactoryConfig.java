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

import io.micronaut.context.annotation.*;

import javax.annotation.Nonnull;

/**
 * Allows configuration of multiple RabbitMQ connections.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Requires(property = ClusterRabbitConnectionFactoryConfig.PREFIX)
@EachProperty(ClusterRabbitConnectionFactoryConfig.PREFIX)
public class ClusterRabbitConnectionFactoryConfig extends RabbitConnectionFactoryConfig {

    public static final String PREFIX = "rabbitmq.servers";

    /**
     * Default constructor.
     *
     * @param name The connection name
     */
    public ClusterRabbitConnectionFactoryConfig(@Parameter String name) {
        super(name);
    }

    /**
     * Sets the RPC configuration.
     *
     * @param rpc The RPC configuration
     */
    public void setRpc(@Nonnull DefaultRpcConfiguration rpc) {
        super.setRpc(rpc);
    }

    /**
     * Sets the channel pool configuration.
     *
     * @param channelPool The channel pool configuration
     */
    public void setChannelPool(@Nonnull DefaultChannelPoolConfiguration channelPool) {
        super.setChannelPool(channelPool);
    }

    /**
     * @see RabbitConnectionFactoryConfig.RpcConfiguration
     */
    @ConfigurationProperties("rpc")
    public static class DefaultRpcConfiguration extends RabbitConnectionFactoryConfig.RpcConfiguration { }

    /**
     * @see RabbitConnectionFactoryConfig.ChannelPoolConfiguration
     */
    @ConfigurationProperties("channel-pool")
    public static class DefaultChannelPoolConfiguration extends ChannelPoolConfiguration { }
}
