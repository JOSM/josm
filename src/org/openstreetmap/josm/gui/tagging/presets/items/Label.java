// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import java.util.Collection;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetReader;
import org.openstreetmap.josm.tools.GBC;

/**
 * Label type.
 */
public class Label extends TextItem {

    /** The location of icon file to display (optional) */
    public String icon;
    /** The size of displayed icon. If not set, default is 16px */
    public String icon_size;

    @Override
    public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel, boolean presetInitiallyMatches) {
        initializeLocaleText(null);
        addLabel(p, getIcon(), locale_text);
        return true;
    }

    /**
     * Adds a new {@code JLabel} to the given panel.
     * @param p The panel
     * @param icon the icon (optional, can be null)
     * @param label The text label
     */
    public static void addLabel(JPanel p, Icon icon, String label) {
        p.add(new JLabel(label, icon, JLabel.LEADING), GBC.eol().fill(GBC.HORIZONTAL));
    }

    /**
     * Returns the label icon, if any.
     * @return the label icon, or {@code null}
     */
    public ImageIcon getIcon() {
        Integer size = parseInteger(icon_size);
        return icon == null ? null : loadImageIcon(icon, TaggingPresetReader.getZipIcons(), size != null ? size : 16);
    }
}
