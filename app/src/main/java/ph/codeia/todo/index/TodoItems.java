package ph.codeia.todo.index;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.Collections;
import java.util.List;

import ph.codeia.todo.Mvp;
import ph.codeia.todo.databinding.ItemIndexBinding;

public class TodoItems extends RecyclerView {

    private static final String NO_CONTROLLER = ""
            + "You must call TodoItems#setController(TodoItems.Controller) "
            + "during activity setup.";

    public interface Controller {
        void checked(int id, boolean on);
        void selected(int id);
        void apply(Action action);

        Controller MISSING = new Controller() {
            @Override
            public void checked(int id, boolean on) {
                throw new UnsupportedOperationException(NO_CONTROLLER);
            }

            @Override
            public void selected(int id) {
                throw new UnsupportedOperationException(NO_CONTROLLER);
            }

            @Override
            public void apply(Action action) {
                throw new UnsupportedOperationException(NO_CONTROLLER);
            }
        };
    }

    public interface Action extends Mvp.Action<State, Action, RecyclerView.Adapter<?>> {}

    public static class State extends Mvp.BaseState<State, Action> {
        private List<Index.Item> items = Collections.emptyList();
    }

    private class RowView extends ViewHolder {
        private final ItemIndexBinding binding;

        public RowView(ItemIndexBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Index.Item item) {
            binding.theTitle.setText(item.title());
            binding.isCompleted.setChecked(item.completed());
            binding.isCompleted.setOnCheckedChangeListener((_v, checked) ->
                    controller.checked((int) getItemId(), checked));
            itemView.setOnClickListener(_v ->
                    controller.selected((int) getItemId()));
        }
    }

    private class Adapter extends RecyclerView.Adapter<RowView> {
        private final State state;

        private Adapter(State state) {
            this.state = state;
        }

        @Override
        public RowView onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater i = LayoutInflater.from(parent.getContext());
            return new RowView(ItemIndexBinding.inflate(i, parent, false));
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
    }

    private Controller controller = Controller.MISSING;

    public TodoItems(Context context) {
        super(context);
    }

    public TodoItems(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TodoItems(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    public void init() {
        controller.apply((state, _a) -> {
            Adapter adapter = new Adapter(state);
            adapter.setHasStableIds(true);
            setAdapter(adapter);
            return state;
        });
    }

    public void setItems(List<Index.Item> newItems) {
        controller.apply((state, adapter) -> {
            state.items = newItems;
            adapter.notifyDataSetChanged();
            return state;
        });
    }
}
