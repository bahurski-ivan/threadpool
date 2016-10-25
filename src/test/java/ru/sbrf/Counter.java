package ru.sbrf;

/**
 * Created by Ivan on 26/10/16.
 */
class Counter {
    private int count;

    synchronized void inc() {
        ++count;
    }

    int get() {
        return count;
    }
}