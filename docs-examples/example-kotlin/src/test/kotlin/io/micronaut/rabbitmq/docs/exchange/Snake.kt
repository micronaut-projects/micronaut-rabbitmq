package io.micronaut.rabbitmq.docs.exchange

class Snake : Animal {

    var isVenomous: Boolean = false

    constructor(name: String, venomous: Boolean) : super(name) {
        this.isVenomous = venomous
    }

    constructor() {}
}
