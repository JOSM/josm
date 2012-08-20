// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import java.awt.Color;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences.ColorKey;

/**
 * A property containing a {@link Color} value.
 * @since 5464
 */
public class ColorProperty extends AbstractProperty<Color> implements ColorKey {

    private final String name;
    
    /**
     * Constructs a new {@code ColorProperty}.
     * @param colName The color name
     * @param defaultValue The default value
     */
    public ColorProperty(String colName, Color defaultValue) {
        super(getColorKey(colName), defaultValue);
        this.name = colName;
    }
    
    @Override
    public Color get() {
        return Main.pref.getColor(this);
    }

    @Override
    public boolean put(Color value) {
        return Main.pref.putColor(getColorKey(name), value);
    }
    
    /**
     * Replies the color key used in JOSM preferences for this property.
     * @param colName The color name
     * @return The color key for this property
     */
    public static String getColorKey(String colName) {
        return colName == null ? null : colName.toLowerCase().replaceAll("[^a-z0-9]+",".");
    }

    @Override
    public String getColorName() {
        return name;
    }

    @Override
    public String getSpecialName() {
        return null;
    }
}
