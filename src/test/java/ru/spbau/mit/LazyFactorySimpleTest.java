package ru.spbau.mit;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.function.Supplier;

/**
 * Created by equi on 13.02.16.
 *
 * @author Kravchenko Dima
 */
public class LazyFactorySimpleTest {
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
        Lazy lazy = lazyFactory.createLazyOneThread(supp);

        for (int i = 0; i < 100; i++) {
            assertEquals(1, lazy.get());
        }
    }

    @Test
    public void testLockFree() {
        Supplier supp = makeSupplier();
        Lazy lazy = lazyFactory.createLazyOneThread(supp);

        for (int i = 0; i < 100; i++) {
            assertEquals(1, lazy.get());
        }
    }

    private Supplier makeSupplier() {
        return new Supplier() {
            private int cnt = 0;

            @Override
            public Integer get() {
                return ++cnt;
            }
        };
    }
}
