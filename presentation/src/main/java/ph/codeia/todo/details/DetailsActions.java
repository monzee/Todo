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
                return back();
            } else {
                return (futureState, futureView) -> futureState
                        .withItem(new Item(todo))
                        .plus(refresh());
            }
        });
    }

    @Override
    public Details.Action refresh() {
        return (state, view) -> {
            view.show(state.item);
            return state;
        };
    }

    @Override
    public Details.Action toggleCompleted() {
        return (state, view) -> state.async(() -> {
            TodoRepository.Todo todo = repo.oneWithId(itemId);
            if (todo == null) {
                Mvp.Log.E.to(view, "todo not found: #%d", itemId);
                return back();
            } else {
                TodoRepository.Todo updated = todo
                        .withCompleted(!state.item.completed());
                repo.put(updated);
                Mvp.Log.D.to(view, "updated status : #%d -> %s", itemId, updated.completed);
                return (futureState, futureView) -> futureState
                        .withItem(new Item(updated))
                        .plus(refresh());
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

    @Override
    public Details.Action back() {
        return (state, view) -> {
            view.goBack();
            return state;
        };
    }

    private Details.Action confirmed() {
        return (state, view) -> state.async(() -> {
            repo.delete(itemId);
            Mvp.Log.D.to(view, "deleted #%d", itemId);
            return back();
        });
    }
}
