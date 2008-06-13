package org.codehaus.groovy.reflection;

import java.lang.ref.SoftReference;

/**
 * Soft reference with lazy initialization under lock
 */
public abstract class LazySoftReference<T> extends LockableObject {
    private FinalizableRef.SoftRef<T> value;

    public T get() {
        SoftReference<T> resRef = value;
        T res;
        if (resRef != null && (res = resRef.get()) != null)
            return res;

        lock ();
        try {
            res = initValue();
            value = new MySoftRef<T>(res);
            return res;
        }
        finally {
            unlock();
        }
    }

    public void set (T newVal) {
        value = new MySoftRef<T>(newVal);
    }

    public T getNullable() {
        SoftReference<T> resRef = value;
        T res;
        if (resRef == null || (res = resRef.get()) == null) {
            return null;
        }
        return res;
    }

    public abstract T initValue();

    protected void finalizeRef() {
        value = null;
    }

    private class MySoftRef<T> extends FinalizableRef.SoftRef<T> {
        public MySoftRef(T res) {
            super(res);
        }

        public void finalizeRef() {
            LazySoftReference.this.finalizeRef();
        }
    }
}
