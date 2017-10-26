// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.StructUtils;
import org.openstreetmap.josm.data.StructUtils.StructEntry;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.OpenBrowser;

/**
 * A menu displaying links to external history viewers for a changeset.
 *
 * @since 12871
 */
public class OpenChangesetPopupMenu extends JPopupMenu {

    /**
     * Constructs a new {@code OpenChangesetPopupMenu} for the given changeset id.
     *
     * @param changesetId the changeset id
     */
    public OpenChangesetPopupMenu(final long changesetId) {
        StructUtils.getListOfStructs(Config.getPref(), "history-dialog.tools", DEFAULT_ENTRIES, ChangesetViewerEntry.class)
                .stream()
                .map(entry -> entry.toAction(changesetId))
                .forEach(this::add);
    }

    /**
     * Displays the popup menu at the lower-left corner of {@code parent}.
     *
     * @param parent the parent component to use for positioning this menu
     */
    public void show(final JComponent parent) {
        final Rectangle r = parent.getBounds();
        show(parent.getParent(), r.x, r.y + r.height);
    }

    private static final List<ChangesetViewerEntry> DEFAULT_ENTRIES = Arrays.asList(
            new ChangesetViewerEntry(tr("View changeset in web browser"), Main.getBaseBrowseUrl() + "/changeset/{0}"),
            new ChangesetViewerEntry(tr("Open {0}", "achavi (Augmented OSM Change Viewer)"), "https://overpass-api.de/achavi/?changeset={0}"),
            new ChangesetViewerEntry(tr("Open {0}", "OSMCha (OSM Changeset Analyzer)"), "https://osmcha.mapbox.com/changesets/{0}"),
            new ChangesetViewerEntry(tr("Open {0}", "OSM History Viewer"), "http://osmhv.openstreetmap.de/changeset.jsp?id={0}"),
            new ChangesetViewerEntry(tr("Open {0}", "WhoDidIt (OSM Changeset Analyzer)"),
                    "http://simon04.dev.openstreetmap.org/whodidit/index.html?changeset={0}&show=1")
    );

    /**
     * Auxiliary class to save a link to a history viewer in the preferences.
     */
    public static class ChangesetViewerEntry {
        /** Name to be displayed in popup menu */
        @StructEntry
        public String name;
        /** Templated service url. <code>{0}</code> will be replaced by changeset id */
        @StructEntry
        public String url;

        /**
         * Constructs a new {@code ChangesetViewerEntry}.
         */
        public ChangesetViewerEntry() {
        }

        ChangesetViewerEntry(String name, String url) {
            this.name = name;
            this.url = url;
        }

        Action toAction(final long changesetId) {
            return new OpenBrowserAction(name, MessageFormat.format(this.url, Long.toString(changesetId)));
        }
    }

    static class OpenBrowserAction extends AbstractAction {
        final String url;

        OpenBrowserAction(String name, String url) {
            super(name);
            putValue(SHORT_DESCRIPTION, tr("Open {0}", url));
            new ImageProvider("help/internet").getResource().attachImageIcon(this, true);
            this.url = url;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            OpenBrowser.displayUrl(url);
        }
    }
}
