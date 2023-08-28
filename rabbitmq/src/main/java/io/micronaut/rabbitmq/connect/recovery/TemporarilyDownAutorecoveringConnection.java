/*
 * Copyright 2017-2023 original authors
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
package io.micronaut.rabbitmq.connect.recovery;

import com.rabbitmq.client.*;
import com.rabbitmq.client.impl.ConnectionParams;
import com.rabbitmq.client.impl.FrameHandlerFactory;
import com.rabbitmq.client.impl.SocketFrameHandlerFactory;
import com.rabbitmq.client.impl.nio.NioParams;
import com.rabbitmq.client.impl.nio.SocketChannelFrameHandlerFactory;
import com.rabbitmq.client.impl.recovery.AutorecoveringConnection;
import io.micronaut.rabbitmq.connect.RabbitConnectionFactoryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static java.util.function.Predicate.not;
import static java.util.Collections.synchronizedList;

/**
 * Default implementation of a {@link TemporarilyDownConnection}.
 *
 * @author Guillermo Calvo
 * @since 4.1.0
 */
class TemporarilyDownAutorecoveringConnection extends AutorecoveringConnection implements TemporarilyDownConnection {

    private static final Logger LOG = LoggerFactory.getLogger(TemporarilyDownAutorecoveringConnection.class);

    private final Function<Throwable, RuntimeException> sneaky = x -> new TemporarilyDownRuntimeException(this, x);
    private final Function<Throwable, IOException> io = x -> new TemporarilyDownIOException(this, x);
    private final AtomicBoolean initialized = new AtomicBoolean();
    private final List<EventuallyUpListener> eventuallyUpListeners = synchronizedList(new ArrayList<>());

    TemporarilyDownAutorecoveringConnection(RabbitConnectionFactoryConfig factory, ExecutorService executor) {
        super(
            factory.params(executor),
            getFrameHandlerFactory(factory, executor),
            getAddressResolver(factory),
            getMetricsCollector(factory)
        );
    }

    private static FrameHandlerFactory getFrameHandlerFactory(RabbitConnectionFactoryConfig factory, ExecutorService executor) {
        final int connectionTimeout = factory.getConnectionTimeout();
        final NioParams nioParams = factory.getNioParams();
        final ConnectionParams params = factory.params(executor);
        final int maxInboundMessageBodySize = params.getMaxInboundMessageBodySize();
        if (nioParams.getNioExecutor() != null || nioParams.getThreadFactory() != null) {
            return new SocketChannelFrameHandlerFactory(
                connectionTimeout,
                nioParams,
                factory.isSSL(),
                factory.getSocketFactory() instanceof SslContextFactory ssl ? ssl : null,
                maxInboundMessageBodySize
            );
        }
        return new SocketFrameHandlerFactory(
            connectionTimeout,
            factory.getSocketFactory(),
            factory.getSocketConfigurator(),
            factory.isSSL(),
            params.getShutdownExecutor(),
            factory.getSocketFactory() instanceof SslContextFactory ssl ? ssl : null,
            maxInboundMessageBodySize
        );
    }

    private static AddressResolver getAddressResolver(RabbitConnectionFactoryConfig factory) {
        final List<Address> addressList = factory.getAddresses()
            .filter(not(List::isEmpty))
            .orElseGet(() -> List.of(new Address(factory.getHost(), factory.getPort())));
        if (addressList.size() > 1) {
            return new ListAddressResolver(addressList);
        }
        return new DnsRecordIpAddressResolver(addressList.get(0), factory.isSSL());
    }

    private static MetricsCollector getMetricsCollector(RabbitConnectionFactoryConfig factory) {
        return Optional.ofNullable(factory.getMetricsCollector())
            .orElseGet(NoOpMetricsCollector::new);
    }

    @Override
    public InetAddress getAddress() {
        checkInitialized(sneaky);
        return super.getAddress();
    }

    @Override
    public int getPort() {
        checkInitialized(sneaky);
        return super.getPort();
    }

    @Override
    public int getChannelMax() {
        checkInitialized(sneaky);
        return super.getChannelMax();
    }

    @Override
    public int getFrameMax() {
        checkInitialized(sneaky);
        return super.getFrameMax();
    }

    @Override
    public int getHeartbeat() {
        checkInitialized(sneaky);
        return super.getHeartbeat();
    }

    @Override
    public Map<String, Object> getClientProperties() {
        checkInitialized(sneaky);
        return super.getClientProperties();
    }

    @Override
    public String getClientProvidedName() {
        checkInitialized(sneaky);
        return super.getClientProvidedName();
    }

    @Override
    public Map<String, Object> getServerProperties() {
        checkInitialized(sneaky);
        return super.getServerProperties();
    }

    @Override
    public Channel createChannel() throws IOException {
        checkInitialized(io);
        return super.createChannel();
    }

    @Override
    public Channel createChannel(int channelNumber) throws IOException {
        checkInitialized(io);
        return super.createChannel(channelNumber);
    }

    @Override
    public void close() throws IOException {
        checkInitialized(io);
        super.close();
    }

    @Override
    public void close(int closeCode, String closeMessage) throws IOException {
        checkInitialized(io);
        super.close(closeCode, closeMessage);
    }

    @Override
    public void close(int timeout) throws IOException {
        checkInitialized(io);
        super.close(timeout);
    }

    @Override
    public void close(int closeCode, String closeMessage, int timeout) throws IOException {
        checkInitialized(io);
        super.close(closeCode, closeMessage, timeout);
    }

    @Override
    public void abort() {
        if (isStillDown()) {
            LOG.warn("Ignoring connection abort because the connection is still down");
        } else {
            super.abort();
        }
    }

    @Override
    public void abort(int closeCode, String closeMessage) {
        if (isStillDown()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Ignoring connection abort({}, {}) because the connection is still down", closeCode, closeMessage);
            }
        } else {
            super.abort(closeCode, closeMessage);
        }
    }

    @Override
    public void abort(int timeout) {
        if (isStillDown()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Ignoring connection abort({}) because the connection is still down", timeout);
            }
        } else {
            super.abort(timeout);
        }
    }

    @Override
    public void abort(int closeCode, String closeMessage, int timeout) {
        if (isStillDown()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Ignoring connection abort({}, {}, {}) because the connection is still down", closeCode, closeMessage, timeout);
            }
        } else {
            super.abort(closeCode, closeMessage, timeout);
        }
    }

    @Override
    public void addBlockedListener(BlockedListener listener) {
        checkInitialized(sneaky);
        super.addBlockedListener(listener);
    }

    @Override
    public BlockedListener addBlockedListener(BlockedCallback blockedCallback, UnblockedCallback unblockedCallback) {
        checkInitialized(sneaky);
        return super.addBlockedListener(blockedCallback, unblockedCallback);
    }

    @Override
    public boolean removeBlockedListener(BlockedListener listener) {
        checkInitialized(sneaky);
        return super.removeBlockedListener(listener);
    }

    @Override
    public void clearBlockedListeners() {
        checkInitialized(sneaky);
        super.clearBlockedListeners();
    }

    @Override
    public ExceptionHandler getExceptionHandler() {
        checkInitialized(sneaky);
        return super.getExceptionHandler();
    }

    @Override
    public String getId() {
        checkInitialized(sneaky);
        return super.getId();
    }

    @Override
    public void setId(String id) {
        checkInitialized(sneaky);
        super.setId(id);
    }

    @Override
    public void addShutdownListener(ShutdownListener listener) {
        checkInitialized(sneaky);
        super.addShutdownListener(listener);
    }

    @Override
    public void removeShutdownListener(ShutdownListener listener) {
        checkInitialized(sneaky);
        super.removeShutdownListener(listener);
    }

    @Override
    public ShutdownSignalException getCloseReason() {
        checkInitialized(sneaky);
        return super.getCloseReason();
    }

    @Override
    public void notifyListeners() {
        checkInitialized(sneaky);
        super.notifyListeners();
    }

    @Override
    public boolean isOpen() {
        checkInitialized(sneaky);
        return super.isOpen();
    }

    @Override
    public boolean isStillDown() {
        return !initialized.get();
    }

    @Override
    public boolean check() {
        return checkInitialized(x -> null); // Ignore any errors
    }

    @Override
    public void addEventuallyUpListener(EventuallyUpListener listener) {
        this.eventuallyUpListeners.add(listener);
    }

    private <E extends Throwable> boolean checkInitialized(Function<Throwable, E> mapper) throws E {
        if (isStillDown()) {
            try {
                return initialize();
            } catch (Exception e) {
                E exception = mapper.apply(e);
                if (exception != null) {
                    throw mapper.apply(e);
                }
            }
        }
        return initialized.get();
    }

    private boolean initialize() throws IOException, TimeoutException {
        synchronized (initialized) {
            try {
                // Try to initialize the connection unless it's already up
                if (initialized.compareAndSet(false, true)) {
                    super.init();
                    LOG.info("RabbitMQ connection is up now");
                    notifyInitialized();
                    return true;
                }
            } catch (Exception e) {
                // We will try to initialize the connection again
                initialized.set(false);
                throw e;
            }
        }
        return initialized.get();
    }

    private void notifyInitialized() {
        for (EventuallyUpListener listener : eventuallyUpListeners) {
            try {
                listener.onConnectionInitialized(this);
            } catch (Exception e) {
                LOG.error("Callback threw an exception", e);
            }
        }
    }
}
