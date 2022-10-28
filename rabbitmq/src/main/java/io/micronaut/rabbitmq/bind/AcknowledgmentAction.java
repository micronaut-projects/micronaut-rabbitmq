/*
 * Copyright 2017-2022 original authors
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
package io.micronaut.rabbitmq.bind;

/**
 * Represents options for controlling the acknowledgment actions.
 * For example, if the consuming method has the auto acknowledgment feature enabled, acknowledgment control must be skipped.
 *
 *  @author sbdvanski
 *  @since 3.4.0
 */
public enum AcknowledgmentAction {
    /**
     * The action of acknowledging a message.
     */
    ACK,

    /**
     * The action of rejecting a message.
     */
    NACK,

    /**
     * The acknowledgment action is going to be skipped.
     */
    NONE
}
