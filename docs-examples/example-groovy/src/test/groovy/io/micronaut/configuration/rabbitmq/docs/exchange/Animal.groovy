package io.micronaut.configuration.rabbitmq.docs.exchange;

abstract class Animal {

    String name

    Animal(String name) {
        this.name = name
    }

    Animal() {}

}
