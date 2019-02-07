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

package io.micronaut.configuration.rabbitmq.bind;

import io.micronaut.core.convert.ArgumentConversionContext;

import javax.inject.Singleton;

/**
 * The default binder for binding an argument from the {@link RabbitConsumerState}
 * that is used if no other binder supports the argument.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Singleton
public class RabbitDefaultBinder implements RabbitArgumentBinder<Object> {

    private final RabbitPropertyBinder propertyBinder;
    private final RabbitBodyBinder bodyBinder;

    /**
     * Default constructor.
     *
     * @param propertyBinder The property binder
     * @param bodyBinder     The body binder
     */
    public RabbitDefaultBinder(RabbitPropertyBinder propertyBinder,
                               RabbitBodyBinder bodyBinder) {
        this.propertyBinder = propertyBinder;
        this.bodyBinder = bodyBinder;
    }

    /**
     * Checks if the argument name matches one of the {@link com.rabbitmq.client.BasicProperties}.
     * If the name does not match, the body of the message is bound to the argument.
     *
     * @param context The conversion context
     * @param messageState The message state
     * @return A binding result
     */
    @Override
    public BindingResult<Object> bind(ArgumentConversionContext<Object> context, RabbitConsumerState messageState) {
        if (propertyBinder.supports(context)) {
            return propertyBinder.bind(context, messageState);
        } else {
            return bodyBinder.bind(context, messageState);
        }
    }
}
