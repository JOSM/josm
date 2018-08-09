// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import org.openstreetmap.josm.spi.preferences.PreferenceChangedListener;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.bugreport.BugReport;

/**
 * This class represents a property that can be represented as String.
 *
 * @author Michael Zangl
 *
 * @param <T> The property content type.
 * @since 10824
 */
public abstract class AbstractToStringProperty<T> extends AbstractProperty<T> {

    /**
     * This is a version of this property that attempts to get the property with a more specialized key and - if that fails - uses the property
     * value as default.
     *
     * @author Michael Zangl
     * @param <T> The content type
     */
    public static class ChildProperty<T> extends AbstractToStringProperty<T> {
        private final AbstractToStringProperty<T> parent;

        ChildProperty(AbstractToStringProperty<T> parent, String key) {
            super(key, null);
            CheckParameterUtil.ensureParameterNotNull(parent, "parent");
            this.parent = parent;
        }

        @Override
        protected void storeDefaultValue() {
            // Default value hidden in preferences.
        }

        @Override
        public T getDefaultValue() {
            return parent.get();
        }

        @Override
        protected T fromString(String string) {
            return parent.fromString(string);
        }

        @Override
        protected String toString(T t) {
            return parent.toString(t);
        }

        @Override
        protected void addListenerImpl(PreferenceChangedListener adapter) {
            super.addListenerImpl(adapter);
            parent.addListenerImpl(adapter);
        }

        @Override
        protected void removeListenerImpl(PreferenceChangedListener adapter) {
            super.removeListenerImpl(adapter);
            parent.removeListenerImpl(adapter);
        }

        @Override
        public CachingProperty<T> cached() {
            throw new UnsupportedOperationException("Not implemented yet.");
        }
    }

    /**
     * Create a new property and store the default value.
     * @param key The key
     * @param defaultValue The default value.
     * @see AbstractProperty#AbstractProperty(String, Object)
     */
    public AbstractToStringProperty(String key, T defaultValue) {
        super(key, defaultValue);
        storeDefaultValue();
    }

    @Override
    public T get() {
        String string = getAsString();
        if (!string.isEmpty()) {
            try {
                return fromString(string);
            } catch (InvalidPreferenceValueException e) {
                Logging.warn(BugReport.intercept(e).put("key", key).put("value", string));
            }
        }
        return getDefaultValue();
    }

    /**
     * Converts the string to an object of the given type.
     * @param string The string
     * @return The object.
     * @throws InvalidPreferenceValueException If the value could not be converted.
     */
    protected abstract T fromString(String string);

    @Override
    public boolean put(T value) {
        String string = value == null ? null : toString(value);
        return getPreferences().put(getKey(), string);
    }

    /**
     * Converts the string to an object of the given type.
     * @param t The object.
     * @return The string representing the object
     * @throws InvalidPreferenceValueException If the value could not be converted.
     */
    protected abstract String toString(T t);

    /**
     * Gets the preference value as String.
     * @return The string preference value.
     */
    protected String getAsString() {
        T def = getDefaultValue();
        String sdef = def == null ? "" : toString(def);
        return getPreferences() != null ? getPreferences().get(key, sdef) : sdef;
    }

    /**
     * Gets a specialized setting value that has the current value as default
     * <p>
     * The key will be getKey().spec
     * @param spec The key specialization
     * @return The property
     */
    public AbstractToStringProperty<T> getSpecialized(String spec) {
        return getChildProperty(getKey() + "." + spec);
    }

    /**
     * Gets a setting that defaults to this setting if the key is not set.
     * @param key The more specialized key.
     * @return The new setting.
     */
    protected AbstractToStringProperty<T> getChildProperty(String key) {
        return new ChildProperty<>(this, key);
    }

}
