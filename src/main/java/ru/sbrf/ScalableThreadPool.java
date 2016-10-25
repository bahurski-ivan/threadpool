package ru.sbrf;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Ivan on 21/10/16.
 */
public class ScalableThreadPool implements ThreadPool {

    private final List<Thread> threads;
    private final int maximumThreadCount;
    private final int minimumThreadCount;
    private final LinkedList<Runnable> tasks = new LinkedList<>();
    private int workingThreadCount;
    private volatile PoolState state = PoolState.START;

    ScalableThreadPool(int minimumThreadCount, int maximumThreadCount) {
        threads = new ArrayList<>(minimumThreadCount);

        this.minimumThreadCount = minimumThreadCount;
        this.maximumThreadCount = maximumThreadCount;

        for (int i = 0; i < minimumThreadCount; ++i)
            threads.add(new Thread(new ThreadWorker()));
    }

    @Override
    public void start() {
        if (state != PoolState.START)
            return;

        state = PoolState.AT_WORK;

        synchronized (threads) {
            threads.forEach(Thread::start);
        }
    }

    @Override
    public void execute(Runnable task) {
        if (state == PoolState.DEAD)
            throw new IllegalStateException("execute called on dead thread pool");

        synchronized (tasks) {
            synchronized (threads) {
                if (threads.size() < maximumThreadCount && (!tasks.isEmpty() || workingThreadCount == threads.size())) {
                    Thread additionalThread = new Thread(new ThreadWorker());
                    threads.add(additionalThread);
                    if (state != PoolState.START)
                        additionalThread.start();
                }
            }

            tasks.add(task);
            tasks.notify();
        }
    }

    @Override
    public void join() throws InterruptedException {
        if (state != PoolState.AT_WORK)
            return;

        state = PoolState.JOIN;

        synchronized (tasks) {
            tasks.notifyAll();
        }

        while (true) {
            Thread thread;

            synchronized (threads) {
                if (!threads.isEmpty())
                    thread = threads.get(0);
                else
                    break;
            }

            thread.join();
        }

        state = PoolState.DEAD;
    }

    private class ThreadWorker implements Runnable {
        private boolean wasWorking = false;

        @Override
        public void run() {
            for (Runnable nextTask; (nextTask = next()) != null; )
                nextTask.run();

            synchronized (threads) {
                threads.remove(Thread.currentThread());
            }
        }

        private Runnable next() {
            Runnable result = null;
            boolean artificialStop = false;

            synchronized (tasks) {
                while (tasks.isEmpty() && !artificialStop) {
                    if (tryToSelfRemove() || state != PoolState.AT_WORK)
                        artificialStop = true;
                    else
                        try {
                            if (wasWorking) {
                                wasWorking = false;
                                --workingThreadCount;
                            }
                            tasks.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            artificialStop = true;
                        }
                }

                if (!artificialStop) {
                    result = tasks.removeFirst();
                    ++workingThreadCount;
                    wasWorking = true;
                }
            }

            return result;
        }

        private boolean tryToSelfRemove() {
            boolean result = false;

            synchronized (threads) {
                if (threads.size() > minimumThreadCount) {
                    threads.remove(Thread.currentThread());
                    result = true;
                }
            }

            return result;
        }
    }

}
