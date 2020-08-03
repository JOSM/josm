// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JColorChooser;
import javax.swing.JMenu;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.mappaint.StyleSetting.ColorStyleSetting;
import org.openstreetmap.josm.gui.mappaint.loader.MapPaintStyleLoader;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;

/**
 * A GUI to set a color style
 * @author Taylor Smock
 * @since 16842
 */
public class ColorStyleSettingGui implements StyleSettingGui {

    private final ColorStyleSetting setting;

    /**
     * Create a new ColorStyleSettingGui
     * @param setting The setting to create the GUI for
     */
    public ColorStyleSettingGui(ColorStyleSetting setting) {
        this.setting = Objects.requireNonNull(setting);
    }

    static class ColorIcon implements Icon {

        private final Color color;
        private final ImageSizes size;

        ColorIcon(Color color, ImageProvider.ImageSizes size) {
            this.color = color;
            this.size = size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Color current = g.getColor();
            g.setColor(color);
            g.drawRect(x, y, getIconWidth(), getIconHeight());
            g.fillRect(x, y, getIconWidth(), getIconHeight());
            g.setColor(current); // So that the text is still black
        }

        @Override
        public int getIconWidth() {
            return size.getAdjustedWidth();
        }

        @Override
        public int getIconHeight() {
            return size.getAdjustedHeight();
        }

    }

    class ColorStyleSettingAction extends AbstractAction {
        ColorStyleSettingAction() {
            super(setting.label, new ColorIcon(setting.getValue(), ImageSizes.SMALLICON));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setting.setValue(JColorChooser.showDialog(MainApplication.getMainPanel(), tr("Choose a color"), setting.getValue()));
            MainApplication.worker.submit(new MapPaintStyleLoader(Collections.singleton(setting.parentStyle)));
        }
    }

    @Override
    public void addMenuEntry(JMenu menu) {
        menu.add(new ColorStyleSettingAction());
    }

}
