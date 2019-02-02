package io.micronaut.configuration.rabbitmq.docs.exchange;

import com.fasterxml.jackson.annotation.JsonCreator;

public class Snake extends Animal {

    private boolean venomous;

    public Snake(String name, boolean venomous) {
        super(name);
        this.venomous = venomous;
    }

    public Snake() { }

    public boolean isVenomous() {
        return venomous;
    }

    public void setVenomous(boolean venomous) {
        this.venomous = venomous;
    }
}
