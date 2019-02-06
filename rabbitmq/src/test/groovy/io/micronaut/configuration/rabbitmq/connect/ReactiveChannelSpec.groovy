package io.micronaut.configuration.rabbitmq.connect

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import io.micronaut.configuration.rabbitmq.AbstractRabbitMQTest
import io.micronaut.configuration.rabbitmq.intercept.DefaultConsumer
import io.micronaut.configuration.rabbitmq.reactive.RxJavaReactivePublisher
import io.micronaut.context.ApplicationContext
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
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
        RxJavaReactivePublisher reactiveChannel = new RxJavaReactivePublisher(channelPool)
        List<Completable> completables = [
        reactiveChannel.publish("", "abc", new AMQP.BasicProperties.Builder().build(), "abc".bytes),
        reactiveChannel.publish("", "abc", new AMQP.BasicProperties.Builder().build(), "def".bytes),
        reactiveChannel.publish("", "abc", new AMQP.BasicProperties.Builder().build(), "ghi".bytes),
        reactiveChannel.publish("", "abc", new AMQP.BasicProperties.Builder().build(), "jkl".bytes)]

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
        RxJavaReactivePublisher reactiveChannel = new RxJavaReactivePublisher(channelPool)


        when:
        reactiveChannel
                .publish("", "abc", new AMQP.BasicProperties.Builder().build(), "abc".bytes)
                .subscribe()

        then:
        conditions.eventually {
            messageCount.get() == 1
        }

        when:
        reactiveChannel
                .publish("", "abc", new AMQP.BasicProperties.Builder().build(), "def".bytes)
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
                ["rabbitmq.port": rabbitContainer.getMappedPort(5672)])
        ChannelPool channelPool = applicationContext.getBean(ChannelPool)
        AtomicInteger integer = new AtomicInteger(2)
        RxJavaReactivePublisher reactiveChannel = new RxJavaReactivePublisher(channelPool)
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
            publishes.add(reactiveChannel.publish("", "abc", null, "abc".bytes).subscribeOn(Schedulers.io()))
        }

        List<Completable> publishes2 = []
        25.times {
            publishes2.add(reactiveChannel.publish("", "abc", null, "abc".bytes).subscribeOn(Schedulers.io()))
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
