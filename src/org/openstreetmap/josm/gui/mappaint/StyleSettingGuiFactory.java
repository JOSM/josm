// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import org.openstreetmap.josm.gui.mappaint.StyleSetting.BooleanStyleSetting;

/**
 * Factory to create matching {@link StyleSettingGui} instances for given
 * {@link StyleSetting} objects.
 * @since 12831
 */
public class StyleSettingGuiFactory {

    /**
     * Create a matching {@link StyleSettingGui} instances for a given
     * {@link StyleSetting} object.
     * @param setting the {@code StyleSetting} object
     * @return matching {@code StyleSettingGui}
     * @throws UnsupportedOperationException when class of {@link StyleSetting}
     * is not supported
     */
    public static StyleSettingGui getStyleSettingGui(StyleSetting setting) {
        if (setting instanceof BooleanStyleSetting) {
            return new BooleanStyleSettingGui((BooleanStyleSetting) setting);
        }
        throw new UnsupportedOperationException("class " + setting.getClass() + " not supported");
    }

}
