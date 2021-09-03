package io.micronaut.rabbitmq.docs.exchange

class Cat extends Animal {

    int lives

    Cat(String name, int lives) {
        super(name)
        this.lives = lives
    }

    Cat() {}
}
