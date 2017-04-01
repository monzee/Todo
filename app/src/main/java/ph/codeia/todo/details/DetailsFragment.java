package ph.codeia.todo.details;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.concurrent.Executor;

import ph.codeia.todo.BaseFragment;
import ph.codeia.todo.Mvp;
import ph.codeia.todo.R;
import ph.codeia.todo.Todo;
import ph.codeia.todo.databinding.ScreenDetailsBinding;
import ph.codeia.todo.util.AndroidUnit;
import ph.codeia.todo.util.FragmentProvider;

public class DetailsFragment extends BaseFragment implements Details.View {

    public static final String ARG_ID = "id";

    public static FragmentProvider<DetailsFragment> of(FragmentActivity activity) {
        return new FragmentProvider.Builder<>(DetailsFragment.class).build(activity);
    }

    public interface SaveState {
        void save(Details.State state);
    }

    private final Executor background = Todo.GLOBALS.io();
    private Details.Presenter presenter;
    private ScreenDetailsBinding layout;
    private Mvp.Unit<Details.State, Details.Action, Details.View> details;

    public void restore(Details.State state) {
        details = new AndroidUnit<>(state);
    }

    public void save(SaveState out) {
        if (details != null) {
            out.save(details.state());
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        int id = getArguments().getInt(ARG_ID, -1);
        if (id == -1) {
            goBack();
            return null;
        }
        presenter = new DetailsActions(Todo.GLOBALS.todoRepository(getContext()), id);
        layout = ScreenDetailsBinding.inflate(inflater, container, false);
        layout.isCompleted.setOnClickListener(_v -> apply(presenter.toggleCompleted()));
        ViewCompat.setTransitionName(layout.theTitle, "title");
        ViewCompat.setTransitionName(layout.isCompleted, "checked");
        setHasOptionsMenu(true);
        apply(shouldLoad() ? presenter.load() : presenter.refresh());
        return layout.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        details.start(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        details.stop();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.details, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.do_delete) {
            apply(presenter.delete());
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void show(Details.Item item) {
        layout.setItem(item);
    }

    @Override
    public void confirmDelete(Details.Action doIt) {
        new AlertDialog.Builder(getActivity())
                .setMessage("Delete this task? This action cannot be undone.")
                .setPositiveButton("Delete", (_cb, _i) -> apply(doIt))
                .setNegativeButton("No, go back", null)
                .create()
                .show();
    }

    @Override
    public void goToEditor() {

    }

    @Override
    public void goBack() {
        getFragmentManager().popBackStack();
    }

    private void apply(Details.Action action) {
        details.apply(background, this, action);
    }
}
