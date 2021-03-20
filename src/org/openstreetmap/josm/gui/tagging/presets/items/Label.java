// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import java.util.Collection;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.GBC;

/**
 * Label type.
 */
public class Label extends TextItem {

    @Override
    public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel, boolean presetInitiallyMatches) {
        initializeLocaleText(null);
        JLabel label = new JLabel(locale_text);
        addIcon(label);
        p.add(label, GBC.eol().fill(GBC.HORIZONTAL));
        return true;
    }

}
