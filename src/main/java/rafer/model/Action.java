package rafer.model;

import java.io.IOException;

public abstract class Action {
    private final String message;

    public Action(String message) {
        this.message = message;
    }

    public abstract void invoke() throws IOException;

    public String toString() {
        return message;
    }
}
