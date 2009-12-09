// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.SwingUtilities;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmTransferException;
import org.xml.sax.SAXException;

/**
 * A task for closing a collection of changesets.
 *
 */
public class CloseChangesetTask extends PleaseWaitRunnable {
    private boolean cancelled;
    private Exception lastException;
    private Collection<Changeset> changesets;
    private ArrayList<Changeset> closedChangesets;

    /**
     * Closes all changesets in <code>changesets</code> if they are not null, if they
     * are still open and if they have an id > 0. Other changesets in the collection
     * are ignored.
     *
     * @param changesets  the collection of changesets. Empty collection assumes, if null.
     */
    public CloseChangesetTask(Collection<Changeset> changesets) {
        super(tr("Closing changeset"), false /* don't ignore exceptions */);
        if (changesets == null) {
            changesets = new ArrayList<Changeset>();
        }
        this.changesets = changesets;
        this.closedChangesets = new ArrayList<Changeset>();
    }

    @Override
    protected void cancel() {
        this.cancelled = true;
        OsmApi.getOsmApi().cancel();
    }

    @Override
    protected void finish() {
        if (cancelled)
            return;
        if (lastException != null) {
            ExceptionDialogUtil.explainException(lastException);
        }
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        for (Changeset cs: closedChangesets) {
                            UploadDialog.getUploadDialog().updateListOfChangesetsAfterUploadOperation(cs);
                        }
                    }
                }
        );
    }

    @Override
    protected void realRun() throws SAXException, IOException, OsmTransferException {
        try {
            for (Changeset cs: changesets) {
                if (cancelled) return;
                if (cs == null || cs.getId() <= 0 || ! cs.isOpen()) {
                    continue;
                }
                getProgressMonitor().subTask(tr("Closing changeset {0}", cs.getId()));
                OsmApi.getOsmApi().closeChangeset(cs, getProgressMonitor().createSubTaskMonitor(1, false));
                closedChangesets.add(cs);
            }
        } catch(Exception e) {
            if (cancelled)
                return;
            lastException = e;
        }
    }
}
