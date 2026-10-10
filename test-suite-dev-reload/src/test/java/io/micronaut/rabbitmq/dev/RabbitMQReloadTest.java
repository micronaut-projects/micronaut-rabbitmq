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
package io.micronaut.rabbitmq.dev;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.reload.ClassChange;
import io.micronaut.context.reload.ClassChangeEvent;
import io.micronaut.context.reload.ReloadStrategy;
import io.micronaut.dev.tck.ReloadHarness;
import io.micronaut.dev.tck.ReloadTck;
import io.micronaut.rabbitmq.connect.ChannelPool;
import io.micronaut.rabbitmq.testcontainers.RabbitMQ;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs an application with a RabbitMQ listener through the development runtime, against a broker, and edits the
 * listener. A change applied in place restarts the consumers on the new listener; the restart cancels the consumers
 * of the retired generation as its context stops, and the next generation consumes on the same connection and channel
 * pool, which development mode retains until a change under {@code rabbitmq} releases them. Nothing of the retired
 * generation stays reachable.
 */
class RabbitMQReloadTest {

    private static final String QUEUE = "dev-reload-queue";

    private static final String LISTENER = """
        package example;

        import io.micronaut.rabbitmq.annotation.Queue;
        import io.micronaut.rabbitmq.annotation.RabbitListener;

        import java.util.List;
        import java.util.concurrent.CopyOnWriteArrayList;

        @RabbitListener
        public class Listener {
            private final List<String> received = new CopyOnWriteArrayList<>();

            @Queue("%s")
            public void receive(String value) {
                received.add("%s " + value);
            }

            public List<String> received() {
                return received;
            }
        }
        """;

    @TempDir
    Path project;

    @Test
    void anInPlaceChangeRestartsTheConsumersAndARestartCancelsThemAndLeavesTheRetiredGenerationCollectable() throws Exception {
        Map<String, String> properties = RabbitMQ.getProperties();
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(properties.get("rabbitmq.uri"));
        factory.setUsername(properties.get("rabbitmq.username"));
        factory.setPassword(properties.get("rabbitmq.password"));
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel();
             ReloadHarness harness = ReloadHarness.inDirectory(project)) {
            channel.queueDeclare(QUEUE, false, false, false, Map.of());
            properties.forEach(harness::property);
            harness.source("example.Listener", LISTENER.formatted(QUEUE, "first"));
            harness.start();
            assertReloaderPresent(harness.context());

            awaitTrue("the first generation consumes the queue", () -> consumers(channel) == 1);
            publish(channel, "one");
            awaitTrue("the first generation receives", () -> received(harness.context()).contains("first one"));
            ReloadTck.assertFollowsReload(harness, RabbitMQReloadTest::listener);

            // the listener class changed in place: the consumers are restarted on a new listener bean
            Object listener = listener(harness.context());
            long inPlaceStart = System.nanoTime();
            changedInPlace(harness, "example.Listener");
            Object recreated = listener(harness.context());
            assertNotSame(listener, recreated);
            awaitTrue("the queue has the one consumer of the new listener", () -> consumers(channel) == 1);
            publish(channel, "in-place");
            awaitTrue("the new listener receives", () -> received(harness.context()).contains("first in-place"));
            System.out.println("The restarted consumers received a message " + millisSince(inPlaceStart) + " ms after the change");
            listener = null;
            recreated = null;

            harness.source("example.Listener", LISTENER.formatted(QUEUE, "second"));
            List<String> firstReceived = received(harness.context());
            Connection retained = harness.context().getBean(Connection.class);
            ChannelPool retainedPool = harness.context().getBean(ChannelPool.class);
            long reloadStart = System.nanoTime();
            harness.reload();
            assertEquals(2, harness.generation());
            assertReloaderPresent(harness.context());

            // the second generation consumes on the connection and the channel pool of the first
            ReloadTck.assertRetained(harness, retained);
            ReloadTck.assertRetained(harness, retainedPool);
            assertSame(retained, harness.context().getBean(Connection.class));
            assertSame(retainedPool, harness.context().getBean(ChannelPool.class));
            assertTrue(retained.isOpen());

            // the retired context cancelled its consumer as it stopped: the queue has the one consumer of the new one
            awaitTrue("the queue has the one consumer of the second generation", () -> consumers(channel) == 1);
            publish(channel, "two");
            awaitTrue("the second generation receives", () -> received(harness.context()).contains("second two"));
            long restarted = millisSince(reloadStart);
            assertTrue(restarted < 30_000, "the second generation received a message " + restarted + " ms after the reload started");
            System.out.println("The second generation received a message " + restarted + " ms after the reload started");
            assertFalse(received(harness.context()).contains("first two"));
            assertEquals(List.of("first in-place"), firstReceived, "the retired listener received nothing more");
            ReloadTck.assertFollowsReload(harness, RabbitMQReloadTest::listener);

            // neither the consumers of the first generation, their channels, the retained connection, nor the
            // development-only reloader keep it reachable
            ReloadTck.assertRetiredGenerationsCollected(harness);
        }
    }

    @Test
    void aChangeUnderTheRabbitMQPrefixReleasesTheRetainedConnection() throws Exception {
        Map<String, String> properties = RabbitMQ.getProperties();
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(properties.get("rabbitmq.uri"));
        factory.setUsername(properties.get("rabbitmq.username"));
        factory.setPassword(properties.get("rabbitmq.password"));
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel();
             ReloadHarness harness = ReloadHarness.inDirectory(project)) {
            channel.queueDeclare(QUEUE, false, false, false, Map.of());
            properties.forEach(harness::property);
            harness.source("example.Listener", LISTENER.formatted(QUEUE, "first"));
            harness.start();
            awaitTrue("the first generation consumes the queue", () -> consumers(channel) == 1);
            Connection first = harness.context().getBean(Connection.class);
            ChannelPool firstPool = harness.context().getBean(ChannelPool.class);

            // the connection configuration changes, together with a class, so the application restarts
            StringBuilder changed = new StringBuilder();
            properties.forEach((key, value) -> changed.append(key).append('=').append(value).append('\n'));
            changed.append("rabbitmq.requested-heartbeat=17\n");
            harness.resource("application.properties", changed.toString());
            harness.source("example.Listener", LISTENER.formatted(QUEUE, "second"));
            harness.reload();
            assertEquals(2, harness.generation());

            Connection second = harness.context().getBean(Connection.class);
            assertNotSame(first, second);
            assertNotSame(firstPool, harness.context().getBean(ChannelPool.class));
            assertFalse(first.isOpen(), "the released connection is closed");
            assertEquals(17, second.getHeartbeat());
            first = null;
            firstPool = null;

            awaitTrue("the queue has the one consumer of the second generation", () -> consumers(channel) == 1);
            publish(channel, "two");
            awaitTrue("the second generation receives on its connection", () -> received(harness.context()).equals(List.of("second two")));
            ReloadTck.assertRetiredGenerationsCollected(harness);
        }
    }

    private static long millisSince(long start) {
        return Duration.ofNanos(System.nanoTime() - start).toMillis();
    }

    private static void publish(Channel channel, String value) throws Exception {
        channel.basicPublish("", QUEUE, null, value.getBytes(StandardCharsets.UTF_8));
    }

    private static long consumers(Channel channel) {
        try {
            return channel.consumerCount(QUEUE);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Tells the running generation that a class was redefined in place, as the development runtime does after it
     * redefined the class. The context is not kept: a reference to it would keep the generation reachable.
     */
    private static void changedInPlace(ReloadHarness harness, String className) {
        ApplicationContext context = harness.context();
        context.publishEvent(new ClassChangeEvent(RabbitMQReloadTest.class, Set.of(), context.getClassLoader(),
            List.of(new ClassChange(className, ClassChange.Kind.MODIFIED)), ReloadStrategy.RELOAD));
    }

    private static void awaitTrue(String what, BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Timed out waiting until " + what);
            }
            Thread.sleep(100);
        }
    }

    private static void assertReloaderPresent(ApplicationContext context) {
        // the bean that follows changes in place exists in development mode only
        assertTrue(context.containsBean(type(context, "io.micronaut.rabbitmq.intercept.DevelopmentRabbitMQReloader")));
    }

    private static Object listener(ApplicationContext context) {
        return context.getBean(type(context, "example.Listener"));
    }

    @SuppressWarnings("unchecked")
    private static List<String> received(ApplicationContext context) {
        try {
            return (List<String>) type(context, "example.Listener").getMethod("received").invoke(listener(context));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Cannot read what the listener received", e);
        }
    }

    private static Class<?> type(ApplicationContext context, String className) {
        try {
            return Class.forName(className, true, context.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new AssertionError(className + " is not in the application", e);
        }
    }
}
