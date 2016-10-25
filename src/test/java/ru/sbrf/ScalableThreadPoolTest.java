package ru.sbrf;

import org.junit.Assert;
import org.junit.Test;

import java.util.stream.IntStream;

/**
 * Created by Ivan on 21/10/16.
 */
public class ScalableThreadPoolTest {
    @Test(timeout = 2200)
    public void startBeforeExecute() throws Exception {
        ThreadPool pool = new ScalableThreadPool(1, 100);
        Counter cc = new Counter();

        pool.start();

        for (int i = 0; i < 100; ++i)
            pool.execute(() -> {
                try {
                    cc.inc();
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

        pool.join();

        Assert.assertTrue(cc.get() == 100);
    }

    @Test(timeout = 2200)
    public void startAfterExecute() throws Exception {
        ThreadPool pool = new ScalableThreadPool(1, 100);
        Counter cc = new Counter();

        for (int i = 0; i < 100; ++i)
            pool.execute(() -> {
                try {
                    cc.inc();
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

        pool.start();
        pool.join();

        Assert.assertTrue(cc.get() == 100);
    }

    @Test(timeout = 1500)
    public void testAdditionalThreadsDeaths() throws Exception {
        ThreadPool pool = new ScalableThreadPool(1, 2);

        class ThreadHolder {
            Thread firstThread;
            Thread secondThread;
        }

        final ThreadHolder holder = new ThreadHolder();

        pool.start();

        pool.execute(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        pool.execute(() -> {
            holder.firstThread = Thread.currentThread();
        });

        Thread.sleep(100);

        Assert.assertFalse(holder.firstThread.isAlive());

        pool.execute(() -> {
            holder.secondThread = Thread.currentThread();
        });

        Thread.sleep(100);

        Assert.assertFalse(holder.firstThread == holder.secondThread);

        pool.join();
    }

    @Test(timeout = 11200)
    public void moreTasksThenThreads() throws Exception {
        ThreadPool pool = new ScalableThreadPool(2, 10);

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
}