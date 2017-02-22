package ph.codeia.todo.index;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import ph.codeia.todo.Mvp;
import ph.codeia.todo.R;
import ph.codeia.todo.data.TodoInMemory;
import ph.codeia.todo.data.TodoRepository;
import ph.codeia.todo.data.TodoSerialized;
import ph.codeia.todo.databinding.ScreenIndexBinding;
import ph.codeia.todo.util.AndroidUnit;
import ph.codeia.todo.util.FragmentProvider;

public class IndexFragment extends Fragment implements Index.View {

    public static FragmentProvider<IndexFragment> of(FragmentActivity activity) {
        return new FragmentProvider
                .Builder<>(IndexFragment.class, TAG)
                .build(activity);
    }

    public interface SaveState {
        void save(Index.State state);
        void save(TodoAdapter.State state);
    }

    private static final String TAG = IndexFragment.class.getCanonicalName();
    private static final Random RANDOM = new Random();
    private static final Executor WORKER = Executors.newSingleThreadExecutor();

    private final TodoAdapter adapter = new TodoAdapter(new TodoAdapter.Controller() {
        @Override
        public void checked(long id, boolean on) {
            apply(presenter.toggle((int) id, on));
        }

        @Override
        public void selected(long id) {
            apply(presenter.details((int) id));
        }
    });

    private TodoRepository repo;
    private Index.Presenter presenter;
    private ScreenIndexBinding layout;
    private AndroidUnit<Index.State, Index.Action, Index.View> screen =
            new AndroidUnit<>(Index.State.ROOT);
    private AndroidUnit<TodoAdapter.State, TodoAdapter.Action, RecyclerView> list =
            new AndroidUnit<>(new TodoAdapter.State());

    public void restore(Index.State screenState, TodoAdapter.State listState) {
        screen = new AndroidUnit<>(screenState);
        list = new AndroidUnit<>(listState);
    }

    public void save(SaveState out) {
        out.save(screen.state());
        out.save(list.state());
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        FragmentActivity activity = getActivity();
        try {
            repo = new TodoSerialized(new File(activity.getCacheDir(), "todos"));
        } catch (IOException | ClassNotFoundException e) {
            repo = new TodoInMemory();
        }
        presenter = new IndexActions(repo);
        layout = ScreenIndexBinding.inflate(inflater, container, false);
        layout.doEnterNew.setOnClickListener(_v -> apply(presenter.add()));
        layout.todoContainer.setLayoutManager(new LinearLayoutManager(activity));
        layout.todoContainer.addItemDecoration(
                new DividerItemDecoration(activity, DividerItemDecoration.VERTICAL));
        setHasOptionsMenu(true);
        return layout.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        applyList(adapter.init());
        if (savedInstanceState == null) {
            apply(presenter.load());
        } else {
            apply(presenter.refresh());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        list.start(layout.todoContainer);
        screen.start(this);
        apply(Index.Action.FLUSH);
    }

    @Override
    public void onPause() {
        super.onPause();
        screen.stop();
        list.stop();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.index, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        apply((state, _v) -> {
            if (state.showCompletedItems && state.showActiveItems) {
                menu.findItem(R.id.both).setChecked(true);
            } else if (state.showCompletedItems) {
                menu.findItem(R.id.completed_only).setChecked(true);
            } else if (state.showActiveItems) {
                menu.findItem(R.id.active_only).setChecked(true);
            }
            return state;
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.populate:
                apply(populate());
                break;
            case R.id.delete:
                apply(presenter.deleteCompleted());
                break;
            case R.id.completed_only:
                item.setChecked(true);
                applyNow(presenter.filter(true, false));
                break;
            case R.id.active_only:
                item.setChecked(true);
                applyNow(presenter.filter(false, true));
                break;
            case R.id.both:
                item.setChecked(true);
                applyNow(presenter.filter(true, true));
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void tell(String message, Object... fmtArgs) {
        Snackbar.make(layout.root, String.format(message, fmtArgs), Snackbar.LENGTH_SHORT)
                .show();
    }

    @Override
    public void show(List<Index.Item> newItems) {
        if (newItems.isEmpty()) {
            layout.emptyMessage.setVisibility(View.VISIBLE);
        } else {
            layout.emptyMessage.setVisibility(View.GONE);
        }
        applyList(adapter.setItems(newItems));
    }

    @Override
    public void spin(boolean busy) {
        if (busy) {
            layout.spinner.setVisibility(View.VISIBLE);
        } else {
            layout.spinner.setVisibility(View.GONE);
        }
    }

    @Override
    public void confirmDelete(Index.Action onConfirm) {
        new AlertDialog.Builder(getActivity())
                .setMessage("Delete all completed tasks? This cannot be undone.")
                .setPositiveButton("Delete", (_d, _i) -> apply(onConfirm))
                .setNegativeButton("No, go back", null)
                .show();
    }

    @Override
    public void goToDetails(int id) {
        tell("showing item:%d", id);
    }

    @Override
    public void goToEntryForm() {
    }

    @Override
    public void log(Mvp.Log level, String message) {
        switch (level) {
            case D:
                Log.d("mz", message);
                break;
            case I:
                Log.i("mz", message);
                break;
            case E:
                Log.e("mz", message);
                break;
        }
    }

    private void apply(Index.Action action) {
        screen.apply(action, this, WORKER);
    }

    private void applyNow(Index.Action action) {
        screen.apply(action, this, Runnable::run);
    }

    private void applyList(TodoAdapter.Action action) {
        list.apply(action, layout.todoContainer, Runnable::run);
    }

    @SuppressLint("NewApi")  // retrolambda can convert the try-let
    private Index.Action populate() {
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
