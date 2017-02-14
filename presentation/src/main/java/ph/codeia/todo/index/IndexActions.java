package ph.codeia.todo.index;

import java.util.ArrayList;
import java.util.List;

import ph.codeia.todo.Mvp;
import ph.codeia.todo.data.TodoRepository;

public class IndexActions implements Index.Presenter {

    private final TodoRepository repo;

    public IndexActions(TodoRepository repo) {
        this.repo = repo;
    }

    @Override
    public Index.Action refresh() {
        return (state, view) -> {
            List<Index.Item> visible = new ArrayList<>();
            for (Index.Item e : state.cache) {
                if (state.showCompletedItems && e.completed()) {
                    visible.add(e);
                }
                if (state.showActiveItems && !e.completed()) {
                    visible.add(e);
                }
            }
            view.show(visible);
            return state;
        };
    }

    @Override
    public Index.Action load() {
        return (state, view) -> {
            view.spin(true);
            return state.async(() -> {
                List<Index.Item> items = new ArrayList<>();
                for (TodoRepository.Todo e : repo.all()) {
                    items.add(new Item(e));
                }
                return (futureState, futureView) -> {
                    futureView.spin(false);
                    return futureState
                            .withCache(items)
                            .plus(refresh());
                };
            });
        };
    }

    @Override
    public Index.Action details(int id) {
        return (state, view) -> {
            view.goToDetails(id);
            return state;
        };
    }

    @Override
    public Index.Action add() {
        return (state, view) -> {
            view.goToEntryForm();
            return state;
        };
    }

    @Override
    public Index.Action toggle(int id, boolean value) {
        return (state, view) -> state.async(() -> {
            TodoRepository.Todo todo = repo.oneWithId(id);
            if (todo == null) {
                Mvp.Log.E.to(view, "no such id: %d", id);
                return Index.Action.NOOP;
            } else {
                TodoRepository.Todo e = todo.withCompleted(value);
                repo.put(e);
                Mvp.Log.D.to(view, "saved #%d: %s", e.id, e.title);
                return (futureState, futureView) -> {
                    List<Index.Item> items = futureState.cache;
                    int i = find(items, id);
                    if (i != -1) {
                        items.set(i, new Item(e));
                    }
                    return futureState.plus(refresh());
                };
            }
        });
    }

    @Override
    public Index.Action deleteCompleted() {
        return (state, view) -> {
            view.confirmDelete(onConfirm());
            return state;
        };
    }

    @Override
    public Index.Action filter(boolean showCompleted, boolean showActive) {
        return (state, view) -> state
                .withCompletedItemsShown(showCompleted)
                .withActiveItemsShown(showActive)
                .plus(refresh());
    }

    private Index.Action onConfirm() {
        return (state, view) -> state.async(() -> {
            int count = 0;
            try (TodoRepository.Transactional r = repo.transact()) {
                for (Index.Item e : state.cache) if (e.completed()) {
                    r.delete((int) e.id());
                    Mvp.Log.D.to(view, "deleted #%d: %s", e.id(), e.title());
                    count++;
                }
            }
            final int deleted = count;
            return (futureState, futureView) -> {
                futureView.tell("Deleted %d items.", deleted);
                return futureState.plus(load());
            };
        });
    }

    private static int find(List<Index.Item> xs, int id) {
        int i = -1;
        for (Index.Item x : xs) {
            i++;
            if (id == x.id()) {
                return i;
            }
        }
        return -1;
    }

}
