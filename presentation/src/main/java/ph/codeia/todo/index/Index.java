package ph.codeia.todo.index;

import java.util.Date;
import java.util.List;

import ph.codeia.todo.Mvp;
import ph.codeia.todo.Repeating;

public interface Index {

    interface Presenter {
        Action refresh();
        Action load();
        Action details(int id);
        Action add();
        Action deleteCompleted();
        Action toggle(int id, boolean complete);
        Action filter(boolean showCompleted, boolean showActive);
    }

    interface View extends Mvp.Debug {
        void tell(String message, Object... fmtArgs);
        void show(List<Item> items);
        void spin(boolean busy);
        void confirmDelete(Action onConfirm);
        void goToDetails(int id);
        void goToEntryForm();
    }

    interface Item extends Repeating.Diffable<Item> {
        String title();
        /**
         * @return the item's completion status. Not completed is the same as
         * "active" or "activated" in the specs.
         */
        boolean completed();
        Date created();
    }

    interface Action extends Mvp.Action<State, Action, View> {
        Action NOOP = (state, _view) -> state;
    }

    class State extends Mvp.BaseState<State, Action> {
        public final boolean showCompletedItems;
        public final boolean showActiveItems;
        public final List<Item> cache;

        public State(boolean showCompletedItems, boolean showActiveItems, List<Item> cache) {
            this.showCompletedItems = showCompletedItems;
            this.showActiveItems = showActiveItems;
            this.cache = cache;
        }

        public State withCache(List<Index.Item> cache) {
            return join(new State(showCompletedItems, showActiveItems, cache));
        }

        public State withCompletedItemsShown(boolean show) {
            if (showCompletedItems == show) {
                return this;
            }
            return join(new State(show, showActiveItems, cache));
        }

        public State withActiveItemsShown(boolean show) {
            if (showActiveItems == show) {
                return this;
            }
            return join(new State(showCompletedItems, show, cache));
        }
    }

}
