// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItemGuiSupport;
import org.openstreetmap.josm.tools.GBC;

/**
 * Used to group optional attributes.
 * @since 8863
 */
public class Optional extends TextItem {

    // TODO: Draw a box around optional stuff
    @Override
    public boolean addToPanel(JPanel p, TaggingPresetItemGuiSupport support) {
        initializeLocaleText(tr("Optional Attributes:"));
        p.add(new JLabel(" "), GBC.eol()); // space
        p.add(new JLabel(locale_text), GBC.eol());
        p.add(new JLabel(" "), GBC.eol()); // space
        return false;
    }
}
