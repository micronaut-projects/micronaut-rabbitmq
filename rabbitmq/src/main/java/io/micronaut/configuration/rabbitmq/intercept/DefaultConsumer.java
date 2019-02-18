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

package io.micronaut.configuration.rabbitmq.intercept;

import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.ShutdownSignalException;

import java.io.IOException;

/**
 * A default {@link Consumer} that delegates cancelled or shutdown
 * with a single method {@link #handleTerminate(String)}.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
public interface DefaultConsumer extends Consumer {

    @Override
    default void handleConsumeOk(String consumerTag) { }

    @Override
    default void handleCancelOk(String consumerTag) {
        handleTerminate(consumerTag);
    }

    @Override
    default void handleCancel(String consumerTag) throws IOException {
        handleTerminate(consumerTag);
    }

    @Override
    default void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
        handleTerminate(consumerTag);
    }

    @Override
    default void handleRecoverOk(String consumerTag) { }

    /**
     * Called when a consumer is cancelled or shut down.
     *
     * @param consumerTag The consumer tag
     */
    default void handleTerminate(String consumerTag) { }
}
