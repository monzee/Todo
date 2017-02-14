package ph.codeia.todo.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

public class TodoSerialized implements TodoRepository.Transactional {
    private final File file;
    private final ThreadLocal<Boolean> inTransaction = new ThreadLocal<>();
    private TodoInMemory delegate;
    private boolean cancelled;

    public TodoSerialized(File file) throws IOException, ClassNotFoundException {
        this.file = file;
        if (file.createNewFile()) {
            delegate = new TodoInMemory();
            save();
        } else {
            load();
        }
    }

    @Override
    public Todo oneWithId(int id) {
        return delegate.oneWithId(id);
    }

    @Override
    public List<Todo> all() {
        return delegate.all();
    }

    @Override
    public Todo add(String title, String description, boolean completed) {
        Todo item = delegate.add(title, description, completed);
        if (!inTransaction()) {
            uncheckedSave();
        }
        return item;
    }

    @Override
    public void put(Todo item) {
        delegate.put(item);
        if (!inTransaction()) {
            uncheckedSave();
        }
    }

    @Override
    public void delete(int id) {
        delegate.delete(id);
        if (!inTransaction()) {
            uncheckedSave();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public TodoSerialized transact() {
        if (inTransaction()) {
            throw new IllegalStateException("nested transactions not supported");
        }
        inTransaction.set(true);
        return this;
    }

    @Override
    public void cancel() {
        if (inTransaction()) {
            cancelled = true;
        }
    }

    @Override
    public void close() throws IOException, ClassNotFoundException {
        if (cancelled) {
            load();
        } else {
            save();
        }
        inTransaction.set(false);
        cancelled = false;
    }

    private boolean inTransaction() {
        Boolean value = inTransaction.get();
        return value != null && value;
    }

    private void uncheckedSave() {
        try {
            save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void load() throws IOException, ClassNotFoundException {
        synchronized (file) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
                delegate = (TodoInMemory) in.readObject();
            }
        }
    }

    private void save() throws IOException {
        synchronized (file) {
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
                out.writeObject(delegate);
            }
        }
    }

}
