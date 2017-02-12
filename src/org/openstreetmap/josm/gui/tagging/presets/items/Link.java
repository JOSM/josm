// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;

import javax.swing.JPanel;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.widgets.UrlLabel;
import org.openstreetmap.josm.tools.GBC;

/**
 * Hyperlink type.
 */
public class Link extends TextItem {

    /** The link to display. */
    public String href; // NOSONAR

    /** The localized version of {@link #href}. */
    public String locale_href; // NOSONAR

    @Override
    public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel, boolean presetInitiallyMatches) {
        initializeLocaleText(tr("More information about this feature"));
        String url = java.util.Optional.ofNullable(locale_href).orElse(href);
        if (url != null) {
            p.add(new UrlLabel(url, locale_text, 2), GBC.eol().insets(0, 10, 0, 0).fill(GBC.HORIZONTAL));
        }
        return false;
    }

    @Override
    protected String fieldsToString() {
        return super.fieldsToString()
                + (href != null ? "href=" + href + ", " : "")
                + (locale_href != null ? "locale_href=" + locale_href + ", " : "");
    }
}
