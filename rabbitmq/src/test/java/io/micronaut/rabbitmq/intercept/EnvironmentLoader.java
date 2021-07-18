package io.micronaut.rabbitmq.intercept;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import com.github.dockerjava.api.model.HealthCheck;
import io.micronaut.context.env.ActiveEnvironment;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.env.PropertySourceLoader;
import io.micronaut.core.io.ResourceLoader;
import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.InternetProtocol;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Abuse the fact that properties are loaded before the application context is initialized to setup the
 * RabbitMQ test cluster.
 */
public class EnvironmentLoader implements PropertySourceLoader {
    private static final int AMQP_PORT = 5672;
    private static final int MANAGEMENTUI_PORT = 15672;
    public static final String EXCHANGE = "test-exchange";
    public static final String QUEUE = "test-durable-queue";
    public static final String NODE1 = "node1";
    public static final String NODE2 = "node2";
    public static final String NODE3 = "node3";
    public static final int NODE1_PORT = AMQP_PORT;
    public static final int NODE2_PORT = AMQP_PORT + 1;
    public static final int NODE3_PORT = AMQP_PORT + 2;
    public static final String NODE1_CONT = "containerNode1";
    public static final String NODE2_CONT = "containerNode2";
    public static final String NODE3_CONT = "containerNode3";

    private static final Logger log = LoggerFactory.getLogger(EnvironmentLoader.class);
    private static final DockerImageName RABBIT_IMAGE = DockerImageName.parse("rabbitmq:3-management");
    private static final String CLUSTER_COOKIE = "test-cluster";
    private static final String RABBIT_CONFIG_PATH = ClassLoader.getSystemResource("rabbit/rabbitmq.conf").getPath();
    private static final String RABBIT_DEFINITIONS_PATH = ClassLoader.getSystemResource("rabbit/definitions.json")
            .getPath();
    private static final Network mqClusterNet = Network.newNetwork();

    private static Map<String, Object> cache = new HashMap<>();

    @Override
    public Optional<PropertySource> load(String resourceName, ResourceLoader resourceLoader) {
        if (cache.isEmpty()) {
            cache.put(NODE1_CONT, new GenericContainer<>(RABBIT_IMAGE));
            cache.put(NODE2_CONT, new GenericContainer<>(RABBIT_IMAGE));
            cache.put(NODE3_CONT, new GenericContainer<>(RABBIT_IMAGE));
            GenericContainer<?> mq1 = configureContainer("rabbitmq1", "Node1", NODE1_PORT);
            // first node must boot up completely so that the other nodes can join the new cluster
            mq1.waitingFor(Wait.forHealthcheck().withStartupTimeout(Duration.ofMinutes(1)));
            mq1.start();
            log.info("first node startup complete");

            GenericContainer<?> mq2 = configureContainer("rabbitmq2", "Node2", NODE2_PORT);
            GenericContainer<?> mq3 = configureContainer("rabbitmq3", "Node3", NODE3_PORT);
            // get the management UI always on the same port for easy monitoring
            addPortBinding(mq3, MANAGEMENTUI_PORT, MANAGEMENTUI_PORT);
            // node 2 and 3 may start up in parallel as they can join the already existing cluster
            mq2.waitingFor(new DoNotWaitStrategy());
            mq3.waitingFor(new DoNotWaitStrategy());
            mq2.start();
            mq3.start();
            Awaitility.await("other cluster nodes start")
                    .atMost(Duration.ofMinutes(1)).pollInterval(Duration.ofSeconds(1))
                    .until(() -> mq2.isHealthy() && mq3.isHealthy());
            log.info("cluster startup complete");
        }
        return Optional.of(PropertySource.of(resourceName, cache));
    }

    @Override
    public Optional<PropertySource> loadEnv(String resourceName, ResourceLoader resourceLoader, ActiveEnvironment activeEnvironment) {
        return Optional.empty();
    }

    @Override
    public Map<String, Object> read(String name, InputStream input) throws IOException {
        throw new UnsupportedOperationException();
    }

    private static class DoNotWaitStrategy extends AbstractWaitStrategy {
        @Override
        protected void waitUntilReady() {
            // NOOP - do not wait
        }
    }

    private static GenericContainer<?> configureContainer(String hostname, String nodeName, int nodePort) {
        GenericContainer<?> mqContainer = (GenericContainer<?>) cache.get("container" + nodeName);
        mqContainer
                .withEnv("RABBITMQ_ERLANG_COOKIE", CLUSTER_COOKIE)
                .withFileSystemBind(RABBIT_CONFIG_PATH, "/etc/rabbitmq/rabbitmq.conf", BindMode.READ_ONLY)
                .withFileSystemBind(RABBIT_DEFINITIONS_PATH, "/etc/rabbitmq/definitions.json", BindMode.READ_ONLY)
                .withNetwork(mqClusterNet)
                .withLogConsumer(new Slf4jLogConsumer(log).withPrefix(hostname))
                .withCreateContainerCmdModifier(cmd -> cmd
                        .withHostName(hostname)
                        .withHealthcheck(new HealthCheck()
                                .withTest(Arrays.asList("CMD-SHELL", "rabbitmqctl status"))
                                .withStartPeriod(Duration.ofMinutes(4).toNanos())
                                .withInterval(Duration.ofSeconds(5).toNanos())
                                .withRetries(10)
                                .withTimeout(Duration.ofSeconds(5).toNanos())));
        // Use fixed port binding, because the dynamic port binding would use different port on each container start.
        // These changing ports would make any reconnect attempt impossible, as the client assumes that the broker
        // address does not change.
        addPortBinding(mqContainer, nodePort, AMQP_PORT);
        return mqContainer;
    }

    private static void addPortBinding(GenericContainer<?> cont, int hostPort, int contPort) {
        cont.getPortBindings().add(String.format("%d:%d/%s",
                hostPort, contPort, InternetProtocol.TCP.toDockerNotation()));
    }
}
