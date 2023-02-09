package io.micronaut.rabbitmq.docs.exchange

import io.micronaut.serde.annotation.Serdeable

@Serdeable
class Cat extends Animal {

    int lives

    Cat(String name, int lives) {
        super(name)
        this.lives = lives
    }

    Cat() {}
}
