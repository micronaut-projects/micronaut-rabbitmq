/*
 * Copyright 2017-2023 original authors
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
package io.micronaut.rabbitmq.connect.recovery;

import com.rabbitmq.client.Connection;

/**
 * Represents a connection to RabbitMQ that is temporarily down.
 *
 * @author Guillermo Calvo
 * @since 4.1.0
 */
public interface TemporarilyDownConnection extends Connection {

    boolean isStillDown();

    boolean check();

    void addEventuallyUpListener(EventuallyUpListener listener);

    /**
     * Callback interface to perform any action when the connection is eventually up.
     */
    @FunctionalInterface
    interface EventuallyUpListener {

        /**
         * Reacts to a connection that is eventually up.
         * @param connection The connection that was temporarily down.
         * @throws Exception if unable to perform the action (the exception will be ignored).
         */
        @SuppressWarnings("java:S112") // We use a generic exception so users may throw anything
        void onConnectionInitialized(TemporarilyDownConnection connection) throws Exception;
    }
}
