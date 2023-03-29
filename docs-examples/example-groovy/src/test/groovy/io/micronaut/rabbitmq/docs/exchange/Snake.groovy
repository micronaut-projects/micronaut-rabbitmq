package io.micronaut.rabbitmq.docs.exchange

import io.micronaut.serde.annotation.Serdeable

@Serdeable
class Snake extends Animal {

    boolean venomous

    Snake(String name, boolean venomous) {
        super(name)
        this.venomous = venomous
    }

    Snake() {}
}
