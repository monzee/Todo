package ph.codeia.todo;

import java.util.ArrayList;
import java.util.List;

public interface Repeating {

    interface Diffable<T extends Diffable<T>> {
        long id();
        boolean sameAs(T other);
    }

    interface RowView<T extends Diffable<T>> {
        void bind(T item);
    }

    interface Action<
            S extends State<S, A, ?>,
            A extends Action<S, A, V>,
            V>
            extends Mvp.Action<S, A, V> {}

    class State<
            S extends State<S, A, T>,
            A extends Action<S, A, ?>,
            T extends Diffable<T>>
            extends Mvp.BaseState<S, A> {

        protected List<T> items = new ArrayList<>();

        @SuppressWarnings("unchecked")
        public S setItems(List<T> items) {
            this.items = items;
            return (S) this;
        }
    }
}
