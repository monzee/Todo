package ph.codeia.todo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import ph.codeia.todo.details.Details;
import ph.codeia.todo.details.DetailsFragment;
import ph.codeia.todo.index.Index;
import ph.codeia.todo.index.IndexFragment;
import ph.codeia.todo.index.TodoAdapter;

public class MainActivity extends AppCompatActivity {

    private static class States implements
            IndexFragment.SaveState,
            DetailsFragment.SaveState {
        Index.State screen = Index.State.ROOT;
        TodoAdapter.State visible = new TodoAdapter.State();
        Details.State details = Details.State.ROOT;

        @Override
        public void save(Index.State state) {
            screen = state;
        }

        @Override
        public void save(TodoAdapter.State state) {
            visible = state;
        }

        @Override
        public void save(Details.State state) {
            details = state;
        }
    }

    private States states = new States();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shell);
        if (savedInstanceState == null) {
            IndexFragment.of(this).replace(R.id.content).commit();
        }
    }

    @Override
    public void onAttachFragment(Fragment f) {
        States saved = (States) getLastCustomNonConfigurationInstance();
        if (saved != null) {
            states = saved;
        }
        if (f instanceof IndexFragment) {
            ((IndexFragment) f).restore(states.screen, states.visible);
        } else if (f instanceof DetailsFragment) {
            ((DetailsFragment) f).restore(states.details);
        }
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        IndexFragment.of(this).forSome(f -> f.save(states));
        DetailsFragment.of(this).forSome(f -> f.save(states));
        return states;
    }
}
