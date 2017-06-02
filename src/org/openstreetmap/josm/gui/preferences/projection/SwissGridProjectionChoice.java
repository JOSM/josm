// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JPanel;

import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.tools.GBC;

/**
 * ProjectionChoice for Swiss grid, CH1903 / LV03 military coordinates (EPSG:21781).
 * <p>
 * This is the old system and <b>not</b> CH1903+ from 1995.
 * @see <a href="https://en.wikipedia.org/wiki/Swiss_coordinate_system">swiss grid</a>
 */
public class SwissGridProjectionChoice extends SingleProjectionChoice {

    /**
     * Constructs a new {@code SwissGridProjectionChoice}.
     */
    public SwissGridProjectionChoice() {
        super(tr("Swiss Grid (Switzerland)"), /* NO-ICON */ "core:swissgrid", "EPSG:21781");
    }

    @Override
    public JPanel getPreferencePanel(ActionListener listener) {
        JPanel p = new JPanel(new GridBagLayout());
        p.add(new HtmlPanel(tr("<i>CH1903 / LV03</i>")), GBC.eol().fill(GBC.HORIZONTAL));
        p.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.BOTH));
        return p;
    }
}
