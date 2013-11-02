// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.tools.ImageProvider;

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

    /**
     * build the GUI
     */
    protected void build() {
        setLayout(new BorderLayout());
        referrers = new JList(model);
        referrers.setCellRenderer(new OsmPrimitivRenderer());
        add(new JScrollPane(referrers), BorderLayout.CENTER);
        referrers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        referrers.addMouseListener(new DblClickMouseAdapter());

        JPanel pnl = new JPanel();
        pnl.setLayout(new FlowLayout(FlowLayout.LEFT));

        ReloadAction reloadAction = new ReloadAction();
        referrers.getModel().addListDataListener(reloadAction);
        pnl.add(new SideButton(reloadAction));
        pnl.add(cbReadFull = new JCheckBox(tr("including immediate children of parent relations")));

        editAction = new EditAction();
        referrers.getSelectionModel().addListSelectionListener(editAction);
        pnl.add(new SideButton(editAction));
        add(pnl, BorderLayout.SOUTH);
    }

    public ReferringRelationsBrowser(OsmDataLayer layer, ReferringRelationsBrowserModel model) {
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

        @Override
        public void actionPerformed(ActionEvent e) {
            boolean full = cbReadFull.isSelected();
            final ParentRelationLoadingTask task = new ParentRelationLoadingTask(
                    model.getRelation(),
                    getLayer(),
                    full,
                    new PleaseWaitProgressMonitor(tr("Loading parent relations"))
            );
            task.setContinuation(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (task.isCanceled() || task.hasError())
                                return;
                            model.populate(task.getParents());
                        }
                    }
            );
            Main.worker.submit(task);
        }

        @Override
        public void contentsChanged(ListDataEvent e) {
            refreshEnabled();
        }

        @Override
        public void intervalAdded(ListDataEvent e) {
            refreshEnabled();
        }

        @Override
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

        @Override
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

        @Override
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
}
