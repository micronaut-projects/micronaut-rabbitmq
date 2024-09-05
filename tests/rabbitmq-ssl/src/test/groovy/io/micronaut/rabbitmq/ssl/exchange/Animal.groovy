package io.micronaut.rabbitmq.ssl.exchange

abstract class Animal {

    String name

    Animal(String name) {
        this.name = name
    }

    Animal() {}
}
