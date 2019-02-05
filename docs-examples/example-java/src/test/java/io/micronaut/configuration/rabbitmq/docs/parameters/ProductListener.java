package io.micronaut.configuration.rabbitmq.docs.parameters;

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.Queue;
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener;
import io.micronaut.context.annotation.Requires;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
// end::imports[]

@Requires(property = "spec.name", value = "BindingSpec")
// tag::clazz[]
@RabbitListener
public class ProductListener {

    List<Integer> messageLengths = Collections.synchronizedList(new ArrayList<>());

    @Queue("product") // <1>
    public void receive(byte[] data) {
        messageLengths.add(data.length);
    }
}
// end::clazz[]