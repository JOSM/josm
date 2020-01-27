// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSetMerger;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.MultiFetchServerObjectReader;
import org.openstreetmap.josm.io.OsmServerBackreferenceReader;
import org.openstreetmap.josm.io.OsmServerReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ExceptionUtil;
import org.xml.sax.SAXException;

/**
 * The asynchronous task for downloading referring primitives
 * @since 2923
 */
public class DownloadReferrersTask extends PleaseWaitRunnable {
    private boolean canceled;
    private Exception lastException;
    private OsmServerReader reader;
    /** the target layer */
    private final OsmDataLayer targetLayer;
    /** the collection of child primitives */
    private final Set<PrimitiveId> children;
    /** the parents */
    private final DataSet parents;

    /**
     * constructor
     *
     * @param targetLayer  the target layer for the downloaded primitives. Must not be null.
     * @param children the collection of child primitives for which parents are to be downloaded
     * @since 15787 (modified interface)
     */
    public DownloadReferrersTask(OsmDataLayer targetLayer, Collection<? extends PrimitiveId> children) {
        super("Download referrers", false /* don't ignore exception*/);
        CheckParameterUtil.ensureParameterNotNull(targetLayer, "targetLayer");
        if (!targetLayer.isDownloadable()) {
            throw new IllegalArgumentException("Non-downloadable layer: " + targetLayer);
        }
        canceled = false;
        this.children = new LinkedHashSet<>();
        if (children != null) {
            children.stream().filter(p -> !p.isNew()).forEach(this.children::add);
        }

        this.targetLayer = targetLayer;
        parents = new DataSet();
    }

    /**
     * constructor
     *
     * @param targetLayer the target layer. Must not be null.
     * @param primitiveId a PrimitiveId object.
     * @param progressMonitor ProgressMonitor to use or null to create a new one.
     * @throws IllegalArgumentException if id &lt;= 0
     * @throws IllegalArgumentException if targetLayer == null
     */
    public DownloadReferrersTask(OsmDataLayer targetLayer, PrimitiveId primitiveId,
            ProgressMonitor progressMonitor) {
        super("Download referrers", progressMonitor, false /* don't ignore exception*/);
        CheckParameterUtil.ensureParameterNotNull(targetLayer, "targetLayer");
        if (primitiveId.isNew())
            throw new IllegalArgumentException(MessageFormat.format(
                    "Cannot download referrers for new primitives (ID {0})", primitiveId.getUniqueId()));
        canceled = false;
        this.children = new LinkedHashSet<>();
        this.children.add(primitiveId);
        this.targetLayer = targetLayer;
        parents = new DataSet();
    }

    @Override
    protected void cancel() {
        canceled = true;
        synchronized (this) {
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
            ExceptionUtil.explainException(lastException);
            return;
        }

        DataSetMerger visitor = new DataSetMerger(targetLayer.getDataSet(), parents);
        visitor.merge();
        SwingUtilities.invokeLater(targetLayer::onPostDownloadFromServer);
        if (visitor.getConflicts().isEmpty())
            return;
        targetLayer.getConflicts().add(visitor.getConflicts());
        JOptionPane.showMessageDialog(
                MainApplication.getMainFrame(),
                trn("There was {0} conflict during import.",
                    "There were {0} conflicts during import.",
                    visitor.getConflicts().size(),
                    visitor.getConflicts().size()
                ),
                trn("Conflict during download", "Conflicts during download", visitor.getConflicts().size()),
                JOptionPane.WARNING_MESSAGE
        );
        MapFrame map = MainApplication.getMap();
        map.conflictDialog.unfurlDialog();
        map.repaint();
    }

    protected void downloadParents(long id, OsmPrimitiveType type, ProgressMonitor progressMonitor) throws OsmTransferException {
        reader = new OsmServerBackreferenceReader(id, type, false).setAllowIncompleteParentWays(true);

        DataSet ds = reader.parseOsm(progressMonitor.createSubTaskMonitor(1, false));
        synchronized (this) { // avoid race condition in cancel()
            reader = null;
            if (canceled)
                return;
        }
        new DataSetMerger(parents, ds).merge();
    }

    @Override
    protected void realRun() throws SAXException, IOException, OsmTransferException {
        try {
            progressMonitor.setTicksCount(children.size());
            int i = 1;
            for (PrimitiveId p : children) {
                if (canceled)
                    return;
                String msg;
                String id = Long.toString(p.getUniqueId());
                switch(p.getType()) {
                case NODE: msg = tr("({0}/{1}) Loading parents of node {2}", i, children.size(), id); break;
                case WAY: msg = tr("({0}/{1}) Loading parents of way {2}", i, children.size(), id); break;
                case RELATION: msg = tr("({0}/{1}) Loading parents of relation {2}", i, children.size(), id); break;
                default: throw new AssertionError();
                }
                progressMonitor.subTask(msg);
                downloadParents(p.getUniqueId(), p.getType(), progressMonitor);
                i++;
            }
            Collection<Way> ways = parents.getWays();

            if (!ways.isEmpty()) {
                // Collect incomplete nodes of parent ways
                Set<Node> nodes = ways.stream().flatMap(w -> w.getNodes().stream().filter(OsmPrimitive::isIncomplete))
                        .collect(Collectors.toSet());
                if (!nodes.isEmpty()) {
                    reader = MultiFetchServerObjectReader.create();
                    ((MultiFetchServerObjectReader) reader).append(nodes);
                    DataSet wayNodes = reader.parseOsm(progressMonitor.createSubTaskMonitor(1, false));
                    synchronized (this) { // avoid race condition in cancel()
                        reader = null;
                    }
                    new DataSetMerger(parents, wayNodes).merge();
                }
            }
        } catch (OsmTransferException e) {
            if (canceled)
                return;
            lastException = e;
        }
    }
}
