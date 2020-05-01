// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Container;
import java.awt.Rectangle;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;

import org.openstreetmap.josm.actions.OpenBrowserAction;
import org.openstreetmap.josm.data.StructUtils;
import org.openstreetmap.josm.data.StructUtils.StructEntry;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.spi.preferences.Config;

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
     * @param primitiveId the primitive id
     * @since 14432
     */
    public OpenChangesetPopupMenu(final long changesetId, final PrimitiveId primitiveId) {
        StructUtils.getListOfStructs(Config.getPref(), "history-dialog.tools", DEFAULT_ENTRIES, ChangesetViewerEntry.class)
                .stream()
                .map(entry -> entry.toAction(changesetId, primitiveId))
                .filter(Objects::nonNull)
                .forEach(this::add);
    }

    /**
     * Displays the popup menu at the lower-left corner of {@code parent}.
     *
     * @param parent the parent component to use for positioning this menu
     */
    public void show(final JComponent parent) {
        Container parentParent = parent.getParent();
        if (parentParent.isShowing()) {
            final Rectangle r = parent.getBounds();
            show(parentParent, r.x, r.y + r.height);
        }
    }

    private static final List<ChangesetViewerEntry> DEFAULT_ENTRIES = Arrays.asList(
            new ChangesetViewerEntry(tr("View changeset in web browser"), Config.getUrls().getBaseBrowseUrl() + "/changeset/{0}"),
            new ChangesetViewerEntry(tr("Open {0}", "achavi (Augmented OSM Change Viewer)"), "https://overpass-api.de/achavi/?changeset={0}"),
            new ChangesetViewerEntry(tr("Open {0}", "OSMCha (OSM Changeset Analyzer)"), "https://osmcha.org/changesets/{0}"),
            new ChangesetViewerEntry(tr("Open {0}", "OSM History Viewer (Mapki)"), "http://osm.mapki.com/history/{1}.php?id={2}"),
            new ChangesetViewerEntry(tr("Open {0}", "OSM History Viewer (Pewu)"), "https://pewu.github.io/osm-history/#/{1}/{2}"),
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
        /**
         * Templated service url.
         * <code>{0}</code> will be replaced by changeset id
         * <code>{1}</code> will be replaced by object type (node, way, relation)
         * <code>{2}</code> will be replaced by object id
         */
        @StructEntry
        public String url;

        /**
         * Constructs a new {@code ChangesetViewerEntry}.
         */
        public ChangesetViewerEntry() {
        }

        ChangesetViewerEntry(String name, String url) {
            this.name = Objects.requireNonNull(name);
            this.url = Objects.requireNonNull(url);
        }

        Action toAction(final long changesetId, PrimitiveId primitiveId) {
            if (primitiveId != null) {
                return new OpenBrowserAction(name, MessageFormat.format(url,
                        Long.toString(changesetId), primitiveId.getType().getAPIName(), Long.toString(primitiveId.getUniqueId())));
            } else if (url.contains("{0}")) {
                return new OpenBrowserAction(name, MessageFormat.format(url, Long.toString(changesetId)));
            }
            return null;
        }
    }

}
