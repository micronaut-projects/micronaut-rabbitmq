package io.micronaut.rabbitmq.reactive

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import io.micronaut.rabbitmq.connect.ChannelPool
import io.micronaut.rabbitmq.connect.RabbitConnectionFactoryConfig
import spock.lang.Specification

class RpcCancellationSpec extends Specification {

    void "test the reply consumer is cancelled before the channel returns to the pool"() {
        given: "an RPC publish on a channel drawn from the pool"
        String consumerTag = "amq.ctag-reply-to"
        Channel channel = Mock(Channel)
        ChannelPool channelPool = Mock(ChannelPool)
        RabbitConnectionFactoryConfig config = Mock(RabbitConnectionFactoryConfig)
        ReactorReactivePublisher publisher = new ReactorReactivePublisher(channelPool, config)

        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .replyTo("amq.rabbitmq.reply-to")
                .build()
        RabbitPublishState publishState = new RabbitPublishState(
                "", "no.consumer.bound.here", false, properties, "hi".bytes)

        when: "the subscriber cancels before a reply arrives, as a timeout would"
        def subscription = publisher.publishRpcInternal(channel, publishState).subscribe()
        subscription.dispose()

        then: "the reply consumer was attached and the message published"
        1 * channel.basicConsume("amq.rabbitmq.reply-to", true, _) >> consumerTag
        1 * channel.basicPublish(_, _, _, _, _)

        then: "the reply consumer is cancelled"
        1 * channel.basicCancel(consumerTag)

        then: "and only then is the channel handed back, so it cannot be drawn while still consuming"
        1 * channelPool.returnChannel(channel)
    }
}
