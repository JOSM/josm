// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.awt.event.ActionEvent;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.mappaint.loader.MapPaintStyleLoader;

/**
 * GUI elements for a {@link StyleSetting.BooleanStyleSetting} class.
 * @since 12831
 */
public class BooleanStyleSettingGui implements StyleSettingGui {

    final StyleSetting.BooleanStyleSetting setting;

    public BooleanStyleSettingGui(StyleSetting.BooleanStyleSetting setting) {
        this.setting = setting;
    }

    @Override
    public void addMenuEntry(JMenu menu) {
        final JCheckBoxMenuItem item = new JCheckBoxMenuItem();
        Action a = new AbstractAction(setting.label) {
            @Override
            public void actionPerformed(ActionEvent e) {
                setting.setValue(item.isSelected());
                MainApplication.worker.submit(new MapPaintStyleLoader(Arrays.asList(setting.parentStyle)));
            }
        };
        item.setAction(a);
        item.setSelected((boolean) setting.getValue());
        menu.add(item);
    }
}
