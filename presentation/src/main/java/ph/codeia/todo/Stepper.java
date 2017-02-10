package ph.codeia.todo;

import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;

public class Stepper<
        S extends Mvp.State<S, A>,
        A extends Mvp.Action<S, A, V>,
        V extends Mvp.Debug>
        extends Mvp.Unit<S, A, V> {

    public Stepper(S state) {
        super(state);
    }

    @Override
    public void apply(A action, V view, Executor worker) {
        apply(action, view);
    }

    @Override
    public void handle(Throwable error, V view) {
        Mvp.Log.E.to(view, error.getMessage());
    }

    public void apply(A action, V view) {
        state = action.fold(state, view);
    }

    public boolean step(V view) {
        Iterator<Future<A>> it = state.iterator();
        if (!it.hasNext()) {
            return false;
        }
        try {
            Future<A> future = it.next();
            it.remove();
            if (future instanceof RunnableFuture) {
                ((RunnableFuture) future).run();
            }
            apply(future.get(), view);
            return true;
        } catch (InterruptedException | ExecutionException e) {
            handle(e, view);
            return false;
        }
    }
}
