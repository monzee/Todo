package ph.codeia.todo;

import android.annotation.SuppressLint;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ph.codeia.todo.index.Index;
import ph.codeia.todo.index.IndexActions;
import ph.codeia.todo.data.TodoInMemory;
import ph.codeia.todo.data.TodoPersistent;
import ph.codeia.todo.data.TodoRepository;
import ph.codeia.todo.databinding.ActivityMainBinding;
import ph.codeia.todo.util.AndroidUnit;

public class MainActivity extends AppCompatActivity implements IndexScreen.Compromise {

    private static final Random RANDOM = new Random();
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    private static final Index.State ROOT = new Index.State(true, true, Collections.emptyList());

    private static class States {
        Index.State screen;
        List<Index.Item> visible;
    }

    private TodoRepository repo;
    private Index.Presenter presenter;
    private IndexScreen view;
    private AndroidUnit<Index.State, Index.Action, Index.View> screen;
    private AndroidUnit<IndexScreen.VisibleItems, IndexScreen.ListAction, IndexScreen> recycler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding widgets = DataBindingUtil.setContentView(this, R.layout.activity_main);
        view = new IndexScreen(this, widgets, this);
        try {
            repo = new TodoPersistent(new File(getCacheDir(), "todos"));
        } catch (IOException | ClassNotFoundException e) {
            view.tell(e.getMessage());
            repo = new TodoInMemory();
            populate(repo);
        }
        presenter = new IndexActions(repo);
        widgets.todoContainer.setLayoutManager(new LinearLayoutManager(this));
        widgets.todoContainer.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        widgets.todoContainer.setAdapter(view.visible.adapter);
        widgets.doEnterNew.setOnClickListener(_v -> apply(presenter.add()));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (screen != null) {
            screen.start();
            recycler.start();
            return;
        }
        recycler = new AndroidUnit<>(this, view.visible);
        States saved = (States) getLastCustomNonConfigurationInstance();
        if (saved == null) {
            screen = new AndroidUnit<>(this, ROOT);
            apply(presenter.load());
        } else {
            screen = new AndroidUnit<>(this, saved.screen);
            view.visible.setItems(saved.visible);
            apply(presenter.refresh());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        screen.stop();
        recycler.stop();
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        States s = new States();
        s.screen = screen.state();
        s.visible = recycler.state().items;
        return s;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.browse, menu);
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
                populate(repo);
                apply(presenter.load());
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

    @Override
    public void checked(int id, boolean on) {
        apply(presenter.toggle(id, on));
    }

    @Override
    public void selected(int id) {
        apply(presenter.details(id));
    }

    @Override
    public void apply(IndexScreen.ListAction action) {
        recycler.apply(action, view, WORKER);
    }

    @Override
    public void apply(Index.Action action) {
        screen.apply(action, view, WORKER);
    }

    @SuppressLint("NewApi")
    private void populate(TodoRepository repo) {
        apply((Index.Action) (state, view) -> state.async(() -> {
            try (TodoRepository.Transactional t = repo.transact()) {
                t.add("quick jumps", "The quick brown fox jumps over the lazy dog.", RANDOM.nextBoolean());
                t.add("learn latin", "Lorem ipsum dolor sit amet", RANDOM.nextBoolean());
                t.add("something good", "foo bar baz bat quux", RANDOM.nextBoolean());
            }
            return Index.Action.NOOP;
        }));
    }

}
