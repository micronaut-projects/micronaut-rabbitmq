package io.micronaut.rabbitmq.connect

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import io.micronaut.context.ApplicationContext
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import io.micronaut.rabbitmq.intercept.DefaultConsumer
import io.micronaut.rabbitmq.reactive.RabbitPublishState
import io.micronaut.rabbitmq.reactive.ReactorReactivePublisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import spock.lang.Stepwise
import spock.util.concurrent.PollingConditions

import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

@Stepwise
class ReactiveChannelSpec extends AbstractRabbitMQTest {

    void "test ack multiple"() {
        ApplicationContext applicationContext = ApplicationContext.run(
                ["rabbitmq.port": rabbitContainer.getMappedPort(5672)])
        ChannelPool channelPool = applicationContext.getBean(ChannelPool)

        when:
        Channel consumeChannel = channelPool.getChannel()
        Boolean consumerAckd = false
        PollingConditions conditions = new PollingConditions(timeout: 5)
        consumeChannel.basicConsume("abc", false, new DefaultConsumer() {
            AtomicInteger count = new AtomicInteger()
            @Override
            void handleTerminate(String consumerTag) {
                println "consumer terminated"
            }

            @Override
            void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                println "received message " + new String(body)
                if (count.incrementAndGet() == 4) {
                    println "count is 4, ack multiple"
                    consumeChannel.basicAck(envelope.getDeliveryTag(), true)
                    consumerAckd = true
                }
            }
        })
        ReactorReactivePublisher reactiveChannel = new ReactorReactivePublisher(channelPool, new SingleRabbitConnectionFactoryConfig())
        List<Mono> monos = ["abc", "def", "ghi", "jkl"].collect {
            Mono.from(reactiveChannel.publish(new RabbitPublishState("", "abc", new AMQP.BasicProperties.Builder().build(), it.bytes)))
        }

        then:
        Flux.merge(monos)
            .blockFirst(Duration.ofSeconds(10)) == null

        conditions.eventually {
            consumerAckd
        }

        cleanup:
        channelPool.returnChannel(consumeChannel)
        applicationContext.close()
    }

    void "test reinitialization"() {
        ApplicationContext applicationContext = ApplicationContext.run(
                ["rabbitmq.port": rabbitContainer.getMappedPort(5672)])
        ChannelPool channelPool = applicationContext.getBean(ChannelPool)
        PollingConditions conditions = new PollingConditions(timeout: 5, initialDelay: 1)
        AtomicInteger messageCount = new AtomicInteger()
        Channel consumeChannel = channelPool.getChannel()
        consumeChannel.basicConsume("abc", true, new DefaultConsumer() {
            @Override
            void handleTerminate(String consumerTag) {}

            @Override
            void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                messageCount.incrementAndGet()
            }
        })
        ReactorReactivePublisher reactiveChannel = new ReactorReactivePublisher(channelPool, new SingleRabbitConnectionFactoryConfig())

        when:
        reactiveChannel
                .publish(new RabbitPublishState("", "abc", new AMQP.BasicProperties.Builder().build(), "abc".bytes))
                .subscribe()

        then:
        conditions.eventually {
            messageCount.get() == 1
        }

        when:
        reactiveChannel
                .publish(new RabbitPublishState("", "abc", new AMQP.BasicProperties.Builder().build(), "def".bytes))
                .subscribe()


        then:
        conditions.eventually {
            messageCount.get() == 2
        }

        cleanup:
        channelPool.returnChannel(consumeChannel)
        applicationContext.close()
    }

    void "test publishing many messages"() {
        ApplicationContext applicationContext = ApplicationContext.run(
                ["rabbitmq.port": rabbitContainer.getMappedPort(5672),
                 "rabbitmq.channelPool.maxIdleChannels": 10])
        ChannelPool channelPool = applicationContext.getBean(ChannelPool)
        AtomicInteger integer = new AtomicInteger(75)
        ReactorReactivePublisher reactiveChannel = new ReactorReactivePublisher(channelPool, new SingleRabbitConnectionFactoryConfig())
        PollingConditions conditions = new PollingConditions(timeout: 10, initialDelay: 1)
        AtomicInteger messageCount = new AtomicInteger()
        Channel consumeChannel = channelPool.getChannel()
        consumeChannel.basicConsume("abc", true, new DefaultConsumer() {
            @Override
            void handleTerminate(String consumerTag) {}

            @Override
            void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                messageCount.incrementAndGet()
            }
        })

        when:
        List<Mono> monos = []
        50.times {
            monos.add(Mono.from(reactiveChannel.publish(new RabbitPublishState("", "abc", null, "abc".bytes))).doOnSuccess({ t ->
                integer.decrementAndGet()
            }))
        }

        List<Mono> monos2 = []
        25.times {
            monos2.add(Mono.from(reactiveChannel.publish(new RabbitPublishState("", "abc", null, "abc".bytes))).doOnSuccess({ t ->
                integer.decrementAndGet()
            }))
        }

        Flux.merge(monos).subscribe()
        Thread.sleep(10)
        Flux.merge(monos2).subscribe()

        then:
        conditions.eventually {
            assert messageCount.get() == 75
            assert integer.get() == 0
        }

        cleanup:
        channelPool.returnChannel(consumeChannel)
        applicationContext.stop()
    }
}
