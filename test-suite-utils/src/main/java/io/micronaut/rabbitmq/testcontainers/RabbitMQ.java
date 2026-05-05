package io.micronaut.rabbitmq.testcontainers;

import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

public class RabbitMQ {
    private static final String IMAGE_NAME = "rabbitmq:3.13-management";
    private static RabbitMQContainer container;

    public static Map<String, String> getProperties() {
        if (container == null || !container.isRunning()) {
            container = new RabbitMQContainer(DockerImageName.parse(IMAGE_NAME));
            container.start();
            do {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } while(!container.isRunning());
            return getProperties(container);
        } else {
            return getProperties(container);
        }
    }

    private static Map<String, String> getProperties(RabbitMQContainer container) {
        return Map.of(
            "rabbitmq.uri", container.getAmqpUrl(),
            "rabbitmq.username", container.getAdminUsername(),
            "rabbitmq.password", container.getAdminPassword()//,
        );
    }
}
