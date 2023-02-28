package io.micronaut.rabbitmq

import com.github.dockerjava.api.model.HealthCheck
import io.micronaut.context.ApplicationContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.InternetProtocol
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.time.Duration

abstract class AbstractRabbitMQClusterTest extends Specification {

    private static final Logger log = LoggerFactory.getLogger(AbstractRabbitMQClusterTest)

    private static final int AMQP_PORT = 5672
    private static final DockerImageName RABBIT_IMAGE = DockerImageName.parse("library/rabbitmq:3.11.9-management")
    private static final String CLUSTER_COOKIE = "test-cluster"
    private static final String RABBIT_CONFIG_PATH = ClassLoader.getSystemResource("rabbit/rabbitmq.conf").path
    private static final String RABBIT_DEFINITIONS_PATH = ClassLoader.getSystemResource("rabbit/definitions.json").path
    private static final Network mqClusterNet = Network.newNetwork()

    protected ApplicationContext applicationContext

    static final String EXCHANGE = "test-exchange"
    static final String QUEUE = "test-durable-queue"
    static final GenericContainer NODE1_CONT = new GenericContainer<>(RABBIT_IMAGE)
    static final GenericContainer NODE2_CONT = new GenericContainer<>(RABBIT_IMAGE)
    static final GenericContainer NODE3_CONT = new GenericContainer<>(RABBIT_IMAGE)
    static int node1Port
    static int node2Port
    static int node3Port

    static {
        getNodePorts()
        log.info("rabbit.conf path: {}", RABBIT_CONFIG_PATH)
        log.info("rabbit definitions path: {}", RABBIT_DEFINITIONS_PATH)
        log.info("rabbit node ports: {}, {}, {}", node1Port, node2Port, node3Port)

        configureContainer(NODE1_CONT, "rabbitmq1", node1Port)
        // first node must boot up completely so that the other nodes can join the new cluster
        NODE1_CONT.waitingFor(Wait.forHealthcheck().withStartupTimeout(Duration.ofMinutes(1)))
        NODE1_CONT.start()
        log.info("first node startup complete")

        configureContainer(NODE2_CONT, "rabbitmq2", node2Port)
        configureContainer(NODE3_CONT, "rabbitmq3", node3Port)
        // node 2 and 3 may start up in parallel as they can join the already existing cluster
        NODE2_CONT.waitingFor(new DoNotWaitStrategy())
        NODE3_CONT.waitingFor(new DoNotWaitStrategy())
        NODE2_CONT.start()
        NODE3_CONT.start()
        new PollingConditions(timeout: 60).eventually {
            assert NODE2_CONT.isHealthy()
            assert NODE3_CONT.isHealthy()
        }
        log.info("cluster startup complete")
    }

    protected void startContext(Map additionalConfig = [:]) {
        Map<String, Object> properties = [
                "spec.name" : getClass().simpleName,
                "rabbitmq.servers.node1.port": node1Port,
                "rabbitmq.servers.node2.port": node2Port,
                "rabbitmq.servers.node3.port": node3Port] << additionalConfig

        log.info("context properties: {}", properties)
        applicationContext = ApplicationContext.run(properties, "test")
    }

    void cleanup() {
        applicationContext?.close()
    }

    private static getNodePorts() {
        try (ServerSocket s1 = new ServerSocket(0)
             ServerSocket s2 = new ServerSocket(0)
             ServerSocket s3 = new ServerSocket(0)) {
            node1Port = s1.localPort
            node2Port = s2.localPort
            node3Port = s3.localPort
        }
    }

    private static configureContainer(GenericContainer mqContainer, String hostname, int nodePort) {
        mqContainer
                .withEnv("RABBITMQ_ERLANG_COOKIE", CLUSTER_COOKIE)
                .withFileSystemBind(RABBIT_CONFIG_PATH, "/etc/rabbitmq/rabbitmq.conf", BindMode.READ_ONLY)
                .withFileSystemBind(RABBIT_DEFINITIONS_PATH, "/etc/rabbitmq/definitions.json", BindMode.READ_ONLY)
                .withNetwork(mqClusterNet)
                .withLogConsumer(new Slf4jLogConsumer(log).withPrefix(hostname))
                .withCreateContainerCmdModifier(cmd -> cmd
                        .withHostName(hostname)
                        .withHealthcheck(new HealthCheck()
                                .withTest(["CMD-SHELL", "rabbitmqctl status"])
                                .withStartPeriod(Duration.ofMinutes(4).toNanos())
                                .withInterval(Duration.ofSeconds(5).toNanos())
                                .withRetries(10)
                                .withTimeout(Duration.ofSeconds(5).toNanos())))
        // Use fixed port binding, because the dynamic port binding would use different port on each container start.
        // These changing ports would make any reconnect attempt impossible, as the client assumes that the broker
        // address does not change.
        addPortBinding(mqContainer, nodePort, AMQP_PORT)
    }

    private static addPortBinding(GenericContainer cont, int hostPort, int contPort) {
        cont.portBindings << String.format("%d:%d/%s",
                hostPort, contPort, InternetProtocol.TCP.toDockerNotation())
    }

    private static class DoNotWaitStrategy extends AbstractWaitStrategy {
        @Override
        protected void waitUntilReady() {
            // NOOP - do not wait
        }
    }
}
