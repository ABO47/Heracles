package earth.terrarium.heracles.client.handlers;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

public class UndoRedoManager {
    private static final UndoRedoManager INSTANCE = new UndoRedoManager();

    private final Deque<Entry> undo = new ArrayDeque<>();
    private final Deque<Entry> redo = new ArrayDeque<>();

    public static UndoRedoManager getInstance() {
        return INSTANCE;
    }

    public synchronized void execute(Runnable apply, Runnable revert) {
        Objects.requireNonNull(apply);
        Objects.requireNonNull(revert);
        apply.run();
        undo.push(new Entry(apply, revert));
        redo.clear();
    }

    public synchronized void undo() {
        if (undo.isEmpty()) return;
        Entry e = undo.pop();
        e.revert.run();
        redo.push(e);
    }

    public synchronized void redo() {
        if (redo.isEmpty()) return;
        Entry e = redo.pop();
        e.apply.run();
        undo.push(e);
    }

    private static class Entry {
        final Runnable apply;
        final Runnable revert;

        Entry(Runnable apply, Runnable revert) {
            this.apply = apply;
            this.revert = revert;
        }
    }
}
