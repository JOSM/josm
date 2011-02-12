// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.gui.mappaint.mapcss.CSSColors;
import org.openstreetmap.josm.tools.Utils;

/**
 * Simple map of properties with dynamic typing.
 */
public class Cascade implements Cloneable {
    
    public static final Cascade EMPTY_CASCADE = new Cascade();

    protected Map<String, Object> prop = new HashMap<String, Object>();

    public <T> T get(String key, T def, Class<T> klass) {
        return get(key, def, klass, false);
    }
    
    /**
     * Get value for the given key
     * @param key the key
     * @param def default value, can be null
     * @param klass the same as T
     * @param suppressWarnings show or don't show a warning when some value is
     *      found, but cannot be converted to the requested type
     * @return if a value with class klass has been mapped to key, returns this
     *      value, def otherwise
     */
    public <T> T get(String key, T def, Class<T> klass, boolean suppressWarnings) {
        if (def != null && !klass.isInstance(def))
            throw new IllegalArgumentException();
        Object o = prop.get(key);
        if (o == null)
            return def;
        T res = convertTo(o, klass);
        if (res == null) {
            if (!suppressWarnings) {
                System.err.println(String.format("Warning: unable to convert property %s to type %s: found %s of type %s!", key, klass, o, o.getClass()));
            }
            return def;
        } else
            return res;
    }

    public Object get(String key) {
        return prop.get(key);
    }

    @SuppressWarnings("unchecked")
    public static <T> T convertTo(Object o, Class<T> klass) {
        if (o == null)
            return null;
        if (klass.isInstance(o))
            return (T) o;

        if (klass == float.class || klass == Float.class)
            return (T) toFloat(o);

        if (klass == double.class || klass == Double.class) {
            o = toFloat(o);
            if (o != null) {
                o = new Double((Float) o);
            }
            return (T) o;
        }

        if (klass == boolean.class || klass == Boolean.class)
            return (T) toBool(o);

        if (klass == float[].class) {
            return (T) toFloatArray(o);
        }

        if (klass == Color.class) {
            return (T) toColor(o);
        }
        return null;
    }

    private static Float toFloat(Object o) {
        if (o instanceof Float)
            return (Float) o;
        if (o instanceof Double)
            return new Float((Double) o);
        if (o instanceof Integer)
            return new Float((Integer) o);
        if (o instanceof String) {
            try {
                float f = Float.parseFloat((String) o);
                return f;
            } catch (NumberFormatException e) {
            }
        }
        return null;
    }

    private static Boolean toBool(Object o) {
        if (o instanceof Boolean)
            return (Boolean) o;
        if (o instanceof String)
            return Boolean.parseBoolean((String) o);
        return null;
    }

    private static float[] toFloatArray(Object o) {
        if (o instanceof float[])
            return (float[]) o;
        if (o instanceof List) {
            List l = (List) o;
            float[] a = new float[l.size()];
            for (int i=0; i<l.size(); ++i) {
                Float f = toFloat(l.get(i));
                if (f == null)
                    return null;
                else
                    a[i] = f;
            }
            return a;
        }
        Float f = toFloat(o);
        if (f != null)
            return new float[] { f };
        return null;
    }

     public static Color toColor(Object o) {
        if (o instanceof Color)
            return (Color) o;
        if (o instanceof String)
            return CSSColors.get((String) o);
        return null;
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

    @Override
    public Cascade clone() {
        @SuppressWarnings("unchecked") 
        HashMap<String, Object> clonedProp = (HashMap) ((HashMap) this.prop).clone();
        Cascade c = new Cascade();
        c.prop = clonedProp;
        return c;
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder("Cascade{ ");
        for (String key : prop.keySet()) {
            res.append(key+":");
            Object val = prop.get(key);
            if (val instanceof float[]) {
                res.append(Arrays.toString((float[]) val));
            } else if (val instanceof Color) {
                res.append(Utils.toString((Color)val));
            } else {
                res.append(val+"");
            }
            res.append("; ");
        }
        return res.append("}").toString();
    }

    public boolean containsKey(String key) {
        return prop.containsKey(key);
    }
}
