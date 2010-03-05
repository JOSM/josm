// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSetMerger;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.Way;
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
 *
 */
public class DownloadReferrersTask extends PleaseWaitRunnable {
    private boolean cancelled;
    private Exception lastException;
    private OsmServerReader reader;
    /** the target layer */
    private OsmDataLayer targetLayer;
    /** the collection of child primitives */
    private Map<Long, OsmPrimitiveType> children;
    /** the parents */
    private DataSet parents;

    /**
     * constructor
     *
     * @param targetLayer  the target layer for the downloaded primitives. Must not be null.
     * @param children the collection of child primitives for which parents are to be downloaded
     *
     */
    public DownloadReferrersTask(OsmDataLayer targetLayer, Collection<OsmPrimitive> children) {
        super("Download referrers", false /* don't ignore exception*/);
        CheckParameterUtil.ensureParameterNotNull(targetLayer, "targetLayer");
        cancelled = false;
        this.children = new HashMap<Long, OsmPrimitiveType>();
        if (children != null) {
            for (OsmPrimitive p: children) {
                if (! p.isNew()) {
                    this.children.put(p.getId(), OsmPrimitiveType.from(p));
                }
            }
        }
        this.targetLayer = targetLayer;
        parents = new DataSet();
    }

    /**
     * constructor
     *
     * @param targetLayer  the target layer for the downloaded primitives. Must not be null.
     * @param primitives  the collection of children for which parents are to be downloaded. Children
     * are specified by their id and  their type.
     *
     */
    public DownloadReferrersTask(OsmDataLayer targetLayer, Map<Long, OsmPrimitiveType> children) {
        super("Download referrers", false /* don't ignore exception*/);
        CheckParameterUtil.ensureParameterNotNull(targetLayer, "targetLayer");
        cancelled = false;
        this.children = new HashMap<Long, OsmPrimitiveType>();
        if (children != null) {
            for (Entry<Long, OsmPrimitiveType> entry : children.entrySet()) {
                if (entry.getKey() > 0 && entry.getValue() != null) {
                    children.put(entry.getKey(), entry.getValue());
                }
            }
        }
        this.targetLayer = targetLayer;
        parents = new DataSet();
    }

    /**
     * constructor
     *
     * @param targetLayer  the target layer. Must not be null.
     * @param id the primitive id. id > 0 required.
     * @param type the primitive type. type != null required
     * @exception IllegalArgumentException thrown if id <= 0
     * @exception IllegalArgumentException thrown if type == null
     * @exception IllegalArgumentException thrown if targetLayer == null
     *
     */
    public DownloadReferrersTask(OsmDataLayer targetLayer, long id, OsmPrimitiveType type) throws IllegalArgumentException {
        super("Download referrers", false /* don't ignore exception*/);
        CheckParameterUtil.ensureParameterNotNull(targetLayer, "targetLayer");
        if (id <= 0)
            throw new IllegalArgumentException(MessageFormat.format("Id > 0 required, got {0}", id));
        CheckParameterUtil.ensureParameterNotNull(type, "type");
        cancelled = false;
        this.children = new HashMap<Long, OsmPrimitiveType>();
        this.children.put(id, type);
        this.targetLayer = targetLayer;
        parents = new DataSet();
    }

    /**
     * constructor
     *
     * @param targetLayer  the target layer. Must not be null.
     * @param primitiveId a PrimitiveId object.
     * @exception IllegalArgumentException thrown if id <= 0
     * @exception IllegalArgumentException thrown if targetLayer == null
     *
     */
    public DownloadReferrersTask(OsmDataLayer targetLayer, PrimitiveId primitiveId) throws IllegalArgumentException {
        super("Download referrers", false /* don't ignore exception*/);
        CheckParameterUtil.ensureParameterNotNull(targetLayer, "targetLayer");
        if (primitiveId.isNew())
            throw new IllegalArgumentException(MessageFormat.format("Cannot download referrers for new primitives (ID {0})", primitiveId.getUniqueId()));
        cancelled = false;
        this.children = new HashMap<Long, OsmPrimitiveType>();
        this.children.put(primitiveId.getUniqueId(), primitiveId.getType());
        this.targetLayer = targetLayer;
        parents = new DataSet();
    }

    @Override
    protected void cancel() {
        cancelled = true;
        synchronized(this) {
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
            ExceptionUtil.explainException(lastException);
            return;
        }

        DataSetMerger visitor = new DataSetMerger(targetLayer.data, parents);
        visitor.merge();
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        targetLayer.fireDataChange();
                        targetLayer.onPostDownloadFromServer();
                        Main.map.mapView.repaint();
                    }
                }
        );
        if (visitor.getConflicts().isEmpty())
            return;
        targetLayer.getConflicts().add(visitor.getConflicts());
        JOptionPane.showMessageDialog(
                Main.parent,
                trn("There was {0} conflict during import.",
                        "There were {0} conflicts during import.",
                        visitor.getConflicts().size(),
                        visitor.getConflicts().size()
                ),
                trn("Conflict during download", "Conflicts during download", visitor.getConflicts().size()),
                JOptionPane.WARNING_MESSAGE
        );
        Main.map.conflictDialog.unfurlDialog();
        Main.map.repaint();
    }

    protected void downloadParents(long id, OsmPrimitiveType type, ProgressMonitor progressMonitor) throws OsmTransferException{
        reader = new OsmServerBackreferenceReader(id, type);
        DataSet ds = reader.parseOsm(progressMonitor.createSubTaskMonitor(1, false));
        synchronized(this) { // avoid race condition in cancel()
            reader = null;
        }
        Collection<Way> ways = ds.getWays();
        DataSetMerger merger;
        if (!ways.isEmpty()) {
            reader = new MultiFetchServerObjectReader();
            for (Way w: ways) {
                ((MultiFetchServerObjectReader)reader).append(w.getNodes());
            }
            DataSet wayNodes = reader.parseOsm(progressMonitor.createSubTaskMonitor(1, false));
            synchronized(this) { // avoid race condition in cancel()
                reader = null;
            }
            merger = new DataSetMerger(ds, wayNodes);
            merger.merge();
        }
        merger = new DataSetMerger(parents, ds);
        merger.merge();
    }

    @Override
    protected void realRun() throws SAXException, IOException, OsmTransferException {
        try {
            progressMonitor.setTicksCount(children.size());
            int i=1;
            for (Entry<Long, OsmPrimitiveType> entry: children.entrySet()) {
                if (cancelled)
                    return;
                String msg = "";
                switch(entry.getValue()) {
                case NODE: msg = tr("({0}/{1}) Loading parents of node {2}", i+1,children.size(), entry.getKey()); break;
                case WAY: msg = tr("({0}/{1}) Loading parents of way {2}", i+1,children.size(), entry.getKey()); break;
                case RELATION: msg = tr("({0}/{1}) Loading parents of relation {2}", i+1,children.size(), entry.getKey()); break;
                }
                progressMonitor.subTask(msg);
                downloadParents(entry.getKey(), entry.getValue(), progressMonitor);
                i++;
            }
        } catch(Exception e) {
            if (cancelled)
                return;
            lastException = e;
        }
    }
}
