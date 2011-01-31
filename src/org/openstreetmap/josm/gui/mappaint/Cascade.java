// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple map of properties with dynamic typing.
 */
public class Cascade {
    
    public static final Cascade EMPTY_CASCADE = new Cascade();

    protected Map<String, Object> prop = new HashMap<String, Object>();

    /**
     * Get value for the given key
     * @param key the key
     * @param def default value, can be null
     * @param klass the same as T
     * @return if a value with class klass has been mapped to key, returns this
     *      value, def otherwise
     */
    public <T> T get(String key, T def, Class klass) {
        if (def != null && !klass.isInstance(def))
            throw new IllegalArgumentException();
        Object o = prop.get(key);
        if (o == null)
            return def;
        if (klass.isInstance(o)) {
            @SuppressWarnings("unchecked") T res = (T) klass.cast(o);
            return res;
        }
        System.err.println(String.format("Warning: wrong type for mappaint property %s: %s expected, but %s found!", key, klass, o.getClass()));
        return def;
    }

    public void put(String key, Object val) {
        prop.put(key, val);
    }

    public void putOrClear(String key, Object val) {
        if (val != null) {
            prop.put(key, val);
        } else {
            prop.remove(key);
        }
    }

    public void remove(String key) {
        prop.remove(key);
    }
}
