// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItemGuiSupport;
import org.openstreetmap.josm.tools.GBC;

/**
 * Label type.
 */
public class Label extends TextItem {

    @Override
    public boolean addToPanel(JPanel p, TaggingPresetItemGuiSupport support) {
        initializeLocaleText(null);
        JLabel label = new JLabel(locale_text);
        addIcon(label);
        label.applyComponentOrientation(support.getDefaultComponentOrientation());
        p.add(label, GBC.eol().fill(GBC.HORIZONTAL));
        return true;
    }

}
