// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.FocusManager;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.actions.HistoryInfoAction;
import org.openstreetmap.josm.actions.relation.AddSelectionToRelations;
import org.openstreetmap.josm.actions.relation.DeleteRelationsAction;
import org.openstreetmap.josm.actions.relation.DuplicateRelationAction;
import org.openstreetmap.josm.actions.relation.EditRelationAction;
import org.openstreetmap.josm.actions.relation.ExportRelationToGpxAction;
import org.openstreetmap.josm.actions.relation.ExportRelationToGpxAction.Mode;
import org.openstreetmap.josm.actions.relation.RecentRelationsAction;
import org.openstreetmap.josm.actions.relation.SelectInRelationListAction;
import org.openstreetmap.josm.actions.relation.SelectRelationAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.OsmData;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent.DatasetEventType;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager.FireMode;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.SelectionEventManager;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.data.osm.search.SearchCompiler;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.PopupMenuHandler;
import org.openstreetmap.josm.gui.PrimitiveRenderer;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.RelationPopupMenus;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;
import org.openstreetmap.josm.gui.util.AbstractTag2LinkPopupListener;
import org.openstreetmap.josm.gui.util.HighlightHelper;
import org.openstreetmap.josm.gui.util.TableHelper;
import org.openstreetmap.josm.gui.widgets.CompileSearchTextDecorator;
import org.openstreetmap.josm.gui.widgets.DisableShortcutsOnFocusGainedTextField;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.PlatformManager;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;
import org.openstreetmap.josm.tools.Utils;

/**
 * A dialog showing all known relations, with buttons to add, edit, and delete them.
 *
 * We don't have such dialogs for nodes, segments, and ways, because those
 * objects are visible on the map and can be selected there. Relations are not.
 */
public class RelationListDialog extends ToggleDialog
        implements DataSetListener, NavigatableComponent.ZoomChangeListener {
    /** The display list. */
    private final JList<IRelation<?>> displaylist;
    /** the list model used */
    private final RelationListModel model;

    private final NewAction newAction;

    /** the popup menu and its handler */
    private final JPopupMenu popupMenu = new JPopupMenu();
    private final transient PopupMenuHandler popupMenuHandler = new PopupMenuHandler(popupMenu);

    private final JosmTextField filter;

    // Actions
    /** the edit action */
    private final EditRelationAction editAction = new EditRelationAction();
    /** the delete action */
    private final DeleteRelationsAction deleteRelationsAction = new DeleteRelationsAction();
    /** the duplicate action */
    private final DuplicateRelationAction duplicateAction = new DuplicateRelationAction();
    /** the select relation action */
    private final SelectRelationAction selectRelationAction = new SelectRelationAction(false);
    /** add all selected primitives to the given relations */
    private final AddSelectionToRelations addSelectionToRelations = new AddSelectionToRelations();

    /** export relation to GPX track action */
    private final ExportRelationToGpxAction exportRelationFromFirstAction =
            new ExportRelationToGpxAction(EnumSet.of(Mode.FROM_FIRST_MEMBER, Mode.TO_FILE));
    private final ExportRelationToGpxAction exportRelationFromLastAction =
            new ExportRelationToGpxAction(EnumSet.of(Mode.FROM_LAST_MEMBER, Mode.TO_FILE));
    private final ExportRelationToGpxAction exportRelationFromFirstToLayerAction =
            new ExportRelationToGpxAction(EnumSet.of(Mode.FROM_FIRST_MEMBER, Mode.TO_LAYER));
    private final ExportRelationToGpxAction exportRelationFromLastToLayerAction =
            new ExportRelationToGpxAction(EnumSet.of(Mode.FROM_LAST_MEMBER, Mode.TO_LAYER));

    private final transient HighlightHelper highlightHelper = new HighlightHelper();
    private final boolean highlightEnabled = Config.getPref().getBoolean("draw.target-highlight", true);
    private final transient RecentRelationsAction recentRelationsAction;

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
        displaylist = new JList<>(model);
        displaylist.setSelectionModel(selectionModel);
        displaylist.setCellRenderer(new NoTooltipOsmRenderer());
        displaylist.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        displaylist.addMouseListener(new MouseEventHandler());

        // the new action
        //
        newAction = new NewAction();

        filter = setupFilter();

        displaylist.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateActionsRelationLists();
        });

        // Setup popup menu handler
        setupPopupMenuHandler();

        JPanel pane = new JPanel(new BorderLayout());
        pane.add(filter, BorderLayout.NORTH);
        pane.add(new JScrollPane(displaylist), BorderLayout.CENTER);

        SideButton editButton = new SideButton(editAction, false);
        recentRelationsAction = new RecentRelationsAction(editButton);

        createLayout(pane, false, Arrays.asList(
                new SideButton(newAction, false),
                editButton,
                new SideButton(duplicateAction, false),
                new SideButton(deleteRelationsAction, false),
                new SideButton(selectRelationAction, false)
        ));

        InputMapUtils.unassignCtrlShiftUpDown(displaylist, JComponent.WHEN_FOCUSED);

        // Select relation on Enter
        InputMapUtils.addEnterAction(displaylist, selectRelationAction);

        // Edit relation on Ctrl-Enter
        displaylist.getActionMap().put("edit", editAction);
        displaylist.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), "edit");

        // Do not hide copy action because of default JList override (fix #9815)
        displaylist.getActionMap().put("copy", MainApplication.getMenu().copy);
        displaylist.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, PlatformManager.getPlatform().getMenuShortcutKeyMaskEx()), "copy");

        HistoryInfoAction historyAction = MainApplication.getMenu().historyinfo;
        displaylist.getActionMap().put("historyAction", historyAction);
        displaylist.getInputMap().put(historyAction.getShortcut().getKeyStroke(), "historyAction");

        updateActionsRelationLists();
    }

    @Override
    public void destroy() {
        recentRelationsAction.destroy();
        popupMenuHandler.setPrimitives(Collections.emptyList());
        model.clear();
        super.destroy();
    }

    /**
     * Enable the "recent relations" dropdown menu next to edit button.
     */
    public void enableRecentRelations() {
        recentRelationsAction.enableArrow();
    }

    // inform all actions about list of relations they need
    private void updateActionsRelationLists() {
        List<IRelation<?>> sel = model.getSelectedRelations();
        popupMenuHandler.setPrimitives(sel);
        selectRelationAction.setPrimitives(sel);

        Component focused = FocusManager.getCurrentKeyboardFocusManager().getFocusOwner();

        //update highlights
        if (highlightEnabled && focused == displaylist && MainApplication.isDisplayingMapView()
                && highlightHelper.highlightOnly(Utils.filteredCollection(sel, Relation.class))) {
            MainApplication.getMap().mapView.repaint();
        }
    }

    @Override
    public void showNotify() {
        MainApplication.getLayerManager().addLayerChangeListener(newAction);
        MainApplication.getLayerManager().addActiveLayerChangeListener(newAction);
        MapView.addZoomChangeListener(this);
        newAction.updateEnabledState();
        DatasetEventManager.getInstance().addDatasetListener(this, FireMode.IN_EDT_CONSOLIDATED);
        SelectionEventManager.getInstance().addSelectionListener(addSelectionToRelations);
        dataChanged(null);
    }

    @Override
    public void hideNotify() {
        MainApplication.getLayerManager().removeActiveLayerChangeListener(newAction);
        MainApplication.getLayerManager().removeLayerChangeListener(newAction);
        MapView.removeZoomChangeListener(this);
        DatasetEventManager.getInstance().removeDatasetListener(this);
        SelectionEventManager.getInstance().removeSelectionListener(addSelectionToRelations);
    }

    private void resetFilter() {
        filter.setText(null);
    }

    /**
     * Initializes the relation list dialog from a dataset. If <code>data</code> is null
     * the dialog is reset to an empty dialog.
     * Otherwise it is initialized with the list of non-deleted and visible relations
     * in the dataset.
     *
     * @param data the dataset. May be null.
     * @since 13957
     */
    protected void initFromData(OsmData<?, ?, ?, ?> data) {
        if (data == null) {
            model.setRelations(null);
            return;
        }
        model.setRelations(data.getRelations());
        model.updateTitle();
        updateActionsRelationLists();
    }

    /**
     * @return The selected relation in the list
     */
    private IRelation<?> getSelected() {
        if (model.getSize() == 1) {
            displaylist.setSelectedIndex(0);
        }
        return displaylist.getSelectedValue();
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
     * @since 13957 (signature)
     */
    public void selectRelations(Collection<? extends IRelation<?>> relations) {
        if (relations == null || relations.isEmpty()) {
            model.setSelectedRelations(null);
        } else {
            model.setSelectedRelations(relations);
            int i = model.getVisibleRelations().indexOf(relations.iterator().next());
            if (i >= 0) {
                // Not all relations have to be in the list
                // (for example when the relation list is hidden, it's not updated with new relations)
                displaylist.scrollRectToVisible(displaylist.getCellBounds(i, i));
            }
        }
    }

    private JosmTextField setupFilter() {
        final JosmTextField f = new DisableShortcutsOnFocusGainedTextField();
        f.setToolTipText(tr("Relation list filter"));
        final CompileSearchTextDecorator decorator = CompileSearchTextDecorator.decorate(f);
        f.addPropertyChangeListener("filter", evt -> model.setFilter(decorator.getMatch()));
        return f;
    }

    static final class NoTooltipOsmRenderer extends PrimitiveRenderer {
        @Override
        protected String getComponentToolTipText(IPrimitive value) {
            // Don't show the default tooltip in the relation list
            return null;
        }
    }

    class MouseEventHandler extends PopupMenuLauncher {

        MouseEventHandler() {
            super(popupMenu);
        }

        @Override
        public void mouseExited(MouseEvent me) {
            if (highlightEnabled) highlightHelper.clear();
        }

        protected void setCurrentRelationAsSelection() {
            MainApplication.getLayerManager().getActiveData().setSelected(displaylist.getSelectedValue());
        }

        protected void editCurrentRelation() {
            IRelation<?> rel = getSelected();
            if (rel instanceof Relation) {
                EditRelationAction.launchEditor((Relation) rel);
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            DataSet ds = MainApplication.getLayerManager().getActiveDataSet();
            if (ds != null && isDoubleClick(e)) {
                if (e.isControlDown() && !ds.isLocked()) {
                    editCurrentRelation();
                } else {
                    setCurrentRelationAsSelection();
                }
            }
        }
    }

    /**
     * The action for creating a new relation.
     */
    static class NewAction extends AbstractAction implements LayerChangeListener, ActiveLayerChangeListener {
        NewAction() {
            putValue(SHORT_DESCRIPTION, tr("Create a new relation"));
            putValue(NAME, tr("New"));
            new ImageProvider("dialogs", "addrelation").getResource().attachImageIcon(this, true);
            updateEnabledState();
        }

        public void run() {
            RelationEditor.getEditor(MainApplication.getLayerManager().getEditLayer(), null, null).setVisible(true);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            run();
        }

        protected void updateEnabledState() {
            setEnabled(MainApplication.getLayerManager().getEditLayer() != null);
        }

        @Override
        public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
            updateEnabledState();
        }

        @Override
        public void layerAdded(LayerAddEvent e) {
            updateEnabledState();
        }

        @Override
        public void layerRemoving(LayerRemoveEvent e) {
            updateEnabledState();
        }

        @Override
        public void layerOrderChanged(LayerOrderChangeEvent e) {
            // Do nothing
        }
    }

    /**
     * The list model for the list of relations displayed in the relation list dialog.
     */
    private class RelationListModel extends AbstractListModel<IRelation<?>> {
        private final transient List<IRelation<?>> relations = new ArrayList<>();
        private transient List<IRelation<?>> filteredRelations;
        private final DefaultListSelectionModel selectionModel;
        private transient SearchCompiler.Match filter;

        RelationListModel(DefaultListSelectionModel selectionModel) {
            this.selectionModel = selectionModel;
        }

        /**
         * Clears the model.
         */
        public void clear() {
            relations.clear();
            if (filteredRelations != null)
                filteredRelations.clear();
            filter = null;
        }

        /**
         * Sorts the model using {@link DefaultNameFormatter} relation comparator.
         */
        public void sort() {
            relations.sort(DefaultNameFormatter.getInstance().getRelationComparator());
        }

        private boolean isValid(IRelation<?> r) {
            return !r.isDeleted() && !r.isIncomplete();
        }

        public void setRelations(Collection<? extends IRelation<?>> relations) {
            List<IRelation<?>> sel = getSelectedRelations();
            this.relations.clear();
            this.filteredRelations = null;
            if (relations == null) {
                selectionModel.clearSelection();
                fireContentsChanged(this, 0, getSize());
                return;
            }
            for (IRelation<?> r: relations) {
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
                if (!(p instanceof Relation)) {
                    continue;
                }

                Relation r = (Relation) p;
                if (relations.contains(r)) {
                    continue;
                }
                if (isValid(r)) {
                    relations.add(r);
                    added = true;
                }
            }
            if (added) {
                List<IRelation<?>> sel = getSelectedRelations();
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
            Set<Relation> removedRelations = removedPrimitives.stream()
                    .filter(p -> p instanceof Relation).map(p -> (Relation) p)
                    .collect(Collectors.toSet());
            if (removedRelations.isEmpty())
                return;
            int size = relations.size();
            relations.removeAll(removedRelations);
            if (filteredRelations != null) {
                filteredRelations.removeAll(removedRelations);
            }
            if (size != relations.size()) {
                List<IRelation<?>> sel = getSelectedRelations();
                sort();
                fireContentsChanged(this, 0, getSize());
                setSelectedRelations(sel);
            }
        }

        private void updateFilteredRelations() {
            if (filter != null) {
                filteredRelations = new ArrayList<>(SubclassFilteredCollection.filter(relations, filter::match));
            } else if (filteredRelations != null) {
                filteredRelations = null;
            }
        }

        public void setFilter(final SearchCompiler.Match filter) {
            this.filter = filter;
            updateFilteredRelations();
            List<IRelation<?>> sel = getSelectedRelations();
            fireContentsChanged(this, 0, getSize());
            setSelectedRelations(sel);
            updateTitle();
        }

        private List<IRelation<?>> getVisibleRelations() {
            return filteredRelations == null ? relations : filteredRelations;
        }

        private IRelation<?> getVisibleRelation(int index) {
            if (index < 0 || index >= getVisibleRelations().size()) return null;
            return getVisibleRelations().get(index);
        }

        @Override
        public IRelation<?> getElementAt(int index) {
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
         * @since 13957 (signature)
         */
        public List<IRelation<?>> getSelectedRelations() {
            return IntStream.range(0, getSize())
                    .filter(selectionModel::isSelectedIndex)
                    .mapToObj(this::getVisibleRelation)
                    .collect(Collectors.toList());
        }

        /**
         * Sets the selected relations.
         *
         * @param sel the list of selected relations
         * @since 13957 (signature)
         */
        public void setSelectedRelations(Collection<? extends IRelation<?>> sel) {
            if (sel != null && !sel.isEmpty()) {
                if (!getVisibleRelations().containsAll(sel)) {
                    resetFilter();
                }
                TableHelper.setSelectedIndices(selectionModel, sel.stream().mapToInt(getVisibleRelations()::indexOf));
            } else {
                TableHelper.setSelectedIndices(selectionModel, IntStream.empty());
            }
        }

        public void updateTitle() {
            if (!relations.isEmpty() && relations.size() != getSize()) {
                RelationListDialog.this.setTitle(tr("Relations: {0}/{1}", getSize(), relations.size()));
            } else if (getSize() > 0) {
                RelationListDialog.this.setTitle(tr("Relations: {0}", getSize()));
            } else {
                RelationListDialog.this.setTitle(tr("Relations"));
            }
        }
    }

    private void setupPopupMenuHandler() {
        List<JMenuItem> checkDisabled = new ArrayList<>();

        RelationPopupMenus.setupHandler(popupMenuHandler, SelectInRelationListAction.class);

        // -- export relation to gpx action
        popupMenuHandler.addSeparator();
        checkDisabled.add(popupMenuHandler.addAction(exportRelationFromFirstAction));
        checkDisabled.add(popupMenuHandler.addAction(exportRelationFromLastAction));
        popupMenuHandler.addSeparator();
        checkDisabled.add(popupMenuHandler.addAction(exportRelationFromFirstToLayerAction));
        checkDisabled.add(popupMenuHandler.addAction(exportRelationFromLastToLayerAction));

        popupMenuHandler.addSeparator();
        popupMenuHandler.addAction(editAction).setVisible(false);
        popupMenuHandler.addAction(duplicateAction).setVisible(false);
        popupMenuHandler.addAction(deleteRelationsAction).setVisible(false);

        ExpertToggleAction.addVisibilitySwitcher(popupMenuHandler.addAction(addSelectionToRelations));

        popupMenuHandler.addListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                for (JMenuItem mi: checkDisabled) {
                    mi.setVisible(mi.getAction().isEnabled());
                    Component sep = popupMenu.getComponent(Math.max(0, popupMenu.getComponentIndex(mi) - 1));
                    if (!(sep instanceof JMenuItem)) {
                        sep.setVisible(mi.isVisible());
                    }
                }
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                // Do nothing
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                // Do nothing
            }
        });

        popupMenuHandler.addListener(new AbstractTag2LinkPopupListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                getSelectedRelations().forEach(relation ->
                        relation.visitKeys((primitive, key, value) -> addLinks(popupMenu, key, value)));
            }
        });
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

    /**
     * Replies the list of selected relations. Empty list, if there are no selected relations.
     * @return the list of selected, non-new relations.
     * @since 13957 (signature)
     */
    public Collection<IRelation<?>> getSelectedRelations() {
        return model.getSelectedRelations();
    }

    /* ---------------------------------------------------------------------------------- */
    /* DataSetListener                                                                    */
    /* ---------------------------------------------------------------------------------- */

    @Override
    public void nodeMoved(NodeMovedEvent event) {
        /* irrelevant in this context */
    }

    @Override
    public void wayNodesChanged(WayNodesChangedEvent event) {
        /* irrelevant in this context */
    }

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
        List<IRelation<?>> sel = model.getSelectedRelations();
        model.sort();
        model.setSelectedRelations(sel);
        displaylist.repaint();
    }

    @Override
    public void tagsChanged(TagsChangedEvent event) {
        OsmPrimitive prim = event.getPrimitive();
        if (!(prim instanceof Relation))
            return;
        // trigger a sort of the relation list because the display name may have changed
        List<IRelation<?>> sel = model.getSelectedRelations();
        model.sort();
        model.setSelectedRelations(sel);
        displaylist.repaint();
    }

    @Override
    public void dataChanged(DataChangedEvent event) {
        initFromData(MainApplication.getLayerManager().getActiveData());
    }

    @Override
    public void otherDatasetChange(AbstractDatasetChangedEvent event) {
        if (event.getType() == DatasetEventType.PRIMITIVE_FLAGS_CHANGED
                && event.getPrimitives().stream().anyMatch(Relation.class::isInstance)) {
            initFromData(MainApplication.getLayerManager().getActiveData());
        }
    }

    @Override
    public void zoomChanged() {
        // re-filter relations
        if (model.filter != null) {
            model.setFilter(model.filter);
        }
    }
}
