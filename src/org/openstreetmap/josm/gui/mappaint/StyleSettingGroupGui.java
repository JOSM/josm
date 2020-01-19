// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.mappaint.BooleanStyleSettingGui.BooleanStyleSettingCheckBoxMenuItem;
import org.openstreetmap.josm.gui.mappaint.StyleSetting.StyleSettingGroup;
import org.openstreetmap.josm.gui.mappaint.loader.MapPaintStyleLoader;
import org.openstreetmap.josm.gui.util.StayOpenCheckBoxMenuItemUI;

/**
 * GUI elements for a {@link StyleSettingGroup} class.
 * @since 15289
 */
public class StyleSettingGroupGui implements StyleSettingGui {

    private final StyleSettingGroup group;
    private final List<StyleSetting> settings;

    /**
     * Constructs a new {@code StyleSettingGroupGui}.
     * @param group style setting group
     * @param settings list of style settings in this group
     */
    public StyleSettingGroupGui(StyleSettingGroup group, List<StyleSetting> settings) {
        this.group = Objects.requireNonNull(group);
        this.settings = Objects.requireNonNull(settings);
    }

    @Override
    public void addMenuEntry(JMenu menu) {
        final JMenu submenu = new JMenu();
        submenu.setText(group.label);
        submenu.setIcon(group.icon);
        // Add the "toggle all settings" action
        if (settings.size() >= 2) {
            JMenuItem item = new JMenuItem(new AbstractAction(tr("Toggle all settings")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    List<BooleanStyleSettingCheckBoxMenuItem> items = Arrays.stream(submenu.getMenuComponents())
                            .filter(c -> c instanceof BooleanStyleSettingCheckBoxMenuItem)
                            .map(c -> (BooleanStyleSettingCheckBoxMenuItem) c)
                            .collect(Collectors.toList());
                    final boolean select = items.stream().anyMatch(cbi -> !cbi.isSelected());
                    items.stream().filter(cbi -> select != cbi.isSelected()).forEach(cbi -> cbi.doClickWithoutRepaint(0));
                    MainApplication.worker.submit(new MapPaintStyleLoader(Arrays.asList(group.parentStyle)));
                }
            });
            item.setUI(new StayOpenCheckBoxMenuItemUI());
            submenu.add(item);
            submenu.addSeparator();
        }
        // Add individual settings
        for (StyleSetting s : settings) {
            s.getStyleSettingGui().addMenuEntry(submenu);
        }
        menu.add(submenu);
    }
}
