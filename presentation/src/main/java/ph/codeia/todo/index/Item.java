package ph.codeia.todo.index;

import java.text.DateFormat;
import java.util.Date;

import ph.codeia.todo.data.TodoRepository;

class Item implements Index.Item {

    private final TodoRepository.Todo todo;

    Item(TodoRepository.Todo todo) {
        this.todo = todo;
    }

    @Override
    public long id() {
        return todo.id;
    }

    @Override
    public boolean sameAs(Index.Item other) {
        return completed() == other.completed() && title().equals(other.title());
    }

    @Override
    public String title() {
        return todo.title;
    }

    @Override
    public boolean completed() {
        return todo.completed;
    }

    @Override
    public Date created() {
        return todo.created;
    }

    @Override
    public String toString() {
        char mark = completed() ? 'x' : ' ';
        return "#" + id() + ": [" + mark + "] " + title()
                + " @ " + DateFormat.getDateInstance(DateFormat.FULL).format(created());
    }

}
