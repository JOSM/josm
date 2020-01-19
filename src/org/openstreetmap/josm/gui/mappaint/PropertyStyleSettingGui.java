// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.mappaint.StyleSetting.PropertyStyleSetting;
import org.openstreetmap.josm.gui.mappaint.loader.MapPaintStyleLoader;

/**
 * GUI elements for a {@link PropertyStyleSetting} class.
 * @since 15731
 */
class PropertyStyleSettingGui<T> implements StyleSettingGui {

    private final PropertyStyleSetting<T> setting;

    /**
     * Constructs a new {@code BooleanStyleSettingGui}.
     * @param setting boolean style setting
     */
    PropertyStyleSettingGui(PropertyStyleSetting<T> setting) {
        this.setting = Objects.requireNonNull(setting);
    }

    class PropertyStyleSettingAction extends AbstractAction {

        PropertyStyleSettingAction() {
            super(setting.label);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final String initialValue = String.valueOf(setting.getValue());
            final String userInput = JOptionPane.showInputDialog(setting.label, initialValue);
            if (userInput != null && !initialValue.equals(userInput)) {
                setting.setStringValue(userInput);
                MainApplication.worker.submit(new MapPaintStyleLoader(Collections.singletonList(setting.parentStyle)));
            }
        }
    }

    @Override
    public void addMenuEntry(JMenu menu) {
        menu.add(new PropertyStyleSettingAction());
    }
}
