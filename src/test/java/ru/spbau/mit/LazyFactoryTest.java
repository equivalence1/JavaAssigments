package ru.spbau.mit;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.function.Supplier;

/**
 * Created by equi on 13.02.16.
 *
 * @author Kravchenko Dima
 */
public class LazyFactoryTest {
    private LazyFactory lazyFactory = new LazyFactory();

    @Test
    public void testOneThread() {
        Supplier supp = makeSupplier();
        Lazy lazy = lazyFactory.createLazyOneThread(supp);

        for (int i = 0; i < 100; i++) {
            assertEquals(1, lazy.get());
        }
    }

    @Test
    public void testMultiThread() {
        Supplier supp = makeSupplier();
        Lazy lazy = lazyFactory.createLazyMultiThread(supp);
        Thread[] threads = new Thread[100];

        for (int i = 0; i < 100; i++) {
            threads[i] = new Thread(() -> {
               assertEquals(1, lazy.get());
            });
            threads[i].start();
        }

        for (int i = 0; i < 100; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                //ignoring
            }
        }
    }

    @Test
    public void testLockFree() {
        Supplier supp = makeSupplier();
        Lazy lazy = lazyFactory.createLazyLockFree(supp);
        Thread[] threads = new Thread[100];

        for (int i = 0; i < 100; i++) {
            threads[i] = new Thread(() -> {
                assertEquals(1, lazy.get());
            });
            threads[i].start();
        }

        for (int i = 0; i < 100; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                //ignoring
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
}