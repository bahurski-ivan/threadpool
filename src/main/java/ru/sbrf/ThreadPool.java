package ru.sbrf;

/**
 * Created by Ivan on 21/10/16.
 */
public interface ThreadPool {
    void start();

    void execute(Runnable task);

    void join() throws InterruptedException;
}
