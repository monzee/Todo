package ph.codeia.todo.index;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;

import ph.codeia.todo.BaseFragment;
import ph.codeia.todo.Mvp;
import ph.codeia.todo.R;
import ph.codeia.todo.Todo;
import ph.codeia.todo.data.TodoRepository;
import ph.codeia.todo.databinding.ScreenIndexBinding;
import ph.codeia.todo.details.DetailsFragment;
import ph.codeia.todo.util.AndroidUnit;
import ph.codeia.todo.util.FragmentProvider;

public class IndexFragment extends BaseFragment implements Index.View {

    public static final String TAG = IndexFragment.class.getCanonicalName();

    public static FragmentProvider<IndexFragment> of(FragmentActivity activity) {
        return new FragmentProvider.Builder<>(IndexFragment.class, TAG)
                .build(activity);
    }

    public interface SaveState {
        void save(Index.State state);
        void save(TodoAdapter.State state);
    }

    private final TodoAdapter adapter = new TodoAdapter(new TodoAdapter.Controller() {
        @Override
        public void checked(long id, boolean on) {
            apply(presenter.setCompleted((int) id, on));
        }

        @Override
        public void selected(View view, long id) {
            transTitle = view.findViewById(R.id.the_title);
            transCheckbox = view.findViewById(R.id.is_completed);
            apply(presenter.details((int) id));
        }
    });

    private final Executor worker = Todo.GLOBALS.io();
    private final Random random = new Random();
    private TodoRepository repo;
    private Index.Presenter presenter;
    private ScreenIndexBinding layout;
    private Mvp.Unit<Index.State, Index.Action, Index.View> screen;
    private Mvp.Unit<TodoAdapter.State, TodoAdapter.Action, RecyclerView> list;
    private View transTitle;
    private View transCheckbox;

    public void restore(Index.State screenState, TodoAdapter.State listState) {
        screen = new AndroidUnit<>(screenState);
        list = new AndroidUnit<>(listState);
    }

    public void save(SaveState out) {
        if (screen != null) {
            out.save(screen.state());
            out.save(list.state());
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        Context context = getContext();
        repo = Todo.GLOBALS.todoRepository(context);
        presenter = new IndexActions(repo);
        layout = ScreenIndexBinding.inflate(inflater, container, false);
        layout.doEnterNew.setOnClickListener(_v -> apply(presenter.add()));
        layout.todoContainer.setLayoutManager(new LinearLayoutManager(context));
        layout.todoContainer.addItemDecoration(
                new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        setHasOptionsMenu(true);
        applyList(adapter.init());
        apply(shouldLoad() ? presenter.load() : presenter.refresh());
        return layout.getRoot();
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
        Index.State state = screen.state();
        if (state.showCompletedItems && state.showActiveItems) {
            menu.findItem(R.id.both).setChecked(true);
        } else if (state.showCompletedItems) {
            setTitle("Completed");
            menu.findItem(R.id.completed_only).setChecked(true);
        } else if (state.showActiveItems) {
            setTitle("Active");
            menu.findItem(R.id.active_only).setChecked(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.populate:
                apply(populate());
                break;
            case R.id.delete:
                apply(presenter.deleteAllCompleted());
                break;
            case R.id.completed_only:
                item.setChecked(true);
                setTitle("Completed");
                applyNow(presenter.filter(true, false));
                break;
            case R.id.active_only:
                item.setChecked(true);
                setTitle("Active");
                applyNow(presenter.filter(false, true));
                break;
            case R.id.both:
                item.setChecked(true);
                setTitle("Todo");
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
        forceLoadOnReenter();
        DetailsFragment.of(getActivity())
                .withArgs(args -> args.putInt(DetailsFragment.KEY_ID, id))
                .replace(R.id.content)
                .addSharedElement(transTitle, "title")
                .addSharedElement(transCheckbox, "checked")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                .addToBackStack(DetailsFragment.TAG)
                .commit();
    }

    @Override
    public void goToEntryForm() {
    }

    private void setTitle(String title) {
        getActivity().setTitle(title);
    }

    private void apply(Index.Action action) {
        screen.apply(action, this, worker);
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
            return state.withBusy(true).async(() -> {
                Thread.sleep(3000);
                try (TodoRepository.Transactional t = repo.transact()) {
                    t.add("quick jumps", "The quick brown fox jumps over the lazy dog.", random.nextBoolean());
                    t.add("learn latin", "Lorem ipsum dolor sit amet", random.nextBoolean());
                    t.add("something good", "foo bar baz bat quux", random.nextBoolean());
                }
                return (futureState, futureView) -> {
                    futureView.spin(false);
                    return futureState.plus(presenter.load());
                };
            });
        };
    }
}
