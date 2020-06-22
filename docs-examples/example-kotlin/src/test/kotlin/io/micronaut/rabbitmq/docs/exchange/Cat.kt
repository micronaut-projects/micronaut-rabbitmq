package io.micronaut.rabbitmq.docs.exchange

class Cat : Animal {

    var lives: Int = 0

    constructor(name: String, lives: Int) : super(name) {
        this.lives = lives
    }

    constructor() {}
}
