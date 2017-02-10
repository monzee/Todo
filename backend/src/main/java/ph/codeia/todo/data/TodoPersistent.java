package ph.codeia.todo.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

public class TodoPersistent implements TodoRepository.Transactional {
    private final File file;
    private TodoInMemory delegate;
    private boolean inTransaction;
    private boolean cancelled;

    public TodoPersistent(File file) throws IOException, ClassNotFoundException {
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
        if (!inTransaction) {
            uncheckedSave();
        }
        return item;
    }

    @Override
    public void put(Todo item) {
        delegate.put(item);
        if (!inTransaction) {
            uncheckedSave();
        }
    }

    @Override
    public void delete(int id) {
        delegate.delete(id);
        if (!inTransaction) {
            uncheckedSave();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public TodoPersistent transact() {
        inTransaction = true;
        return this;
    }

    @Override
    public void cancel() {
        if (inTransaction) {
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
        inTransaction = false;
        cancelled = false;
    }

    private void uncheckedSave() {
        try {
            save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized void load() throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            delegate = (TodoInMemory) in.readObject();
        }
    }

    private synchronized void save() throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(delegate);
        }
    }

}
