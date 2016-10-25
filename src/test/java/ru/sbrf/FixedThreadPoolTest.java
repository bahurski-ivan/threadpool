package ru.sbrf;

import org.junit.Assert;
import org.junit.Test;

import java.util.stream.IntStream;

/**
 * Created by Ivan on 21/10/16.
 */
public class FixedThreadPoolTest {
    @Test(timeout = 11000)
    public void startAfterTasks() throws Exception {
        ThreadPool pool = new FixedThreadPool(10);

        final Counter cc = new Counter();

        IntStream.range(0, 100)
                .forEach((i) -> {
                    pool.execute(() -> {
                        cc.inc();
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    });
                });

        pool.start();
        pool.join();

        Assert.assertTrue(cc.get() == 100);
    }

    @Test(timeout = 11000)
    public void startBeforeTasks() throws Exception {
        ThreadPool pool = new FixedThreadPool(10);

        pool.start();

        final Counter cc = new Counter();

        IntStream.range(0, 100)
                .forEach((i) -> {
                    pool.execute(() -> {
                        cc.inc();
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    });
                });

        pool.join();

        Assert.assertTrue(cc.get() == 100);
    }

}