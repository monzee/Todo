package ph.codeia.todo.index;

import android.content.Context;
import android.support.v7.widget.RecyclerView;

import java.util.concurrent.Executor;

import ph.codeia.todo.Mvp;
import ph.codeia.todo.Todo;
import ph.codeia.todo.data.TodoRepository;
import ph.codeia.todo.util.AndroidUnit;

public interface IndexComponent {
    TodoRepository repository();
    Index.Presenter presenter();
    Mvp.Unit<Index.State, Index.Action, Index.View> screen(Index.State state);
    Mvp.Unit<TodoAdapter.State, TodoAdapter.Action, RecyclerView> visible(TodoAdapter.State state);
    Executor screenWorker();
    Executor visibleWorker();

    class Production implements IndexComponent {
        private final Todo.Component globals;
        private final Context context;

        public Production(Todo.Component globals, Context context) {
            this.globals = globals;
            this.context = context;
        }

        @Override
        public TodoRepository repository() {
            return globals.todoRepository(context);
        }

        @Override
        public Index.Presenter presenter() {
            return new IndexActions(repository());
        }

        @Override
        public Mvp.Unit<Index.State, Index.Action, Index.View>
        screen(Index.State state) {
            return new AndroidUnit<>(state);
        }

        @Override
        public Mvp.Unit<TodoAdapter.State, TodoAdapter.Action, RecyclerView>
        visible(TodoAdapter.State state) {
            return new AndroidUnit<>(state);
        }

        @Override
        public Executor screenWorker() {
            return globals.io();
        }

        @Override
        public Executor visibleWorker() {
            return globals.compute();
        }
    }
}
