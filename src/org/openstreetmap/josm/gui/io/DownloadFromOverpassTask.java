// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSetMerger;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.io.OverpassDownloadReader;
import org.xml.sax.SAXException;

/**
 * Download OSM data from Overpass API
 *
 */
public class DownloadFromOverpassTask extends PleaseWaitRunnable {
    private boolean canceled;
    private final String request;
    private final DataSet ds;
    private Exception lastException;

    /**
     * Constructor
     * @param request the overpass query
     * @param ds the {@code DataSet} instance that should contain the downloaded data
     * @param monitor ProgressMonitor to use or null to create a new one.
     */
    public DownloadFromOverpassTask(String request, DataSet ds, ProgressMonitor monitor) {
        super(tr("Download objects via Overpass API"), monitor, false);
        this.request = request;
        this.ds = ds;
    }

    @Override
    protected void cancel() {
        canceled = true;
    }

    @Override
    protected void realRun() throws SAXException, IOException, OsmTransferException {
        try {
            OverpassDownloadReader reader = new OverpassDownloadReader(new Bounds(0, 0, 0, 0),
                    OverpassDownloadReader.OVERPASS_SERVER.get(), request);
            DataSet tmpDs = reader.parseOsm(progressMonitor.createSubTaskMonitor(1, false));
            if (!canceled) {
                new DataSetMerger(ds, tmpDs).merge();
            }
        } catch (OsmTransferException e) {
            if (canceled)
                return;
            lastException = e;
        }

    }

    @Override
    protected void finish() {
        if (canceled)
            return;
        if (lastException != null) {
            ExceptionDialogUtil.explainException(lastException);
        }
    }
}
