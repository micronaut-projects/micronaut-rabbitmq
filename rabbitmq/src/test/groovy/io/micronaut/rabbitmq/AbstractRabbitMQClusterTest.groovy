package io.micronaut.rabbitmq

import com.github.dockerjava.api.model.HealthCheck
import io.micronaut.context.ApplicationContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.Network
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.time.Duration

import static org.testcontainers.utility.MountableFile.forHostPath

abstract class AbstractRabbitMQClusterTest extends Specification {

    private static final Logger log = LoggerFactory.getLogger(AbstractRabbitMQClusterTest)

    private static final int AMQP_PORT = 5672
    private static final DockerImageName RABBIT_IMAGE = DockerImageName.parse("rabbitmq:3.8-management")
    private static final String CLUSTER_COOKIE = "test-cluster"
    private static final String RABBIT_CONFIG_PATH = ClassLoader.getSystemResource("rabbit/rabbitmq.conf").path
    private static final String RABBIT_DEFINITIONS_PATH = ClassLoader.getSystemResource("rabbit/definitions.json").path
    private static final Network mqClusterNet = Network.newNetwork()

    protected ApplicationContext applicationContext

    static final String EXCHANGE = "test-exchange"
    static final String QUEUE = "test-durable-queue"
    static final RabbitMQContainer NODE1_CONT = new RabbitMQContainer(RABBIT_IMAGE)
    static final RabbitMQContainer NODE2_CONT = new RabbitMQContainer(RABBIT_IMAGE)
    static final RabbitMQContainer NODE3_CONT = new RabbitMQContainer(RABBIT_IMAGE)
    static int node1Port
    static int node2Port
    static int node3Port

    static {
        log.info("rabbit.conf path: {}", RABBIT_CONFIG_PATH)
        log.info("rabbit definitions path: {}", RABBIT_DEFINITIONS_PATH)

        configureContainer(NODE1_CONT, "rabbitmq1")
        // first node must boot up completely so that the other nodes can join the new cluster
        NODE1_CONT.waitingFor(Wait.forHealthcheck().withStartupTimeout(Duration.ofMinutes(1)))
        NODE1_CONT.start()
        node1Port = NODE1_CONT.getMappedPort(AMQP_PORT)

        log.info("first node startup complete")

        configureContainer(NODE2_CONT, "rabbitmq2")
        configureContainer(NODE3_CONT, "rabbitmq3")
        // node 2 and 3 may start up in parallel as they can join the already existing cluster
        NODE2_CONT.waitingFor(new DoNotWaitStrategy())
        NODE3_CONT.waitingFor(new DoNotWaitStrategy())
        NODE2_CONT.start()
        NODE3_CONT.start()

        node2Port = NODE2_CONT.getMappedPort(AMQP_PORT)
        node3Port = NODE3_CONT.getMappedPort(AMQP_PORT)

        log.info("rabbit node ports: {}, {}, {}", node1Port, node2Port, node3Port)

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

    private static configureContainer(RabbitMQContainer mqContainer, String hostname) {
        mqContainer
                .withEnv("RABBITMQ_ERLANG_COOKIE", CLUSTER_COOKIE)
                .withCopyFileToContainer(forHostPath(RABBIT_CONFIG_PATH), "/etc/rabbitmq/rabbitmq.conf")
                .withCopyFileToContainer(forHostPath(RABBIT_DEFINITIONS_PATH), "/etc/rabbitmq/definitions.json")
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
    }

    private static class DoNotWaitStrategy extends AbstractWaitStrategy {
        @Override
        protected void waitUntilReady() {
            // NOOP - do not wait
        }
    }
}
