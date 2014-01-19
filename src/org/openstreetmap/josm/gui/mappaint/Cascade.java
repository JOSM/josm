// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.Utils.equal;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.mappaint.mapcss.CSSColors;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.Utils;

/**
 * Simple map of properties with dynamic typing.
 */
public final class Cascade implements Cloneable {

    public static final Cascade EMPTY_CASCADE = new Cascade();

    protected Map<String, Object> prop = new HashMap<String, Object>();

    private final static Pattern HEX_COLOR_PATTERN = Pattern.compile("#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})");

    public <T> T get(String key, T def, Class<T> klass) {
        return get(key, def, klass, false);
    }

    /**
     * Get value for the given key
     * @param <T> the expected type
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
                Main.warn(String.format("Unable to convert property %s to type %s: found %s of type %s!", key, klass, o, o.getClass()));
            }
            return def;
        } else
            return res;
    }

    public Object get(String key) {
        return prop.get(key);
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

        if (klass == float[].class)
            return (T) toFloatArray(o);

        if (klass == Color.class)
            return (T) toColor(o);

        if (klass == String.class) {
            if (o instanceof Keyword)
                return (T) ((Keyword) o).val;

            return (T) o.toString();
        }

        return null;
    }

    private static Float toFloat(Object o) {
        if (o instanceof Number)
            return ((Number) o).floatValue();
        if (o instanceof String && !((String) o).isEmpty()) {
            try {
                return Float.parseFloat((String) o);
            } catch (NumberFormatException e) {
                Main.debug("'"+o+"' cannot be converted to float");
            }
        }
        return null;
    }

    private static Boolean toBool(Object o) {
        if (o instanceof Boolean)
            return (Boolean) o;
        if (o instanceof Keyword) {
            String s = ((Keyword) o).val;
            if (equal(s, "true") || equal(s, "yes"))
                return true;
            if (equal(s, "false") || equal(s, "no"))
                return false;
        }
        return null;
    }

    private static float[] toFloatArray(Object o) {
        if (o instanceof float[])
            return (float[]) o;
        if (o instanceof List) {
            List<?> l = (List<?>) o;
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

    private static Color toColor(Object o) {
        if (o instanceof Color)
            return (Color) o;
        if (o instanceof Keyword)
            return CSSColors.get(((Keyword) o).val);
        if (o instanceof String) {
            Color c = CSSColors.get((String) o);
            if (c != null)
                return c;
            if (HEX_COLOR_PATTERN.matcher((String) o).matches()) {
                return ColorHelper.html2color((String) o);
            }
        }
        return null;
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
