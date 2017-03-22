package ph.codeia.todo;

import org.junit.AfterClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BacklogTest {
    private static ExecutorService BG = Executors.newCachedThreadPool();

    @AfterClass
    public static void tearDown() {
        BG.shutdown();
    }

    @Test(timeout = 10000)
    public void await_blocks_until_all_futures_are_completed() throws InterruptedException {
        Mvp.Backlog b = new Mvp.Backlog();
        CountDownLatch barrier = new CountDownLatch(1);
        b.willProduceAction();
        b.willProduceAction();
        b.willProduceAction();
        BG.execute(() -> {
            try {
                out("starting: 1");
                Thread.sleep(1000);
                out("starting: 2");
                Thread.sleep(1000);
                out("starting: 3");
                Thread.sleep(1000);
                out("done: 3");
                b.didProduceAction();
                out("done: 2");
                b.didProduceAction();
                out("done: 1");
                b.didProduceAction();
                b.willProduceAction();
                barrier.countDown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        BG.execute(() -> {
            try {
                barrier.await();
                out("starting: 4");
                Thread.sleep(1000);
                out("done: 4");
                b.didProduceAction();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        b.awaitCleared();
        out("all done.");
    }

    void out(String s, Object... fmtArgs) {
        System.out.println(String.format(s, fmtArgs));
    }
}
