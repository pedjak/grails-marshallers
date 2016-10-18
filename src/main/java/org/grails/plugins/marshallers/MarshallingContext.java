package org.grails.plugins.marshallers;

import org.apache.commons.lang.NotImplementedException;

import java.lang.ref.WeakReference;
import java.util.*;

/**
 * Context data container backed by threadlocal variable. Stored context values are wrapped in weak references in
 * order to prevent memory leak
 *
 * @author Denis Halupa
 */
public class MarshallingContext implements Map<String, Object> {
    private ThreadLocal<Map<String, WeakReference>> instance = new ThreadLocal<Map<String, WeakReference>>() {
        @Override
        protected Map<String, WeakReference> initialValue() {
            return new HashMap<String, WeakReference>();
        }
    };

    @Override
    public int size() {
        return instance.get().size();
    }

    @Override
    public boolean isEmpty() {
        return instance.get().isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return instance.get().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        throw new NotImplementedException("not implemented for marshalling context");
    }

    @Override
    public Object get(Object key) {
        WeakReference r = instance.get().get(key);
        return r.get();
    }

    @Override
    public Object put(String key, Object value) {
        WeakReference r = new WeakReference(value);
        return instance.get().put(key, r);
    }

    @Override
    public Object remove(Object key) {
        return instance.get().remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        throw new NotImplementedException("not implemented for marshalling context");
    }


    @Override
    public void clear() {
        instance.get().clear();
    }

    @Override
    public Set<String> keySet() {
        throw new NotImplementedException("not implemented for marshalling context");
    }

    @Override
    public Collection<Object> values() {
        throw new NotImplementedException("not implemented for marshalling context");
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        throw new NotImplementedException("not implemented for marshalling context");
    }
}
