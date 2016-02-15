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
    private LazyFactory lazyFactory = new LazyFactory();

    @Test
    public void testMultiThreadSimple() {
        Supplier supp = makeSupplier();
        Lazy lazy = lazyFactory.createLazyMultiThread(supp);
        Thread[] threads = new Thread[100];

        final List<Object> lazyResults = new ArrayList<>();

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

        checkAllEquals(lazyResults);
    }

    @Test
    public void testMultiThreadWithNull() {
        Supplier supp = makeNullSupplier();
        Lazy lazy = lazyFactory.createLazyMultiThread(supp);
        Thread[] threads = new Thread[100];

        final List<Object> lazyResults = new ArrayList<>();

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

        checkAllEquals(lazyResults);
    }

    @Test
    public void testLockFree() {
        Supplier supp = makeSupplier();
        Lazy lazy = lazyFactory.createLazyLockFree(supp);
        Thread[] threads = new Thread[100];

        final List<Object> lazyResults = new ArrayList<>();

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

        checkAllEquals(lazyResults);
    }

    @Test
    public void testLockFreeWithNull() {
        Supplier supp = makeNullSupplier();
        Lazy lazy = lazyFactory.createLazyLockFree(supp);
        Thread[] threads = new Thread[100];

        final List<Object> lazyResults = new ArrayList<>();

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

        checkAllEquals(lazyResults);
    }

    private void checkAllEquals(List<Object> lazyResults) {
        if (lazyResults.get(0) != null) {
            for (Object obj : lazyResults) {
                assertTrue(obj.equals(lazyResults.get(0)));
            }
        } else {
            for (Object obj : lazyResults) {
                assertTrue(obj == null);
            }
        }
    }

    private Supplier makeSupplier() {
        return new Supplier() {
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

    private Supplier makeNullSupplier() {
        return new Supplier() {
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
