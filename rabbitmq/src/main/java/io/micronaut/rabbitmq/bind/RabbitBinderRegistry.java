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
package io.micronaut.rabbitmq.bind;

import io.micronaut.core.bind.ArgumentBinder;
import io.micronaut.core.bind.ArgumentBinderRegistry;
import io.micronaut.core.bind.annotation.Bindable;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.ArrayUtils;
import jakarta.inject.Singleton;

import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Used to determine which {@link RabbitArgumentBinder} to use for any given argument.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Singleton
public class RabbitBinderRegistry implements ArgumentBinderRegistry<RabbitConsumerState> {

    private final Map<Class<? extends Annotation>, ArgumentBinder<?, RabbitConsumerState>> byAnnotation = new LinkedHashMap<>();
    private final Map<Integer, ArgumentBinder<?, RabbitConsumerState>> byType = new LinkedHashMap<>();
    private final RabbitDefaultBinder defaultBinder;

    /**
     * Default constructor.
     *
     * @param defaultBinder The binder to use when one cannot be found for an argument
     * @param binders The list of binders to choose from to bind an argument
     */
    public RabbitBinderRegistry(RabbitDefaultBinder defaultBinder,
                                RabbitArgumentBinder... binders) {
        this.defaultBinder = defaultBinder;
        if (ArrayUtils.isNotEmpty(binders)) {
            for (RabbitArgumentBinder binder : binders) {
                if (binder instanceof RabbitAnnotatedArgumentBinder) {
                    RabbitAnnotatedArgumentBinder<?> annotatedBinder = (RabbitAnnotatedArgumentBinder<?>) binder;
                    byAnnotation.put(
                            annotatedBinder.getAnnotationType(),
                            binder
                    );
                } else if (binder instanceof RabbitTypeArgumentBinder) {
                    RabbitTypeArgumentBinder<?> typedBinder = (RabbitTypeArgumentBinder<?>) binder;
                    byType.put(
                            typedBinder.argumentType().typeHashCode(),
                            typedBinder
                    );
                }
            }
        }
    }

    @Override
    public <T> Optional<ArgumentBinder<T, RabbitConsumerState>> findArgumentBinder(Argument<T> argument) {
        Optional<Class<? extends Annotation>> opt = argument.getAnnotationMetadata().getAnnotationTypeByStereotype(Bindable.class);
        if (opt.isPresent()) {
            Class<? extends Annotation> annotationType = opt.get();
            ArgumentBinder binder = byAnnotation.get(annotationType);
            if (binder != null) {
                return Optional.of(binder);
            }
        } else {
            ArgumentBinder binder = byType.get(argument.typeHashCode());
            if (binder != null) {
                return Optional.of(binder);
            }
        }
        return Optional.of((ArgumentBinder<T, RabbitConsumerState>) defaultBinder);
    }
}
