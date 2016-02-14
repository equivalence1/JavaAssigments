package ru.spbau.mit;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Created by equi on 13.02.16.
 *
 * @author Kravchenko Dima
 */
public class LazyFactory {
    public <T> Lazy<T> createLazyOneThread(final Supplier<T> supplier) {
        return new Lazy<T>() {
            private T toReturn = null;
            private boolean isCalculated = false;

            @Override
            public T get() {
                if (!isCalculated) {
                    toReturn = supplier.get();
                    isCalculated = true;
                    return toReturn;
                } else
                    return toReturn;
            }
        };
    }

    public <T> Lazy<T> createLazyMultiThread(final Supplier<T> supplier) {
        return new Lazy<T>() {
            private T toReturn = null;
            private volatile boolean isCalculated = false;

            @Override
            public T get() {
                if (!isCalculated) {
                    synchronized (this) {
                        if (!isCalculated) {
                            toReturn = supplier.get();
                            isCalculated = true;
                        }
                    }
                }
                return toReturn;
            }
        };
    }

    public <T> Lazy<T> createLazyLockFree(final Supplier<T> supplier) {
        return new Lazy<T>() {
            private AtomicReference<T> toReturnRef = new AtomicReference<>();
            private AtomicInteger isCalculated = new AtomicInteger(0);
            /**
             * 0 -- not calculated, 1 -- in progress, 2 -- calculated
             */

            @Override
            public T get() {
                if (isCalculated.get() == 0) {
                    T oldValue = toReturnRef.get();
                    T newValue = supplier.get();

                    if (isCalculated.compareAndSet(0, 1)) {
                        toReturnRef.compareAndSet(oldValue, newValue);
                        isCalculated.set(2);
                    }
                }
                while (isCalculated.get() == 1) {
                    Thread.yield();
                    /**
                     * We need to wait. Calculation is in progress.
                     * It could be done better with Condition (I could `await`
                     * here and `signal` right after setting `isCalculated` to 2)
                     * but we have restriction -- only 2 references per
                     * instance of `Lazy` class
                     */
                }
                return toReturnRef.get();
            }
        };
    }
}
