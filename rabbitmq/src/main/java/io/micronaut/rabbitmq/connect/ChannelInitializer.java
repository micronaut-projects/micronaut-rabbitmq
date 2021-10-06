/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.rabbitmq.connect;

import com.rabbitmq.client.Channel;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * A base class to extend from to do initialization work with
 * a channel before any consumers or producers are created.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
public abstract class ChannelInitializer implements BeanCreatedEventListener<ChannelPool> {
    private static final Logger LOG = LoggerFactory.getLogger(ChannelInitializer.class);

    /**
     * Do any work with a channel.
     *
     * @param channel The channel to use
     * @param name - The name of the related connection, like configured under rabbitmq.servers
     * @throws IOException If any error occurs
     */
    public abstract void initialize(Channel channel, String name) throws IOException;

    @Override
    public ChannelPool onCreated(BeanCreatedEvent<ChannelPool> event) {
        ChannelPool pool = event.getBean();
        Channel channel = null;
        try {
            channel = pool.getChannel();
            initialize(channel, pool.getName());
        } catch (Throwable e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Initialization of the channel has failed due to error", e);
            }
        } finally {
            if (channel != null) {
                pool.returnChannel(channel);
            }
        }
        return pool;
    }
}
