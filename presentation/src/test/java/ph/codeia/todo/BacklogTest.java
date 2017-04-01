package ph.codeia.todo;

import org.junit.AfterClass;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class BacklogTest {
    private static ExecutorService BG = Executors.newCachedThreadPool();
    private static Random RAND = new Random();

    @AfterClass
    public static void tearDown() {
        BG.shutdown();
    }

    @Test(timeout = 30000)
    public void await_blocks_until_all_futures_are_completed() throws InterruptedException {
        Mvp.Backlog jobs = new Mvp.Backlog();
        AtomicInteger c = new AtomicInteger();
        int n = 10;
        IntStream.range(0, n).forEach(i -> {
            jobs.willProduceAction();
            out("started: %d", i);
            long start = System.currentTimeMillis();
            BG.execute(() -> {
                fuzz(10);
                out("done: %d +%dms @%s",
                        i,
                        System.currentTimeMillis() - start,
                        Thread.currentThread().getName());
                c.incrementAndGet();
                jobs.didProduceAction();
            });
        });
        jobs.awaitCleared();
        assertEquals(n, c.get());
    }

    void out(String s, Object... fmtArgs) {
        System.out.println(String.format(s, fmtArgs));
    }

    static void fuzz(int maxQuarterSeconds) {
        try {
            Thread.sleep((RAND.nextInt(maxQuarterSeconds) + 1) * 250);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
