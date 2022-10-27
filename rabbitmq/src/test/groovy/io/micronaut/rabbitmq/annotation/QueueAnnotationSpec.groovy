package io.micronaut.rabbitmq.annotation

import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.MessageMapping
import io.micronaut.rabbitmq.AbstractRabbitMQTest

class QueueAnnotationSpec extends AbstractRabbitMQTest {

    void 'test that @Queue value aliases to @MessageMapping'() {
        given:
        startContext()

        def definition = applicationContext.getBeanDefinition(MyConsumer)

        when:
        def method = definition.getRequiredMethod('receive', String)
        def annotationValue = method.getValue(MessageMapping, String[])

        then:
        annotationValue.isPresent()
        annotationValue.get().contains 'simple'
    }

    void 'test that @Queue autoAcknowledgment flag is disabled by default'() {
        given:
        startContext()

        def definition = applicationContext.getBeanDefinition(MyConsumer)

        when:
        def method = definition.getRequiredMethod('receive', String)
        def annotationValue = method.getAnnotation(Queue.class)
        def autoAcknowledgment = annotationValue.booleanValue("autoAcknowledgment")

        then:
        autoAcknowledgment.isEmpty()
    }

    void 'test that @Queue autoAcknowledgment flag is enabled'() {
        given:
        startContext()

        def definition = applicationContext.getBeanDefinition(MyConsumer)

        when:
        def method = definition.getRequiredMethod('receiveAndAutoAck', String)
        def annotationValue = method.getAnnotation(Queue.class)
        def autoAcknowledgment = annotationValue.booleanValue("autoAcknowledgment")

        then:
        autoAcknowledgment.isPresent()
        autoAcknowledgment.get() == Boolean.TRUE
    }

    @Requires(property = 'spec.name', value = 'QueueAnnotationSpec')
    @RabbitListener
    static class MyConsumer {

        List<String> stuff = []

        @Queue(value = 'simple')
        void receive(String thing) {
            stuff << thing
        }

        @Queue(value = 'simple', autoAcknowledgment = true)
        void receiveAndAutoAck(String thing) {
            stuff << thing
        }
    }
}
