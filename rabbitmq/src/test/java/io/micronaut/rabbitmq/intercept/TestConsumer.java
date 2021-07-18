package io.micronaut.rabbitmq.intercept;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import io.micronaut.messaging.annotation.MessageBody;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.exception.RabbitListenerException;
import io.micronaut.rabbitmq.exception.RabbitListenerExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TestConsumer implements RabbitListenerExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(TestConsumer.class);
    public final Set<String> consumedMessages = new LinkedHashSet<>();
    public RabbitListenerException lastException;

    @Queue(EnvironmentLoader.QUEUE)
    public void handleMessage(@MessageBody String body) {
        consumedMessages.add(body);
        log.info("{} received: {}", consumedMessages.size(), body);
    }

    @Override
    public void handle(RabbitListenerException e) {
        lastException = e;
        String msg = e.getMessageState()
                .map(state -> state.getBody())
                .map(String::new)
                .orElse("<<no message>>");
        consumedMessages.remove(msg);
        log.warn("{} failed to consume: {}", consumedMessages.size(), msg, e);
    }
}
