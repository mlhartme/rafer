package rafer.model;

import net.oneandone.inline.Console;

import java.io.IOException;
import java.util.Collection;
import java.util.TreeSet;

public class Patch {
    private final Collection<Action> actions;

    public Patch() {
        this.actions = new TreeSet<>();
    }

    public void add(Action action) {
        actions.add(action);
    }

    public void apply() throws IOException {
        for (Action action : actions) {
            action.invoke();
        }
    }

    public int applyAndReport(Console console) {
        int errors;
        int i;
        int max;

        errors = 0;
        i = 1;
        max = actions.size();
        for (Action action : actions) {
            try {
                console.info.println(action.toString() + " (" + i + "/" + max + ")" );
                action.invoke();
            } catch (IOException e) {
                console.error.println("error: " + action.toString() + ": " + e.getMessage());
                e.printStackTrace(console.verbose);
                errors++;
            }
            i++;
        }
        return errors;
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
