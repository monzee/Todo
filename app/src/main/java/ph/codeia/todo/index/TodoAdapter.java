package ph.codeia.todo.index;

import android.support.v4.view.ViewCompat;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collections;
import java.util.List;

import ph.codeia.todo.Mvp;
import ph.codeia.todo.databinding.ItemIndexBinding;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.RowView> {

    public interface Controller {
        void checked(long id, boolean on);
        void selected(View row, long id);
    }

    public interface Action extends Mvp.Action<State, Action, RecyclerView> {}

    public static class State extends Mvp.BaseState<State, Action> {
        private List<Index.Item> items = Collections.emptyList();

        @Override
        public String toString() {
            return "size: " + items.size();
        }
    }

    public static class RowView extends RecyclerView.ViewHolder {
        final ItemIndexBinding layout;

        public RowView(ItemIndexBinding layout) {
            super(layout.getRoot());
            this.layout = layout;
        }
    }

    private final State state;
    private final Controller controller;

    public TodoAdapter(State state, Controller controller) {
        this.controller = controller;
        this.state = state != null ? state : new State();
        setHasStableIds(true);
    }

    @Override
    public RowView onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemIndexBinding layout = ItemIndexBinding.inflate(inflater, parent, false);
        layout.setController(controller);
        return new RowView(layout);
    }

    @Override
    public void onBindViewHolder(RowView holder, int position) {
        holder.layout.setRow(state.items.get(position));
        ViewCompat.setTransitionName(holder.layout.theTitle, "title:" + position);
        ViewCompat.setTransitionName(holder.layout.isCompleted, "checked:" + position);
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
        return (_s, view) -> {
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
                // technically leaking the old view here, but this happens so
                // quickly that i think it's fine. i'm not even sure why this is
                // an async action in the first place. the else branch almost
                // never happens unless there's thousands of items in the list.
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
