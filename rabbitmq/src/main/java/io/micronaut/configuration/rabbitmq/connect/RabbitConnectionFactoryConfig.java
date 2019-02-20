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

import com.rabbitmq.client.ConnectionFactory;
import io.micronaut.context.annotation.ConfigurationProperties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Optional;

/**
 * The default RabbitMQ configuration class.
 *
 * Allows RabbitMQ client to leverage Micronaut properties configuration
 *
 * @author benrhine
 * @since 1.0
 */
@ConfigurationProperties("rabbitmq")
public class RabbitConnectionFactoryConfig extends ConnectionFactory {

    private RpcConfiguration rpc = new RpcConfiguration();

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
     * Configuration for RPC.
     */
    @ConfigurationProperties("rpc")
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
         * Sets the timeout duration before cancelling an RPC call.
         *
         * @param timeout The timeout
         */
        public void setTimeout(@Nullable Duration timeout) {
            this.timeout = timeout;
        }
    }
}
