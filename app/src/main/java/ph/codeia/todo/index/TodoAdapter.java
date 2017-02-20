package ph.codeia.todo.index;

import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.Collections;
import java.util.List;

import ph.codeia.todo.Mvp;
import ph.codeia.todo.databinding.ItemIndexBinding;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.RowView> {

    public interface Controller {
        void checked(int id, boolean on);
        void selected(int id);
    }

    public interface Action extends Mvp.Action<State, Action, RecyclerView> {}

    public static class State extends Mvp.BaseState<State, Action> {
        private List<Index.Item> items = Collections.emptyList();
    }

    public class RowView extends RecyclerView.ViewHolder {
        final ItemIndexBinding widgets;

        public RowView(ItemIndexBinding widgets) {
            super(widgets.getRoot());
            this.widgets = widgets;
        }

        public void bind(Index.Item row) {
            widgets.theTitle.setText(row.title());
            widgets.isCompleted.setChecked(row.completed());
            itemView.setOnClickListener(_v ->
                    controller.selected((int) getItemId()));
            widgets.isCompleted.setOnCheckedChangeListener((_cb, checked) ->
                    controller.checked((int) getItemId(), checked));
        }
    }

    private final Controller controller;
    private State state;

    public TodoAdapter(Controller controller) {
        this.controller = controller;
    }

    @Override
    public RowView onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new RowView(ItemIndexBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(RowView holder, int position) {
        holder.bind(state.items.get(position));
    }

    @Override
    public int getItemCount() {
        return state.items.size();
    }

    @Override
    public long getItemId(int position) {
        return state.items.get(position).id();
    }

    public Action init() {
        return (state, view) -> {
            this.state = state;
            setHasStableIds(true);
            view.setAdapter(this);
            return state;
        };
    }

    public Action setItems(List<Index.Item> newItems) {
        return (state, view) -> state.async(() -> {
            DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                final List<Index.Item> oldItems = state.items;

                @Override
                public int getOldListSize() {
                    return oldItems.size();
                }

                @Override
                public int getNewListSize() {
                    return newItems.size();
                }

                @Override
                public boolean areItemsTheSame(int oldPos, int newPos) {
                    return oldItems.get(oldPos).id() == newItems.get(newPos).id();
                }

                @Override
                public boolean areContentsTheSame(int oldPos, int newPos) {
                    return oldItems.get(oldPos).sameAs(newItems.get(newPos));
                }
            }, false);
            return (futureState, futureView) -> {
                futureState.items = newItems;
                if (view == futureView) {
                    diff.dispatchUpdatesTo(this);
                } else {
                    futureView.getAdapter().notifyDataSetChanged();
                }
                return futureState;
            };
        });
    }
}
