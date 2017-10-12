// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import java.awt.Color;
import java.util.Locale;

import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ColorHelper;

/**
 * A property containing a {@link Color} value.
 * @since 5464
 * @deprecated (since 12987) replaced by {@link NamedColorProperty}
 */
@Deprecated
public class ColorProperty extends AbstractToStringProperty<Color> {

    private final String name;

    /**
     * Constructs a new {@code ColorProperty}.
     * @param colName The color name
     * @param defaultValue The default value as HTML string
     */
    public ColorProperty(String colName, String defaultValue) {
        this(colName, ColorHelper.html2color(defaultValue));
    }

    /**
     * Constructs a new {@code ColorProperty}.
     * @param colName The color name
     * @param defaultValue The default value
     */
    public ColorProperty(String colName, Color defaultValue) {
        super(getColorKey(colName), defaultValue);
        CheckParameterUtil.ensureParameterNotNull(defaultValue, "defaultValue");
        this.name = colName;
        getPreferences().registerColor(getColorKey(colName), colName);
    }

    @Override
    public Color get() {
        // Removing this implementation breaks binary compatibility due to the way generics work
        return super.get();
    }

    @Override
    public boolean put(Color value) {
        // Removing this implementation breaks binary compatibility due to the way generics work
        return super.put(value);
    }

    @Override
    protected Color fromString(String string) {
        return ColorHelper.html2color(string);
    }

    @Override
    protected String toString(Color t) {
        return ColorHelper.color2html(t, true);
    }

    /**
     * Gets a color of which the value can be set.
     * @param colorName the name of the color.
     * @return The child property that inherits this value if it is not set.
     */
    public AbstractToStringProperty<Color> getChildColor(String colorName) {
        return getChildProperty(getColorKey(colorName));
    }

    /**
     * Gets the name this color was registered with.
     * @return The name.
     */
    public String getName() {
        return name;
    }

    /**
     * Replies the color key used in JOSM preferences for this property.
     * @param colName The color name
     * @return The color key for this property
     */
    public static String getColorKey(String colName) {
        return colName == null ? null : "color." + colName.toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9]+", ".");
    }

    @Override
    public String toString() {
        return "ColorProperty [name=" + name + ", defaultValue=" + getDefaultValue() + "]";
    }
}
