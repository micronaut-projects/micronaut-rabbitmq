package io.micronaut.rabbitmq.docs.exchange;

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
