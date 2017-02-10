package ph.codeia.todo.util;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import java.util.concurrent.Executor;

import ph.codeia.todo.Mvp;

public class AndroidUnit<
        S extends Mvp.State<S, A>,
        A extends Mvp.Action<S, A, V>,
        V>
        extends Mvp.Unit<S, A, V> {

    private final Context context;
    private final Handler handler;
    private final Mvp.ErrorHandler<V> errorHandler;

    public AndroidUnit(Context context, S state) {
        this(context, state, null);
    }

    public AndroidUnit(Context context, S state, Mvp.ErrorHandler<V> errorHandler) {
        super(state);
        this.context = context;
        this.errorHandler = errorHandler;
        handler = new Handler(context.getMainLooper());
    }

    @Override
    public void apply(A action, V view, Executor worker) {
        main(() -> super.apply(action, view, worker));
    }

    @Override
    public void handle(Throwable e, V view) {
        if (errorHandler != null) {
            errorHandler.handle(e, view);
        } else {
            if (!isStopped) {
                main(() -> {
                    if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    } else {
                        throw new RuntimeException(e);
                    }
                });
            }
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
