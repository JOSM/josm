// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

import javax.swing.SwingUtilities;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSetMerger;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.OsmServerObjectReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;
import org.xml.sax.SAXException;

/**
 * The asynchronous task for fully downloading a collection of relations. Does a full download
 * for each relations and merges the relation into an {@link OsmDataLayer}
 * @since 2563
 */
public class DownloadRelationTask extends PleaseWaitRunnable {
    private boolean canceled;
    private Exception lastException;
    private final Collection<Relation> relations;
    private final OsmDataLayer layer;
    private OsmServerObjectReader objectReader;

    /**
     * Creates the download task
     *
     * @param relations a collection of relations. Must not be null.
     * @param layer the layer which data is to be merged into
     * @throws IllegalArgumentException if relations is null
     * @throws IllegalArgumentException if layer is null
     */
    public DownloadRelationTask(Collection<Relation> relations, OsmDataLayer layer) {
        super(tr("Download relations"), false /* don't ignore exception */);
        CheckParameterUtil.ensureParameterNotNull(relations, "relations");
        CheckParameterUtil.ensureParameterNotNull(layer, "layer");
        this.relations = relations;
        this.layer = layer;
        if (!layer.isDownloadable()) {
            throw new IllegalArgumentException("Non-downloadable layer: " + layer);
        }
    }

    @Override
    protected void cancel() {
        canceled = true;
        synchronized (this) {
            if (objectReader != null) {
                objectReader.cancel();
            }
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

    @Override
    protected void realRun() throws SAXException, IOException, OsmTransferException {
        try {
            final DataSet allDownloads = new DataSet();
            int i = 0;
            getProgressMonitor().setTicksCount(relations.size());
            for (Relation relation: relations) {
                i++;
                getProgressMonitor().setCustomText(tr("({0}/{1}): Downloading relation ''{2}''...", i, relations.size(),
                        relation.getDisplayName(DefaultNameFormatter.getInstance())));
                synchronized (this) {
                    if (canceled) return;
                    objectReader = new OsmServerObjectReader(relation.getPrimitiveId(), true /* full download */);
                }
                DataSet dataSet = objectReader.parseOsm(
                        getProgressMonitor().createSubTaskMonitor(0, false)
                );
                if (dataSet == null)
                    return;
                synchronized (this) {
                    if (canceled) return;
                    objectReader = null;
                }
                DataSetMerger merger = new DataSetMerger(allDownloads, dataSet);
                merger.merge();
                getProgressMonitor().worked(1);
            }

            SwingUtilities.invokeAndWait(() -> {
                layer.mergeFrom(allDownloads);
                layer.onPostDownloadFromServer();
                MainApplication.getMap().repaint();
            });
        } catch (OsmTransferException | InvocationTargetException | InterruptedException e) {
            if (canceled) {
                Logging.warn(tr("Ignoring exception because task was canceled. Exception: {0}", e.toString()));
                return;
            }
            lastException = e;
        }
    }
}
