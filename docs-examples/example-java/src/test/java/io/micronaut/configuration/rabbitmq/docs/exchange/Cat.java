package io.micronaut.configuration.rabbitmq.docs.exchange;

import com.fasterxml.jackson.annotation.JsonCreator;

public class Cat extends Animal {

    private int lives;

    public Cat(String name, int lives) {
        super(name);
        this.lives = lives;
    }

    public Cat() { }

    public int getLives() {
        return lives;
    }

    public void setLives(int lives) {
        this.lives = lives;
    }
}
