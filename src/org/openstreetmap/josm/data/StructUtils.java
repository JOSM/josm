// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonWriter;

import org.openstreetmap.josm.spi.preferences.IPreferences;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.Utils;

/**
 * Utility methods to convert struct-like classes to a string map and back.
 *
 * A "struct" is a class that has some fields annotated with {@link StructEntry}.
 * Those fields will be respected when converting an object to a {@link Map} and
 * back.
 * @since 12851
 */
public final class StructUtils {

    private StructUtils() {
        // hide constructor
    }

    /**
     * Annotation used for converting objects to String Maps and vice versa.
     * Indicates that a certain field should be considered in the conversion process. Otherwise it is ignored.
     *
     * @see #serializeStruct(java.lang.Object, java.lang.Class)
     * @see #deserializeStruct(java.util.Map, java.lang.Class)
     */
    @Retention(RetentionPolicy.RUNTIME) // keep annotation at runtime
    public @interface StructEntry { }

    /**
     * Annotation used for converting objects to String Maps.
     * Indicates that a certain field should be written to the map, even if the value is the same as the default value.
     *
     * @see #serializeStruct(java.lang.Object, java.lang.Class)
     */
    @Retention(RetentionPolicy.RUNTIME) // keep annotation at runtime
    public @interface WriteExplicitly { }

    /**
     * Get a list of hashes which are represented by a struct-like class.
     * Possible properties are given by fields of the class klass that have the @pref annotation.
     * Default constructor is used to initialize the struct objects, properties then override some of these default values.
     * @param <T> klass type
     * @param preferences preferences to look up the value
     * @param key main preference key
     * @param klass The struct class
     * @return a list of objects of type T or an empty list if nothing was found
     */
    public static <T> List<T> getListOfStructs(IPreferences preferences, String key, Class<T> klass) {
        return Optional.ofNullable(getListOfStructs(preferences, key, null, klass)).orElseGet(Collections::emptyList);
    }

    /**
     * same as above, but returns def if nothing was found
     * @param <T> klass type
     * @param preferences preferences to look up the value
     * @param key main preference key
     * @param def default value
     * @param klass The struct class
     * @return a list of objects of type T or {@code def} if nothing was found
     */
    public static <T> List<T> getListOfStructs(IPreferences preferences, String key, Collection<T> def, Class<T> klass) {
        List<Map<String, String>> prop =
            preferences.getListOfMaps(key, def == null ? null : serializeListOfStructs(def, klass));
        if (prop == null)
            return def == null ? null : new ArrayList<>(def);
        return prop.stream().map(p -> deserializeStruct(p, klass)).collect(Collectors.toList());
    }

    /**
     * Convenience method that saves a MapListSetting which is provided as a collection of objects.
     *
     * Each object is converted to a <code>Map&lt;String, String&gt;</code> using the fields with {@link StructEntry} annotation.
     * The field name is the key and the value will be converted to a string.
     *
     * Considers only fields that have the {@code @StructEntry} annotation.
     * In addition it does not write fields with null values. (Thus they are cleared)
     * Default values are given by the field values after default constructor has been called.
     * Fields equal to the default value are not written unless the field has the {@link WriteExplicitly} annotation.
     * @param <T> the class,
     * @param preferences the preferences to save to
     * @param key main preference key
     * @param val the list that is supposed to be saved
     * @param klass The struct class
     * @return true if something has changed
     */
    public static <T> boolean putListOfStructs(IPreferences preferences, String key, Collection<T> val, Class<T> klass) {
        return preferences.putListOfMaps(key, serializeListOfStructs(val, klass));
    }

    private static <T> List<Map<String, String>> serializeListOfStructs(Collection<T> l, Class<T> klass) {
        if (l == null)
            return null;
        List<Map<String, String>> vals = new ArrayList<>();
        for (T struct : l) {
            if (struct != null) {
                vals.add(serializeStruct(struct, klass));
            }
        }
        return vals;
    }

    /**
     * Convert an object to a String Map, by using field names and values as map key and value.
     *
     * The field value is converted to a String.
     *
     * Only fields with annotation {@link StructEntry} are taken into account.
     *
     * Fields will not be written to the map if the value is null or unchanged
     * (compared to an object created with the no-arg-constructor).
     * The {@link WriteExplicitly} annotation overrides this behavior, i.e. the default value will also be written.
     *
     * @param <T> the class of the object <code>struct</code>
     * @param struct the object to be converted
     * @param klass the class T
     * @return the resulting map (same data content as <code>struct</code>)
     */
    @SuppressWarnings("deprecation")
    public static <T> Map<String, String> serializeStruct(T struct, Class<T> klass) {
        T structPrototype;
        try {
            structPrototype = klass.getConstructor().newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException(ex);
        }

        Map<String, String> hash = new LinkedHashMap<>();
        for (Field f : klass.getDeclaredFields()) {
            if (f.getAnnotation(Preferences.pref.class) == null && f.getAnnotation(StructEntry.class) == null) {
                continue;
            }
            Utils.setObjectsAccessible(f);
            try {
                Object fieldValue = f.get(struct);
                Object defaultFieldValue = f.get(structPrototype);
                if (fieldValue != null && (
                        f.getAnnotation(Preferences.writeExplicitly.class) != null ||
                        f.getAnnotation(WriteExplicitly.class) != null ||
                        !Objects.equals(fieldValue, defaultFieldValue))) {
                    String key = f.getName().replace('_', '-');
                    if (fieldValue instanceof Map) {
                        hash.put(key, mapToJson((Map<?, ?>) fieldValue));
                    } else if (fieldValue instanceof MultiMap) {
                        hash.put(key, multiMapToJson((MultiMap<?, ?>) fieldValue));
                    } else {
                        hash.put(key, fieldValue.toString());
                    }
                }
            } catch (IllegalAccessException ex) {
                throw new JosmRuntimeException(ex);
            }
        }
        return hash;
    }

    /**
     * Converts a String-Map to an object of a certain class, by comparing map keys to field names of the class and assigning
     * map values to the corresponding fields.
     *
     * The map value (a String) is converted to the field type. Supported types are: boolean, Boolean, int, Integer, double,
     * Double, String, Map&lt;String, String&gt; and Map&lt;String, List&lt;String&gt;&gt;.
     *
     * Only fields with annotation {@link StructEntry} are taken into account.
     * @param <T> the class
     * @param hash the string map with initial values
     * @param klass the class T
     * @return an object of class T, initialized as described above
     */
    @SuppressWarnings("deprecation")
    public static <T> T deserializeStruct(Map<String, String> hash, Class<T> klass) {
        T struct = null;
        try {
            struct = klass.getConstructor().newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException(ex);
        }
        for (Map.Entry<String, String> keyValue : hash.entrySet()) {
            Object value;
            Field f;
            try {
                f = klass.getDeclaredField(keyValue.getKey().replace('-', '_'));
            } catch (NoSuchFieldException ex) {
                Logging.trace(ex);
                continue;
            }
            if (f.getAnnotation(Preferences.pref.class) == null && f.getAnnotation(StructEntry.class) == null) {
                continue;
            }
            Utils.setObjectsAccessible(f);
            if (f.getType() == Boolean.class || f.getType() == boolean.class) {
                value = Boolean.valueOf(keyValue.getValue());
            } else if (f.getType() == Integer.class || f.getType() == int.class) {
                try {
                    value = Integer.valueOf(keyValue.getValue());
                } catch (NumberFormatException nfe) {
                    continue;
                }
            } else if (f.getType() == Double.class || f.getType() == double.class) {
                try {
                    value = Double.valueOf(keyValue.getValue());
                } catch (NumberFormatException nfe) {
                    continue;
                }
            } else if (f.getType() == String.class) {
                value = keyValue.getValue();
            } else if (f.getType().isAssignableFrom(Map.class)) {
                value = mapFromJson(keyValue.getValue());
            } else if (f.getType().isAssignableFrom(MultiMap.class)) {
                value = multiMapFromJson(keyValue.getValue());
            } else
                throw new JosmRuntimeException("unsupported preference primitive type");

            try {
                f.set(struct, value);
            } catch (IllegalArgumentException ex) {
                throw new AssertionError(ex);
            } catch (IllegalAccessException ex) {
                throw new JosmRuntimeException(ex);
            }
        }
        return struct;
    }

    @SuppressWarnings("rawtypes")
    private static String mapToJson(Map map) {
        StringWriter stringWriter = new StringWriter();
        try (JsonWriter writer = Json.createWriter(stringWriter)) {
            JsonObjectBuilder object = Json.createObjectBuilder();
            for (Object o: map.entrySet()) {
                Map.Entry e = (Map.Entry) o;
                Object evalue = e.getValue();
                object.add(e.getKey().toString(), evalue.toString());
            }
            writer.writeObject(object.build());
        }
        return stringWriter.toString();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Map mapFromJson(String s) {
        Map ret = null;
        try (JsonReader reader = Json.createReader(new StringReader(s))) {
            JsonObject object = reader.readObject();
            ret = new HashMap(object.size());
            for (Map.Entry<String, JsonValue> e: object.entrySet()) {
                JsonValue value = e.getValue();
                if (value instanceof JsonString) {
                    // in some cases, when JsonValue.toString() is called, then additional quotation marks are left in value
                    ret.put(e.getKey(), ((JsonString) value).getString());
                } else {
                    ret.put(e.getKey(), e.getValue().toString());
                }
            }
        }
        return ret;
    }

    @SuppressWarnings("rawtypes")
    private static String multiMapToJson(MultiMap map) {
        StringWriter stringWriter = new StringWriter();
        try (JsonWriter writer = Json.createWriter(stringWriter)) {
            JsonObjectBuilder object = Json.createObjectBuilder();
            for (Object o: map.entrySet()) {
                Map.Entry e = (Map.Entry) o;
                Set evalue = (Set) e.getValue();
                JsonArrayBuilder a = Json.createArrayBuilder();
                for (Object evo: evalue) {
                    a.add(evo.toString());
                }
                object.add(e.getKey().toString(), a.build());
            }
            writer.writeObject(object.build());
        }
        return stringWriter.toString();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static MultiMap multiMapFromJson(String s) {
        MultiMap ret = null;
        try (JsonReader reader = Json.createReader(new StringReader(s))) {
            JsonObject object = reader.readObject();
            ret = new MultiMap(object.size());
            for (Map.Entry<String, JsonValue> e: object.entrySet()) {
                JsonValue value = e.getValue();
                if (value instanceof JsonArray) {
                    for (JsonString js: ((JsonArray) value).getValuesAs(JsonString.class)) {
                        ret.put(e.getKey(), js.getString());
                    }
                } else if (value instanceof JsonString) {
                    // in some cases, when JsonValue.toString() is called, then additional quotation marks are left in value
                    ret.put(e.getKey(), ((JsonString) value).getString());
                } else {
                    ret.put(e.getKey(), e.getValue().toString());
                }
            }
        }
        return ret;
    }
}
