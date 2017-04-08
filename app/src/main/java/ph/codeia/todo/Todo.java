package ph.codeia.todo;

import android.app.Application;
import android.content.Context;
import android.widget.Toast;

import com.squareup.leakcanary.LeakCanary;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import ph.codeia.todo.data.TodoInMemory;
import ph.codeia.todo.data.TodoRepository;
import ph.codeia.todo.data.TodoSerialized;

public class Todo extends Application {

    public interface Component {
        Executor io();
        Executor compute();
        TodoRepository todoRepository(Context context);
    }

    public static final Component GLOBALS = new Component() {
        Executor io;
        Executor compute;
        TodoRepository repo;

        @Override
        public synchronized Executor io() {
            if (io == null) {
                io = Executors.newCachedThreadPool();
            }
            return io;
        }

        @Override
        public synchronized Executor compute() {
            if (compute == null) {
                compute = Executors.newSingleThreadExecutor();
            }
            return compute;
        }

        @Override
        public synchronized TodoRepository todoRepository(Context context) {
            if (repo == null) {
                try {
                    repo = new TodoSerialized(new File(context.getCacheDir(), "todos"));
                } catch (ClassNotFoundException | IOException e) {
                    Toast.makeText(context, "couldn't read/create cache file", Toast.LENGTH_SHORT)
                            .show();
                    repo = new TodoInMemory();
                }
            }
            return repo;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);
    }
}
