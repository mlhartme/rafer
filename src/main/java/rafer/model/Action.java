package rafer.model;

import java.io.IOException;

public abstract class Action implements Comparable<Action> {
    private final String message;

    public Action(String message) {
        this.message = message;
    }

    public abstract void invoke() throws IOException;

    public String toString() {
        return message;
    }

    public int compareTo(Action action) {
        return toString().compareTo(action.toString());
    }
}
