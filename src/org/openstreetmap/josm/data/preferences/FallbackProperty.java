// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import org.openstreetmap.josm.spi.preferences.PreferenceChangedListener;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Property that wraps another property along with a fallback property that is used as default value.
 *
 * @param <T> The content type
 * @since 12987
 */
public class FallbackProperty<T> extends AbstractProperty<T> {

    private final AbstractProperty<T> property;
    private final AbstractProperty<T> fallback;

    /**
     * Constructs a new {@code FallbackProperty}.
     * @param property the wrapped property
     * @param fallback fallback property that takes effect in the {@link #get()} method when
     * {@code property} is not set
     */
    public FallbackProperty(AbstractProperty<T> property, AbstractProperty<T> fallback) {
        super(property.getKey(), null);
        CheckParameterUtil.ensureParameterNotNull(property, "property");
        CheckParameterUtil.ensureParameterNotNull(fallback, "fallback");
        this.property = property;
        this.fallback = fallback;
    }

    /**
     * Get the wrapped property.
     * @return the wrapped property
     */
    public AbstractProperty<T> getDelegateProperty() {
        return property;
    }

    /**
     * Get the fallback property.
     * @return the fallback property
     */
    public AbstractProperty<T> getFallbackProperty() {
        return fallback;
    }

    @Override
    protected void storeDefaultValue() {
        // Default value hidden in preferences.
    }

    @Override
    public boolean isSet() {
        return property.isSet();
    }

    @Override
    public T getDefaultValue() {
        return fallback.getDefaultValue();
    }

    @Override
    public void remove() {
        property.remove();
    }

    @Override
    public T get() {
        if (property.isSet()) {
            return property.get();
        }
        return fallback.get();
    }

    @Override
    public boolean put(T value) {
        return property.put(value);
    }

    @Override
    protected void addListenerImpl(PreferenceChangedListener adapter) {
        property.addListenerImpl(adapter);
        fallback.addListenerImpl(adapter);
    }

    @Override
    public void addWeakListener(ValueChangeListener<? super T> listener) {
        property.addWeakListener(listener);
        fallback.addWeakListener(listener);
    }

    @Override
    protected void removeListenerImpl(PreferenceChangedListener adapter) {
        property.removeListenerImpl(adapter);
        fallback.removeListenerImpl(adapter);
    }

}
