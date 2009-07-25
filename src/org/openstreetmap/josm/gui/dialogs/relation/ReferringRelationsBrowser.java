// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSource;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.visitor.MergeVisitor;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmServerBackreferenceReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.ImageProvider;
import org.xml.sax.SAXException;

/**
 * This is browser for a list of relations which refer to another relations
 *
 *
 */
public class ReferringRelationsBrowser extends JPanel {

    /** the list of relations */
    private JList referrers;
    private ReferringRelationsBrowserModel model;
    private OsmDataLayer layer;
    private JCheckBox cbReadFull;
    private EditAction editAction;
    private final GenericRelationEditor relationEditor;

    /**
     * build the GUI
     */
    protected void build() {
        setLayout(new BorderLayout());
        referrers = new JList(model);
        referrers.setCellRenderer(new OsmPrimitivRenderer());
        add(referrers, BorderLayout.CENTER);
        referrers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        referrers.addMouseListener(new DblClickMouseAdapter());

        JPanel pnl = new JPanel();
        pnl.setLayout(new FlowLayout(FlowLayout.LEFT));

        ReloadAction reloadAction = new ReloadAction();
        referrers.getModel().addListDataListener(reloadAction);
        pnl.add(new SideButton(reloadAction));
        pnl.add(new JLabel(tr("including immediate children of parent relations")));
        pnl.add(cbReadFull = new JCheckBox());

        editAction = new EditAction();
        referrers.getSelectionModel().addListSelectionListener(editAction);
        pnl.add(new SideButton(editAction));
        add(pnl, BorderLayout.SOUTH);
    }

    public ReferringRelationsBrowser(OsmDataLayer layer, ReferringRelationsBrowserModel model, GenericRelationEditor relationEditor) {
        this.relationEditor = relationEditor;
        this.model = model;
        this.layer = layer;
        build();
    }

    public void init() {
        model.populate(getLayer().data);
    }

    protected OsmDataLayer getLayer() {
        return layer;
    }

    /**
     * Action for loading the parent relations of a relation
     *
     */
    class ReloadAction extends AbstractAction implements ListDataListener {
        public ReloadAction() {
            putValue(SHORT_DESCRIPTION, tr("Load parent relations"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "refresh"));
            putValue(NAME, tr("Reload"));
            refreshEnabled();
        }

        protected void refreshEnabled() {
            setEnabled(model.canReload());
        }

        public void actionPerformed(ActionEvent e) {
            boolean full = cbReadFull.isSelected();
            ReloadTask task = new ReloadTask(full, relationEditor);
            Main.worker.submit(task);
        }

        public void contentsChanged(ListDataEvent e) {
            refreshEnabled();
        }

        public void intervalAdded(ListDataEvent e) {
            refreshEnabled();
        }

        public void intervalRemoved(ListDataEvent e) {
            refreshEnabled();
        }
    }

    /**
     * Action for editing the currently selected relation
     *
     */
    class EditAction extends AbstractAction implements ListSelectionListener {
        public EditAction() {
            putValue(SHORT_DESCRIPTION, tr("Edit the currently selected relation"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "edit"));
            putValue(NAME, tr("Edit"));
            refreshEnabled();
        }

        protected void refreshEnabled() {
            setEnabled(referrers.getSelectionModel().getMinSelectionIndex() >=0);
        }

        public void actionPerformed(ActionEvent e) {
            run();
        }

        public void run() {
            int idx = referrers.getSelectedIndex();
            if (idx < 0) return;
            Relation r = model.get(idx);
            if (r == null) return;
            RelationEditor editor = RelationEditor.getEditor(getLayer(), r, null);
            editor.setVisible(true);
        }

        public void valueChanged(ListSelectionEvent e) {
            refreshEnabled();
        }
    }

    class DblClickMouseAdapter extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2)  {
                editAction.run();
            }
        }
    }

    /**
     * Asynchronous task for loading the parent relations
     *
     */
    class ReloadTask extends PleaseWaitRunnable {
        private boolean cancelled;
        private Exception lastException;
        private DataSet referrers;
        private boolean full;

        public ReloadTask(boolean full, Dialog parent) {
            super(tr("Download referring relations"), new PleaseWaitProgressMonitor(parent), false /* don't ignore exception */);
            referrers = null;
        }
        @Override
        protected void cancel() {
            cancelled = true;
            OsmApi.getOsmApi().cancel();
        }

        protected void showLastException() {
            String msg = lastException.getMessage();
            if (msg == null) {
                msg = lastException.toString();
            }
            JOptionPane.showMessageDialog(
                    null,
                    msg,
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
        }

        @Override
        protected void finish() {
            if (cancelled) return;
            if (lastException != null) {
                showLastException();
                return;
            }
            final ArrayList<Relation> parents = new ArrayList<Relation>();
            for (Relation parent : referrers.relations) {
                parents.add((Relation)getLayer().data.getPrimitiveById(parent.id));
            }
            SwingUtilities.invokeLater(
                    new Runnable() {
                        public void run() {
                            model.populate(parents);
                        }
                    }
            );
        }

        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {
            try {
                progressMonitor.indeterminateSubTask(null);
                OsmServerBackreferenceReader reader = new OsmServerBackreferenceReader(model.getRelation(), full);
                referrers = reader.parseOsm(progressMonitor.createSubTaskMonitor(1, false));
                if (referrers != null) {
                    final MergeVisitor visitor = new MergeVisitor(getLayer().data, referrers);
                    visitor.merge();

                    // copy the merged layer's data source info
                    for (DataSource src : referrers.dataSources) {
                        getLayer().data.dataSources.add(src);
                    }
                    // FIXME: this is necessary because there are  dialogs listening
                    // for DataChangeEvents which manipulate Swing components on this
                    // thread.
                    //
                    SwingUtilities.invokeLater(
                            new Runnable() {
                                public void run() {
                                    getLayer().fireDataChange();
                                }
                            }
                    );

                    if (visitor.getConflicts().isEmpty())
                        return;
                    getLayer().getConflicts().add(visitor.getConflicts());
                    JOptionPane op = new JOptionPane(
                            tr("There were {0} conflicts during import.",
                                    visitor.getConflicts().size()),
                                    JOptionPane.WARNING_MESSAGE
                    );
                    JDialog dialog = op.createDialog(ReferringRelationsBrowser.this, tr("Conflicts in data"));
                    dialog.setAlwaysOnTop(true);
                    dialog.setModal(true);
                    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                    dialog.setVisible(true);
                }
            } catch(Exception e) {
                if (cancelled) {
                    System.out.println(tr("Warning: ignoring exception because task is cancelled. Exception: {0}", e.toString()));
                    return;
                }
                lastException = e;
            }
        }
    }
}
