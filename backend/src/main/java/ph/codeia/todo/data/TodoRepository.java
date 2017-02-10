package ph.codeia.todo.data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public interface TodoRepository {
    Todo oneWithId(int id);
    List<Todo> all();
    Todo add(String title, String description, boolean completed);
    void put(Todo item);
    void delete(int id);
    <T extends Transactional> T transact();

    interface Transactional extends TodoRepository, AutoCloseable {
        void cancel();
    }

    class Todo implements Serializable {
        private static final long serialVersionUID = 1L;

        public final int id;
        public final String title;
        public final String description;
        public final boolean completed;
        public final Date created;

        public Todo(int id, String title, String description, boolean completed, Date created) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.completed = completed;
            this.created = created;
        }

        public Todo withTitle(String title) {
            if (title.equals(this.title)) {
                return this;
            }
            return new Todo(id, title, description, completed, created);
        }

        public Todo withDescription(String description) {
            if (description.equals(this.description)) {
                return this;
            }
            return new Todo(id, title, description, completed, created);
        }

        public Todo withCompleted(boolean completed) {
            if (completed == this.completed) {
                return this;
            }
            return new Todo(id, title, description, completed, created);
        }

        public Todo withCreated(Date created) {
            if (created.compareTo(this.created) == 0) {
                return this;
            }
            return new Todo(id, title, description, completed, created);
        }

    }
}
