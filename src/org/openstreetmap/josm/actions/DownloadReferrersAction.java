// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
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
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmServerBackreferenceReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ExceptionUtil;
import org.openstreetmap.josm.tools.Shortcut;
import org.xml.sax.SAXException;

/**
 * This action loads the set of primitives referring to the current selection from the OSM
 * server.
 *
 */
public class DownloadReferrersAction extends JosmAction{

    public DownloadReferrersAction() {
        super(tr("Download parent ways/relations..."), "downloadreferrers", tr("Download primitives referring to one of the selected primitives"),
                Shortcut.registerShortcut("file:downloadreferrers", tr("File: {0}", tr("Download parent ways/relations...")), KeyEvent.VK_D, Shortcut.GROUPS_ALT2+Shortcut.GROUP_HOTKEY), true);
        putValue("help", ht("/Action/Downloadreferrers"));
    }

    /**
     * Downloads the primitives referring to the primitives in <code>primitives</code>
     * into the target layer <code>targetLayer</code>.
     * Does nothing if primitives is null or empty.
     *
     * @param targetLayer  the target layer. Must not be null.
     * @param children the collection of child primitives.
     * @exception IllegalArgumentException thrown if targetLayer is null
     */
    static public void downloadReferrers(OsmDataLayer targetLayer, Collection<OsmPrimitive> children) throws IllegalArgumentException {
        if (children == null || children.isEmpty()) return;
        Main.worker.submit(new DownloadReferrersTask(targetLayer, children));
    }

    /**
     * Downloads the primitives referring to the primitives in <code>primitives</code>
     * into the target layer <code>targetLayer</code>.
     * Does nothing if primitives is null or empty.
     *
     * @param targetLayer  the target layer. Must not be null.
     * @param children the collection of primitives, given as map of ids and types
     * @exception IllegalArgumentException thrown if targetLayer is null
     */
    static public void downloadReferrers(OsmDataLayer targetLayer, Map<Long, OsmPrimitiveType> children) throws IllegalArgumentException {
        if (children == null || children.isEmpty()) return;
        Main.worker.submit(new DownloadReferrersTask(targetLayer, children));
    }

    /**
     * Downloads the primitives referring to the primitive given by <code>id</code> and
     * <code>type</code>.
     *
     *
     * @param targetLayer  the target layer. Must not be null.
     * @param id the primitive id. id > 0 required.
     * @param type the primitive type. type != null required
     * @exception IllegalArgumentException thrown if targetLayer is null
     * @exception IllegalArgumentException thrown if id <= 0
     * @exception IllegalArgumentException thrown if type == null
     */
    static public void downloadReferrers(OsmDataLayer targetLayer, long id, OsmPrimitiveType type) throws IllegalArgumentException {
        if (id <= 0)
            throw new IllegalArgumentException(MessageFormat.format("Id > 0 required, got {0}", id));
        CheckParameterUtil.ensureParameterNotNull(type, "type");
        Main.worker.submit(new DownloadReferrersTask(targetLayer, id, type));
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled() || ! Main.isDisplayingMapView())
            return;
        OsmDataLayer layer = Main.map.mapView.getEditLayer();
        if (layer == null)
            return;
        Collection<OsmPrimitive> primitives = layer.data.getSelected();
        downloadReferrers(layer,primitives);
    }

    /**
     * The asynchronous task for downloading referring primitives
     *
     */
    public static class DownloadReferrersTask extends PleaseWaitRunnable {
        private boolean cancelled;
        private Exception lastException;
        private OsmServerBackreferenceReader reader;
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
        }

        protected void downloadParents(long id, OsmPrimitiveType type, ProgressMonitor progressMonitor) throws OsmTransferException{
            reader = new OsmServerBackreferenceReader(id, type);
            DataSet ds = reader.parseOsm(progressMonitor);
            synchronized(this) { // avoid race condition in cancel()
                reader = null;
            }
            DataSetMerger visitor = new DataSetMerger(parents, ds);
            visitor.merge();
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
                    downloadParents(entry.getKey(), entry.getValue(), progressMonitor.createSubTaskMonitor(1, false));
                    i++;
                }
            } catch(Exception e) {
                if (cancelled)
                    return;
                lastException = e;
            }
        }
    }

    @Override
    protected void updateEnabledState() {
        if (getCurrentDataSet() == null) {
            setEnabled(false);
        } else {
            updateEnabledState(getCurrentDataSet().getSelected());
        }
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }
}
