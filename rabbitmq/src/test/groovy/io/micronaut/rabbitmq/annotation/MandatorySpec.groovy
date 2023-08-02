package io.micronaut.rabbitmq.annotation

import io.micronaut.context.annotation.Requires
import io.micronaut.core.util.StringUtils
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import io.micronaut.rabbitmq.connect.ChannelPoolListener
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import spock.util.concurrent.PollingConditions

class MandatorySpec extends AbstractRabbitMQTest {

    void "test publishing to a queue that does not exist with mandatory on and off"() {
        startContext()
        MandatorySender1 mandatorySender1 = applicationContext.getBean(MandatorySender1)
        MandatorySender2 mandatorySender2 = applicationContext.getBean(MandatorySender2)
        NotMandatorySender1 notMandatorySender1 = applicationContext.getBean(NotMandatorySender1)
        NotMandatorySender2 notMandatorySender2 = applicationContext.getBean(NotMandatorySender2)
        ChannelPoolListener listener = applicationContext.getBean(ChannelPoolListener)

        when:
        mandatorySender1.send("this message will be returned")
        notMandatorySender1.send("this message will NOT be returned")
        Mono.from(notMandatorySender2.send("this message will not be returned either")).block()
        Mono.from(mandatorySender2.send("this message will be returned too")).block()

        then:
        new PollingConditions(timeout: 5).eventually {
            listener.returns.size() > 1
        }

        and:
        listener.returns[0].replyText == 'NO_ROUTE'
        listener.returns[0].routingKey == 'no-such-queue'
        listener.returns[0].body == 'this message will be returned'.bytes
        listener.returns[1].replyText == 'NO_ROUTE'
        listener.returns[1].routingKey == 'no-such-queue'
        listener.returns[1].body == 'this message will be returned too'.bytes

        and:
        listener.returns.stream().noneMatch(it -> it.body == 'this message will NOT be returned'.bytes)
        listener.returns.stream().noneMatch(it -> it.body == 'this message will not be returned either'.bytes)
    }

    void "test mandatory annotation applied to methods and method parameters"() {
        startContext()
        MandatorySender1 mandatorySender1 = applicationContext.getBean(MandatorySender1)
        MandatorySender3 mandatorySender3 = applicationContext.getBean(MandatorySender3)
        ParametricSender parametricSender = applicationContext.getBean(ParametricSender)
        ChannelPoolListener listener = applicationContext.getBean(ChannelPoolListener)

        when:
        mandatorySender3.send("this message will be returned")
        parametricSender.send(false, "this message will NOT be returned")
        mandatorySender1.sendNotMandatory("this message will not be returned either")
        parametricSender.send(true, "this message will be returned too")

        then:
        new PollingConditions(timeout: 5).eventually {
            listener.returns.size() > 1
        }

        and:
        listener.returns[0].replyText == 'NO_ROUTE'
        listener.returns[0].routingKey == 'no-such-queue'
        listener.returns[0].body == 'this message will be returned'.bytes
        listener.returns[1].replyText == 'NO_ROUTE'
        listener.returns[1].routingKey == 'no-such-queue'
        listener.returns[1].body == 'this message will be returned too'.bytes

        and:
        listener.returns.stream().noneMatch(it -> it.body == 'this message will NOT be returned'.bytes)
        listener.returns.stream().noneMatch(it -> it.body == 'this message will not be returned either'.bytes)
    }

    @Requires(property = "spec.name", value = "MandatorySpec")
    @RabbitClient
    static interface NotMandatorySender1 {
        @Binding("no-such-queue")
        void send(String message)
    }

    @Requires(property = "spec.name", value = "MandatorySpec")
    @RabbitClient
    @Mandatory("false")
    static interface NotMandatorySender2 {
        @Binding("no-such-queue")
        Publisher<Void> send(String message)
    }

    @Requires(property = "spec.name", value = "MandatorySpec")
    @RabbitClient
    @Mandatory
    static interface MandatorySender1 {
        @Binding("no-such-queue")
        void send(String message)

        @Mandatory(StringUtils.FALSE)
        @Binding("no-such-queue")
        void sendNotMandatory(String message)
    }

    @Requires(property = "spec.name", value = "MandatorySpec")
    @RabbitClient
    @Mandatory("true")
    static interface MandatorySender2 {
        @Binding("no-such-queue")
        Publisher<Void> send(String message)
    }

    @Requires(property = "spec.name", value = "MandatorySpec")
    @RabbitClient
    static interface MandatorySender3 {
        @Binding("no-such-queue")
        @Mandatory("true")
        void send(String message)
    }

    @Requires(property = "spec.name", value = "MandatorySpec")
    @RabbitClient
    static interface ParametricSender {
        @Binding("no-such-queue")
        void send(@Mandatory boolean mandatory, String message)
    }
}
