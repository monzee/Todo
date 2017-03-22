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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
    }

    enum Log {
        D, I, E;

        public void to(Debug logger, String message, Object... fmtArgs) {
            logger.log(this, String.format(message, fmtArgs));
        }
    }

    class Backlog {
        private volatile int running = 0;
        private final Lock monitor = new ReentrantLock();
        private final Condition noRunningProducers = monitor.newCondition();

        public void willProduceAction() {
            monitor.lock();
            running++;
            monitor.unlock();
        }

        public void didProduceAction() {
            monitor.lock();
            if (--running < 1) {
                noRunningProducers.signalAll();
            }
            monitor.unlock();
        }

        public void awaitCleared() throws InterruptedException {
            monitor.lockInterruptibly();
            try {
                while (running > 0) {
                    noRunningProducers.await();
                }
            } finally {
                monitor.unlock();
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
         * @return A completed future that can be added onto a state's future
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
         * @return A runnable future that can be added onto a state's future
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
         * @return the unit's current state.
         */
        public S state() {
            return state;
        }

        /**
         * Starts the unit.
         *
         * Computes a new state before accepting new actions. If the unit might
         * have executed an async task before restarting (i.e. an action that
         * calls/returns {@link State#async(Future)}), you should start the unit
         * using {@link #start(Action, Object, Executor)} instead.
         *
         * @param view View to be used by the actions to execute.
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
         * @param after The action to perform when the backlog is cleared. A
         *              no-op is fine, i.e. {@code (state, view) -> state}.
         *              Unfortunately, that cannot be instantiated here because
         *              of the recursive type parameters so you have to pass one
         * @param view View to be used by the post action and the error handler.
         * @param worker NEVER PASS AN IMMEDIATE EXECUTOR (i.e. Runnable::run).
         *               You will probably deadlock the main thread.
         */
        public void start(A after, V view, Executor worker) {
            start(view);
            WeakReference<V> viewRef = new WeakReference<>(view);
            worker.execute(() -> {
                try {
                    state.backlog().awaitCleared();
                    apply(after, viewRef, worker);
                } catch (InterruptedException e) {
                    handle(e, viewRef.get());
                }
            });
        }

        /**
         * Stops the unit.
         *
         * Any actions passed to {@link #apply(Action, Object, Executor)} while
         * stopped will be enqueued onto the state's future queue as a
         * completed future. They will be folded when the unit is restarted.
         */
        public void stop() {
            isStopped = true;
        }

        /**
         * Computes a new state by folding an action into the current state.
         *
         * @param action The action to fold.
         * @param view A channel for side effects.
         * @param worker Provides a thread to execute async calls in
         */
        public void apply(A action, V view, Executor worker) {
            apply(action, new WeakReference<>(view), worker);
        }

        protected void apply(A action, WeakReference<V> viewRef, Executor worker) {
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
                            A nextAction = future.get();
                            backlog.didProduceAction();
                            apply(nextAction, viewRef, worker);
                        } catch (InterruptedException | ExecutionException e) {
                            handle(e, viewRef.get());
                        }
                    });
                }
            }
        }

    }

}
