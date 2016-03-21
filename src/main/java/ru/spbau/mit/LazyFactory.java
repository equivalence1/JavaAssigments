package ru.spbau.mit;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Supplier;

/**
 * Created by equi on 13.02.16.
 *
 * @author Kravchenko Dima
 */
public class LazyFactory {
    public <T> Lazy<T> createLazyOneThread(final Supplier<T> supplier) {
        return new LazyOneThread<>(supplier);
    }

    public <T> Lazy<T> createLazyMultiThread(final Supplier<T> supplier) {
        return new LazyMultiThread<>(supplier);
    }

    public <T> Lazy<T> createLazyLockFree(final Supplier<T> supplier) {
        return new LazyLockFree<>(supplier);
    }

    private static class LazyOneThread<T> implements Lazy<T> {
        private T mValue;
        private Supplier<T> mSupplier;

        public LazyOneThread(Supplier<T> supplier) {
            mSupplier = supplier;
        }

        public T get() {
            if (mSupplier != null) {
                mValue = mSupplier.get();
                mSupplier = null;
            }

            return mValue;
        }
    }

    private static class LazyMultiThread<T> implements Lazy<T> {
        private volatile T mValue;
        private volatile Supplier<T> mSupplier;

        public LazyMultiThread(Supplier<T> supplier) {
            mSupplier = supplier;
        }

        public T get() {
            if (mSupplier != null) {
                synchronized (this) {
                    if (mSupplier == null) {
                        mValue = mSupplier.get();
                        mSupplier = null;
                    }
                }
            }

            return mValue;
        }
    }

    private static class LazyLockFree<T> implements Lazy<T> {
        private static final Object NONE = new Object();
        private static final
                AtomicReferenceFieldUpdater<LazyLockFree, Object> mUpdater =
                        AtomicReferenceFieldUpdater.newUpdater(
                                LazyLockFree.class, Object.class, "mValue");
        private volatile Object mValue = NONE;
        private volatile Supplier<T> mSupplier;

        public LazyLockFree(Supplier<T> supplier) {
            mSupplier = supplier;
        }

        @SuppressWarnings("unchecked")
        public T get() {
            if (mSupplier != null) {
                if (mUpdater.compareAndSet(this, NONE, mSupplier.get())) {
                    mSupplier = null;
                }
            }

            return (T) mValue;
        }
    }
}
