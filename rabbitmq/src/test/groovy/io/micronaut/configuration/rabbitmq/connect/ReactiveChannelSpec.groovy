package io.micronaut.configuration.rabbitmq.connect

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import io.micronaut.configuration.rabbitmq.AbstractRabbitMQTest
import io.micronaut.configuration.rabbitmq.intercept.DefaultConsumer
import io.micronaut.configuration.rabbitmq.reactivex.ReactiveChannel
import io.micronaut.context.ApplicationContext
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
        Channel channel = channelPool.getChannel()
        channel.basicConsume("abc", false, new DefaultConsumer() {
            AtomicInteger count = new AtomicInteger()
            @Override
            void handleTerminate(String consumerTag) {

            }

            @Override
            void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                if (count.incrementAndGet() == 4) {
                    channel.basicAck(envelope.getDeliveryTag(), true)
                }
            }
        })
        ReactiveChannel reactiveChannel = new ReactiveChannel(channel)
        List<Completable> completables = [
        reactiveChannel.publish("", "abc", new AMQP.BasicProperties.Builder().build(), "abc".bytes),
        reactiveChannel.publish("", "abc", new AMQP.BasicProperties.Builder().build(), "def".bytes),
        reactiveChannel.publish("", "abc", new AMQP.BasicProperties.Builder().build(), "ghi".bytes),
        reactiveChannel.publish("", "abc", new AMQP.BasicProperties.Builder().build(), "jkl".bytes)]

        then:
        Completable.merge(completables).blockingGet(5, TimeUnit.SECONDS) == null

        cleanup:
        applicationContext.close()
    }

    void "test reinitialization"() {
        ApplicationContext applicationContext = ApplicationContext.run(
                ["rabbitmq.port": rabbitContainer.getMappedPort(5672)])
        ChannelPool channelPool = applicationContext.getBean(ChannelPool)
        PollingConditions conditions = new PollingConditions(timeout: 5, initialDelay: 1)
        AtomicInteger messageCount = new AtomicInteger()
        Channel channel = channelPool.getChannel()
        channel.basicConsume("def", true, new DefaultConsumer() {
            @Override
            void handleTerminate(String consumerTag) {}

            @Override
            void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                messageCount.incrementAndGet()
            }
        })
        ReactiveChannel reactiveChannel = new ReactiveChannel(channel)

        when:
        reactiveChannel
                .publish("", "def", new AMQP.BasicProperties.Builder().build(), "abc".bytes)
                .subscribe()

        then:
        conditions.eventually {
            messageCount.get() == 1
        }

        when:
        reactiveChannel
                .publish("", "def", new AMQP.BasicProperties.Builder().build(), "def".bytes)
                .subscribe()


        then:
        conditions.eventually {
            messageCount.get() == 2
        }

        cleanup:
        channelPool.returnChannel(channel)
        applicationContext.close()
    }
}
