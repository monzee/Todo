package ph.codeia.todo.details;

import ph.codeia.todo.Mvp;

public interface Details {

    interface Presenter {
        Action load();
        Action refresh();
        Action toggleCompleted();
        Action delete();
        Action edit();
        Action back();
    }

    interface View extends Mvp.Debug {
        void show(Item item);
        void confirmDelete(Action doIt);
        void goToEditor();
        void goBack();
    }

    interface Item {
        String title();
        String description();
        boolean completed();
    }

    interface Action extends Mvp.Action<State, Action, View> {}

    class State extends Mvp.BaseState<State, Action> {
        public static final State ROOT = new State(null);
        public final Item item;

        public State(Item item) {
            this.item = item;
        }

        public State withItem(Item item) {
            return join(new State(item));
        }
    }

}
