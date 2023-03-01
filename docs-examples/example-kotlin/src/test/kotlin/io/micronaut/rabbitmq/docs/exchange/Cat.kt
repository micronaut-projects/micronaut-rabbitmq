package io.micronaut.rabbitmq.docs.exchange

import io.micronaut.serde.annotation.Serdeable

@Serdeable
class Cat(name: String? = null, lives: Int = 0) : Animal(name)
