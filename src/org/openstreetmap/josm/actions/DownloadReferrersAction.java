// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Collection;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.MergeVisitor;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.Layer.LayerChangeListener;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmServerBackreferenceReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.Shortcut;
import org.xml.sax.SAXException;

/**
 * This action loads the set of primitives referring to the current selection from the OSM
 * server.
 *
 *
 */
public class DownloadReferrersAction extends JosmAction implements SelectionChangedListener, LayerChangeListener {

    public DownloadReferrersAction() {
        super(tr("Download referrers from OSM..."), "downloadreferrers", tr("Download primitives referring to one of the selected primitives"),
                Shortcut.registerShortcut("file:downloadreferrers", tr("File: {0}", tr("Download referrers...")), KeyEvent.VK_D, Shortcut.GROUPS_ALT2+Shortcut.GROUP_HOTKEY), true);

        Layer.listeners.add(this);
        DataSet.selListeners.add(this);
        refreshEnabled();
    }

    /**
     * Downloads the primitives referring to the primitives in <code>primitives</code>.
     * Does nothing if primitives is null or empty.
     *
     * @param primitives the collection of primitives.
     */
    public void downloadReferrers(Collection<OsmPrimitive> primitives) {
        if (primitives == null || primitives.isEmpty()) return;
        Main.worker.submit(new DownloadReferrersTask(primitives));
    }


    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        OsmDataLayer layer = Main.map.mapView.getEditLayer();
        Collection<OsmPrimitive> primitives = layer.data.getSelected();
        downloadReferrers(primitives);
    }

    /**
     * The asynchronous task for downloading referring primitives
     *
     */
    class DownloadReferrersTask extends PleaseWaitRunnable {
        private DataSet ds;
        private boolean cancelled;
        Exception lastException;
        private Collection<OsmPrimitive> primitives;
        private DataSet parents;

        public DownloadReferrersTask(Collection<OsmPrimitive> primitives) {
            super("Download referrers", false /* don't ignore exception*/);
            cancelled = false;
            this.primitives = primitives;
            parents = new DataSet();
        }

        protected void showLastException() {
            String msg = lastException.getMessage();
            if (msg == null) {
                msg = lastException.toString();
            }
            JOptionPane.showMessageDialog(
                    Main.map,
                    msg,
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
        }

        @Override
        protected void cancel() {
            cancelled = true;
            OsmApi.getOsmApi().cancel();
        }

        @Override
        protected void finish() {
            if (cancelled)
                return;
            if (lastException != null) {
                showLastException();
                return;
            }

            MergeVisitor visitor = new MergeVisitor(Main.map.mapView.getEditLayer().data, parents);
            visitor.merge();
            SwingUtilities.invokeLater(
                    new Runnable() {
                        public void run() {
                            Main.map.mapView.getEditLayer().fireDataChange();
                            Main.map.mapView.repaint();
                        }
                    }
            );
            if (visitor.getConflicts().isEmpty())
                return;
            Main.map.mapView.getEditLayer().getConflicts().add(visitor.getConflicts());
            JOptionPane op = new JOptionPane(
                    tr("There were {0} conflicts during import.",
                            visitor.getConflicts().size()),
                            JOptionPane.WARNING_MESSAGE
            );
            JDialog dialog = op.createDialog(null, tr("Conflicts in data"));
            dialog.setAlwaysOnTop(true);
            dialog.setModal(true);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setVisible(true);
        }

        protected void downloadParents(OsmPrimitive primitive, ProgressMonitor progressMonitor) throws OsmTransferException{
            OsmServerBackreferenceReader reader = new OsmServerBackreferenceReader(primitive);
            DataSet ds = reader.parseOsm(progressMonitor);
            MergeVisitor visitor = new MergeVisitor(parents, ds);
            visitor.merge();
        }

        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {
            try {
                progressMonitor.setTicksCount(primitives.size());
                int i=1;
                for (OsmPrimitive primitive: primitives) {
                    if (cancelled)
                        return;
                    progressMonitor.subTask(tr("({0}/{1}) Loading parents of primitive {2}", i+1,primitives.size(), primitive.getName()));
                    downloadParents(primitive, progressMonitor.createSubTaskMonitor(1, false));
                    i++;
                }
            } catch(Exception e) {
                if (cancelled)
                    return;
                lastException = e;
            }
        }
    }

    /**
     * Refreshes the enabled state
     *
     */
    protected void refreshEnabled() {
        setEnabled(Main.map != null
                && Main.map.mapView !=null
                && Main.map.mapView.getEditLayer() != null
                && ! Main.map.mapView.getEditLayer().data.getSelected().isEmpty()
        );
    }

    /* ---------------------------------------------------------------------------------- */
    /* Interface SelectionChangeListener                                                  */
    /* ---------------------------------------------------------------------------------- */
    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        refreshEnabled();
    }

    /* ---------------------------------------------------------------------------------- */
    /* Interface LayerChangeListener                                                      */
    /* ---------------------------------------------------------------------------------- */
    public void activeLayerChange(Layer oldLayer, Layer newLayer) {
        refreshEnabled();
    }

    public void layerAdded(Layer newLayer) {
        refreshEnabled();
    }

    public void layerRemoved(Layer oldLayer) {
        refreshEnabled();
    }
}
