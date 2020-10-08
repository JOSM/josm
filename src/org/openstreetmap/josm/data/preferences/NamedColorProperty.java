// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ColorHelper;

/**
 * A property containing a {@link Color} value with additional information associated to it.
 *
 * The additional information is used to describe the color in the
 * {@link org.openstreetmap.josm.gui.preferences.display.ColorPreference}, so it can be recognized
 * and customized by the user.
 * @since 12987
 */
public class NamedColorProperty extends AbstractToStringProperty<Color> {

    public static final String NAMED_COLOR_PREFIX = "clr.";

    public static final String COLOR_CATEGORY_GENERAL = "general";
    public static final String COLOR_CATEGORY_MAPPAINT = "mappaint";

    private final String category;
    private final String source;
    private final String name;

    /**
     * Construct a new {@code NamedColorProperty}.
     * @param category a category, can be any identifier, but the following values are recognized by
     * the GUI preferences: {@link #COLOR_CATEGORY_GENERAL} and {@link #COLOR_CATEGORY_MAPPAINT}
     * @param source a filename or similar associated with the color, can be null if not applicable
     * @param name a short description of the color
     * @param defaultValue the default value, can be null
     */
    public NamedColorProperty(String category, String source, String name, Color defaultValue) {
        super(getKey(category, source, name), getUIColor("JOSM." + getKey(category, source, name), defaultValue));
        CheckParameterUtil.ensureParameterNotNull(category, "category");
        CheckParameterUtil.ensureParameterNotNull(name, "name");
        this.category = category;
        this.source = source;
        this.name = name;
    }

    /**
     * Construct a new {@code NamedColorProperty}.
     * @param name a short description of the color
     * @param defaultValue the default value, can be null
     */
    public NamedColorProperty(String name, Color defaultValue) {
        this(COLOR_CATEGORY_GENERAL, null, name, defaultValue);
    }

    private static String getKey(String category, String source, String name) {
        CheckParameterUtil.ensureParameterNotNull(category, "category");
        CheckParameterUtil.ensureParameterNotNull(name, "name");
        return NAMED_COLOR_PREFIX + category + "." + (source == null ? "" : source + ".") + name;
    }

    private static Color getUIColor(String uiKey, Color defaultValue) {
        Color color = UIManager.getColor(uiKey);
        return color != null ? color : defaultValue;
    }

    private List<String> getDefaultValuePref() {
        return defaultValue == null ? null : getValuePref(defaultValue, category, source, name);
    }

    @Override
    protected void storeDefaultValue() {
        // This is required due to the super() initializer calling this method.
        if (category != null) {
            super.storeDefaultValue();
        }
    }

    @Override
    public Color get() {
        List<String> data = getPreferences().getList(getKey(), getDefaultValuePref()); // store default value
        if (super.isSet() && data != null && !data.isEmpty()) {
            return ColorHelper.html2color(data.get(0));
        }
        return defaultValue;
    }

    @Override
    public boolean isSet() {
        get(); // trigger migration
        return super.isSet();
    }

    /**
     * Get the category for this color.
     * @return the category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Get the source, i.e.&nbsp;a filename or layer name associated with the color.
     * May return null if not applicable.
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * Get the color name (a short description of the color).
     * @return the color name
     */
    public String getName() {
        return name;
    }

    private static List<String> getValuePref(Color color, String category, String source, String name) {
        CheckParameterUtil.ensureParameterNotNull(color, "color");
        CheckParameterUtil.ensureParameterNotNull(category, "category");
        CheckParameterUtil.ensureParameterNotNull(name, "name");
        return Arrays.asList(ColorHelper.color2html(color, true), category, source == null ? "" : source, name);
    }

    @Override
    public boolean put(Color value) {
        return getPreferences().putList(getKey(), value == null ? null : getValuePref(value, category, source, name));
    }

    /**
     * Return a more specialized color, that will fall back to this color, if not set explicitly.
     * @param category the category of the specialized color
     * @param source the source of the specialized color
     * @param name the name of the specialized color
     * @return a {@link FallbackProperty} that will the return the specialized color, if set, but
     * fall back to this property as default value
     */
    public FallbackProperty<Color> getChildColor(String category, String source, String name) {
        return new FallbackProperty<>(new NamedColorProperty(category, source, name, defaultValue), this);
    }

    /**
     * Return a more specialized color, that will fall back to this color, if not set explicitly.
     * @param name the name of the specialized color
     * @return a {@link FallbackProperty} that will the return the specialized color, if set, but
     * fall back to this property as default value
     */
    public FallbackProperty<Color> getChildColor(String name) {
        return getChildColor(category, source, name);
    }

    @Override
    protected Color fromString(String string) {
        return ColorHelper.html2color(string);
    }

    @Override
    protected String toString(Color color) {
        return ColorHelper.color2html(color);
    }
}
