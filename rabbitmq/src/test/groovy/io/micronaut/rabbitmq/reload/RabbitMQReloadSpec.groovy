package io.micronaut.rabbitmq.reload

import com.rabbitmq.client.Channel
import groovy.transform.PackageScope
import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultBeanContext
import io.micronaut.context.annotation.Requires
import io.micronaut.context.reload.ClassChange
import io.micronaut.context.reload.ClassChangeEvent
import io.micronaut.context.reload.ReloadStrategy
import io.micronaut.core.type.Argument
import io.micronaut.inject.BeanDefinition
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.rabbitmq.annotation.RabbitListener
import io.micronaut.rabbitmq.bind.RabbitBinderRegistry
import io.micronaut.rabbitmq.connect.ChannelInitializer
import io.micronaut.rabbitmq.connect.ChannelPool
import io.micronaut.rabbitmq.intercept.MutableBasicProperties
import io.micronaut.rabbitmq.intercept.RabbitMQConsumerAdvice
import io.micronaut.rabbitmq.intercept.RabbitMQIntroductionAdvice
import io.micronaut.rabbitmq.serdes.RabbitMessageSerDes
import io.micronaut.rabbitmq.serdes.RabbitMessageSerDesRegistry
import io.micronaut.context.annotation.Factory
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import io.micronaut.core.bind.ArgumentBinder
import io.micronaut.core.convert.ArgumentConversionContext
import io.micronaut.rabbitmq.bind.RabbitConsumerState
import io.micronaut.rabbitmq.bind.RabbitTypeArgumentBinder
import jakarta.inject.Singleton

import java.util.concurrent.atomic.AtomicInteger

import java.util.concurrent.CopyOnWriteArrayList

/**
 * The development reloader against a broker.
 */
class RabbitMQReloadSpec extends AbstractRabbitMQTest {

    private static final String RELOADER = 'io.micronaut.rabbitmq.intercept.DevelopmentRabbitMQReloader'
    static final String QUEUE = 'dev-reload'
    static final String METHOD_QUEUE = 'dev-reload-method'

    void "in development mode an in-place change of a listener class restarts the consumers on a new advice and a new listener, and a restart or an unrelated change does not"() {
        given:
        devContext(true)
        ReloadListener listener = applicationContext.getBean(ReloadListener)
        RabbitMQConsumerAdvice advice = applicationContext.getBean(RabbitMQConsumerAdvice)

        expect: 'the reloader exists only in development mode'
        applicationContext.containsBean(reloader())
        waitFor { assert consumers() == 1 }

        when:
        applicationContext.getBean(ReloadClient).send('one')

        then:
        waitFor { assert listener.received == ['one'] }

        when: 'the application restarts: the new context starts its own consumers'
        applicationContext.publishEvent(classChange([ReloadListener.classLoader] as Set, [], ReloadStrategy.RESTART))

        then:
        applicationContext.getBean(RabbitMQConsumerAdvice).is(advice)
        consumers() == 1

        when: 'a class that is not a listener is redefined in place'
        applicationContext.publishEvent(classChange([] as Set, [new ClassChange(RabbitMQReloadSpec.name, ClassChange.Kind.MODIFIED)], ReloadStrategy.RELOAD))

        then:
        applicationContext.getBean(RabbitMQConsumerAdvice).is(advice)
        applicationContext.getBean(ReloadListener).is(listener)
        consumers() == 1

        when: 'the listener class is redefined in place'
        applicationContext.publishEvent(classChange([] as Set, [new ClassChange(ReloadListener.name, ClassChange.Kind.MODIFIED)], ReloadStrategy.RELOAD))
        ReloadListener recreated = applicationContext.getBean(ReloadListener)

        then: 'the advice and the listener are new, and the queue has exactly one consumer, of the new listener'
        !applicationContext.getBean(RabbitMQConsumerAdvice).is(advice)
        !recreated.is(listener)
        waitFor { assert consumers() == 1 }

        when:
        applicationContext.getBean(ReloadClient).send('two')

        then: 'messages reach the new listener only'
        waitFor { assert recreated.received == ['two'] }
        listener.received == ['one']
    }

    void "in development mode a serializer change or a retired classloader recreates the registries, the client advice and the clients, and restarts the consumers"() {
        given:
        devContext(true)
        ReloadClient client = applicationContext.getBean(ReloadClient)
        RabbitMessageSerDesRegistry serDes = applicationContext.getBean(RabbitMessageSerDesRegistry)
        RabbitBinderRegistry binders = applicationContext.getBean(RabbitBinderRegistry)
        RabbitMQIntroductionAdvice publishers = applicationContext.getBean(RabbitMQIntroductionAdvice)
        RabbitMQConsumerAdvice advice = applicationContext.getBean(RabbitMQConsumerAdvice)
        ReloadListener listener = applicationContext.getBean(ReloadListener)
        waitFor { assert consumers() == 1 }

        when: 'a serializer is redefined in place'
        applicationContext.publishEvent(classChange([] as Set, [new ClassChange(ReloadSerDes.name, ClassChange.Kind.MODIFIED)], ReloadStrategy.RELOAD))

        then: 'the registries and the client advice are new, and so are the beans that received them'
        !applicationContext.getBean(RabbitMessageSerDesRegistry).is(serDes)
        !applicationContext.getBean(RabbitBinderRegistry).is(binders)
        !applicationContext.getBean(RabbitMQIntroductionAdvice).is(publishers)
        !applicationContext.getBean(ReloadClient).is(client)
        !applicationContext.getBean(RabbitMQConsumerAdvice).is(advice)

        and: 'the consumers are started again, the listener bean being unchanged'
        applicationContext.getBean(ReloadListener).is(listener)
        waitFor { assert consumers() == 1 }

        when:
        applicationContext.getBean(ReloadClient).send('one')

        then:
        waitFor { assert listener.received == ['one'] }

        when: 'a reload retires a classloader'
        serDes = applicationContext.getBean(RabbitMessageSerDesRegistry)
        advice = applicationContext.getBean(RabbitMQConsumerAdvice)
        applicationContext.publishEvent(classChange([ReloadListener.classLoader] as Set, [], ReloadStrategy.RELOAD))

        then:
        !applicationContext.getBean(RabbitMessageSerDesRegistry).is(serDes)
        !applicationContext.getBean(RabbitMQConsumerAdvice).is(advice)
        waitFor { assert consumers() == 1 }

        when:
        applicationContext.getBean(ReloadClient).send('two')

        then:
        waitFor { assert listener.received == ['one', 'two'] }
    }

    void "in development mode a listener definition registered while running restarts the consumers once"() {
        given:
        devContext(true)
        RabbitMQConsumerAdvice advice = applicationContext.getBean(RabbitMQConsumerAdvice)
        BeanDefinition<?> definition = applicationContext.getBeanDefinition(ReloadListener)
        waitFor { assert consumers() == 1 }

        when: 'the launcher swaps the definition of the listener for another of the same class'
        ((DefaultBeanContext) applicationContext).notifyDefinitionChange([definition], [definition])

        then: 'every consumer started before is cancelled, and exactly one consumes for the listener'
        !applicationContext.getBean(RabbitMQConsumerAdvice).is(advice)
        waitFor { assert consumers() == 1 }

        when:
        applicationContext.getBean(ReloadClient).send('one')

        then:
        waitFor { assert applicationContext.getBean(ReloadListener).received == ['one'] }
    }

    void "in development mode a serializer definition registered while running recreates the serializer registry"() {
        given:
        devContext(true)
        RabbitMessageSerDesRegistry serDes = applicationContext.getBean(RabbitMessageSerDesRegistry)
        BeanDefinition<?> definition = applicationContext.getBeanDefinition(ReloadSerDes)

        when:
        ((DefaultBeanContext) applicationContext).notifyDefinitionChange([definition], [definition])

        then:
        !applicationContext.getBean(RabbitMessageSerDesRegistry).is(serDes)
        waitFor { assert consumers() == 1 }
    }

    void "in development mode the context starts the consumers again on a new advice when another module recreates a bean the advice received, without the reloader"() {
        given:
        devContext(true)
        RabbitMQConsumerAdvice advice = applicationContext.getBean(RabbitMQConsumerAdvice)
        ReloadListener listener = applicationContext.getBean(ReloadListener)
        waitFor { assert consumers() == 1 }

        when: 'a module recreates the serializer registry for a change of its own, which destroys the advice'
        applicationContext.recreate(applicationContext.getBean(RabbitMessageSerDesRegistry))

        then: 'the context created the advice again at once and gave it the queue methods: no class change follows'
        !applicationContext.getBean(RabbitMQConsumerAdvice).is(advice)
        waitFor { assert consumers() == 1 }

        when:
        applicationContext.getBean(ReloadClient).send('one')

        then:
        waitFor { assert listener.received == ['one'] }
    }

    void "in development mode a context that does not track bean dependencies keeps the consumers running"() {
        given:
        devContext(false)
        RabbitMQConsumerAdvice advice = applicationContext.getBean(RabbitMQConsumerAdvice)
        waitFor { assert consumers() == 1 }

        expect:
        applicationContext.containsBean(reloader())

        when:
        applicationContext.publishEvent(classChange([] as Set, [new ClassChange(ReloadListener.name, ClassChange.Kind.MODIFIED)], ReloadStrategy.RELOAD))

        then:
        applicationContext.getBean(RabbitMQConsumerAdvice).is(advice)
        consumers() == 1
    }

    void "outside development mode there is no reloader, and the same beans keep their consumers through class changes"() {
        given:
        startContext()
        RabbitMQConsumerAdvice advice = applicationContext.getBean(RabbitMQConsumerAdvice)
        RabbitMessageSerDesRegistry serDes = applicationContext.getBean(RabbitMessageSerDesRegistry)
        ReloadListener listener = applicationContext.getBean(ReloadListener)
        waitFor { assert consumers() == 1 }

        expect:
        !applicationContext.containsBean(reloader())

        when:
        applicationContext.publishEvent(classChange([ReloadListener.classLoader] as Set, [new ClassChange(ReloadListener.name, ClassChange.Kind.MODIFIED)], ReloadStrategy.RELOAD))

        then:
        applicationContext.getBean(RabbitMQConsumerAdvice).is(advice)
        applicationContext.getBean(RabbitMessageSerDesRegistry).is(serDes)
        applicationContext.getBean(ReloadListener).is(listener)
        consumers() == 1

        when:
        applicationContext.getBean(ReloadClient).send('one')

        then: 'the listener consumes as before'
        waitFor { assert listener.received == ['one'] }
    }

    void "in development mode a definition of a bean whose listener is on a method only, registered while running, restarts the consumers"() {
        given:
        devContext(true)
        RabbitMQConsumerAdvice advice = applicationContext.getBean(RabbitMQConsumerAdvice)
        MethodListener listener = applicationContext.getBean(MethodListener)
        BeanDefinition<?> definition = applicationContext.getBeanDefinition(MethodListener)
        waitFor { assert consumers(METHOD_QUEUE) == 1 }

        when: 'the launcher swaps the definition of a bean that has @Queue on a method and no @RabbitListener'
        ((DefaultBeanContext) applicationContext).notifyDefinitionChange([definition], [definition])

        then: 'the advice and the listener are new, and one consumer consumes for it'
        !applicationContext.getBean(RabbitMQConsumerAdvice).is(advice)
        !applicationContext.getBean(MethodListener).is(listener)
        waitFor { assert consumers(METHOD_QUEUE) == 1 }
    }

    void "in development mode a definition of a bean that is both a serializer and a binder restarts the consumers once"() {
        given:
        devContext(true)
        BeanDefinition<?> definition = applicationContext.getBeanDefinition(SerDesAndBinder)
        int created = AdviceCreations.count.get()

        when:
        ((DefaultBeanContext) applicationContext).notifyDefinitionChange([definition], [definition])

        then: 'the consumer advice was created again exactly once'
        AdviceCreations.count.get() == created + 1
        waitFor { assert consumers() == 1 }
    }

    void "in development mode an in-place change of a factory that produces a serializer recreates the registries"() {
        given:
        devContext(true)
        RabbitMessageSerDesRegistry serDes = applicationContext.getBean(RabbitMessageSerDesRegistry)
        RabbitMQConsumerAdvice advice = applicationContext.getBean(RabbitMQConsumerAdvice)

        when:
        applicationContext.publishEvent(classChange([] as Set, [new ClassChange(SerDesFactory.name, ClassChange.Kind.MODIFIED)], ReloadStrategy.RELOAD))

        then:
        !applicationContext.getBean(RabbitMessageSerDesRegistry).is(serDes)
        !applicationContext.getBean(RabbitMQConsumerAdvice).is(advice)
        waitFor { assert consumers() == 1 }
    }

    void "in development mode an in-place change of a class with a listener annotation on a package-private method restarts the consumers"() {
        given:
        devContext(true)
        RabbitMQConsumerAdvice advice = applicationContext.getBean(RabbitMQConsumerAdvice)

        expect: 'no bean definition says the class is a listener: only the redefined class does'
        !applicationContext.getBeanDefinitions(PackagePrivateQueue).any()

        when:
        applicationContext.publishEvent(classChange([] as Set, [new ClassChange(PackagePrivateQueue.name, ClassChange.Kind.MODIFIED)], ReloadStrategy.RELOAD))

        then:
        !applicationContext.getBean(RabbitMQConsumerAdvice).is(advice)
        waitFor { assert consumers() == 1 }
    }

    private void devContext(boolean track) {
        applicationContext = ApplicationContext.builder()
            .properties(["rabbitmq.host": rabbitContainer.host,
                         "rabbitmq.port": rabbitContainer.getMappedPort(5672),
                         "spec.name": getClass().simpleName,
                         "micronaut.dev.enabled": true])
            .environments("test")
            .beanDependencyTrackingEnabled(track)
            .start()
    }

    /**
     * The consumers the broker has for the queue.
     */
    private long consumers(String queue = QUEUE) {
        ChannelPool pool = applicationContext.getBean(ChannelPool)
        Channel channel = pool.getChannel()
        try {
            return channel.consumerCount(queue)
        } finally {
            pool.returnChannel(channel)
        }
    }

    private static Class<?> reloader() {
        return Class.forName(RELOADER)
    }

    private ClassChangeEvent classChange(Set<ClassLoader> retired, List<ClassChange> changes, ReloadStrategy strategy) {
        return new ClassChangeEvent(this, retired, RabbitMQReloadSpec.classLoader, changes, strategy)
    }

    /**
     * As a class redefined in place could be, with a listener annotation newly on a package-private method.
     */
    static class PackagePrivateQueue {
        @PackageScope
        @Queue('dev-reload-package-private')
        void receive(String value) {
        }
    }

    @Singleton
    @Requires(property = 'spec.name', value = 'RabbitMQReloadSpec')
    static class ReloadQueue extends ChannelInitializer {
        @Override
        void initialize(Channel channel, String name) throws IOException {
            channel.queueDeclare(QUEUE, false, false, false, [:])
            channel.queueDeclare(METHOD_QUEUE, false, false, false, [:])
        }
    }

    @RabbitListener
    @Requires(property = 'spec.name', value = 'RabbitMQReloadSpec')
    static class ReloadListener {
        final List<String> received = new CopyOnWriteArrayList<>()

        @Queue(QUEUE)
        void receive(String value) {
            received.add(value)
        }
    }

    @RabbitClient
    @Requires(property = 'spec.name', value = 'RabbitMQReloadSpec')
    static interface ReloadClient {
        @Binding(QUEUE)
        void send(String value)
    }

    @Singleton
    @Requires(property = 'spec.name', value = 'RabbitMQReloadSpec')
    static class ReloadSerDes implements RabbitMessageSerDes<UUID> {
        @Override
        UUID deserialize(io.micronaut.rabbitmq.bind.RabbitConsumerState state, Argument<UUID> type) {
            return UUID.fromString(new String(state.body))
        }

        @Override
        byte[] serialize(UUID data, MutableBasicProperties properties) {
            return data?.toString()?.bytes
        }

        @Override
        boolean supports(Argument<UUID> type) {
            return type.type == UUID
        }
    }

    @Singleton
    @Requires(property = 'spec.name', value = 'RabbitMQReloadSpec')
    static class MethodListener {
        @Queue(METHOD_QUEUE)
        void receive(String value) {
        }
    }

    @Singleton
    @Requires(property = 'spec.name', value = 'RabbitMQReloadSpec')
    static class SerDesAndBinder implements RabbitMessageSerDes<Locale>, RabbitTypeArgumentBinder<Locale> {
        @Override
        Locale deserialize(RabbitConsumerState state, Argument<Locale> type) {
            return Locale.forLanguageTag(new String(state.body))
        }

        @Override
        byte[] serialize(Locale data, MutableBasicProperties properties) {
            return data?.toLanguageTag()?.bytes
        }

        @Override
        boolean supports(Argument<Locale> type) {
            return type.type == Locale
        }

        @Override
        Argument<Locale> argumentType() {
            return Argument.of(Locale)
        }

        @Override
        ArgumentBinder.BindingResult<Locale> bind(ArgumentConversionContext<Locale> context, RabbitConsumerState source) {
            return ArgumentBinder.BindingResult.UNSATISFIED
        }
    }

    @Factory
    @Requires(property = 'spec.name', value = 'RabbitMQReloadSpec')
    static class SerDesFactory {
        @Singleton
        FactorySerDes serDes() {
            return new FactorySerDes()
        }
    }

    static class FactorySerDes implements RabbitMessageSerDes<Currency> {
        @Override
        Currency deserialize(RabbitConsumerState state, Argument<Currency> type) {
            return Currency.getInstance(new String(state.body))
        }

        @Override
        byte[] serialize(Currency data, MutableBasicProperties properties) {
            return data?.currencyCode?.bytes
        }

        @Override
        boolean supports(Argument<Currency> type) {
            return type.type == Currency
        }
    }

    @Singleton
    @Requires(property = 'spec.name', value = 'RabbitMQReloadSpec')
    static class AdviceCreations implements BeanCreatedEventListener<RabbitMQConsumerAdvice> {
        static final AtomicInteger count = new AtomicInteger()

        @Override
        RabbitMQConsumerAdvice onCreated(BeanCreatedEvent<RabbitMQConsumerAdvice> event) {
            count.incrementAndGet()
            return event.bean
        }
    }
}
