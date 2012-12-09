// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.search.SearchCompiler;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager.FireMode;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.relation.DownloadRelationMemberTask;
import org.openstreetmap.josm.gui.dialogs.relation.DownloadRelationTask;
import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.widgets.ListPopupMenu;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.Predicate;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * A dialog showing all known relations, with buttons to add, edit, and
 * delete them.
 *
 * We don't have such dialogs for nodes, segments, and ways, because those
 * objects are visible on the map and can be selected there. Relations are not.
 */
public class RelationListDialog extends ToggleDialog implements DataSetListener {
    /** The display list. */
    private JList displaylist;
    /** the list model used */
    private RelationListModel model;

    /** the edit action */
    private EditAction editAction;
    /** the delete action */
    private DeleteAction deleteAction;
    private NewAction newAction;
    private AddToRelation addToRelation;
    /** the popup menu */
    private RelationDialogPopupMenu popupMenu;

    /**
     * constructor
     */
    public RelationListDialog() {
        super(tr("Relations"), "relationlist", tr("Open a list of all relations."),
                Shortcut.registerShortcut("subwindow:relations", tr("Toggle: {0}", tr("Relations")),
                KeyEvent.VK_R, Shortcut.ALT_SHIFT), 150);

        // create the list of relations
        //
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
        model = new RelationListModel(selectionModel);
        displaylist = new JList(model);
        displaylist.setSelectionModel(selectionModel);
        displaylist.setCellRenderer(new OsmPrimitivRenderer() {
            /**
             * Don't show the default tooltip in the relation list.
             */
            @Override
            protected String getComponentToolTipText(OsmPrimitive value) {
                return null;
            }
        });
        displaylist.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        displaylist.addMouseListener(new MouseEventHandler());

        // the new action
        //
        newAction = new NewAction();

        // the edit action
        //
        editAction = new EditAction();
        displaylist.addListSelectionListener(editAction);

        // the duplicate action
        //
        DuplicateAction duplicateAction = new DuplicateAction();
        displaylist.addListSelectionListener(duplicateAction);

        // the delete action
        //
        deleteAction = new DeleteAction();
        displaylist.addListSelectionListener(deleteAction);

        // the select action
        //
        SelectAction selectAction = new SelectAction(false);
        displaylist.addListSelectionListener(selectAction);

        final JTextField filter = new JTextField();
        filter.setToolTipText(tr("Relation list filter"));
        filter.getDocument().addDocumentListener(new DocumentListener() {

            private void setFilter() {
                try {
                    filter.setBackground(UIManager.getColor("TextField.background"));
                    filter.setToolTipText(tr("Relation list filter"));
                    model.setFilter(SearchCompiler.compile(filter.getText(), false, false));
                } catch (SearchCompiler.ParseError ex) {
                    filter.setBackground(new Color(255, 224, 224));
                    filter.setToolTipText(ex.getMessage());
                    model.setFilter(new SearchCompiler.Always());
                }
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                setFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                setFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                setFilter();
            }
        });

        JPanel pane = new JPanel(new BorderLayout());
        pane.add(filter, BorderLayout.NORTH);
        pane.add(new JScrollPane(displaylist), BorderLayout.CENTER);
        createLayout(pane, false, Arrays.asList(new SideButton[]{
                new SideButton(newAction, false),
                new SideButton(editAction, false),
                new SideButton(duplicateAction, false),
                new SideButton(deleteAction, false),
                new SideButton(selectAction, false)
        }));

        // activate DEL in the list of relations
        //displaylist.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,0), "deleteRelation");
        //displaylist.getActionMap().put("deleteRelation", deleteAction);

        InputMapUtils.unassignCtrlShiftUpDown(displaylist, JComponent.WHEN_FOCUSED);
        
        // Select relation on Ctrl-Enter
        InputMapUtils.addEnterAction(displaylist, selectAction);

        addToRelation = new AddToRelation();
        popupMenu = new RelationDialogPopupMenu(displaylist);

        // Edit relation on Ctrl-Enter
        displaylist.getActionMap().put("edit", editAction);
        displaylist.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_MASK), "edit");
    }

    @Override public void showNotify() {
        MapView.addLayerChangeListener(newAction);
        newAction.updateEnabledState();
        DatasetEventManager.getInstance().addDatasetListener(this, FireMode.IN_EDT);
        DataSet.addSelectionListener(addToRelation);
        dataChanged(null);
    }

    @Override public void hideNotify() {
        MapView.removeLayerChangeListener(newAction);
        DatasetEventManager.getInstance().removeDatasetListener(this);
        DataSet.removeSelectionListener(addToRelation);
    }

    /**
     * Initializes the relation list dialog from a layer. If <code>layer</code> is null
     * or if it isn't an {@link OsmDataLayer} the dialog is reset to an empty dialog.
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
        model.updateTitle();
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
        selectRelations(Collections.singleton(relation));
    }

    /**
     * Selects the relations in the list of relations.
     * @param relations  the relations to be selected
     */
    public void selectRelations(Collection<Relation> relations) {
        if (relations == null || relations.isEmpty()) {
            model.setSelectedRelations(null);
        } else {
            model.setSelectedRelations(relations);
            Integer i = model.getRelationIndex(relations.iterator().next());
            if (i != null) { // Not all relations have to be in the list (for example when the relation list is hidden, it's not updated with new relations)
                displaylist.scrollRectToVisible(displaylist.getCellBounds(i, i));
            }
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
            if (Main.main.getEditLayer() == null) return;
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
            popupMenu.show(displaylist, p.x, p.y-3);
        }
        @Override public void mousePressed(MouseEvent e) {
            if (Main.main.getEditLayer() == null) return;
            if (e.isPopupTrigger()) {
                openPopup(e);
            }
        }
        @Override public void mouseReleased(MouseEvent e) {
            if (Main.main.getEditLayer() == null) return;
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
            putValue(NAME, tr("Edit"));
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
            putValue(NAME, tr("Delete"));
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
            if (!isEnabled())
                return;
            List<Relation> toDelete = new LinkedList<Relation>();
            for (int i : displaylist.getSelectedIndices()) {
                toDelete.add(model.getRelation(i));
            }
            for (Relation r : toDelete) {
                deleteRelation(r);
            }
            displaylist.clearSelection();
        }

        public void valueChanged(ListSelectionEvent e) {
            setEnabled(displaylist.getSelectedIndices() != null && displaylist.getSelectedIndices().length > 0);
        }
    }

    /**
     * The action for creating a new relation
     *
     */
    static class NewAction extends AbstractAction implements LayerChangeListener{
        public NewAction() {
            putValue(SHORT_DESCRIPTION,tr("Create a new relation"));
            putValue(NAME, tr("New"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "addrelation"));
            updateEnabledState();
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
            putValue(NAME, tr("Duplicate"));
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
        boolean add;
        public SelectAction(boolean add) {
            putValue(SHORT_DESCRIPTION, add ? tr("Add the selected relations to the current selection")
                    : tr("Set the current selection to the list of selected relations"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "select"));
            putValue(NAME, add ? tr("Select relation (add)") : tr("Select relation"));
            this.add = add;
            updateEnabledState();
        }

        public void actionPerformed(ActionEvent e) {
            if (!isEnabled()) return;
            int [] idx = displaylist.getSelectedIndices();
            if (idx == null || idx.length == 0) return;
            ArrayList<OsmPrimitive> selection = new ArrayList<OsmPrimitive>(idx.length);
            for (int i: idx) {
                selection.add(model.getRelation(i));
            }
            if(add) {
                Main.map.mapView.getEditLayer().data.addSelected(selection);
            } else {
                Main.map.mapView.getEditLayer().data.setSelected(selection);
            }
        }

        protected void updateEnabledState() {
            setEnabled(displaylist.getSelectedIndices() != null && displaylist.getSelectedIndices().length > 0);
        }

        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    /**
     * Sets the current selection to the list of relations selected in this dialog
     *
     */
    class SelectMembersAction extends AbstractAction implements ListSelectionListener{
        boolean add;
        public SelectMembersAction(boolean add) {
            putValue(SHORT_DESCRIPTION,add ? tr("Add the members of all selected relations to current selection")
                    : tr("Select the members of all selected relations"));
            putValue(SMALL_ICON, ImageProvider.get("selectall"));
            putValue(NAME, add ? tr("Select members (add)") : tr("Select members"));
            this.add = add;
            updateEnabledState();
        }

        public void actionPerformed(ActionEvent e) {
            if (!isEnabled()) return;
            List<Relation> relations = model.getSelectedRelations();
            HashSet<OsmPrimitive> members = new HashSet<OsmPrimitive>();
            for(Relation r: relations) {
                members.addAll(r.getMemberPrimitives());
            }
            if(add) {
                Main.map.mapView.getEditLayer().data.addSelected(members);
            } else {
                Main.map.mapView.getEditLayer().data.setSelected(members);
            }
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
            Main.worker.submit(new DownloadRelationTask(
                    model.getSelectedNonNewRelations(),
                    Main.map.mapView.getEditLayer())
                    );
        }
    }

    /**
     * Action for downloading incomplete members of selected relations
     *
     */
    class DownloadSelectedIncompleteMembersAction extends AbstractAction implements ListSelectionListener{
        public DownloadSelectedIncompleteMembersAction() {
            putValue(SHORT_DESCRIPTION, tr("Download incomplete members of selected relations"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs/relation", "downloadincompleteselected"));
            putValue(NAME, tr("Download incomplete members"));
            updateEnabledState();
        }

        public Set<OsmPrimitive> buildSetOfIncompleteMembers(List<Relation> rels) {
            Set<OsmPrimitive> ret = new HashSet<OsmPrimitive>();
            for(Relation r: rels) {
                ret.addAll(r.getIncompleteMembers());
            }
            return ret;
        }

        public void actionPerformed(ActionEvent e) {
            if (!isEnabled())
                return;
            List<Relation> rels = model.getSelectedRelationsWithIncompleteMembers();
            if (rels.isEmpty()) return;
            Main.worker.submit(new DownloadRelationMemberTask(
                    rels,
                    buildSetOfIncompleteMembers(rels),
                    Main.map.mapView.getEditLayer()
                    ));
        }

        protected void updateEnabledState() {
            setEnabled(!model.getSelectedRelationsWithIncompleteMembers().isEmpty());
        }

        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    class AddToRelation extends AbstractAction implements ListSelectionListener, SelectionChangedListener {

        public AddToRelation() {
            super("", ImageProvider.get("dialogs/conflict", "copyendright"));
            putValue(SHORT_DESCRIPTION, tr("Add all objects selected in the current dataset after the last member"));
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Collection<Command> cmds = new LinkedList<Command>();
            for (Relation orig : getSelectedRelations()) {
                Command c = GenericRelationEditor.addPrimitivesToRelation(orig, Main.main.getCurrentDataSet().getSelected());
                if (c != null) {
                    cmds.add(c);
                }
            }
            if (!cmds.isEmpty()) {
                Main.main.undoRedo.add(new SequenceCommand(tr("Add selection to relation"), cmds));
            }
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            putValue(NAME, trn("Add selection to {0} relation", "Add selection to {0} relations",
                    getSelectedRelations().size(), getSelectedRelations().size()));
        }

        @Override
        public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
            setEnabled(newSelection != null && !newSelection.isEmpty());
        }
    }

    /**
     * The list model for the list of relations displayed in the relation list
     * dialog.
     *
     */
    private class RelationListModel extends AbstractListModel {
        private final ArrayList<Relation> relations = new ArrayList<Relation>();
        private ArrayList<Relation> filteredRelations;
        private DefaultListSelectionModel selectionModel;
        private SearchCompiler.Match filter;

        public RelationListModel(DefaultListSelectionModel selectionModel) {
            this.selectionModel = selectionModel;
        }

        public Relation getRelation(int idx) {
            return relations.get(idx);
        }

        public void sort() {
            Collections.sort(
                    relations,
                    DefaultNameFormatter.getInstance().getRelationComparator()
                    );
        }

        private boolean isValid(Relation r) {
            return !r.isDeleted() && r.isVisible() && !r.isIncomplete();
        }

        public void setRelations(Collection<Relation> relations) {
            List<Relation> sel =  getSelectedRelations();
            this.relations.clear();
            if (relations == null) {
                selectionModel.clearSelection();
                fireContentsChanged(this,0,getSize());
                return;

            }
            for (Relation r: relations) {
                if (isValid(r)) {
                    this.relations.add(r);
                }
            }
            sort();
            fireIntervalAdded(this, 0, getSize());
            setSelectedRelations(sel);
        }

        /**
         * Add all relations in <code>addedPrimitives</code> to the model for the
         * relation list dialog
         *
         * @param addedPrimitives the collection of added primitives. May include nodes,
         * ways, and relations.
         */
        public void addRelations(Collection<? extends OsmPrimitive> addedPrimitives) {
            boolean added = false;
            for (OsmPrimitive p: addedPrimitives) {
                if (! (p instanceof Relation)) {
                    continue;
                }

                Relation r = (Relation)p;
                if (relations.contains(r)) {
                    continue;
                }
                if (isValid(r)) {
                    relations.add(r);
                    added = true;
                }
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
        public void removeRelations(Collection<? extends OsmPrimitive> removedPrimitives) {
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

        /**
         * Replies the list of selected relations with incomplete members
         *
         * @return the list of selected relations with incomplete members
         */
        public List<Relation> getSelectedRelationsWithIncompleteMembers() {
            List<Relation> ret = getSelectedNonNewRelations();
            Iterator<Relation> it = ret.iterator();
            while(it.hasNext()) {
                Relation r = it.next();
                if (!r.hasIncompleteMembers()) {
                    it.remove();
                }
            }
            return ret;
        }

        public void setFilter(final SearchCompiler.Match filter) {
            this.filter = filter;
            this.filteredRelations = new ArrayList<Relation>(Utils.filter(relations, new Predicate<Relation>() {
                @Override
                public boolean evaluate(Relation r) {
                    return filter.match(r);
                }
            }));
            List<Relation> sel = getSelectedRelations();
            fireContentsChanged(this, 0, getSize());
            setSelectedRelations(sel);
            updateTitle();
        }

        private List<Relation> getVisibleRelations() {
            return filteredRelations == null ? relations : filteredRelations;
        }

        @Override
        public Object getElementAt(int index) {
            if (index < 0 || index >= getVisibleRelations().size()) return null;
            return getVisibleRelations().get(index);
        }

        @Override
        public int getSize() {
            return getVisibleRelations().size();
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
        public void setSelectedRelations(Collection<Relation> sel) {
            selectionModel.clearSelection();
            if (sel == null || sel.isEmpty())
                return;
            for (Relation r: sel) {
                int i = relations.indexOf(r);
                if (i<0) {
                    continue;
                }
                selectionModel.addSelectionInterval(i,i);
            }
        }

        /**
         * Returns the index of the relation
         *
         * @return index of relation (null if it cannot be found)
         */
        public Integer getRelationIndex(Relation rel) {
            int i = relations.indexOf(rel);
            if (i<0)
                return null;
            return i;
        }

        public void updateTitle() {
            if (relations.size() > 0 && relations.size() != getSize()) {
                RelationListDialog.this.setTitle(tr("Relations: {0}/{1}", getSize(), relations.size()));
            } else if (getSize() > 0) {
                RelationListDialog.this.setTitle(tr("Relations: {0}", getSize()));
            } else {
                RelationListDialog.this.setTitle(tr("Relations"));
            }
        }
    }

    class RelationDialogPopupMenu extends ListPopupMenu {

        public RelationDialogPopupMenu(JList list) {
            super(list);

            // -- download members action
            add(new DownloadMembersAction());

            // -- download incomplete members action
            add(new DownloadSelectedIncompleteMembersAction());

            addSeparator();

            // -- select members action
            add(new SelectMembersAction(false));
            add(new SelectMembersAction(true));

            // -- select action
            add(new SelectAction(false));
            add(new SelectAction(true));

            addSeparator();

            add(addToRelation);
        }
    }

    public void addPopupMenuSeparator() {
        popupMenu.addSeparator();
    }

    public JMenuItem addPopupMenuAction(Action a) {
        return popupMenu.add(a);
    }

    public void addPopupMenuListener(PopupMenuListener l) {
        popupMenu.addPopupMenuListener(l);
    }

    public void removePopupMenuListener(PopupMenuListener l) {
        popupMenu.addPopupMenuListener(l);
    }

    public Collection<Relation> getSelectedRelations() {
        return model.getSelectedRelations();
    }

    /* ---------------------------------------------------------------------------------- */
    /* DataSetListener                                                                    */
    /* ---------------------------------------------------------------------------------- */

    public void nodeMoved(NodeMovedEvent event) {/* irrelevant in this context */}

    public void wayNodesChanged(WayNodesChangedEvent event) {/* irrelevant in this context */}

    public void primitivesAdded(final PrimitivesAddedEvent event) {
        model.addRelations(event.getPrimitives());
        model.updateTitle();
    }

    public void primitivesRemoved(final PrimitivesRemovedEvent event) {
        model.removeRelations(event.getPrimitives());
        model.updateTitle();
    }

    public void relationMembersChanged(final RelationMembersChangedEvent event) {
        List<Relation> sel = model.getSelectedRelations();
        model.sort();
        model.setSelectedRelations(sel);
        displaylist.repaint();
    }

    public void tagsChanged(TagsChangedEvent event) {
        OsmPrimitive prim = event.getPrimitive();
        if (prim == null || ! (prim instanceof Relation))
            return;
        // trigger a sort of the relation list because the display name may
        // have changed
        //
        List<Relation> sel = model.getSelectedRelations();
        model.sort();
        model.setSelectedRelations(sel);
        displaylist.repaint();
    }

    public void dataChanged(DataChangedEvent event) {
        initFromLayer(Main.main.getEditLayer());
    }

    public void otherDatasetChange(AbstractDatasetChangedEvent event) {/* ignore */}
}
