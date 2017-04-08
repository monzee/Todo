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
    public void apply(Executor worker, V view, A action) {
        apply(view, action);
    }

    @Override
    public void handle(Throwable error, V view) {
        view.log(Mvp.Log.E, error);
    }

    public void apply(V view, A action) {
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
            apply(view, future.get());
            return true;
        } catch (InterruptedException | ExecutionException e) {
            handle(e, view);
            return false;
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    public void drain(V view) {
        while (step(view));
    }
}
