package ph.codeia.todo.util;

import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;

import ph.codeia.todo.Mvp;

public class AndroidUnit<
        S extends Mvp.State<S, A>,
        A extends Mvp.Action<S, A, V>,
        V>
extends Mvp.Unit<S, A, V> {

    private final Handler handler;
    private final Mvp.ErrorHandler<V> errorHandler;

    public AndroidUnit(S state) {
        this(state, null);
    }

    public AndroidUnit(S state, Mvp.ErrorHandler<V> errorHandler) {
        this(new Handler(Looper.getMainLooper()), state, errorHandler);
    }

    public AndroidUnit(Handler handler, S state, Mvp.ErrorHandler<V> errorHandler) {
        super(state);
        this.handler = handler;
        this.errorHandler = errorHandler;
    }

    @Override
    public void apply(A action, WeakReference<V> viewRef, Executor worker) {
        main(() -> super.apply(action, viewRef, worker));
    }

    @Override
    public void handle(Throwable e, V view) {
        if (errorHandler != null && view != null) {
            errorHandler.handle(e, view);
        } else if (!isStopped) {
            main(() -> {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new RuntimeException(e);
                }
            });
        } else {
            e.printStackTrace();
        }
    }

    private void main(Runnable block) {
        if (Thread.currentThread() == handler.getLooper().getThread()) {
            block.run();
        } else {
            handler.post(block);
        }
    }

}
