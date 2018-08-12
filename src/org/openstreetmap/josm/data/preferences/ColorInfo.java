// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import java.awt.Color;
import java.util.List;

import org.openstreetmap.josm.tools.ColorHelper;

/**
 * Data class to hold information on a named color setting.
 */
public class ColorInfo {

    private String category;
    private String source;
    private String name;
    private Color value;
    private Color defaultValue;

    /**
     * Constructs a new {@code ColorInfo}.
     */
    public ColorInfo() {
    }

    /**
     * Constructs a new {@code ColorInfo}.
     * @param category the category of the color setting
     * @param source the source (related file), can be null
     * @param name the color name
     * @param value the color value set in the preferences, null if not set
     * @param defaultValue the default value for this color setting, can be null
     * @see org.openstreetmap.josm.data.preferences.NamedColorProperty
     */
    public ColorInfo(String category, String source, String name, Color value, Color defaultValue) {
        this.category = category;
        this.source = source;
        this.name = name;
        this.value = value;
        this.defaultValue = defaultValue;
    }

    /**
     * Get the category.
     * @return the category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Get the source.
     * @return the source, can be null
     */
    public String getSource() {
        return source;
    }

    /**
     * Get the name.
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the color value in the preferences (if set).
     * @return the color value, can be null
     */
    public Color getValue() {
        return value;
    }

    /**
     * Get the default value for this color setting.
     * @return the default value, can be null
     */
    public Color getDefaultValue() {
        return defaultValue;
    }

    /**
     * Set the category.
     * @param category the category
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Set the source.
     * @param source the source
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Set the name.
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set the color value.
     * @param value the value
     */
    public void setValue(Color value) {
        this.value = value;
    }

    /**
     * Set the default value.
     * @param defaultValue the default value
     */
    public void setDefaultValue(Color defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * Constructs a new {@code ColorInfo} from raw preference value.
     * @param lst the list
     * @param isDefault if the list represents a default value or not
     * @return corresponding {@code ColorInfo} object or null in case of invalid input
     */
    public static ColorInfo fromPref(List<String> lst, boolean isDefault) {
        if (lst == null || lst.size() < 4) {
            return null;
        }
        Color clr = ColorHelper.html2color(lst.get(0));
        if (clr == null) {
            return null;
        }
        ColorInfo info = new ColorInfo();
        if (isDefault) {
            info.defaultValue = clr;
        } else {
            info.value = clr;
        }
        info.category = lst.get(1);
        info.source = lst.get(2);
        if (info.source.isEmpty()) {
            info.source = null;
        }
        info.name = lst.get(3);
        return info;
    }

    @Override
    public String toString() {
        return "ColorInfo [" + (category != null ? "category=" + category + ", " : "")
                + (source != null ? "source=" + source + ", " : "") + (name != null ? "name=" + name + ", " : "")
                + (value != null ? "value=" + value + ", " : "")
                + (defaultValue != null ? "defaultValue=" + defaultValue : "") + "]";
    }
}
