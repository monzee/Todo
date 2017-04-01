package ph.codeia.todo;

import java.lang.ref.WeakReference;
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
 * <p>
 * We define an application as an input, output, and some sequence of states
 * in-between.
 * <ul>
 * <li> an {@link Action} is a procedure that can be initiated by a user or the
 * system that transforms a State object into another State.
 * <li> a View is an type bound to the action that projects current State into
 * some user-facing device, most commonly a screen.
 * <li> a {@link State} is a snapshot of the application at some point during
 * its lifetime. It holds a queue of future actions waiting to be run and tracks
 * the number of concurrently running actions.
 * <li> a {@link Unit} binds the three together. It holds a state object,
 * accepts an action and runs it to derive a new state, replaces its state with
 * the result and waits for the next action.
 * </ul>
 * A presenter represents a set of valid Actions that may be taken at any
 * point in the application. It is not defined here because the unit only cares
 * about the action, not the object from where it came.
 * <p>
 * The signature of {@link Action} is what makes the presenter a presenter: it
 * takes some View object in addition to the old state. Actions are not pure
 * functions; the view acts as a channel for side effects while the action
 * computes a new state. Because of this design,
 * <ul>
 * <li> the presenter receives a reference to the view albeit in an indirect
 * way. This turns out to be an advantage because the presenter object wouldn't
 * have to have {@code #bind(View)} and {@code #unbind()} pairs, thus reducing
 * opportunities to leak stale contexts.
 * <li> all display logic lives in the presenter and not in a self-contained
 * view that knows how to render itself from the state like in a ReactJS-
 * inspired system.
 * </ul>
 */
public interface Mvp {

    interface State<
            S extends State<S, A>,
            A extends Action<S, A, ?>>
    extends Iterable<Future<A>> {
        S async(Future<A> future);
        Backlog backlog();
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

    interface Debug {
        void log(Log level, String message);
        void log(Log level, Throwable error);
    }

    enum Log {
        D, I, E;

        public void to(Debug logger, String message, Object... fmtArgs) {
            logger.log(this, String.format(message, fmtArgs));
        }

        public void to(Debug logger, String message, Throwable error) {
            logger.log(this, message);
            logger.log(this, error);
        }
    }

    class Backlog {
        private final Object lock = new Object();
        private volatile int running = 0;

        public void willProduceAction() {
            synchronized (lock) {
                running++;
            }
        }

        public void didProduceAction() {
            if (running == 0) {
                throw new IllegalStateException("Imbalanced produce calls");
            }
            synchronized (lock) {
                if (--running == 0) {
                    lock.notifyAll();
                }
            }
        }

        public void awaitCleared() throws InterruptedException {
            synchronized (lock) {
                while (running > 0) {
                    lock.wait();
                }
            }
        }
    }

    class BaseState<
            S extends BaseState<S, A>,
            A extends Action<S, A, ?>>
    implements State<S, A> {

        protected Queue<Future<A>> futures = new ConcurrentLinkedQueue<>();
        protected Backlog backlog = new Backlog();

        public S plus(A action) {
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

        @Override
        public Backlog backlog() {
            return backlog;
        }

        protected S join(S instance) {
            instance.futures = futures;
            instance.backlog = backlog;
            return instance;
        }
    }

    abstract class Unit<
            S extends State<S, A>,
            A extends Action<S, A, V>,
            V>
    implements ErrorHandler<V> {

        /**
         * Returns a realized future.
         *
         * @param action The action to wrap in a future.
         * @param <A> The action type.
         * @return A completed future that can be added to a state's future
         * action queue.
         */
        public static <A> Future<A> now(A action) {
            FutureTask<A> future = new FutureTask<>(() -> action);
            future.run();
            return future;
        }

        /**
         * Returns a runnable future.
         *
         * @param action The action callable to wrap.
         * @param <A> The action type.
         * @return A runnable future that can be added to a state's future
         * action queue.
         */
        public static <A> Future<A> future(Callable<A> action) {
            return new FutureTask<>(action);
        }

        protected S state;
        protected boolean isStopped;

        protected Unit(S state) {
            this.state = state;
        }

        /**
         * Returns the state for introspection or saving.
         *
         * @return The unit's current state. Avoid mutating.
         */
        public S state() {
            return state;
        }

        /**
         * Starts the unit.
         * <p>
         * Computes a new state before accepting new actions. If the unit might
         * have folded an async state before restarting (i.e. in an action that
         * calls/returns {@link State#async(Future)}), you should start the unit
         * using {@link #start(Executor, V, A)} instead.
         *
         * @param view View to be used by the actions to fold.
         */
        public void start(V view) {
            isStopped = false;
            for (Iterator<Future<A>> it = state.iterator(); it.hasNext();) {
                Future<A> future = it.next();
                if (future.isDone()) {
                    it.remove();
                    try {
                        state = future.get().fold(state, view);
                    } catch (InterruptedException | ExecutionException e) {
                        handle(e, view);
                    }
                }
            }
        }

        /**
         * Waits in the background until all pending tasks from the previous
         * incarnation are done and the new actions are enqueued in the state.
         *
         * @param worker NEVER PASS AN IMMEDIATE EXECUTOR (i.e. {@code
         *               Runnable::run}). You will most likely deadlock the main
         *               thread.
         * @param view View to be used by the actions and the error handler.
         * @param after The action to perform when the backlog is cleared. A
         *              no-op is fine, i.e. {@code (state, view) -> state}.
         *              Unfortunately, that cannot be instantiated here because
         *              of the recursive type parameters so you have to pass one
         */
        public void start(Executor worker, V view, A after) {
            start(view);
            WeakReference<V> viewRef = new WeakReference<>(view);
            worker.execute(() -> {
                try {
                    state.backlog().awaitCleared();
                    apply(worker, viewRef, after);
                } catch (InterruptedException e) {
                    handle(e, viewRef.get());
                }
            });
        }

        /**
         * Stops the unit.
         * <p>
         * Any actions passed to {@link #apply(Executor, V, A)} while stopped
         * will be enqueued onto the state's future queue as a completed future.
         * They will be folded when the unit is restarted.
         */
        public void stop() {
            isStopped = true;
        }

        /**
         * Computes a new state by folding an action into the current state.
         *
         * @param worker Provides a thread to execute async calls in
         * @param view A channel for side effects to be used by the actions.
         * @param action The action to fold.
         */
        public void apply(Executor worker, V view, A action) {
            apply(worker, new WeakReference<>(view), action);
        }

        protected void apply(Executor worker, WeakReference<V> viewRef, A action) {
            if (isStopped) {
                state = state.async(now(action));
            } else {
                state = action.fold(state, viewRef.get());
                Backlog backlog = state.backlog();
                for (Iterator<Future<A>> it = state.iterator(); it.hasNext();) {
                    Future<A> future = it.next();
                    it.remove();
                    backlog.willProduceAction();
                    worker.execute(() -> {
                        try {
                            if (future instanceof RunnableFuture && !future.isDone()) {
                                ((RunnableFuture) future).run();
                            }
                            apply(worker, viewRef, future.get());
                        } catch (InterruptedException | ExecutionException e) {
                            handle(e, viewRef.get());
                        } finally {
                            backlog.didProduceAction();
                        }
                    });
                }
            }
        }

    }

}
