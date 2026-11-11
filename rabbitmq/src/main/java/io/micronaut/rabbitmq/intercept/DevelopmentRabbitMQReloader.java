/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.rabbitmq.intercept;

import io.micronaut.context.BeanContext;
import io.micronaut.context.BeanRegistration;
import io.micronaut.context.Qualifier;
import io.micronaut.context.WatchableBeanContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.env.DevelopmentActive;
import io.micronaut.context.reload.ClassChange;
import io.micronaut.context.reload.ClassChangeEvent;
import io.micronaut.context.reload.ReloadStrategy;
import io.micronaut.context.watch.BeanDefinitionChange;
import io.micronaut.context.watch.BeanDefinitionWatcher;
import io.micronaut.context.watch.ClassChangeWatcher;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.order.Ordered;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.BeanType;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitClient;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import io.micronaut.rabbitmq.bind.RabbitArgumentBinder;
import io.micronaut.rabbitmq.bind.RabbitBinderRegistry;
import io.micronaut.rabbitmq.serdes.RabbitMessageSerDes;
import io.micronaut.rabbitmq.serdes.RabbitMessageSerDesRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Keeps the RabbitMQ consumers, publishers and serializers in step with the code in development mode. It exists only
 * in development mode, so nothing of it is on the path of a message consumed or published.
 *
 * <ul>
 *     <li>A definition of a listener bean, with {@link RabbitListener} or {@link Queue} on the class or on a method,
 *     registered or removed, or a listener class changed in place,
 *     restarts the consumers: the changed listener beans are recreated, then the {@link RabbitMQConsumerAdvice},
 *     which cancels every consumer it started and returns its channel to the pool.</li>
 *     <li>A {@link RabbitMessageSerDes}, {@link RabbitArgumentBinder} or {@link RabbitClient} definition registered
 *     or removed, a class of one of them, of a registry, or of a factory that produces one, changed in place, or a
 *     class change applied in place that
 *     retires a classloader, recreates the serializer registry, the binder registry and the
 *     {@link RabbitMQIntroductionAdvice}, whose publisher state is cached by method. The beans that received them,
 *     the {@link RabbitClient} beans and the consumer advice among them, are destroyed with them, as the dependency
 *     graph records, and the consumers are restarted.</li>
 *     <li>A class change applied in place that touches none of these recreates nothing.</li>
 * </ul>
 *
 * <p>The context creates a recreated consumer advice, or one destroyed as a dependent of a recreated bean, again at
 * once, and gives it the {@link Queue} methods it gave it at startup, so that it starts the consumers again on top
 * of the new beans. That covers a bean the advice received that another module recreates, such as a JSON mapper.</p>
 *
 * <p>A change that restarts the application is ignored: the new context starts new consumers, and the one it
 * replaces cancels its own as it stops. The consumer advice has no way to stop the consumers of one bean, so a
 * change of one listener restarts all of them. Each bean is recreated through
 * {@link WatchableBeanContext#recreate(Object)}; a context that does not track bean dependencies recreates
 * nothing, and the consumers keep running as they are until a restart.</p>
 *
 * <p>The watches run after those of other modules, so that a mapper another module recreates for the same change
 * is in place before this reloader restarts the consumers. It holds the context only, never a RabbitMQ bean: a bean
 * that received one is a dependent of it, which recreating it would destroy along with its watches.</p>
 *
 * @author graemerocher
 * @since 5.2.0
 */
@Internal
@Context
@DevelopmentActive
final class DevelopmentRabbitMQReloader {

    private static final Logger LOG = LoggerFactory.getLogger(DevelopmentRabbitMQReloader.class);

    /**
     * The beans that cache what they were built from: the serializer and binder registries, which hold the beans
     * registered when they were created, and the client advice, which caches the publisher state by method.
     */
    private static final List<Class<?>> REGISTRIES = List.of(RabbitMessageSerDesRegistry.class, RabbitBinderRegistry.class, RabbitMQIntroductionAdvice.class);

    /**
     * The types of a class whose change makes the registries stale.
     */
    private static final List<Class<?>> REGISTERED_TYPES = List.of(RabbitMessageSerDes.class, RabbitMessageSerDesRegistry.class, RabbitArgumentBinder.class, RabbitBinderRegistry.class);

    /**
     * The stereotypes of a listener, on the class or a method.
     */
    private static final List<Class<? extends Annotation>> LISTENER_STEREOTYPES = List.of(RabbitListener.class, Queue.class);

    /**
     * The stereotypes of a client.
     */
    private static final List<Class<? extends Annotation>> CLIENTS = List.of(RabbitClient.class);

    /**
     * The listener beans: a {@link RabbitListener} or {@link Queue} on the class, or on an executable method.
     */
    private static final Qualifier<Object> LISTENERS = new ListenerQualifier();

    /**
     * The beans the registries are built from: the serializers, the binders, their registries and the clients.
     */
    private static final Qualifier<Object> REGISTERED = new RegisteredQualifier();

    private final BeanContext beanContext;

    /**
     * @param beanContext The context, watched when it can be
     */
    DevelopmentRabbitMQReloader(BeanContext beanContext) {
        this.beanContext = beanContext;
        if (beanContext instanceof WatchableBeanContext watchable) {
            watchable.definitions().qualifier(LISTENERS).watch(new ListenerDefinitionsWatcher());
            // one watch, so that a bean that is several of these, such as a serializer that is a binder too, restarts once
            watchable.definitions().qualifier(REGISTERED).watch(new RegistryDefinitionsWatcher());
            watchable.classChanges().watch(new ClassWatcher());
        }
    }

    private void onClassChange(ClassChangeEvent change) {
        if (change.strategy() == ReloadStrategy.RESTART) {
            // the new context starts its own consumers, and the one it replaces cancels these as it stops
            return;
        }
        if (!change.retiredLoaders().isEmpty()) {
            restartConsumers(true, List.of(), "a reload retired a classloader");
            return;
        }
        boolean registries = false;
        List<String> listeners = new ArrayList<>();
        for (ClassChange classChange : change.changes()) {
            String className = classChange.className();
            Class<?> type = load(className, change.newLoader());
            if (isListener(type)) {
                listeners.add(className);
            }
            if (isRegisteredOrClient(type)) {
                registries = true;
            }
        }
        // what the beans were as the context was compiled: a change applied in place leaves the definitions as they were
        for (BeanDefinition<?> definition : changedDefinitions(change)) {
            String beanType = definition.getBeanType().getName();
            if (declares(definition, LISTENER_STEREOTYPES) && !listeners.contains(beanType)) {
                listeners.add(beanType);
            }
            if (isRegisteredBean(definition) || declares(definition, CLIENTS)) {
                registries = true;
            }
        }
        // a change that touches none of these needs nothing: a module that recreated a bean the consumer advice
        // received, such as a JSON mapper, destroyed the advice with it, and the context created it again and
        // restarted it
        if (registries || !listeners.isEmpty()) {
            restartConsumers(registries, listeners, registries ? "a serializer, a binder or a RabbitMQ client changed" : listeners + " changed");
        }
    }

    private static Class<?> load(String className, ClassLoader loader) {
        try {
            return Class.forName(className, false, loader);
        } catch (ClassNotFoundException | LinkageError e) {
            // removed, or not loadable on its own: nothing of the new generation is built from it
            return null;
        }
    }

    private static boolean isListener(Class<?> type) {
        if (type == null) {
            return false;
        }
        if (hasAnnotation(type.getAnnotations(), RabbitListener.class)) {
            return true;
        }
        try {
            // declared methods, of every visibility, through the hierarchy: a listener method need not be public
            for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
                for (Method method : current.getDeclaredMethods()) {
                    if (hasAnnotation(method.getAnnotations(), Queue.class)) {
                        return true;
                    }
                }
            }
        } catch (LinkageError e) {
            // a method signature that does not resolve: the class cannot be a working listener
        }
        return false;
    }

    private static boolean isRegisteredOrClient(Class<?> type) {
        if (type == null) {
            return false;
        }
        for (Class<?> registeredType : REGISTERED_TYPES) {
            if (registeredType.isAssignableFrom(type)) {
                return true;
            }
        }
        return hasAnnotation(type.getAnnotations(), RabbitClient.class);
    }

    /**
     * Whether an annotation is present, or is the stereotype of one that is.
     */
    private static boolean hasAnnotation(Annotation[] annotations, Class<? extends Annotation> wanted) {
        for (Annotation annotation : annotations) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType == wanted || annotationType.isAnnotationPresent(wanted)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The definitions of the context built from a class of a change applied in place: its bean type or a superclass
     * of it, the factory that declares it, or the class its advice intercepts, as the event tells.
     *
     * @param change The change
     * @return The definitions
     */
    private List<BeanDefinition<?>> changedDefinitions(ClassChangeEvent change) {
        List<BeanDefinition<?>> changed = new ArrayList<>();
        for (BeanDefinition<?> definition : beanContext.getAllBeanDefinitions()) {
            if (change.affectsDefinition(definition)) {
                changed.add(definition);
            }
        }
        return changed;
    }

    /**
     * Whether a definition carries one of the stereotypes, on the class or an executable method.
     *
     * @param definition The definition
     * @param stereotypes The stereotypes
     * @return Whether one is there
     */
    private static boolean declares(BeanDefinition<?> definition, List<Class<? extends Annotation>> stereotypes) {
        try {
            for (Class<? extends Annotation> stereotype : stereotypes) {
                if (definition.getAnnotationMetadata().hasStereotype(stereotype)) {
                    return true;
                }
            }
            for (ExecutableMethod<?, ?> method : definition.getExecutableMethods()) {
                for (Class<? extends Annotation> stereotype : stereotypes) {
                    if (method.getAnnotationMetadata().hasStereotype(stereotype)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (RuntimeException | LinkageError e) {
            // a method that no longer resolves: what it was is unknown, so it counts
            return true;
        }
    }

    /**
     * Whether a bean is one the registries are built from: a serializer, a binder, a registry of them, or a client.
     */
    private static boolean isRegisteredBean(BeanType<?> candidate) {
        Class<?> beanType = candidate.getBeanType();
        for (Class<?> registeredType : REGISTERED_TYPES) {
            if (registeredType.isAssignableFrom(beanType)) {
                return true;
            }
        }
        AnnotationMetadata metadata = candidate.getAnnotationMetadata();
        for (Class<? extends Annotation> client : CLIENTS) {
            if (metadata.hasStereotype(client)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Recreates what changed and the consumer advice, which cancels every consumer. The context creates the advice
     * again at once and gives it the {@link Queue} methods, as at startup, so that it starts the consumers again on
     * top of the new beans. Nothing is created that was not created already, but for the listener beans the advice
     * starts.
     *
     * @param registries Whether to recreate the serializer and binder registries and the client advice, and the
     * beans that received them
     * @param listeners The listener classes that changed, whose beans are recreated
     * @param reason Why, for the log
     */
    private void restartConsumers(boolean registries, List<String> listeners, String reason) {
        if (!(beanContext instanceof WatchableBeanContext context)) {
            return;
        }
        // taken first: recreating one destroys the beans that received it, as the graph records them. The listener
        // beans go first, as the advice starts its consumers again as soon as it is recreated, on the listener beans
        // it finds then; it goes last, recreated already, and so skipped, when a registry it received was
        List<Object> beans = new ArrayList<>();
        if (!listeners.isEmpty()) {
            Set<String> names = new HashSet<>(listeners);
            for (BeanRegistration<?> registration : beanContext.getActiveBeanRegistrations(LISTENERS)) {
                if (names.contains(registration.getBeanDefinition().getBeanType().getName())) {
                    add(beans, registration.bean());
                }
            }
        }
        if (registries) {
            for (Class<?> type : REGISTRIES) {
                for (BeanRegistration<?> registration : beanContext.getActiveBeanRegistrations(type)) {
                    add(beans, registration.bean());
                }
            }
        }
        for (BeanRegistration<RabbitMQConsumerAdvice> registration : beanContext.getActiveBeanRegistrations(RabbitMQConsumerAdvice.class)) {
            add(beans, registration.bean());
        }
        if (beans.isEmpty()) {
            return;
        }
        LOG.debug("Restarting the RabbitMQ consumers: {}", reason);
        for (Object bean : beans) {
            // false for a bean destroyed with one recreated before it, and for all of them in a context that does
            // not track bean dependencies: they are kept, and the consumers keep running until a restart
            context.recreate(bean);
        }
    }

    private static void add(List<Object> beans, Object bean) {
        for (Object taken : beans) {
            if (taken == bean) {
                return;
            }
        }
        beans.add(bean);
    }

    private static List<String> names(BeanDefinitionChange<?> change) {
        List<String> changed = new ArrayList<>();
        for (BeanDefinition<?> definition : change.removed()) {
            changed.add(definition.getBeanType().getName());
        }
        for (BeanDefinition<?> definition : change.added()) {
            changed.add(definition.getBeanType().getName());
        }
        return changed;
    }

    private static boolean ignored(BeanDefinitionChange<?> change) {
        return change.initial() || (change.added().isEmpty() && change.removed().isEmpty());
    }

    /**
     * Restarts the consumers when a listener definition is registered or removed. The first batch is what the
     * consumer advice was given at startup.
     */
    private final class ListenerDefinitionsWatcher implements BeanDefinitionWatcher<Object>, Ordered {
        @Override
        public void onChange(BeanDefinitionChange<Object> change) {
            if (!ignored(change)) {
                restartConsumers(false, names(change), "listener definitions changed");
            }
        }

        @Override
        public int getOrder() {
            return Ordered.LOWEST_PRECEDENCE;
        }
    }

    /**
     * Selects the listener beans, whether the listener stereotype is on the class or only on a method. The class
     * metadata of a bean is read first, which needs nothing loaded. The methods are read only for a bean that requires
     * method processing, as a bean whose methods a consumer advice receives does; that skips the executable methods of
     * every other bean, and the class that holds them. A reference, which has no methods to read, is kept when it
     * requires method processing.
     */
    private static final class ListenerQualifier implements Qualifier<Object> {
        @Override
        public <B extends BeanType<Object>> Stream<B> reduce(Class<Object> beanType, Stream<B> candidates) {
            return candidates.filter(ListenerQualifier::isListenerBean);
        }

        @Override
        public boolean doesQualify(Class<Object> beanType, BeanType<Object> candidate) {
            return isListenerBean(candidate);
        }

        private static boolean isListenerBean(BeanType<?> candidate) {
            if (hasStereotype(candidate.getAnnotationMetadata())) {
                return true;
            }
            if (!candidate.requiresMethodProcessing()) {
                return false;
            }
            if (!(candidate instanceof BeanDefinition<?> definition)) {
                return true;
            }
            for (ExecutableMethod<?, ?> method : definition.getExecutableMethods()) {
                if (hasStereotype(method.getAnnotationMetadata())) {
                    return true;
                }
            }
            return false;
        }

        private static boolean hasStereotype(AnnotationMetadata metadata) {
            for (Class<? extends Annotation> stereotype : LISTENER_STEREOTYPES) {
                if (metadata.hasStereotype(stereotype)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return "listeners";
        }
    }

    /**
     * Selects the beans the registries are built from: a serializer, a binder, a registry of them, or a client.
     */
    private static final class RegisteredQualifier implements Qualifier<Object> {
        @Override
        public <B extends BeanType<Object>> Stream<B> reduce(Class<Object> beanType, Stream<B> candidates) {
            return candidates.filter(DevelopmentRabbitMQReloader::isRegisteredBean);
        }

        @Override
        public boolean doesQualify(Class<Object> beanType, BeanType<Object> candidate) {
            return isRegisteredBean(candidate);
        }

        @Override
        public String toString() {
            return "serializers, binders and clients";
        }
    }

    /**
     * Recreates the registries when a serializer, binder, registry or client definition is registered or removed. The
     * first batch is what they were built from.
     */
    private final class RegistryDefinitionsWatcher implements BeanDefinitionWatcher<Object>, Ordered {
        @Override
        public void onChange(BeanDefinitionChange<Object> change) {
            if (!ignored(change)) {
                restartConsumers(true, List.of(), names(change) + " registered or removed");
            }
        }

        @Override
        public int getOrder() {
            return Ordered.LOWEST_PRECEDENCE;
        }
    }

    /**
     * Follows a class change applied in place.
     */
    private final class ClassWatcher implements ClassChangeWatcher, Ordered {
        @Override
        public void onChange(ClassChangeEvent change) {
            onClassChange(change);
        }

        @Override
        public int getOrder() {
            return Ordered.LOWEST_PRECEDENCE;
        }
    }

}
