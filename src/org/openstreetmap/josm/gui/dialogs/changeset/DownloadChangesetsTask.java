// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.SwingUtilities;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.ChangesetCache;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.io.OsmServerChangesetReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.xml.sax.SAXException;

public class DownloadChangesetsTask extends PleaseWaitRunnable{

    private Set<Integer> idsToDownload;
    private OsmServerChangesetReader reader;
    private boolean cancelled;
    private Exception lastException;
    private List<Changeset> downloadedChangesets;

    public DownloadChangesetsTask(Collection<Integer> ids) {
        super(tr("Download changesets"));
        idsToDownload = new HashSet<Integer>();
        if (ids == null ||  ids.isEmpty())
            return;
        for (int id: ids) {
            if (id <= 0) {
                continue;
            }
            idsToDownload.add(id);
        }
    }

    @Override
    protected void cancel() {
        cancelled = true;
        synchronized (this) {
            if (reader != null) {
                reader.cancel();
            }
        }
    }

    @Override
    protected void finish() {
        if (cancelled)
            return;
        if (lastException != null) {
            ExceptionDialogUtil.explainException(lastException);
        }
        Runnable r = new Runnable() {
            public void run() {
                ChangesetCache.getInstance().update(downloadedChangesets);
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    @Override
    protected void realRun() throws SAXException, IOException, OsmTransferException {
        try {
            synchronized (this) {
                reader = new OsmServerChangesetReader();
            }
            downloadedChangesets = reader.readChangesets(idsToDownload, getProgressMonitor().createSubTaskMonitor(0, false));
        } catch(Exception e) {
            if (cancelled)
                // ignore exception if cancelled
                return;
            if (e instanceof RuntimeException)
                throw (RuntimeException)e;
            lastException = e;
        }
    }
}
