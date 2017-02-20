package ph.codeia.todo;

import android.annotation.SuppressLint;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ph.codeia.todo.data.TodoInMemory;
import ph.codeia.todo.data.TodoRepository;
import ph.codeia.todo.data.TodoSerialized;
import ph.codeia.todo.databinding.ActivityMainBinding;
import ph.codeia.todo.index.Index;
import ph.codeia.todo.index.IndexActions;
import ph.codeia.todo.index.IndexScreen;
import ph.codeia.todo.index.TodoAdapter;
import ph.codeia.todo.util.AndroidUnit;

public class MainActivity extends AppCompatActivity {

    private static final Random RANDOM = new Random();
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    private static final Index.State ROOT = new Index.State(true, true, false, Collections.emptyList());

    private static class States {
        Index.State screen;
        TodoAdapter.State visible;
    }

    private TodoRepository repo;
    private Index.Presenter presenter;
    private IndexScreen view;
    private ActivityMainBinding widgets;
    private AndroidUnit<Index.State, Index.Action, Index.View> screen;
    private AndroidUnit<TodoAdapter.State, TodoAdapter.Action, RecyclerView> visible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        widgets = DataBindingUtil.setContentView(this, R.layout.activity_main);

        try {
            repo = new TodoSerialized(new File(getCacheDir(), "todos"));
        } catch (IOException | ClassNotFoundException e) {
            repo = new TodoInMemory();
        }
        presenter = new IndexActions(repo);
        TodoAdapter todoAdapter = new TodoAdapter(new TodoAdapter.Controller() {
            @Override
            public void checked(int id, boolean on) {
                apply(presenter.toggle(id, on));
            }

            @Override
            public void selected(int id) {
                apply(presenter.details(id));
            }
        });
        view = new IndexScreen(this, widgets, todoAdapter, new IndexScreen.Compromise() {
            @Override
            public void apply(Index.Action action) {
                MainActivity.this.apply(action);
            }

            @Override
            public void applyList(TodoAdapter.Action action) {
                MainActivity.this.applyList(action);
            }
        });

        widgets.doEnterNew.setOnClickListener(_v -> apply(presenter.add()));
        widgets.todoContainer.setLayoutManager(new LinearLayoutManager(this));
        widgets.todoContainer.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        States saved = (States) getLastCustomNonConfigurationInstance();
        if (saved == null) {
            screen = new AndroidUnit<>(ROOT);
            visible = new AndroidUnit<>(new TodoAdapter.State());
            applyList(todoAdapter.init());
            apply(presenter.load());
        } else {
            screen = new AndroidUnit<>(saved.screen);
            visible = new AndroidUnit<>(saved.visible);
            applyList(todoAdapter.init());
            apply(presenter.refresh());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        visible.start(widgets.todoContainer);
        screen.start(view);
        apply(Index.Action.FLUSH);
    }

    @Override
    protected void onStop() {
        super.onStop();
        screen.stop();
        visible.stop();
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        States s = new States();
        s.screen = screen.state();
        s.visible = visible.state();
        return s;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.index, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Index.State state = screen.state();
        if (state.showCompletedItems && state.showActiveItems) {
            menu.findItem(R.id.both).setChecked(true);
        } else if (state.showCompletedItems) {
            menu.findItem(R.id.completed_only).setChecked(true);
        } else if (state.showActiveItems) {
            menu.findItem(R.id.active_only).setChecked(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.populate:
                apply(populate(repo));
                break;
            case R.id.delete:
                apply(presenter.deleteCompleted());
                break;
            case R.id.completed_only:
                item.setChecked(true);
                apply(presenter.filter(true, false));
                break;
            case R.id.active_only:
                item.setChecked(true);
                apply(presenter.filter(false, true));
                break;
            case R.id.both:
                item.setChecked(true);
                apply(presenter.filter(true, true));
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    void apply(Index.Action action) {
        screen.apply(action, view, WORKER);
    }

    void applyList(TodoAdapter.Action action) {
        visible.apply(action, widgets.todoContainer, WORKER);
    }

    @SuppressLint("NewApi")  // retrolambda can convert the try-let
    private Index.Action populate(TodoRepository repo) {
        return (state, view) -> {
            view.spin(true);
            return state.async(() -> {
                Thread.sleep(3000);
                try (TodoRepository.Transactional t = repo.transact()) {
                    t.add("quick jumps", "The quick brown fox jumps over the lazy dog.", RANDOM.nextBoolean());
                    t.add("learn latin", "Lorem ipsum dolor sit amet", RANDOM.nextBoolean());
                    t.add("something good", "foo bar baz bat quux", RANDOM.nextBoolean());
                }
                return (futureState, futureView) -> {
                    futureView.spin(false);
                    return futureState.plus(presenter.load());
                };
            });
        };
    }

}
