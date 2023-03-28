package io.micronaut.rabbitmq.connect

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import io.micronaut.rabbitmq.intercept.DefaultConsumer
import io.micronaut.rabbitmq.reactive.RabbitPublishState
import io.micronaut.rabbitmq.reactive.ReactorReactivePublisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import spock.lang.Stepwise

import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

@Stepwise
class ReactiveChannelSpec extends AbstractRabbitMQTest {

    void "test ack multiple"() {
        startContext()

        ChannelPool channelPool = applicationContext.getBean(ChannelPool)

        when:
        Channel consumeChannel = channelPool.channel
        boolean consumerAckd = false
        consumeChannel.basicConsume("abc", false, new DefaultConsumer() {

            AtomicInteger count = new AtomicInteger()

            @Override
            void handleTerminate(String consumerTag) {
                println "consumer terminated"
            }

            @Override
            void handleDelivery(String consumerTag, Envelope envelope,
                                AMQP.BasicProperties properties, byte[] body) throws IOException {
                if (count.incrementAndGet() == 4) {
                    consumeChannel.basicAck(envelope.deliveryTag, true)
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

        waitFor {
            consumerAckd
        }

        cleanup:
        channelPool.returnChannel(consumeChannel)
    }

    void "test reinitialization"() {
        startContext()

        ChannelPool channelPool = applicationContext.getBean(ChannelPool)
        AtomicInteger messageCount = new AtomicInteger()
        Channel consumeChannel = channelPool.channel

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
        waitFor {
            messageCount.get() == 1
        }

        when:
        reactiveChannel
                .publish(new RabbitPublishState("", "abc", new AMQP.BasicProperties.Builder().build(), "def".bytes))
                .subscribe()


        then:
        waitFor {
            messageCount.get() == 2
        }

        cleanup:
        channelPool.returnChannel(consumeChannel)
    }

    void "test publishing many messages"() {
        startContext("rabbitmq.channelPool.maxIdleChannels": 10)

        ChannelPool channelPool = applicationContext.getBean(ChannelPool)
        AtomicInteger integer = new AtomicInteger(75)
        ReactorReactivePublisher reactiveChannel = new ReactorReactivePublisher(channelPool, new SingleRabbitConnectionFactoryConfig())
        AtomicInteger messageCount = new AtomicInteger()
        Channel consumeChannel = channelPool.channel

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
        sleep 10
        Flux.merge(monos2).subscribe()

        then:
        waitFor {
            assert messageCount.get() == 75
            assert integer.get() == 0
        }

        cleanup:
        channelPool.returnChannel(consumeChannel)
    }
}
