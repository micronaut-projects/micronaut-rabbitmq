package io.micronaut.rabbitmq.ssl

import com.rabbitmq.client.ConnectionFactory
import io.micronaut.context.ApplicationContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.RabbitMQContainer
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import java.security.KeyStore

abstract class AbstractRabbitMQSSLTest extends Specification {
    private static final Logger log = LoggerFactory.getLogger(AbstractRabbitMQSSLTest)

    private static final int AMQP_PORT = 5672
    private static final int AMQPS_PORT = 5671

    public static final String RABBIT_CONTAINER_VERSION = "3.13.1"

    @Shared
    @AutoCleanup
    RabbitMQContainer rabbitContainer = new RabbitMQContainer("rabbitmq:" + RABBIT_CONTAINER_VERSION)

    def setupSpec() {
        rabbitContainer.start()
    }

    protected ApplicationContext applicationContext
    protected PollingConditions conditions = new PollingConditions(timeout: 5)

    protected void startContext(Map additionalConfig = [:]) {
        applicationContext = ApplicationContext.run(
                ["rabbitmq.port": rabbitContainer.getMappedPort(AMQPS_PORT),
                 "spec.name": getClass().simpleName] << additionalConfig, "test")
    }

    protected void waitFor(Closure<?> conditionEvaluator) {
        conditions.eventually conditionEvaluator
    }

    void cleanup() {
        applicationContext?.close()
    }

    // TODO this is example from rabbitmq TLS docs to integrate, and change test to use the connection
    // https://www.rabbitmq.com/ssl.html#java-client-connecting-with-peer-verification
    void configureForSsl(ConnectionFactory factory) {
        char[] keyPassphrase = "MySecretPassword".toCharArray()
        KeyStore ks = KeyStore.getInstance("PKCS12")
        ks.load(new FileInputStream("/path/to/client_key.p12"), keyPassphrase)

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509")
        kmf.init(ks, keyPassphrase)

        char[] trustPassphrase = "rabbitstore".toCharArray()
        KeyStore tks = KeyStore.getInstance("JKS")
        tks.load(new FileInputStream("/path/to/trustStore"), trustPassphrase)

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509")
        tmf.init(tks)

        SSLContext sslContext = SSLContext.getInstance("TLSv1.2")
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null)

        factory.setHost("localhost")
        factory.setPort(AMQPS_PORT)
        factory.enableHostnameVerification()
        // this is the key part, using no-arg factory.useSslProtocol() is not adequate for prod use
        factory.useSslProtocol(sslContext)
    }
}
