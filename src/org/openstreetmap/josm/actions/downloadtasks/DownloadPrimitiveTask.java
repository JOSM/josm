// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.CheckParameterUtil.ensureParameterNotNull;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmServerObjectReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.xml.sax.SAXException;

/**
 * The asynchronous task for updating a collection of objects using multi fetch.
 *
 */
public class DownloadPrimitiveTask extends PleaseWaitRunnable {
    private DataSet ds;
    private boolean canceled;
    private Exception lastException;
    private PrimitiveId primitiveId;
    private OsmDataLayer layer;
    private OsmServerObjectReader reader;

    /**
     * Creates the  task
     *
     * @param layer the layer in which primitives are updated. Must not be null.
     * @param toUpdate a collection of primitives to update from the server. Set to
     * the empty collection if null.
     * @throws IllegalArgumentException thrown if layer is null.
     */
    public DownloadPrimitiveTask(PrimitiveId id, OsmDataLayer layer) {
        super(tr("Download object"), false /* don't ignore exception */);
        ensureParameterNotNull(layer, "layer");
        this.layer = layer;
        this.primitiveId = id;
    }

    @Override
    protected void cancel() {
        canceled = true;
        synchronized(this) {
            if (reader != null) {
                reader.cancel();
            }
        }
    }

    @Override
    protected void finish() {
        if (canceled)
            return;
        if (lastException != null) {
            ExceptionDialogUtil.explainException(lastException);
            return;
        }
        Runnable r = new Runnable() {
            public void run() {
                layer.mergeFrom(ds);
                layer.onPostDownloadFromServer();
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(r);
            } catch(InterruptedException e) {
                e.printStackTrace();
            } catch(InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void realRun() throws SAXException, IOException, OsmTransferException {
        this.ds = new DataSet();
        try {
            synchronized(this) {
                if (canceled) return;
                reader = new OsmServerObjectReader(primitiveId, true);
            }
            ds = reader.parseOsm(progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
            synchronized(this) {
                reader = null;
            }
        } catch(Exception e) {
            if (canceled)
                return;
            lastException = e;
        }
    }
}
