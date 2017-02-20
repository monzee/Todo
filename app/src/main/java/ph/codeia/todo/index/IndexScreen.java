package ph.codeia.todo.index;

import android.app.Activity;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;

import java.util.List;

import ph.codeia.todo.Mvp;
import ph.codeia.todo.databinding.ActivityMainBinding;

public class IndexScreen implements Index.View {

    public interface Compromise {
        void apply(Index.Action action);
        void applyList(TodoAdapter.Action action);
    }

    private final Activity activity;
    private final ActivityMainBinding widgets;
    private final TodoAdapter adapter;
    private final Compromise compromise;

    public IndexScreen(
            Activity activity,
            ActivityMainBinding widgets,
            TodoAdapter adapter,
            Compromise compromise) {
        this.activity = activity;
        this.widgets = widgets;
        this.adapter = adapter;
        this.compromise = compromise;
    }

    @Override
    public void tell(String message, Object... fmtArgs) {
        Snackbar.make(widgets.root, String.format(message, fmtArgs), Snackbar.LENGTH_SHORT)
                .show();
    }

    @Override
    public void show(List<Index.Item> newItems) {
        if (newItems.isEmpty()) {
            widgets.emptyMessage.setVisibility(View.VISIBLE);
        } else {
            widgets.emptyMessage.setVisibility(View.GONE);
        }
        compromise.applyList(adapter.setItems(newItems));
    }

    @Override
    public void spin(boolean busy) {
        if (busy) {
            widgets.spinner.setVisibility(View.VISIBLE);
        } else {
            widgets.spinner.setVisibility(View.GONE);
        }
    }

    @Override
    public void confirmDelete(Index.Action onConfirm) {
        new AlertDialog.Builder(activity)
                .setMessage("Delete all completed tasks? This cannot be undone.")
                .setPositiveButton("Delete", (_d, _i) -> compromise.apply(onConfirm))
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

}
