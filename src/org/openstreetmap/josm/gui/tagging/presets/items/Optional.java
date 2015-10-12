// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.GBC;

public class Optional extends TextItem {

    // TODO: Draw a box around optional stuff
    @Override
    public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel, boolean presetInitiallyMatches) {
        initializeLocaleText(tr("Optional Attributes:"));
        p.add(new JLabel(" "), GBC.eol()); // space
        p.add(new JLabel(locale_text), GBC.eol());
        p.add(new JLabel(" "), GBC.eol()); // space
        return false;
    }
}
