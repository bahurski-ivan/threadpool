package ru.sbrf;

import java.util.Arrays;
import java.util.LinkedList;

/**
 * Created by Ivan on 21/10/16.
 */
public class FixedThreadPool implements ThreadPool {
    private final Thread[] threads;
    private final LinkedList<Runnable> tasks = new LinkedList<>();
    private volatile PoolState state;

    FixedThreadPool(int threadCount) {
        final Runnable threadWorker = () -> {
            Runnable nextTask;
            boolean artificialStop = false;

            do {
                nextTask = null;

                synchronized (tasks) {
                    while (tasks.isEmpty() && !artificialStop) {
                        if (state != PoolState.AT_WORK) {
                            artificialStop = true;
                        } else {
                            try {
                                tasks.wait();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                artificialStop = true;
                            }
                        }
                    }

                    nextTask = artificialStop ? null : tasks.removeFirst();
                }

                if (nextTask != null)
                    nextTask.run();

            } while (nextTask != null);
        };

        threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; ++i)
            threads[i] = new Thread(threadWorker);

        state = PoolState.START;
    }

    @Override
    public void start() {
        if (state != PoolState.START)
            return;

        state = PoolState.AT_WORK;
        Arrays.stream(threads).forEach(Thread::start);
    }

    @Override
    public void execute(Runnable task) throws IllegalStateException {
        if (state == PoolState.DEAD)
            throw new IllegalStateException("execute called on dead thread pool");

        synchronized (tasks) {
            tasks.add(task);
            tasks.notify();
        }
    }

    @Override
    public void join() throws InterruptedException {
        if (state != PoolState.AT_WORK)
            return;

        synchronized (tasks) {
            tasks.notifyAll();
        }

        state = PoolState.JOIN;
        for (Thread t : threads)
            t.join();
    }
}
