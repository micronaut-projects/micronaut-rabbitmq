package io.micronaut.rabbitmq.connect

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import io.micronaut.context.ApplicationContext
import io.micronaut.rabbitmq.intercept.DefaultConsumer
import io.micronaut.rabbitmq.reactive.RabbitPublishState
import io.micronaut.rabbitmq.reactive.RxJavaReactivePublisher
import io.reactivex.Completable
import spock.lang.Stepwise
import spock.util.concurrent.PollingConditions

import java.util.concurrent.TimeUnit
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
        RxJavaReactivePublisher reactiveChannel = new RxJavaReactivePublisher(channelPool, new SingleRabbitConnectionFactoryConfig())
        List<Completable> completables = ["abc", "def", "ghi", "jkl"].collect {
            Completable.fromPublisher(reactiveChannel.publish(new RabbitPublishState("", "abc", new AMQP.BasicProperties.Builder().build(), it.bytes)))
        }

        then:
        Completable.merge(completables)
                .blockingGet(10, TimeUnit.SECONDS) == null

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
        RxJavaReactivePublisher reactiveChannel = new RxJavaReactivePublisher(channelPool, new SingleRabbitConnectionFactoryConfig())


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
        AtomicInteger integer = new AtomicInteger(2)
        RxJavaReactivePublisher reactiveChannel = new RxJavaReactivePublisher(channelPool, new SingleRabbitConnectionFactoryConfig())
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
        List<Completable> publishes = []
        50.times {
            publishes.add(Completable.fromPublisher(reactiveChannel.publish(new RabbitPublishState("", "abc", null, "abc".bytes))))
        }

        List<Completable> publishes2 = []
        25.times {
            publishes2.add(Completable.fromPublisher(reactiveChannel.publish(new RabbitPublishState("", "abc", null, "abc".bytes))))
        }

        Completable.merge(publishes).subscribe({ -> integer.decrementAndGet()})
        Thread.sleep(10)
        Completable.merge(publishes2).subscribe({ -> integer.decrementAndGet()})

        then:
        conditions.eventually {
            integer.get() == 0
            messageCount.get() == 75
        }

        cleanup:
        channelPool.returnChannel(consumeChannel)
        applicationContext.stop()
    }
}
