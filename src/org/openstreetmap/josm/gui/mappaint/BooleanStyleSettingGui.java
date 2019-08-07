// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.mappaint.StyleSetting.BooleanStyleSetting;
import org.openstreetmap.josm.gui.mappaint.loader.MapPaintStyleLoader;
import org.openstreetmap.josm.gui.util.StayOpenCheckBoxMenuItemUI;

/**
 * GUI elements for a {@link BooleanStyleSetting} class.
 * @since 12831
 */
public class BooleanStyleSettingGui implements StyleSettingGui {

    final StyleSetting.BooleanStyleSetting setting;

    /**
     * Constructs a new {@code BooleanStyleSettingGui}.
     * @param setting boolean style setting
     */
    public BooleanStyleSettingGui(BooleanStyleSetting setting) {
        this.setting = Objects.requireNonNull(setting);
    }

    static class BooleanStyleSettingCheckBoxMenuItem extends JCheckBoxMenuItem {
        boolean noRepaint = false;

        public BooleanStyleSettingCheckBoxMenuItem(BooleanStyleSetting setting) {
            setAction(new AbstractAction(setting.label) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setting.setValue(isSelected());
                    if (!noRepaint) {
                        MainApplication.worker.submit(new MapPaintStyleLoader(Arrays.asList(setting.parentStyle)));
                    }
                }
            });
            setSelected((boolean) setting.getValue());
            setUI(new StayOpenCheckBoxMenuItemUI());
        }

        public void doClickWithoutRepaint(int pressTime) {
            noRepaint = true;
            doClick(pressTime);
            noRepaint = false;
        }
    }

    @Override
    public void addMenuEntry(JMenu menu) {
        menu.add(new BooleanStyleSettingCheckBoxMenuItem(setting));
    }
}
