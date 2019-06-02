// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.ChangesetCache;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.io.OsmTransferException;
import org.xml.sax.SAXException;

/**
 * This is an asynchronous task for downloading a collection of changests from the OSM server.
 *
 * The  task only downloads the changeset properties without the changeset content. It
 * updates the global {@link ChangesetCache}.
 * @since 2613
 */
public class ChangesetHeaderDownloadTask extends AbstractChangesetDownloadTask {

    class DownloadTask extends RunnableDownloadTask {
        /** the list of changeset ids to download */
        private final Set<Integer> toDownload = new HashSet<>();
        /** whether to include discussions or not */
        private final boolean includeDiscussion;

        DownloadTask(Component parent, Collection<Integer> ids, boolean includeDiscussion) {
            super(parent, tr("Download changesets"));
            this.includeDiscussion = includeDiscussion;
            for (int id: ids != null ? ids : Collections.<Integer>emptyList()) {
                if (id <= 0) {
                    continue;
                }
                toDownload.add(id);
            }
        }

        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {
            try {
                downloadedChangesets.addAll(reader.readChangesets(toDownload, includeDiscussion,
                        getProgressMonitor().createSubTaskMonitor(0, false)));
            } catch (OsmTransferException e) {
                if (isCanceled())
                    // ignore exception if canceled
                    return;
                // remember other exceptions
                rememberLastException(e);
            }
        }

        @Override
        protected void finish() {
            rememberDownloadedData(downloadedChangesets);
            if (isCanceled())
                return;
            if (lastException != null) {
                ExceptionDialogUtil.explainException(lastException);
            }
            updateChangesets();
        }
    }

    /**
     * Creates the download task for a collection of changeset ids. Uses a {@link org.openstreetmap.josm.gui.PleaseWaitDialog}
     * whose parent is {@link MainApplication#getMainFrame}.
     *
     * Null ids or or ids &lt;= 0 in the id collection are ignored.
     *
     * @param ids the collection of ids. Empty collection assumed if null.
     */
    public ChangesetHeaderDownloadTask(Collection<Integer> ids) {
        this(MainApplication.getMainFrame(), ids, false);
    }

    /**
     * Creates the download task for a collection of changeset ids. Uses a {@link org.openstreetmap.josm.gui.PleaseWaitDialog}
     * whose parent is the parent window of <code>dialogParent</code>.
     *
     * Null ids or or ids &lt;= 0 in the id collection are ignored.
     *
     * @param dialogParent the parent reference component for the {@link org.openstreetmap.josm.gui.PleaseWaitDialog}. Must not be null.
     * @param ids the collection of ids. Empty collection assumed if null.
     * @throws IllegalArgumentException if dialogParent is null
     */
    public ChangesetHeaderDownloadTask(Component dialogParent, Collection<Integer> ids) {
        this(dialogParent, ids, false);
    }

    /**
     * Creates the download task for a collection of changeset ids, with possibility to download changeset discussion.
     * Uses a {@link org.openstreetmap.josm.gui.PleaseWaitDialog} whose parent is the parent window of <code>dialogParent</code>.
     *
     * Null ids or or ids &lt;= 0 in the id collection are ignored.
     *
     * @param dialogParent the parent reference component for the {@link org.openstreetmap.josm.gui.PleaseWaitDialog}. Must not be null.
     * @param ids the collection of ids. Empty collection assumed if null.
     * @param includeDiscussion determines if discussion comments must be downloaded or not
     * @throws IllegalArgumentException if dialogParent is null
     * @since 7704
     */
    public ChangesetHeaderDownloadTask(Component dialogParent, Collection<Integer> ids, boolean includeDiscussion) {
        setDownloadTask(new DownloadTask(dialogParent, ids, includeDiscussion));
    }

    /**
     * Builds a download task from for a collection of changesets.
     *
     * Ignores null values and changesets with {@link Changeset#isNew()} == true.
     *
     * @param changesets the collection of changesets. Assumes an empty collection if null.
     * @return the download task
     */
    public static ChangesetHeaderDownloadTask buildTaskForChangesets(Collection<Changeset> changesets) {
        return buildTaskForChangesets(MainApplication.getMainFrame(), changesets);
    }

    /**
     * Builds a download task from for a collection of changesets.
     *
     * Ignores null values and changesets with {@link Changeset#isNew()} == true.
     *
     * @param parent the parent component relative to which the {@link org.openstreetmap.josm.gui.PleaseWaitDialog} is displayed.
     * Must not be null.
     * @param changesets the collection of changesets. Assumes an empty collection if null.
     * @return the download task
     * @throws NullPointerException if parent is null
     */
    public static ChangesetHeaderDownloadTask buildTaskForChangesets(Component parent, Collection<Changeset> changesets) {
        return new ChangesetHeaderDownloadTask(Objects.requireNonNull(parent, "parent"),
                changesets == null ? Collections.<Integer>emptySet() :
                    changesets.stream().filter(cs -> cs != null && !cs.isNew()).map(Changeset::getId).collect(Collectors.toSet()));
    }
}
