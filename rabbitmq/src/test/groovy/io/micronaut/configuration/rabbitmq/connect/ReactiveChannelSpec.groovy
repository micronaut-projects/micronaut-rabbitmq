package io.micronaut.configuration.rabbitmq.connect

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import io.micronaut.configuration.rabbitmq.AbstractRabbitMQTest
import io.micronaut.configuration.rabbitmq.intercept.DefaultConsumer
import io.micronaut.configuration.rabbitmq.reactivex.ReactiveChannel
import io.micronaut.context.ApplicationContext
import io.reactivex.Completable

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

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
        reactiveChannel.finish()
                .doAfterSuccess({c -> channelPool.returnChannel(c)})
                .blockingGet()
        Completable.merge(completables).blockingGet(2, TimeUnit.SECONDS) == null

        cleanup:
        applicationContext.close()
    }
}
