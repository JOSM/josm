// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.FocusManager;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.relation.AddSelectionToRelations;
import org.openstreetmap.josm.actions.relation.DeleteRelationsAction;
import org.openstreetmap.josm.actions.relation.DownloadMembersAction;
import org.openstreetmap.josm.actions.relation.DownloadSelectedIncompleteMembersAction;
import org.openstreetmap.josm.actions.relation.DuplicateRelationAction;
import org.openstreetmap.josm.actions.relation.EditRelationAction;
import org.openstreetmap.josm.actions.relation.SelectMembersAction;
import org.openstreetmap.josm.actions.relation.SelectRelationAction;
import org.openstreetmap.josm.actions.search.SearchCompiler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
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
import org.openstreetmap.josm.gui.PopupMenuHandler;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.HighlightHelper;
import org.openstreetmap.josm.gui.widgets.DisableShortcutsOnFocusGainedTextField;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
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
    private final JList displaylist;
    /** the list model used */
    private final RelationListModel model;

    private final NewAction newAction;

    /** the popup menu and its handler */
    private final JPopupMenu popupMenu = new JPopupMenu();
    private final PopupMenuHandler popupMenuHandler = new PopupMenuHandler(popupMenu);

    private final JosmTextField filter;

    // Actions
    /** the edit action */
    private final EditRelationAction editAction = new EditRelationAction();
    /** the delete action */
    private final DeleteRelationsAction deleteRelationsAction = new DeleteRelationsAction();
    /** the duplicate action */
    private final DuplicateRelationAction duplicateAction = new DuplicateRelationAction();
    private final DownloadMembersAction downloadMembersAction = new DownloadMembersAction();
    private final DownloadSelectedIncompleteMembersAction downloadSelectedIncompleteMembersAction = new DownloadSelectedIncompleteMembersAction();
    private final SelectMembersAction selectMembersAction = new SelectMembersAction(false);
    private final SelectMembersAction addMembersToSelectionAction = new SelectMembersAction(true);
    private final SelectRelationAction selectRelationAction = new SelectRelationAction(false);
    private final SelectRelationAction addRelationToSelectionAction = new SelectRelationAction(true);
    /** add all selected primitives to the given relations */
    private final AddSelectionToRelations addSelectionToRelations = new AddSelectionToRelations();

    HighlightHelper highlightHelper = new HighlightHelper();
    private boolean highlightEnabled = Main.pref.getBoolean("draw.target-highlight", true);
    /**
     * Constructs <code>RelationListDialog</code>
     */
    public RelationListDialog() {
        super(tr("Relations"), "relationlist", tr("Open a list of all relations."),
                Shortcut.registerShortcut("subwindow:relations", tr("Toggle: {0}", tr("Relations")),
                KeyEvent.VK_R, Shortcut.ALT_SHIFT), 150, true);

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

        filter = setupFilter();

        displaylist.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateActionsRelationLists();
            }
        });

        // Setup popup menu handler
        setupPopupMenuHandler();

        JPanel pane = new JPanel(new BorderLayout());
        pane.add(filter, BorderLayout.NORTH);
        pane.add(new JScrollPane(displaylist), BorderLayout.CENTER);
        createLayout(pane, false, Arrays.asList(new SideButton[]{
                new SideButton(newAction, false),
                new SideButton(editAction, false),
                new SideButton(duplicateAction, false),
                new SideButton(deleteRelationsAction, false),
                new SideButton(selectRelationAction, false)
        }));

        InputMapUtils.unassignCtrlShiftUpDown(displaylist, JComponent.WHEN_FOCUSED);

        // Select relation on Ctrl-Enter
        InputMapUtils.addEnterAction(displaylist, selectRelationAction);

        // Edit relation on Ctrl-Enter
        displaylist.getActionMap().put("edit", editAction);
        displaylist.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_MASK), "edit");

        updateActionsRelationLists();
    }

    // inform all actions about list of relations they need
    private void updateActionsRelationLists() {
        List<Relation> sel = model.getSelectedRelations();
        popupMenuHandler.setPrimitives(sel);

        Component focused = FocusManager.getCurrentKeyboardFocusManager().getFocusOwner();

        //update highlights
        if (highlightEnabled && focused==displaylist && Main.isDisplayingMapView()) {
            if (highlightHelper.highlightOnly(sel)) {
                Main.map.mapView.repaint();
            }
        }
    }

    @Override public void showNotify() {
        MapView.addLayerChangeListener(newAction);
        newAction.updateEnabledState();
        DatasetEventManager.getInstance().addDatasetListener(this, FireMode.IN_EDT);
        DataSet.addSelectionListener(addSelectionToRelations);
        dataChanged(null);
    }

    @Override public void hideNotify() {
        MapView.removeLayerChangeListener(newAction);
        DatasetEventManager.getInstance().removeDatasetListener(this);
        DataSet.removeSelectionListener(addSelectionToRelations);
    }

    private void resetFilter() {
        filter.setText(null);
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
        if (!(layer instanceof OsmDataLayer)) {
            model.setRelations(null);
            return;
        }
        OsmDataLayer l = (OsmDataLayer)layer;
        model.setRelations(l.data.getRelations());
        model.updateTitle();
        updateActionsRelationLists();
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
            Integer i = model.getVisibleRelationIndex(relations.iterator().next());
            if (i != null) { // Not all relations have to be in the list (for example when the relation list is hidden, it's not updated with new relations)
                displaylist.scrollRectToVisible(displaylist.getCellBounds(i, i));
            }
        }
    }

    private JosmTextField  setupFilter() {
        final JosmTextField f = new DisableShortcutsOnFocusGainedTextField();
        f.setToolTipText(tr("Relation list filter"));
        f.getDocument().addDocumentListener(new DocumentListener() {

            private void setFilter() {
                try {
                    f.setBackground(UIManager.getColor("TextField.background"));
                    f.setToolTipText(tr("Relation list filter"));
                    model.setFilter(SearchCompiler.compile(filter.getText(), false, false));
                } catch (SearchCompiler.ParseError ex) {
                    f.setBackground(new Color(255, 224, 224));
                    f.setToolTipText(ex.getMessage());
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
        return f;
    }

    class MouseEventHandler extends PopupMenuLauncher {

        public MouseEventHandler() {
            super(popupMenu);
        }

        @Override
        public void mouseExited(MouseEvent me) {
            if (highlightEnabled) highlightHelper.clear();
        }

        protected void setCurrentRelationAsSelection() {
            Main.main.getCurrentDataSet().setSelected((Relation)displaylist.getSelectedValue());
        }

        protected void editCurrentRelation() {
            EditRelationAction.launchEditor(getSelected());
        }

        @Override public void mouseClicked(MouseEvent e) {
            if (!Main.main.hasEditLayer()) return;
            if (isDoubleClick(e)) {
                if (e.isControlDown()) {
                    editCurrentRelation();
                } else {
                    setCurrentRelationAsSelection();
                }
            }
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

        @Override
        public void actionPerformed(ActionEvent e) {
            run();
        }

        protected void updateEnabledState() {
            setEnabled(Main.main != null && Main.main.hasEditLayer());
        }

        @Override
        public void activeLayerChange(Layer oldLayer, Layer newLayer) {
            updateEnabledState();
    }

        @Override
        public void layerAdded(Layer newLayer) {
            updateEnabledState();
        }

        @Override
        public void layerRemoved(Layer oldLayer) {
            updateEnabledState();
        }
    }

    /**
     * The list model for the list of relations displayed in the relation list
     * dialog.
     *
     */
    private class RelationListModel extends AbstractListModel {
        private final List<Relation> relations = new ArrayList<Relation>();
        private List<Relation> filteredRelations;
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
            this.filteredRelations = null;
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
            updateFilteredRelations();
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
                updateFilteredRelations();
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
            if (filteredRelations != null) {
                filteredRelations.removeAll(removedRelations);
            }
            if (size != relations.size()) {
                List<Relation> sel = getSelectedRelations();
                sort();
                fireContentsChanged(this, 0, getSize());
                setSelectedRelations(sel);
            }
        }

        private void updateFilteredRelations() {
            if (filter != null) {
                filteredRelations = new ArrayList<Relation>(Utils.filter(relations, new Predicate<Relation>() {
                    @Override
                    public boolean evaluate(Relation r) {
                        return filter.match(r);
                    }
                }));
            } else if (filteredRelations != null) {
                filteredRelations = null;
            }
        }

        public void setFilter(final SearchCompiler.Match filter) {
            this.filter = filter;
            updateFilteredRelations();
            List<Relation> sel = getSelectedRelations();
            fireContentsChanged(this, 0, getSize());
            setSelectedRelations(sel);
            updateTitle();
        }

        private List<Relation> getVisibleRelations() {
            return filteredRelations == null ? relations : filteredRelations;
        }

        private Relation getVisibleRelation(int index) {
            if (index < 0 || index >= getVisibleRelations().size()) return null;
            return getVisibleRelations().get(index);
        }

        @Override
        public Object getElementAt(int index) {
            return getVisibleRelation(index);
        }

        @Override
        public int getSize() {
            return getVisibleRelations().size();
        }

        /**
         * Replies the list of selected relations. Empty list,
         * if there are no selected relations.
         *
         * @return the list of selected, non-new relations.
         */
        public List<Relation> getSelectedRelations() {
            List<Relation> ret = new ArrayList<Relation>();
            for (int i=0; i<getSize();i++) {
                if (!selectionModel.isSelectedIndex(i)) {
                    continue;
                }
                ret.add(getVisibleRelation(i));
            }
            return ret;
        }

        /**
         * Sets the selected relations.
         *
         * @param sel the list of selected relations
         */
        public void setSelectedRelations(Collection<Relation> sel) {
            selectionModel.clearSelection();
            if (sel == null || sel.isEmpty())
                return;
            if (!getVisibleRelations().containsAll(sel)) {
                resetFilter();
            }
            for (Relation r: sel) {
                Integer i = getVisibleRelationIndex(r);
                if (i != null) {
                    selectionModel.addSelectionInterval(i,i);
                }
            }
        }

        /**
         * Returns the index of the relation
         * @param rel The relation to look for
         *
         * @return index of relation (null if it cannot be found)
         */
        public Integer getRelationIndex(Relation rel) {
            int i = relations.indexOf(rel);
            if (i<0)
                return null;
            return i;
        }

        private Integer getVisibleRelationIndex(Relation rel) {
            int i = getVisibleRelations().indexOf(rel);
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

    private final void setupPopupMenuHandler() {

        // -- select action
        popupMenuHandler.addAction(selectRelationAction);
        popupMenuHandler.addAction(addRelationToSelectionAction);

        // -- select members action
        popupMenuHandler.addAction(selectMembersAction);
        popupMenuHandler.addAction(addMembersToSelectionAction);

        popupMenuHandler.addSeparator();
        // -- download members action
        popupMenuHandler.addAction(downloadMembersAction);

        // -- download incomplete members action
        popupMenuHandler.addAction(downloadSelectedIncompleteMembersAction);

        popupMenuHandler.addSeparator();
        popupMenuHandler.addAction(editAction).setVisible(false);
        popupMenuHandler.addAction(duplicateAction).setVisible(false);
        popupMenuHandler.addAction(deleteRelationsAction).setVisible(false);

        popupMenuHandler.addAction(addSelectionToRelations);
    }

    /* ---------------------------------------------------------------------------------- */
    /* Methods that can be called from plugins                                            */
    /* ---------------------------------------------------------------------------------- */

    /**
     * Replies the popup menu handler.
     * @return The popup menu handler
     */
    public PopupMenuHandler getPopupMenuHandler() {
        return popupMenuHandler;
    }

    public Collection<Relation> getSelectedRelations() {
        return model.getSelectedRelations();
    }

    /* ---------------------------------------------------------------------------------- */
    /* DataSetListener                                                                    */
    /* ---------------------------------------------------------------------------------- */

    @Override
    public void nodeMoved(NodeMovedEvent event) {/* irrelevant in this context */}

    @Override
    public void wayNodesChanged(WayNodesChangedEvent event) {/* irrelevant in this context */}

    @Override
    public void primitivesAdded(final PrimitivesAddedEvent event) {
        model.addRelations(event.getPrimitives());
        model.updateTitle();
    }

    @Override
    public void primitivesRemoved(final PrimitivesRemovedEvent event) {
        model.removeRelations(event.getPrimitives());
        model.updateTitle();
    }

    @Override
    public void relationMembersChanged(final RelationMembersChangedEvent event) {
        List<Relation> sel = model.getSelectedRelations();
        model.sort();
        model.setSelectedRelations(sel);
        displaylist.repaint();
    }

    @Override
    public void tagsChanged(TagsChangedEvent event) {
        OsmPrimitive prim = event.getPrimitive();
        if (!(prim instanceof Relation))
            return;
        // trigger a sort of the relation list because the display name may
        // have changed
        //
        List<Relation> sel = model.getSelectedRelations();
        model.sort();
        model.setSelectedRelations(sel);
        displaylist.repaint();
    }

    @Override
    public void dataChanged(DataChangedEvent event) {
        initFromLayer(Main.main.getEditLayer());
    }

    @Override
    public void otherDatasetChange(AbstractDatasetChangedEvent event) {/* ignore */}
}
