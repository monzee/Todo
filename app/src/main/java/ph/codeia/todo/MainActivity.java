package ph.codeia.todo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import ph.codeia.todo.index.Index;
import ph.codeia.todo.index.IndexFragment;
import ph.codeia.todo.index.TodoAdapter;
import ph.codeia.todo.util.FragmentProvider;
import ph.codeia.todo.util.Pattern;

public class MainActivity extends AppCompatActivity {

    private static class States implements IndexFragment.SaveState {
        Index.State screen;
        TodoAdapter.State visible;

        @Override
        public void save(Index.State state) {
            screen = state;
        }

        @Override
        public void save(TodoAdapter.State state) {
            visible = state;
        }
    }

    private States states = new States();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shell);
        States saved = (States) getLastCustomNonConfigurationInstance();
        if (saved != null) {
            states = saved;
        }
        FragmentProvider<IndexFragment> index = IndexFragment.of(this);
        index.check(new Pattern.Maybe<IndexFragment>() {
            @Override
            public void present(IndexFragment indexFragment) {
                indexFragment.restore(states.screen, states.visible);
            }

            @Override
            public void absent() {
                index.replace(R.id.content).commit();
            }
        });
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        IndexFragment.of(this).get(s -> s.save(states));
        return states;
    }
}
