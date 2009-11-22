package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSetListener;
import org.openstreetmap.josm.data.osm.NameFormatter;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.layer.DataChangeListener;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.Layer.LayerChangeListener;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * A dialog showing all known relations, with buttons to add, edit, and
 * delete them.
 *
 * We don't have such dialogs for nodes, segments, and ways, because those
 * objects are visible on the map and can be selected there. Relations are not.
 */
public class RelationListDialog extends ToggleDialog implements LayerChangeListener, DataSetListener, DataChangeListener {
    //private static final Logger logger = Logger.getLogger(RelationListDialog.class.getName());

    /** The display list. */
    private JList displaylist;
    /** the list model used */
    private RelationListModel model;

    /** the edit action */
    private EditAction editAction;
    /** the delete action */
    private DeleteAction deleteAction;
    private NewAction newAction;
    /** the popup menu */
    private RelationDialogPopupMenu popupMenu;

    /**
     * constructor
     */
    public RelationListDialog() {
        super(tr("Relations"), "relationlist", tr("Open a list of all relations."),
                Shortcut.registerShortcut("subwindow:relations", tr("Toggle: {0}", tr("Relations")), KeyEvent.VK_R, Shortcut.GROUP_LAYER), 150);

        // create the list of relations
        //
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
        model = new RelationListModel(selectionModel);
        displaylist = new JList(model);
        displaylist.setSelectionModel(selectionModel);
        displaylist.setCellRenderer(new OsmPrimitivRenderer());
        displaylist.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        displaylist.addMouseListener(new MouseEventHandler());
        add(new JScrollPane(displaylist), BorderLayout.CENTER);

        // create the panel with buttons
        //
        JPanel buttonPanel = new JPanel(new GridLayout(1,3));

        // the new action
        //
        newAction = new NewAction();
        buttonPanel.add(new SideButton(newAction), GBC.std());

        // the edit action
        //
        editAction = new EditAction();
        displaylist.addListSelectionListener(editAction);
        buttonPanel.add(new SideButton(editAction), GBC.std());

        // the duplicate action
        //
        DuplicateAction duplicateAction = new DuplicateAction();
        displaylist.addListSelectionListener(duplicateAction);
        buttonPanel.add(new SideButton(duplicateAction), GBC.std());

        // the delete action
        //
        deleteAction = new DeleteAction();
        displaylist.addListSelectionListener(deleteAction);
        buttonPanel.add(new SideButton(deleteAction), GBC.eol());

        // the select action
        //
        SelectAction selectAction = new SelectAction();
        displaylist.addListSelectionListener(selectAction);
        buttonPanel.add(new SideButton(selectAction), GBC.eol());

        add(buttonPanel, BorderLayout.SOUTH);

        // activate DEL in the list of relations
        displaylist.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,0), "deleteRelation");
        displaylist.getActionMap().put("deleteRelation", deleteAction);

        popupMenu = new RelationDialogPopupMenu();
    }

    @Override public void showNotify() {
        Layer.listeners.add(this);
        Layer.listeners.add(newAction);
        // Register as a data set listener for the current edit layer only.
        // See also activeLayerChanged
        if (Main.main.getEditLayer() != null) {
            Main.main.getEditLayer().data.addDataSetListener(this);
        }
    }

    @Override public void hideNotify() {
        Layer.listeners.remove(this);
        Layer.listeners.remove(newAction);
        Layer.listeners.add(newAction);
        // unregistering from *all* data layer is somewhat overkill but it
        // doesn't harm either.
        for (OsmDataLayer layer:Main.map.mapView.getLayersOfType(OsmDataLayer.class)) {
            layer.data.removeDataSetListener(this);
        }
    }

    /**
     * Initializes the relation list dialog from a layer. If <code>layer</code> is null
     * or if it isn't an {@see OsmDataLayer} the dialog is reset to an empty dialog.
     * Otherwise it is initialized with the list of non-deleted and visible relations
     * in the layer's dataset.
     * 
     * @param layer the layer. May be null.
     */
    protected void initFromLayer(Layer layer) {
        if (layer == null || ! (layer instanceof OsmDataLayer)) {
            model.setRelations(null);
            return;
        }
        OsmDataLayer l = (OsmDataLayer)layer;
        model.setRelations(l.data.getRelations());
        if(model.getSize() > 0) {
            setTitle(tr("Relations: {0}", model.getSize()));
        } else {
            setTitle(tr("Relations"));
        }
    }

    /**
     * Adds a selection listener to the relation list.
     *
     * @param listener the listener to add
     */
    public void addListSelectionListener(ListSelectionListener listener) {
        displaylist.addListSelectionListener(listener);
    }

    /**
     * Removes a selection listener from the relation list.
     *
     * @param listener the listener to remove
     */
    public void removeListSelectionListener(ListSelectionListener listener) {
        displaylist.removeListSelectionListener(listener);
    }

    /**
     * @return The selected relation in the list
     */
    private Relation getSelected() {
        if(model.getSize() == 1) {
            displaylist.setSelectedIndex(0);
        }
        return (Relation) displaylist.getSelectedValue();
    }

    /**
     * Selects the relation <code>relation</code> in the list of relations.
     *
     * @param relation  the relation
     */
    public void selectRelation(Relation relation) {
        if (relation == null) {
            model.setSelectedRelations(null);
        } else {
            model.setSelectedRelations(Collections.singletonList(relation));
        }
    }

    class MouseEventHandler extends MouseAdapter {
        protected void setCurrentRelationAsSelection() {
            Main.main.getCurrentDataSet().setSelected((Relation)displaylist.getSelectedValue());
        }

        protected void editCurrentRelation() {
            new EditAction().launchEditor(getSelected());
        }

        @Override public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                if (e.isControlDown()) {
                    editCurrentRelation();
                } else {
                    setCurrentRelationAsSelection();
                }
            }
        }
        private void openPopup(MouseEvent e) {
            Point p = e.getPoint();
            int index = displaylist.locationToIndex(p);
            if (index < 0) return;
            if (!displaylist.getCellBounds(index, index).contains(e.getPoint()))
                return;
            if (! displaylist.isSelectedIndex(index)) {
                displaylist.setSelectedIndex(index);
            }
            popupMenu.show(RelationListDialog.this, p.x, p.y-3);
        }
        @Override public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                openPopup(e);
            }
        }
        @Override public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                openPopup(e);
            }
        }
    }

    /**
     * The edit action
     *
     */
    class EditAction extends AbstractAction implements ListSelectionListener{
        public EditAction() {
            putValue(SHORT_DESCRIPTION,tr( "Open an editor for the selected relation"));
            //putValue(NAME, tr("Edit"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "edit"));
            setEnabled(false);
        }
        protected Collection<RelationMember> getMembersForCurrentSelection(Relation r) {
            Collection<RelationMember> members = new HashSet<RelationMember>();
            Collection<OsmPrimitive> selection = Main.map.mapView.getEditLayer().data.getSelected();
            for (RelationMember member: r.getMembers()) {
                if (selection.contains(member.getMember())) {
                    members.add(member);
                }
            }
            return members;
        }

        public void launchEditor(Relation toEdit) {
            if (toEdit == null)
                return;
            RelationEditor.getEditor(Main.map.mapView.getEditLayer(),toEdit, getMembersForCurrentSelection(toEdit)).setVisible(true);
        }

        public void actionPerformed(ActionEvent e) {
            if (!isEnabled())
                return;
            launchEditor(getSelected());
        }

        public void valueChanged(ListSelectionEvent e) {
            setEnabled(displaylist.getSelectedIndices() != null && displaylist.getSelectedIndices().length == 1);
        }
    }

    /**
     * The delete action
     *
     */
    class DeleteAction extends AbstractAction implements ListSelectionListener {
        class AbortException extends Exception {}

        public DeleteAction() {
            putValue(SHORT_DESCRIPTION,tr("Delete the selected relation"));
            //putValue(NAME, tr("Delete"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "delete"));
            setEnabled(false);
        }

        protected void deleteRelation(Relation toDelete) {
            if (toDelete == null)
                return;
            org.openstreetmap.josm.actions.mapmode.DeleteAction.deleteRelation(
                    Main.main.getEditLayer(),
                    toDelete
            );
        }

        public void actionPerformed(ActionEvent e) {
            if (!isEnabled()) return;
            int [] idx  = displaylist.getSelectedIndices();
            ArrayList<Relation> toDelete = new ArrayList<Relation>(idx.length);
            for (int i: idx) {
                toDelete.add(model.getRelation(i));
            }
            for (Relation r: toDelete) {
                deleteRelation(r);
            }
        }

        public void valueChanged(ListSelectionEvent e) {
            setEnabled(displaylist.getSelectedIndices() != null && displaylist.getSelectedIndices().length > 0);
        }
    }

    /**
     * The action for creating a new relation
     *
     */
    class NewAction extends AbstractAction implements LayerChangeListener{
        public NewAction() {
            putValue(SHORT_DESCRIPTION,tr("Create a new relation"));
            //putValue(NAME, tr("New"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "addrelation"));
            setEnabled(false);
        }

        public void run() {
            RelationEditor.getEditor(Main.main.getEditLayer(),null, null).setVisible(true);
        }

        public void actionPerformed(ActionEvent e) {
            run();
        }

        protected void updateEnabledState() {
            setEnabled(Main.main != null && Main.main.getEditLayer() != null);
        }

        public void activeLayerChange(Layer oldLayer, Layer newLayer) {
            updateEnabledState();
        }

        public void layerAdded(Layer newLayer) {
            updateEnabledState();
        }

        public void layerRemoved(Layer oldLayer) {
            updateEnabledState();
        }
    }

    /**
     * Creates a new relation with a copy of the current editor state
     *
     */
    class DuplicateAction extends AbstractAction implements ListSelectionListener {
        public DuplicateAction() {
            putValue(SHORT_DESCRIPTION, tr("Create a copy of this relation and open it in another editor window"));
            putValue(SMALL_ICON, ImageProvider.get("duplicate"));
            //putValue(NAME, tr("Duplicate"));
            updateEnabledState();
        }

        public void launchEditorForDuplicate(Relation original) {
            Relation copy = new Relation(original, true);
            copy.setModified(true);
            RelationEditor editor = RelationEditor.getEditor(
                    Main.main.getEditLayer(),
                    copy,
                    null /* no selected members */
            );
            editor.setVisible(true);
        }

        public void actionPerformed(ActionEvent e) {
            if (!isEnabled())
                return;
            launchEditorForDuplicate(getSelected());
        }

        protected void updateEnabledState() {
            setEnabled(displaylist.getSelectedIndices() != null && displaylist.getSelectedIndices().length == 1);
        }

        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    /**
     * Sets the current selection to the list of relations selected in this dialog
     *
     */
    class SelectAction extends AbstractAction implements ListSelectionListener{
        public SelectAction() {
            putValue(SHORT_DESCRIPTION,tr("Set the current selection to the list of selected relations"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "select"));
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            if (!isEnabled()) return;
            int [] idx = displaylist.getSelectedIndices();
            if (idx == null || idx.length == 0) return;
            ArrayList<OsmPrimitive> selection = new ArrayList<OsmPrimitive>(idx.length);
            for (int i: idx) {
                selection.add(model.getRelation(i));
            }
            Main.map.mapView.getEditLayer().data.setSelected(selection);
        }

        public void valueChanged(ListSelectionEvent e) {
            setEnabled(displaylist.getSelectedIndices() != null && displaylist.getSelectedIndices().length > 0);
        }
    }

    /**
     * Sets the current selection to the list of relations selected in this dialog
     *
     */
    class SelectMembersAction extends AbstractAction implements ListSelectionListener{
        public SelectMembersAction() {
            putValue(SHORT_DESCRIPTION,tr("Select the members of all selected relations"));
            putValue(SMALL_ICON, ImageProvider.get("selectall"));
            putValue(NAME, tr("Select members"));
            updateEnabledState();
        }

        public void actionPerformed(ActionEvent e) {
            if (!isEnabled()) return;
            List<Relation> relations = model.getSelectedRelations();
            HashSet<OsmPrimitive> members = new HashSet<OsmPrimitive>();
            for(Relation r: relations) {
                members.addAll(r.getMemberPrimitives());
            }
            Main.map.mapView.getEditLayer().data.setSelected(members);
        }

        protected void updateEnabledState() {
            setEnabled(displaylist.getSelectedIndices() != null && displaylist.getSelectedIndices().length > 0);
        }

        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    /**
     * The action for downloading members of all selected relations
     * 
     */
    class DownloadMembersAction extends AbstractAction implements ListSelectionListener{

        public DownloadMembersAction() {
            putValue(SHORT_DESCRIPTION,tr("Download all members of the selected relations"));
            putValue(NAME, tr("Download members"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "downloadincomplete"));
            putValue("help", ht("/Dialog/RelationList#DownloadMembers"));
            updateEnabledState();
        }

        protected void updateEnabledState() {
            setEnabled(! model.getSelectedNonNewRelations().isEmpty());
        }

        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }

        public void actionPerformed(ActionEvent e) {
            List<Relation> relations = model.getSelectedNonNewRelations();
            if (relations.isEmpty())
                return;
            Main.worker.submit(new GenericRelationEditor.DownloadTask(
                    model.getSelectedNonNewRelations(),
                    Main.map.mapView.getEditLayer(), null));
        }
    }

    /**
     * The list model for the list of relations displayed in the relation list
     * dialog.
     *
     */
    private static  class RelationListModel extends AbstractListModel {
        private final ArrayList<Relation> relations = new ArrayList<Relation>();
        private DefaultListSelectionModel selectionModel;

        public RelationListModel(DefaultListSelectionModel selectionModel) {
            this.selectionModel = selectionModel;
        }

        public Relation getRelation(int idx) {
            return relations.get(idx);
        }

        public synchronized void setRelations(Collection<Relation> relations) {
            List<Relation> sel =  getSelectedRelations();
            this.relations.clear();
            for (Relation r: relations) {
                if (! r.isDeleted() && r.isVisible() && !r.incomplete) {
                    this.relations.add(r);
                }
            }
            sort();
            fireIntervalAdded(this, 0, getSize());
            setSelectedRelations(sel);
        }

        public synchronized void sort() {
            Collections.sort(
                    relations,
                    new Comparator<Relation>() {
                        NameFormatter formatter = DefaultNameFormatter.getInstance();

                        public int compare(Relation r1, Relation r2) {
                            return r1.getDisplayName(formatter).compareTo(r2.getDisplayName(formatter));
                        }
                    }
            );
        }

        /**
         * Add all relations in <code>addedPrimitives</code> to the model for the
         * relation list dialog
         * 
         * @param addedPrimitives the collection of added primitives. May include nodes,
         * ways, and relations.
         */
        public synchronized void addRelations(Collection<? extends OsmPrimitive> addedPrimitives) {
            if (addedPrimitives == null || addedPrimitives.isEmpty()) return;
            boolean added = false;
            for (OsmPrimitive p: addedPrimitives) {
                if (! (p instanceof Relation)) {
                    continue;
                }
                if (relations.contains(p)) {
                    continue;
                }
                relations.add((Relation)p);
                added = true;
            }
            if (added) {
                List<Relation> sel = getSelectedRelations();
                sort();
                fireIntervalAdded(this, 0, getSize());
                setSelectedRelations(sel);
            }
        }

        /**
         * Removes all relations in <code>removedPrimitives</code> from the model
         * 
         * @param removedPrimitives the removed primitives. May include nodes, ways,
         *   and relations
         */
        public synchronized void removeRelations(Collection<? extends OsmPrimitive> removedPrimitives) {
            if (removedPrimitives == null) return;
            // extract the removed relations
            //
            Set<Relation> removedRelations = new HashSet<Relation>();
            for (OsmPrimitive p: removedPrimitives) {
                if (! (p instanceof Relation)) {
                    continue;
                }
                removedRelations.add((Relation)p);
            }
            if (removedRelations.isEmpty())
                return;
            int size = relations.size();
            relations.removeAll(removedRelations);
            if (size != relations.size()) {
                List<Relation> sel = getSelectedRelations();
                sort();
                fireContentsChanged(this, 0, getSize());
                setSelectedRelations(sel);
            }
        }

        public Object getElementAt(int index) {
            return relations.get(index);
        }

        public int getSize() {
            return relations.size();
        }

        /**
         * Replies the list of selected, non-new relations. Empty list,
         * if there are no selected, non-new relations.
         * 
         * @return the list of selected, non-new relations.
         */
        public List<Relation> getSelectedNonNewRelations() {
            ArrayList<Relation> ret = new ArrayList<Relation>();
            for (int i=0; i<getSize();i++) {
                if (!selectionModel.isSelectedIndex(i)) {
                    continue;
                }
                if (relations.get(i).isNew()) {
                    continue;
                }
                ret.add(relations.get(i));
            }
            return ret;
        }

        /**
         * Replies the list of selected relations. Empty list,
         * if there are no selected relations.
         * 
         * @return the list of selected, non-new relations.
         */
        public List<Relation> getSelectedRelations() {
            ArrayList<Relation> ret = new ArrayList<Relation>();
            for (int i=0; i<getSize();i++) {
                if (!selectionModel.isSelectedIndex(i)) {
                    continue;
                }
                ret.add(relations.get(i));
            }
            return ret;
        }

        /**
         * Sets the selected relations.
         * 
         * @return sel the list of selected relations
         */
        public synchronized void setSelectedRelations(List<Relation> sel) {
            selectionModel.clearSelection();
            if (sel == null || sel.isEmpty() || relations == null)
                return;
            for (Relation r: sel) {
                int i = relations.indexOf(r);
                if (i<0) {
                    continue;
                }
                selectionModel.addSelectionInterval(i,i);
            }
        }
    }

    class RelationDialogPopupMenu extends JPopupMenu {
        protected void build() {
            // -- download members action
            //
            DownloadMembersAction downloadMembersAction = new DownloadMembersAction();
            displaylist.addListSelectionListener(downloadMembersAction);
            add(downloadMembersAction);

            // -- select members action
            //
            SelectMembersAction selectMembersAction = new SelectMembersAction();
            displaylist.addListSelectionListener(selectMembersAction);
            add(selectMembersAction);
        }

        public RelationDialogPopupMenu() {
            build();
        }
    }

    /* ---------------------------------------------------------------------------------- */
    /* LayerChangeListener                                                                */
    /* ---------------------------------------------------------------------------------- */
    public void activeLayerChange(Layer a, Layer b) {
        initFromLayer(b);
        if (a != null && a instanceof OsmDataLayer) {
            ((OsmDataLayer)a).data.removeDataSetListener(this);
            ((OsmDataLayer)a).listenerDataChanged.remove(this);
        }
        if (b != null && b instanceof OsmDataLayer) {
            ((OsmDataLayer)b).data.addDataSetListener(this);
            ((OsmDataLayer)b).listenerDataChanged.add(this);
        }

    }
    public void layerRemoved(Layer a) {/* irrelevant in this context */}
    public void layerAdded(Layer a) {/* irrelevant in this context */}


    /* ---------------------------------------------------------------------------------- */
    /* DataSetListener                                                                    */
    /* ---------------------------------------------------------------------------------- */

    public void nodeMoved(Node node) {/* irrelevant in this context */}

    public void wayNodesChanged(Way way) {/* irrelevant in this context */}

    public void primtivesAdded(final Collection<? extends OsmPrimitive> added) {
        Runnable task = new Runnable() {
            public void run() {
                model.addRelations(added);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    public void primtivesRemoved(final Collection<? extends OsmPrimitive> removed) {
        Runnable task = new Runnable() {
            public void run() {
                model.removeRelations(removed);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    public void relationMembersChanged(final Relation r) {
        Runnable task = new Runnable() {
            public void run() {
                List<Relation> sel = model.getSelectedRelations();
                model.sort();
                model.setSelectedRelations(sel);
                displaylist.repaint();
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    public void tagsChanged(OsmPrimitive prim) {
        if (prim == null || ! (prim instanceof Relation))
            return;
        Runnable task = new Runnable() {
            public void run() {
                // trigger a sort of the relation list because the display name may
                // have changed
                //
                List<Relation> sel = model.getSelectedRelations();
                model.sort();
                model.setSelectedRelations(sel);
                displaylist.repaint();
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    public void dataChanged() {
        Layer l = Main.main.getEditLayer();
        if (l != null) {
            initFromLayer(l);
        }
    }

    /* ---------------------------------------------------------------------------------- */
    /* DataSetListener                                                                    */
    /* ---------------------------------------------------------------------------------- */
    public void dataChanged(OsmDataLayer l) {
        if (l != null && l == Main.main.getEditLayer()) {
            initFromLayer(l);
        }
    }
}
