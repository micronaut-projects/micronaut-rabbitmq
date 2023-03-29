package io.micronaut.rabbitmq.docs.exchange

import io.micronaut.serde.annotation.Serdeable

@Serdeable
class Snake(name: String? = null, venomous: Boolean = false) : Animal(name)
