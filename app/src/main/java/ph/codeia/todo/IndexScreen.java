package ph.codeia.todo;

import android.app.Activity;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import ph.codeia.todo.index.Index;
import ph.codeia.todo.databinding.ActivityMainBinding;
import ph.codeia.todo.databinding.ItemBrowseBinding;

class IndexScreen implements Index.View {

    interface Compromise {
        void checked(int id, boolean on);
        void selected(int id);
        void apply(Index.Action action);
        void apply(ListAction action);
    }

    interface ListAction
            extends Repeating.Action<VisibleItems, ListAction, IndexScreen> {
    }

    class VisibleItems
            extends Repeating.State<VisibleItems, ListAction, Index.Item> {
        public final
        RecyclerView.Adapter<ItemRowView> adapter = new RecyclerView.Adapter<ItemRowView>() {
            {
                setHasStableIds(true);
            }

            @Override
            public ItemRowView onCreateViewHolder(ViewGroup parent, int viewType) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                return new ItemRowView(ItemBrowseBinding.inflate(inflater, parent, false));
            }

            @Override
            public void onBindViewHolder(ItemRowView holder, int position) {
                holder.bind(items.get(position));
            }

            @Override
            public int getItemCount() {
                return items.size();
            }

            @Override
            public long getItemId(int position) {
                return items.get(position).id();
            }
        };
    }

    class ItemRowView
            extends RecyclerView.ViewHolder
            implements Repeating.RowView<Index.Item> {

        final ItemBrowseBinding widgets;

        ItemRowView(ItemBrowseBinding widgets) {
            super(widgets.getRoot());
            this.widgets = widgets;
        }

        @Override
        public void bind(Index.Item item) {
            widgets.theTitle.setText(item.title());
            widgets.isCompleted.setChecked(item.completed());
            widgets.isCompleted.setOnCheckedChangeListener((_cb, checked) ->
                    compromise.checked((int) getItemId(), checked));
            widgets.getRoot().setOnClickListener(_v -> compromise.selected((int) getItemId()));
        }
    }

    public final VisibleItems visible = new VisibleItems();

    private final Activity activity;
    private final ActivityMainBinding widgets;
    private final Compromise compromise;

    IndexScreen(Activity activity, ActivityMainBinding widgets, Compromise compromise) {
        this.activity = activity;
        this.widgets = widgets;
        this.compromise = compromise;
    }

    @Override
    public void tell(String message, Object... fmtArgs) {
        Snackbar.make(widgets.root, String.format(message, fmtArgs), Snackbar.LENGTH_SHORT)
                .show();
    }

    @Override
    public void show(List<Index.Item> items) {
        compromise.apply(diff(items));
    }

    @Override
    public void spin(boolean busy) {
        if (busy) {
            widgets.spinner.show();
        } else {
            widgets.spinner.hide();
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

    private void nothing() {
        widgets.emptyMessage.setVisibility(View.VISIBLE);
    }

    private void something() {
        widgets.emptyMessage.setVisibility(View.GONE);
    }

    private static ListAction diff(List<Index.Item> newItems) {
        return (state, view) -> state.async(() -> {
            DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return state.items.size();
                }

                @Override
                public int getNewListSize() {
                    return newItems.size();
                }

                @Override
                public boolean areItemsTheSame(int oldPos, int newPos) {
                    return state.items.get(oldPos).id() == newItems.get(newPos).id();
                }

                @Override
                public boolean areContentsTheSame(int oldPos, int newPos) {
                    return state.items.get(oldPos).sameAs(newItems.get(newPos));
                }
            }, false);
            return (futureState, futureView) -> {
                if (newItems.isEmpty()) {
                    futureView.nothing();
                } else {
                    futureView.something();
                }
                futureState.setItems(newItems);
                diff.dispatchUpdatesTo(futureState.adapter);
                return futureState;
            };
        });
    }
}
