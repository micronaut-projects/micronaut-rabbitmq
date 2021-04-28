package io.micronaut.rabbitmq.annotation;

import io.micronaut.core.bind.annotation.Bindable;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Denotes a {@link java.util.Map} argument that should be used as a source
 * of message headers.
 *
 * @since 2.5.0
 * @author James Kleeh
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.PARAMETER})
@Bindable
public @interface RabbitHeaders {
}
