package ph.codeia.todo.details;

import ph.codeia.todo.Mvp;
import ph.codeia.todo.data.TodoRepository;

public class DetailsActions implements Details.Presenter {

    private final TodoRepository repo;
    private final int itemId;

    public DetailsActions(TodoRepository repo, int itemId) {
        this.repo = repo;
        this.itemId = itemId;
    }

    @Override
    public Details.Action load() {
        return (state, view) -> state.async(() -> {
            TodoRepository.Todo todo = repo.oneWithId(itemId);
            if (todo == null) {
                Mvp.Log.E.to(view, "todo not found: #%d", itemId);
                return Details.Action.BACK;
            } else {
                return (futureState, futureView) -> {
                    Item item = new Item(todo);
                    futureView.show(item);
                    return futureState.withItem(item);
                };
            }
        });
    }

    @Override
    public Details.Action toggleCompleted() {
        return (state, view) -> state.async(() -> {
            TodoRepository.Todo todo = repo.oneWithId(itemId);
            if (todo == null) {
                Mvp.Log.E.to(view, "todo not found: #%d", itemId);
                return Details.Action.BACK;
            } else {
                TodoRepository.Todo updated = todo
                        .withCompleted(!state.item.completed());
                repo.put(updated);
                Mvp.Log.D.to(view, "updated status : #%d -> %s", itemId, updated.completed);
                return (futureState, futureView) -> {
                    Item item = new Item(updated);
                    futureView.show(item);
                    return futureState.withItem(item);
                };
            }
        });
    }

    @Override
    public Details.Action delete() {
        return (state, view) -> {
            view.confirmDelete(confirmed());
            return state;
        };
    }

    @Override
    public Details.Action edit() {
        return (state, view) -> {
            view.goToEditor();
            return state;
        };
    }

    private Details.Action confirmed() {
        return (state, view) -> state.async(() -> {
            repo.delete(itemId);
            Mvp.Log.D.to(view, "deleted #%d", itemId);
            return Details.Action.BACK;
        });
    }
}
