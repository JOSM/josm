// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.dialogs.properties.HelpAction;
import org.openstreetmap.josm.gui.widgets.UrlLabel;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.LanguageInfo;

/**
 * Hyperlink type.
 * @since 8863
 */
public class Link extends TextItem {

    /** The OSM wiki page to display. */
    public String wiki; // NOSONAR

    /** The link to display. */
    public String href; // NOSONAR

    /** The localized version of {@link #href}. */
    public String locale_href; // NOSONAR

    @Override
    public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel, boolean presetInitiallyMatches) {
        initializeLocaleText(tr("More information about this feature"));
        Optional.ofNullable(buildUrlLabel()).ifPresent(label -> p.add(label, GBC.eol().insets(0, 10, 0, 0).fill(GBC.HORIZONTAL)));
        return false;
    }

    protected UrlLabel buildUrlLabel() {
        final String url = getUrl();
        if (wiki != null) {
            return new UrlLabel(url, locale_text, 2) {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        // Open localized page if exists
                        HelpAction.displayHelp(Arrays.asList(
                                LanguageInfo.getWikiLanguagePrefix(LanguageInfo.LocaleType.OSM_WIKI) + wiki,
                                wiki));
                    } else {
                        super.mouseClicked(e);
                    }
                }
            };
        } else if (href != null || locale_href != null) {
            return new UrlLabel(url, locale_text, 2);
        }
        return null;
    }

    /**
     * Returns the link URL.
     * @return the link URL
     * @since 15423
     */
    public String getUrl() {
        if (wiki != null) {
            return Config.getUrls().getOSMWiki() + "/wiki/" + wiki;
        } else if (href != null || locale_href != null) {
            return Optional.ofNullable(locale_href).orElse(href);
        }
        return null;
    }

    @Override
    protected String fieldsToString() {
        return super.fieldsToString()
                + (wiki != null ? "wiki=" + wiki + ", " : "")
                + (href != null ? "href=" + href + ", " : "")
                + (locale_href != null ? "locale_href=" + locale_href + ", " : "");
    }
}
