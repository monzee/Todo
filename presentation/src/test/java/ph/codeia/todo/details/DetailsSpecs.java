package ph.codeia.todo.details;

import org.junit.Before;

import ph.codeia.todo.Mvp;
import ph.codeia.todo.Stepper;
import ph.codeia.todo.data.TodoInMemory;
import ph.codeia.todo.data.TodoRepository;

import static org.junit.Assert.*;

public class DetailsSpecs {
    Stepper<Details.State, Details.Action, Details.View> details;
    TodoRepository m;
    Details.Presenter p;
    FakeView v;

    @Before
    public void setup() {
        m = new TodoInMemory();
        p = new DetailsActions(m, 1);
        v = new FakeView();
        details = new Stepper<>(Details.State.ROOT);
    }

    static class FakeView implements Details.View {
        String title;
        String description;
        boolean checked;
        Details.Action delete;

        void confirm(Stepper<Details.State, Details.Action, Details.View> unit) {
            if (delete != null) {
                unit.apply(delete, this);
                delete = null;
            }
        }

        @Override
        public void show(Details.Item item) {
            title = item.title();
            description = item.description();
            checked = item.completed();
        }

        @Override
        public void confirmDelete(Details.Action doIt) {
            delete = doIt;
        }

        @Override
        public void goToEditor() {

        }

        @Override
        public void goBack() {

        }

        @Override
        public void log(Mvp.Log level, String message) {
            if (level == Mvp.Log.E) {
                System.err.println(message);
            } else {
                System.out.println(message);
            }
        }
    }
}