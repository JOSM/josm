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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

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
import org.openstreetmap.josm.data.osm.DataSet;
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
public class RelationListDialog extends ToggleDialog implements LayerChangeListener, DataSetListener {
    private static final Logger logger = Logger.getLogger(RelationListDialog.class.getName());

    /** The display list. */
    private JList displaylist;
    /** the list model used */
    private RelationListModel model;

    /** the edit action */
    private EditAction editAction;
    /** the delete action */
    private DeleteAction deleteAction;
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
        NewAction newAction = new NewAction();
        Layer.listeners.add(newAction);
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
        displaylist.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,0), "deleteRelation");
        displaylist.getActionMap().put("deleteRelation", deleteAction);

        popupMenu = new RelationDialogPopupMenu();

        // register as layer listener
        //
        Layer.listeners.add(this);
    }

    @Override public void setVisible(boolean b) {
        super.setVisible(b);
        if (b) {
            updateList();
        }
    }

    protected int getNumRelations() {
        if (Main.main.getCurrentDataSet() == null) return 0;
        return Main.main.getCurrentDataSet().getRelations().size();
    }

    /**
     * Replies the list of complete, non-deleted relations in the dataset <code>ds</code>,
     * sorted by display name.
     * 
     * @param ds the dataset
     * @return the list of relations
     */
    protected ArrayList<Relation> getDisplayedRelationsInSortOrder(DataSet ds) {
        ArrayList<Relation> relations = new ArrayList<Relation>(ds.getRelations().size());
        for (Relation r : ds.getRelations()) {
            if (!r.isUsable() || !r.isVisible()) {
                continue;
            }
            relations.add(r);
        }

        Collections.sort(
                relations,
                new Comparator<Relation>() {
                    NameFormatter formatter = DefaultNameFormatter.getInstance();

                    public int compare(Relation r1, Relation r2) {
                        return r1.getDisplayName(formatter).compareTo(r2.getDisplayName(formatter));
                    }
                }
        );
        return relations;
    }

    public void updateList() {
        if (Main.main.getCurrentDataSet() == null) {
            model.setRelations(null);
            return;
        }
        Relation[] selected = getAllSelected();

        model.setRelations(getDisplayedRelationsInSortOrder(Main.main.getCurrentDataSet()));
        if(model.getSize() > 0) {
            setTitle(tr("Relations: {0}", model.getSize()));
        } else {
            setTitle(tr("Relations"));
        }
        selectRelations(selected);
    }

    public void activeLayerChange(Layer a, Layer b) {
        updateList();
    }

    public void layerRemoved(Layer a) {
        if (a instanceof OsmDataLayer) {
            ((OsmDataLayer)a).data.removeDataSetListener(this);
        }
        updateList();
    }

    public void layerAdded(Layer a) {
        if (a instanceof OsmDataLayer) {
            ((OsmDataLayer)a).data.addDataSetListener(this);
        }
    }

    /**
     * Returns the currently selected relation, or null.
     *
     * @return the currently selected relation, or null
     */
    public Relation getCurrentRelation() {
        return (Relation) displaylist.getSelectedValue();
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
     * @return All selected relations in the list, possibly empty List
     */
    private Relation[] getAllSelected() {
        return Arrays.asList(displaylist.getSelectedValues()).toArray(new Relation[0]);
    }

    /**
     * Selects the relation <code>relation</code> in the list of relations.
     *
     * @param relation  the relation
     */
    public void selectRelation(Relation relation) {
        selectRelations(new Relation[] {relation});
    }

    /**
     * Selects the relations <code>relations</code> in the list of relations.
     *
     * @param relations  the relations (may be empty)
     */
    public void selectRelations(Relation[] relations) {
        List<Integer> sel = new ArrayList<Integer>();
        for (Relation r : relations) {
            if (r == null) {
                continue;
            }
            int idx = model.getIndexOfRelation(r);
            if (idx != -1) {
                sel.add(idx);
            }
        }
        if (sel.isEmpty()) {
            displaylist.clearSelection();
            return;
        } else {
            int fst = Collections.min(sel);
            displaylist.scrollRectToVisible(displaylist.getCellBounds(fst, fst));
        }

        int[] aSel = new int[sel.size()];       //FIXME: how to cast Integer[] -> int[] ?
        for (int i=0; i<sel.size(); ++i) {
            aSel[i] = sel.get(i);
        }

        displaylist.setSelectedIndices(aSel);
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
     * The edit action
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
            RelationEditor.getEditor(Main.map.mapView.getEditLayer(),null, null).setVisible(true);
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

    private static  class RelationListModel extends AbstractListModel {
        private ArrayList<Relation> relations;
        private DefaultListSelectionModel selectionModel;

        public RelationListModel(DefaultListSelectionModel selectionModel) {
            this.selectionModel = selectionModel;
        }

        public ArrayList<Relation> getRelations() {
            return relations;
        }

        public Relation getRelation(int idx) {
            return relations.get(idx);
        }

        public void setRelations(ArrayList<Relation> relations) {
            this.relations = relations;
            fireIntervalAdded(this, 0, getSize());
        }

        public void addRelations(Collection<? extends OsmPrimitive> addedPrimitives) {
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
                fireIntervalAdded(this, 0, getSize());
            }
        }

        public Object getElementAt(int index) {
            if (relations == null) return null;
            return relations.get(index);
        }

        public int getSize() {
            if (relations == null) return 0;
            return relations.size();
        }

        public int getIndexOfRelation(Relation relation) {
            if (relation == null) return -1;
            return relations.indexOf(relation);
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

    public void nodeMoved(Node node) { }

    public void wayNodesChanged(Way way) { }

    public void primtivesAdded(Collection<? extends OsmPrimitive> added) {
        model.addRelations(added);
    }

    public void primtivesRemoved(Collection<? extends OsmPrimitive> removed) {
        updateList();
    }

    public void relationMembersChanged(Relation r) {
        // trigger a repaint of the relation list
        displaylist.repaint();
    }

    public void tagsChanged(OsmPrimitive prim) {
        if (prim instanceof Relation) {
            // trigger a repaint of the relation list
            displaylist.repaint();
        }
    }
}
