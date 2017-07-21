package rafer.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Update {
    private final List<Action> actions;

    public Update() {
        this.actions = new ArrayList<>();
    }

    public void add(Action action) {
        actions.add(action);
    }

    public void invoke() throws IOException {
        for (Action action : actions) {
            action.invoke();
        }
    }

    public int size() {
        return actions.size();
    }

    public boolean isEmpty() {
        return actions.isEmpty();
    }

    public String toString() {
        StringBuilder result;

        if (isEmpty()) {
            return "(no actions)";
        } else {
            result = new StringBuilder();
            for (Action action : actions) {
                result.append(action.toString());
                result.append('\n');
            }
            return result.toString();
        }
    }
}
