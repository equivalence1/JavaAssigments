package ru.spbau.mit;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Created by equi on 13.02.16.
 *
 * @author Kravchenko Dima
 */
public class LazyFactoryRaceTest {
    private final LazyFactory mLazyFactory = new LazyFactory();

    @Test
    public void testMultiThread() {
        Supplier<Integer> supp = makeSupplier();
        Lazy<Integer> lazy = mLazyFactory.createLazyMultiThread(supp);
        Thread[] threads = new Thread[100];
        final List<Object> lazyResults = new ArrayList<>();

        createThreadsAndCompute(threads, lazy, lazyResults);
        checkAllEquals(lazyResults);
    }

    @Test
    public void testMultiThreadWithNull() {
        Supplier<Integer> supp = makeNullSupplier();
        Lazy<Integer> lazy = mLazyFactory.createLazyMultiThread(supp);
        Thread[] threads = new Thread[100];
        final List<Object> lazyResults = new ArrayList<>();

        createThreadsAndCompute(threads, lazy, lazyResults);
        checkAllEquals(lazyResults);
    }

    @Test
    public void testLockFree() {
        Supplier<Integer> supp = makeSupplier();
        Lazy<Integer> lazy = mLazyFactory.createLazyLockFree(supp);
        Thread[] threads = new Thread[100];
        final List<Object> lazyResults = new ArrayList<>();

        createThreadsAndCompute(threads, lazy, lazyResults);
        checkAllEquals(lazyResults);
    }

    @Test
    public void testLockFreeWithNull() {
        Supplier<Integer> supp = makeNullSupplier();
        Lazy<Integer> lazy = mLazyFactory.createLazyLockFree(supp);
        Thread[] threads = new Thread[100];
        final List<Object> lazyResults = new ArrayList<>();

        createThreadsAndCompute(threads, lazy, lazyResults);
        checkAllEquals(lazyResults);
    }

    private void createThreadsAndCompute(Thread[] threads, Lazy lazy,
                                      List<Object> lazyResults) {
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 5; j++) {
                    Object obj = lazy.get();
                    synchronized (lazyResults) {
                        lazyResults.add(obj);
                    }
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                //ignoring
            }
        }
    }

    private void checkAllEquals(List<Object> lazyResults) {
        for (Object obj : lazyResults) {
            assertTrue(obj == lazyResults.get(0));
            /**
             * I use here assertTrue instead of assertEquals on purpose.
             * If I used assertEquals it would invoke method .equals()
             * but in this case I need not just test that they are equal
             * but test that they reference to the exactly same object.
             */
        }
    }

    private Supplier<Integer> makeSupplier() {
        return new Supplier<Integer>() {
            private int cnt = 0;
            private boolean was = false;

            @Override
            public Integer get() {
                try {
                    /**
                     * for better testing I want first Thread to wait longer than others
                     */
                    if (was)
                        Thread.sleep(1000);
                    else {
                        was = true;
                        Thread.sleep(2000);
                    }
                } catch (InterruptedException e) {
                    //ignoring
                }
                return ++cnt;
            }
        };
    }

    private Supplier<Integer> makeNullSupplier() {
        return new Supplier<Integer>() {
            private boolean was = false;

            @Override
            public Integer get() {
                try {
                    /**
                     * for better testing I want first Thread to wait longer than others
                     */
                    if (was)
                        Thread.sleep(1000);
                    else {
                        was = true;
                        Thread.sleep(2000);
                    }
                } catch (InterruptedException e) {
                    //ignoring
                }
                return null;
            }
        };
    }
}
