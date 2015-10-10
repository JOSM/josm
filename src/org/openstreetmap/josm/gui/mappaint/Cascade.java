// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

    private Map<String, Object> prop = new HashMap<>();

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("#([0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})");

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
            throw new IllegalArgumentException(def+" is not an instance of "+klass);
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
            if (o instanceof Color) {
                Color c = (Color) o;
                int alpha = c.getAlpha();
                if (alpha != 255)
                    return (T) String.format("#%06x%02x", ((Color) o).getRGB() & 0x00ffffff, alpha);
                return (T) String.format("#%06x", ((Color) o).getRGB() & 0x00ffffff);

            }

            return (T) o.toString();
        }

        return null;
    }

    private static Float toFloat(Object o) {
        if (o instanceof Number)
            return ((Number) o).floatValue();
        if (o instanceof String && !((String) o).isEmpty()) {
            try {
                return Float.valueOf((String) o);
            } catch (NumberFormatException e) {
                if (Main.isDebugEnabled()) {
                    Main.debug("'"+o+"' cannot be converted to float");
                }
            }
        }
        return null;
    }

    private static Boolean toBool(Object o) {
        if (o instanceof Boolean)
            return (Boolean) o;
        String s = null;
        if (o instanceof Keyword) {
            s = ((Keyword) o).val;
        } else if (o instanceof String) {
            s = (String) o;
        }
        if (s != null)
            return !(s.isEmpty() || "false".equals(s) || "no".equals(s) || "0".equals(s) || "0.0".equals(s));
        if (o instanceof Number)
            return ((Number) o).floatValue() != 0;
        if (o instanceof List)
            return !((List) o).isEmpty();
        if (o instanceof float[])
            return ((float[]) o).length != 0;

        return null;
    }

    private static float[] toFloatArray(Object o) {
        if (o instanceof float[])
            return (float[]) o;
        if (o instanceof List) {
            List<?> l = (List<?>) o;
            float[] a = new float[l.size()];
            for (int i = 0; i < l.size(); ++i) {
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
            return new float[] {f};
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
        Map<String, Object> clonedProp = (Map<String, Object>) ((HashMap) this.prop).clone();
        Cascade c = new Cascade();
        c.prop = clonedProp;
        return c;
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder("Cascade{ ");
        for (Entry<String, Object> entry : prop.entrySet()) {
            res.append(entry.getKey()+':');
            Object val = entry.getValue();
            if (val instanceof float[]) {
                res.append(Arrays.toString((float[]) val));
            } else if (val instanceof Color) {
                res.append(Utils.toString((Color) val));
            } else if (val != null) {
                res.append(val);
            }
            res.append("; ");
        }
        return res.append('}').toString();
    }

    public boolean containsKey(String key) {
        return prop.containsKey(key);
    }
}
