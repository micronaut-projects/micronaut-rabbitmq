package io.micronaut.rabbitmq.docs;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.ApplicationContextConfigurer;
import io.micronaut.context.annotation.ContextConfigurer;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertySource;
import io.micronaut.rabbitmq.testcontainers.RabbitMQ;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Supplies the connection properties of the shared RabbitMQ test container to the Python tests run
 * with the {@code rabbitmq} environment, like the {@code TestPropertyProvider} of the Java, Kotlin
 * and Groovy example tests does. With the additional {@code rabbitmq-cluster} environment the test
 * container is also configured as the {@code product-cluster} server
 * ({@code rabbitmq.servers.product-cluster.*}), like the {@code ConnectionSpec} of the other
 * languages does.
 * <p>
 * The configurer is written in Java because Micronaut Test calls {@code TestPropertyProvider} before
 * the application context, and with it the GraalPy runtime, exists, so a Python test class cannot
 * supply the container properties. It uses the {@link #configure(ApplicationContext)} callback because
 * the {@link io.micronaut.context.ApplicationContextBuilder} is configured before {@code @MicronautTest}
 * selects the environments, so the {@code rabbitmq} environment can only be checked on the built context.
 */
@ContextConfigurer
public class RabbitMQTestConfigurer implements ApplicationContextConfigurer {

    public static final String RABBITMQ_ENVIRONMENT = "rabbitmq";
    public static final String CLUSTER_ENVIRONMENT = "rabbitmq-cluster";

    @Override
    public void configure(ApplicationContext applicationContext) {
        Environment environment = applicationContext.getEnvironment();
        if (environment.getActiveNames().contains(RABBITMQ_ENVIRONMENT)) {
            Map<String, Object> properties = new HashMap<>(RabbitMQ.getProperties());
            if (environment.getActiveNames().contains(CLUSTER_ENVIRONMENT)) {
                URI uri = URI.create(properties.get("rabbitmq.uri").toString());
                properties.put("rabbitmq.servers.product-cluster.uri", uri.toString());
                properties.put("rabbitmq.servers.product-cluster.host", uri.getHost());
                properties.put("rabbitmq.servers.product-cluster.port", String.valueOf(uri.getPort()));
            }
            environment.addPropertySource(PropertySource.of(RABBITMQ_ENVIRONMENT, properties));
        }
    }
}
