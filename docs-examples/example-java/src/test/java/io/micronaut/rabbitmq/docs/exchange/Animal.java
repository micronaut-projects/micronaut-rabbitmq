package io.micronaut.rabbitmq.docs.exchange;

public abstract class Animal {

    private String name;

    public Animal(String name) {
        this.name = name;
    }

    public Animal() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
