package io.micronaut.rabbitmq.docs.exchange

import io.micronaut.serde.annotation.Serdeable

@Serdeable
class Cat : Animal {

    var lives: Int = 0

    constructor(name: String, lives: Int) : super(name) {
        this.lives = lives
    }

    constructor() {}
}
