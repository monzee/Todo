package ph.codeia.todo;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

/**
 * A state-centric MVP abstraction.
 *
 * We define an application as an input, output, and some sequence of states
 * in-between.
 *
 * - an {@link Action} is a procedure that can be initiated by a user or the
 *   system that transforms a State object into another State.
 * - a View is an interface bound to the action that projects current State
 *   into some user-facing output device, most likely a screen.
 * - a {@link State} is a snapshot of the application at some point during its
 *   lifetime. It holds a queue of future actions waiting to be run.
 * - a {@link Unit} binds the three together. It holds a state object, accepts
 *   an action and runs it to derive a new state, replaces its state with the
 *   result and waits for the next action.
 *
 * A presenter represents a set of valid Actions that may be taken at any
 * point in the application. It is not defined here because the unit only cares
 * about the action, not the object from where it came.
 *
 * The signature of {@link Action} is what makes the presenter a presenter: it
 * takes some View interface in addition to the old state. Actions are not pure
 * functions. The view acts as a side output channel while the action computes
 * a new state. Therefore, the output logic lives in the presenter and not in a
 * self-contained view that knows how to render itself from the state.
 */
public interface Mvp {

    interface State<
            S extends State<S, A>,
            A extends Action<S, A, ?>>
            extends Iterable<Future<A>> {
        S async(Future<A> future);
    }

    interface Action<
            S extends State<S, A>,
            A extends Action<S, A, V>,
            V> {
        S fold(S state, V view);
    }

    interface ErrorHandler<V> {
        void handle(Throwable error, V view);
    }

    abstract class BaseState<
            S extends BaseState<S, A>,
            A extends Action<S, A, ?>>
            implements State<S, A> {

        protected Queue<Future<A>> futures = new ConcurrentLinkedQueue<>();

        public S async(A action) {
            return async(Unit.now(action));
        }

        public S async(Callable<A> action) {
            return async(Unit.future(action));
        }

        @SuppressWarnings("unchecked")
        @Override
        public S async(Future<A> future) {
            futures.add(future);
            return (S) this;
        }

        @Override
        public Iterator<Future<A>> iterator() {
            return futures.iterator();
        }

        protected S join(S instance) {
            instance.futures = futures;
            return instance;
        }
    }

    abstract class Unit<
            S extends State<S, A>,
            A extends Action<S, A, V>,
            V>
            implements ErrorHandler<V> {

        public static <A> Future<A> now(A action) {
            FutureTask<A> future = new FutureTask<>(() -> action);
            future.run();
            return future;
        }

        public static <A> Future<A> future(Callable<A> action) {
            return new FutureTask<>(action);
        }

        protected S state;
        protected boolean isStopped;

        protected Unit(S state) {
            this.state = state;
        }

        public S state() {
            return state;
        }

        public void start() {
            isStopped = false;
        }

        public void stop() {
            isStopped = true;
        }

        public void apply(A action, V view, Executor worker) {
            if (isStopped) {
                state = state.async(now(action));
            } else {
                state = action.fold(state, view);
                for (Iterator<Future<A>> it = state.iterator(); it.hasNext();) {
                    Future<A> future = it.next();
                    it.remove();
                    worker.execute(() -> {
                        try {
                            if (future instanceof RunnableFuture && !future.isDone()) {
                                ((RunnableFuture) future).run();
                            }
                            apply(future.get(), view, worker);
                        } catch (InterruptedException | ExecutionException e) {
                            handle(e, view);
                        }
                    });
                }
            }
        }
    }

    interface Debug {
        void log(Log level, String message);
    }

    enum Log {
        D, I, E;

        public void to(Debug logger, String message, Object... fmtArgs) {
            logger.log(this, String.format(message, fmtArgs));
        }
    }
}
