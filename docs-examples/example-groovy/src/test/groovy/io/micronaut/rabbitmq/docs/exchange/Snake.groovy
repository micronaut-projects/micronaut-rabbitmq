package io.micronaut.rabbitmq.docs.exchange;

class Snake extends Animal {

    boolean venomous

    Snake(String name, boolean venomous) {
        super(name)
        this.venomous = venomous
    }

    Snake() {}
}
