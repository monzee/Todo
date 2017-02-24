package ph.codeia.todo.details;

import ph.codeia.todo.data.TodoRepository;

class Item implements Details.Item {
    private final TodoRepository.Todo item;

    Item(TodoRepository.Todo item) {
        this.item = item;
    }

    @Override
    public String title() {
        return item.title;
    }

    @Override
    public String description() {
        return item.description;
    }

    @Override
    public boolean completed() {
        return item.completed;
    }
}
