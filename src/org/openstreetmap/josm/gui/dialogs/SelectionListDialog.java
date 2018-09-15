// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.actions.AbstractSelectAction;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.AutoScaleAction.AutoScaleMode;
import org.openstreetmap.josm.actions.relation.DownloadSelectedIncompleteMembersAction;
import org.openstreetmap.josm.actions.relation.EditRelationAction;
import org.openstreetmap.josm.actions.relation.SelectInRelationListAction;
import org.openstreetmap.josm.data.osm.DataSelectionListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmData;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveComparator;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
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
import org.openstreetmap.josm.data.osm.search.SearchSetting;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PopupMenuHandler;
import org.openstreetmap.josm.gui.PrimitiveRenderer;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.datatransfer.PrimitiveTransferable;
import org.openstreetmap.josm.gui.datatransfer.data.PrimitiveTransferData;
import org.openstreetmap.josm.gui.history.HistoryBrowserDialogManager;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.HighlightHelper;
import org.openstreetmap.josm.gui.widgets.ListPopupMenu;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.bugreport.BugReport;

/**
 * A small tool dialog for displaying the current selection.
 * @since 8
 */
public class SelectionListDialog extends ToggleDialog {
    private JList<OsmPrimitive> lstPrimitives;
    private final DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
    private final SelectionListModel model = new SelectionListModel(selectionModel);

    private final SelectAction actSelect = new SelectAction();
    private final SearchAction actSearch = new SearchAction();
    private final ShowHistoryAction actShowHistory = new ShowHistoryAction();
    private final ZoomToJOSMSelectionAction actZoomToJOSMSelection = new ZoomToJOSMSelectionAction();
    private final ZoomToListSelection actZoomToListSelection = new ZoomToListSelection();
    private final SelectInRelationListAction actSetRelationSelection = new SelectInRelationListAction();
    private final EditRelationAction actEditRelationSelection = new EditRelationAction();
    private final DownloadSelectedIncompleteMembersAction actDownloadSelIncompleteMembers = new DownloadSelectedIncompleteMembersAction();

    /** the popup menu and its handler */
    private final ListPopupMenu popupMenu;
    private final transient PopupMenuHandler popupMenuHandler;

    /**
     * Builds the content panel for this dialog
     */
    protected void buildContentPanel() {
        lstPrimitives = new JList<>(model);
        lstPrimitives.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        lstPrimitives.setSelectionModel(selectionModel);
        lstPrimitives.setCellRenderer(new PrimitiveRenderer());
        lstPrimitives.setTransferHandler(new SelectionTransferHandler());
        if (!GraphicsEnvironment.isHeadless()) {
            lstPrimitives.setDragEnabled(true);
        }

        lstPrimitives.getSelectionModel().addListSelectionListener(actSelect);
        lstPrimitives.getSelectionModel().addListSelectionListener(actShowHistory);

        // the select action
        final SideButton selectButton = new SideButton(actSelect);
        selectButton.createArrow(e -> SelectionHistoryPopup.launch(selectButton, model.getSelectionHistory()));

        // the search button
        final SideButton searchButton = new SideButton(actSearch);
        searchButton.createArrow(e -> SearchPopupMenu.launch(searchButton), true);

        createLayout(lstPrimitives, true, Arrays.asList(
            selectButton, searchButton, new SideButton(actShowHistory)
        ));
    }

    /**
     * Constructs a new {@code SelectionListDialog}.
     */
    public SelectionListDialog() {
        super(tr("Selection"), "selectionlist", tr("Open a selection list window."),
                Shortcut.registerShortcut("subwindow:selection", tr("Toggle: {0}",
                tr("Current Selection")), KeyEvent.VK_T, Shortcut.ALT_SHIFT),
                150, // default height
                true // default is "show dialog"
        );

        buildContentPanel();
        model.addListDataListener(new TitleUpdater());
        model.addListDataListener(actZoomToJOSMSelection);

        popupMenu = new ListPopupMenu(lstPrimitives);
        popupMenuHandler = setupPopupMenuHandler();

        lstPrimitives.addListSelectionListener(e -> {
            actZoomToListSelection.valueChanged(e);
            popupMenuHandler.setPrimitives(model.getSelected());
        });

        lstPrimitives.addMouseListener(new MouseEventHandler());

        InputMapUtils.addEnterAction(lstPrimitives, actZoomToListSelection);
    }

    @Override
    public void showNotify() {
        SelectionEventManager.getInstance().addSelectionListenerForEdt(actShowHistory);
        SelectionEventManager.getInstance().addSelectionListenerForEdt(model);
        DatasetEventManager.getInstance().addDatasetListener(model, FireMode.IN_EDT);
        MainApplication.getLayerManager().addActiveLayerChangeListener(actSearch);
        // editLayerChanged also gets the selection history of the level. Listener calls setJOSMSelection when fired.
        MainApplication.getLayerManager().addAndFireActiveLayerChangeListener(model);
        actSearch.updateEnabledState();
    }

    @Override
    public void hideNotify() {
        MainApplication.getLayerManager().removeActiveLayerChangeListener(actSearch);
        MainApplication.getLayerManager().removeActiveLayerChangeListener(model);
        SelectionEventManager.getInstance().removeSelectionListener(actShowHistory);
        SelectionEventManager.getInstance().removeSelectionListener(model);
        DatasetEventManager.getInstance().removeDatasetListener(model);
    }

    /**
     * Responds to double clicks on the list of selected objects and launches the popup menu
     */
    class MouseEventHandler extends PopupMenuLauncher {
        private final HighlightHelper helper = new HighlightHelper();
        private final boolean highlightEnabled = Config.getPref().getBoolean("draw.target-highlight", true);

        MouseEventHandler() {
            super(popupMenu);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            int idx = lstPrimitives.locationToIndex(e.getPoint());
            if (idx < 0) return;
            if (isDoubleClick(e)) {
                DataSet ds = MainApplication.getLayerManager().getActiveDataSet();
                if (ds == null) return;
                OsmPrimitive osm = model.getElementAt(idx);
                Collection<OsmPrimitive> sel = ds.getSelected();
                if (sel.size() != 1 || !sel.iterator().next().equals(osm)) {
                    // Select primitive if it's not the whole current selection
                    ds.setSelected(Collections.singleton(osm));
                } else if (osm instanceof Relation) {
                    // else open relation editor if applicable
                    actEditRelationSelection.actionPerformed(null);
                }
            } else if (highlightEnabled && MainApplication.isDisplayingMapView() && helper.highlightOnly(model.getElementAt(idx))) {
                MainApplication.getMap().mapView.repaint();
            }
        }

        @Override
        public void mouseExited(MouseEvent me) {
            if (highlightEnabled) helper.clear();
            super.mouseExited(me);
        }
    }

    private PopupMenuHandler setupPopupMenuHandler() {
        PopupMenuHandler handler = new PopupMenuHandler(popupMenu);
        handler.addAction(actZoomToJOSMSelection);
        handler.addAction(actZoomToListSelection);
        handler.addSeparator();
        handler.addAction(actSetRelationSelection);
        handler.addAction(actEditRelationSelection);
        handler.addSeparator();
        handler.addAction(actDownloadSelIncompleteMembers);
        return handler;
    }

    /**
     * Replies the popup menu handler.
     * @return The popup menu handler
     */
    public PopupMenuHandler getPopupMenuHandler() {
        return popupMenuHandler;
    }

    /**
     * Replies the selected OSM primitives.
     * @return The selected OSM primitives
     */
    public Collection<OsmPrimitive> getSelectedPrimitives() {
        return model.getSelected();
    }

    /**
     * Updates the dialog title with a summary of the current JOSM selection
     */
    class TitleUpdater implements ListDataListener {
        protected void updateTitle() {
            setTitle(model.getJOSMSelectionSummary());
        }

        @Override
        public void contentsChanged(ListDataEvent e) {
            updateTitle();
        }

        @Override
        public void intervalAdded(ListDataEvent e) {
            updateTitle();
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
            updateTitle();
        }
    }

    /**
     * Launches the search dialog
     */
    static class SearchAction extends AbstractAction implements ActiveLayerChangeListener {
        /**
         * Constructs a new {@code SearchAction}.
         */
        SearchAction() {
            putValue(NAME, tr("Search"));
            putValue(SHORT_DESCRIPTION, tr("Search for objects"));
            new ImageProvider("dialogs", "search").getResource().attachImageIcon(this, true);
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isEnabled()) return;
            org.openstreetmap.josm.actions.search.SearchAction.search();
        }

        protected void updateEnabledState() {
            setEnabled(MainApplication.getLayerManager().getActiveData() != null);
        }

        @Override
        public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
            updateEnabledState();
        }
    }

    /**
     * Sets the current JOSM selection to the OSM primitives selected in the list
     * of this dialog
     */
    class SelectAction extends AbstractSelectAction implements ListSelectionListener {
        /**
         * Constructs a new {@code SelectAction}.
         */
        SelectAction() {
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Collection<OsmPrimitive> sel = model.getSelected();
            if (sel.isEmpty()) return;
            OsmData<?, ?, ?, ?> ds = MainApplication.getLayerManager().getActiveData();
            if (ds == null) return;
            ds.setSelected(sel);
            model.selectionModel.setSelectionInterval(0, sel.size()-1);
        }

        protected void updateEnabledState() {
            setEnabled(!model.isSelectionEmpty());
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    /**
     * The action for showing history information of the current history item.
     */
    class ShowHistoryAction extends AbstractAction implements ListSelectionListener, DataSelectionListener {
        /**
         * Constructs a new {@code ShowHistoryAction}.
         */
        ShowHistoryAction() {
            putValue(NAME, tr("History"));
            putValue(SHORT_DESCRIPTION, tr("Display the history of the selected objects."));
            new ImageProvider("dialogs", "history").getResource().attachImageIcon(this, true);
            updateEnabledState(model.getSize());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Collection<OsmPrimitive> sel = model.getSelected();
            if (sel.isEmpty() && model.getSize() != 1) {
                return;
            } else if (sel.isEmpty()) {
                sel = Collections.singleton(model.getElementAt(0));
            }
            HistoryBrowserDialogManager.getInstance().showHistory(sel);
        }

        protected void updateEnabledState(int osmSelectionSize) {
            // See #10830 - allow to click on history button is a single object is selected, even if not selected again in the list
            setEnabled(!model.isSelectionEmpty() || osmSelectionSize == 1);
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState(model.getSize());
        }

        @Override
        public void selectionChanged(SelectionChangeEvent event) {
            updateEnabledState(event.getSelection().size());
        }
    }

    /**
     * The action for zooming to the primitives in the current JOSM selection
     *
     */
    class ZoomToJOSMSelectionAction extends AbstractAction implements ListDataListener {

        ZoomToJOSMSelectionAction() {
            putValue(NAME, tr("Zoom to selection"));
            putValue(SHORT_DESCRIPTION, tr("Zoom to selection"));
            new ImageProvider("dialogs/autoscale", "selection").getResource().attachImageIcon(this, true);
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            AutoScaleAction.autoScale(AutoScaleMode.SELECTION);
        }

        public void updateEnabledState() {
            setEnabled(model.getSize() > 0);
        }

        @Override
        public void contentsChanged(ListDataEvent e) {
            updateEnabledState();
        }

        @Override
        public void intervalAdded(ListDataEvent e) {
            updateEnabledState();
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
            updateEnabledState();
        }
    }

    /**
     * The action for zooming to the primitives which are currently selected in
     * the list displaying the JOSM selection
     *
     */
    class ZoomToListSelection extends AbstractAction implements ListSelectionListener {
        /**
         * Constructs a new {@code ZoomToListSelection}.
         */
        ZoomToListSelection() {
            putValue(NAME, tr("Zoom to selected element(s)"));
            putValue(SHORT_DESCRIPTION, tr("Zoom to selected element(s)"));
            new ImageProvider("dialogs/autoscale", "selection").getResource().attachImageIcon(this, true);
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            BoundingXYVisitor box = new BoundingXYVisitor();
            Collection<OsmPrimitive> sel = model.getSelected();
            if (sel.isEmpty()) return;
            box.computeBoundingBox(sel);
            if (box.getBounds() == null)
                return;
            box.enlargeBoundingBox();
            MainApplication.getMap().mapView.zoomTo(box);
        }

        protected void updateEnabledState() {
            setEnabled(!model.isSelectionEmpty());
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    /**
     * The list model for the list of OSM primitives in the current JOSM selection.
     *
     * The model also maintains a history of the last {@link SelectionListModel#SELECTION_HISTORY_SIZE}
     * JOSM selection.
     *
     */
    static class SelectionListModel extends AbstractListModel<OsmPrimitive>
    implements ActiveLayerChangeListener, DataSelectionListener, DataSetListener {

        private static final int SELECTION_HISTORY_SIZE = 10;

        // Variable to store history from currentDataSet()
        private LinkedList<Collection<? extends OsmPrimitive>> history;
        private final transient List<OsmPrimitive> selection = new ArrayList<>();
        private final DefaultListSelectionModel selectionModel;

        /**
         * Constructor
         * @param selectionModel the selection model used in the list
         */
        SelectionListModel(DefaultListSelectionModel selectionModel) {
            this.selectionModel = selectionModel;
        }

        /**
         * Replies a summary of the current JOSM selection
         *
         * @return a summary of the current JOSM selection
         */
        public synchronized String getJOSMSelectionSummary() {
            if (selection.isEmpty()) return tr("Selection");
            int numNodes = 0;
            int numWays = 0;
            int numRelations = 0;
            for (OsmPrimitive p: selection) {
                switch(p.getType()) {
                case NODE: numNodes++; break;
                case WAY: numWays++; break;
                case RELATION: numRelations++; break;
                default: throw new AssertionError();
                }
            }
            return tr("Sel.: Rel.:{0} / Ways:{1} / Nodes:{2}", numRelations, numWays, numNodes);
        }

        /**
         * Remembers a JOSM selection the history of JOSM selections
         *
         * @param selection the JOSM selection. Ignored if null or empty.
         */
        public void remember(Collection<? extends OsmPrimitive> selection) {
            if (selection == null) return;
            if (selection.isEmpty()) return;
            if (history == null) return;
            if (history.isEmpty()) {
                history.add(selection);
                return;
            }
            if (history.getFirst().equals(selection)) return;
            history.addFirst(selection);
            for (int i = 1; i < history.size(); ++i) {
                if (history.get(i).equals(selection)) {
                    history.remove(i);
                    break;
                }
            }
            int maxsize = Config.getPref().getInt("select.history-size", SELECTION_HISTORY_SIZE);
            while (history.size() > maxsize) {
                history.removeLast();
            }
        }

        /**
         * Replies the history of JOSM selections
         *
         * @return history of JOSM selections
         */
        public List<Collection<? extends OsmPrimitive>> getSelectionHistory() {
            return history;
        }

        @Override
        public synchronized OsmPrimitive getElementAt(int index) {
            return selection.get(index);
        }

        @Override
        public synchronized int getSize() {
            return selection.size();
        }

        /**
         * Determines if no OSM primitives are currently selected.
         * @return {@code true} if no OSM primitives are currently selected
         * @since 10383
         */
        public boolean isSelectionEmpty() {
            return selectionModel.isSelectionEmpty();
        }

        /**
         * Replies the collection of OSM primitives currently selected in the view of this model
         *
         * @return choosen elements in the view
         */
        public synchronized Collection<OsmPrimitive> getSelected() {
            Set<OsmPrimitive> sel = new HashSet<>();
            for (int i = 0; i < getSize(); i++) {
                if (selectionModel.isSelectedIndex(i)) {
                    sel.add(selection.get(i));
                }
            }
            return sel;
        }

        /**
         * Sets the OSM primitives to be selected in the view of this model
         *
         * @param sel the collection of primitives to select
         */
        public synchronized void setSelected(Collection<OsmPrimitive> sel) {
            selectionModel.setValueIsAdjusting(true);
            selectionModel.clearSelection();
            if (sel != null) {
                for (OsmPrimitive p: sel) {
                    int i = selection.indexOf(p);
                    if (i >= 0) {
                        selectionModel.addSelectionInterval(i, i);
                    }
                }
            }
            selectionModel.setValueIsAdjusting(false);
        }

        @Override
        protected void fireContentsChanged(Object source, int index0, int index1) {
            Collection<OsmPrimitive> sel = getSelected();
            super.fireContentsChanged(source, index0, index1);
            setSelected(sel);
        }

        /**
         * Sets the collection of currently selected OSM objects
         *
         * @param selection the collection of currently selected OSM objects
         */
        public void setJOSMSelection(final Collection<? extends OsmPrimitive> selection) {
            synchronized (this) {
                this.selection.clear();
                if (selection != null) {
                    this.selection.addAll(selection);
                    sort();
                }
            }
            GuiHelper.runInEDTAndWait(new Runnable() {
                @Override public void run() {
                    fireContentsChanged(this, 0, getSize());
                    if (selection != null) {
                        remember(selection);
                    }
                }
            });
        }

        /**
         * Triggers a refresh of the view for all primitives in {@code toUpdate}
         * which are currently displayed in the view
         *
         * @param toUpdate the collection of primitives to update
         */
        public synchronized void update(Collection<? extends OsmPrimitive> toUpdate) {
            if (toUpdate == null) return;
            if (toUpdate.isEmpty()) return;
            Collection<OsmPrimitive> sel = getSelected();
            for (OsmPrimitive p: toUpdate) {
                int i = selection.indexOf(p);
                if (i >= 0) {
                    super.fireContentsChanged(this, i, i);
                }
            }
            setSelected(sel);
        }

        /**
         * Sorts the current elements in the selection
         */
        public synchronized void sort() {
            int size = selection.size();
            if (size > 1 && size <= Config.getPref().getInt("selection.no_sort_above", 100_000)) {
                boolean quick = size > Config.getPref().getInt("selection.fast_sort_above", 10_000);
                Comparator<OsmPrimitive> c = Config.getPref().getBoolean("selection.sort_relations_before_ways", true)
                        ? OsmPrimitiveComparator.orderingRelationsWaysNodes()
                        : OsmPrimitiveComparator.orderingWaysRelationsNodes();
                try {
                    selection.sort(c.thenComparing(quick
                            ? OsmPrimitiveComparator.comparingUniqueId()
                            : OsmPrimitiveComparator.comparingNames()));
                } catch (IllegalArgumentException e) {
                    throw BugReport.intercept(e).put("size", size).put("quick", quick).put("selection", selection);
                }
            }
        }

        /* ------------------------------------------------------------------------ */
        /* interface ActiveLayerChangeListener                                      */
        /* ------------------------------------------------------------------------ */
        @Override
        public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
            DataSet newData = e.getSource().getEditDataSet();
            if (newData == null) {
                setJOSMSelection(null);
                history = null;
            } else {
                history = newData.getSelectionHistory();
                setJOSMSelection(newData.getAllSelected());
            }
        }

        /* ------------------------------------------------------------------------ */
        /* interface DataSelectionListener                                          */
        /* ------------------------------------------------------------------------ */
        @Override
        public void selectionChanged(SelectionChangeEvent event) {
            setJOSMSelection(event.getSelection());
        }

        /* ------------------------------------------------------------------------ */
        /* interface DataSetListener                                                */
        /* ------------------------------------------------------------------------ */
        @Override
        public void dataChanged(DataChangedEvent event) {
            // refresh the whole list
            fireContentsChanged(this, 0, getSize());
        }

        @Override
        public void nodeMoved(NodeMovedEvent event) {
            // may influence the display name of primitives, update the data
            update(event.getPrimitives());
        }

        @Override
        public void otherDatasetChange(AbstractDatasetChangedEvent event) {
            // may influence the display name of primitives, update the data
            update(event.getPrimitives());
        }

        @Override
        public void relationMembersChanged(RelationMembersChangedEvent event) {
            // may influence the display name of primitives, update the data
            update(event.getPrimitives());
        }

        @Override
        public void tagsChanged(TagsChangedEvent event) {
            // may influence the display name of primitives, update the data
            update(event.getPrimitives());
        }

        @Override
        public void wayNodesChanged(WayNodesChangedEvent event) {
            // may influence the display name of primitives, update the data
            update(event.getPrimitives());
        }

        @Override
        public void primitivesAdded(PrimitivesAddedEvent event) {
            /* ignored - handled by SelectionChangeListener */
        }

        @Override
        public void primitivesRemoved(PrimitivesRemovedEvent event) {
            /* ignored - handled by SelectionChangeListener*/
        }
    }

    /**
     * A specialized {@link JMenuItem} for presenting one entry of the search history
     *
     * @author Jan Peter Stotz
     */
    protected static class SearchMenuItem extends JMenuItem implements ActionListener {
        protected final transient SearchSetting s;

        public SearchMenuItem(SearchSetting s) {
            super(Utils.shortenString(s.toString(),
                    org.openstreetmap.josm.actions.search.SearchAction.MAX_LENGTH_SEARCH_EXPRESSION_DISPLAY));
            this.s = s;
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            org.openstreetmap.josm.actions.search.SearchAction.searchWithoutHistory(s);
        }
    }

    /**
     * The popup menu for the search history entries
     *
     */
    protected static class SearchPopupMenu extends JPopupMenu {
        public static void launch(Component parent) {
            if (org.openstreetmap.josm.actions.search.SearchAction.getSearchHistory().isEmpty())
                return;
            if (parent.isShowing()) {
                JPopupMenu menu = new SearchPopupMenu();
                Rectangle r = parent.getBounds();
                menu.show(parent, r.x, r.y + r.height);
            }
        }

        /**
         * Constructs a new {@code SearchPopupMenu}.
         */
        public SearchPopupMenu() {
            for (SearchSetting ss: org.openstreetmap.josm.actions.search.SearchAction.getSearchHistory()) {
                add(new SearchMenuItem(ss));
            }
        }
    }

    /**
     * A specialized {@link JMenuItem} for presenting one entry of the selection history
     *
     * @author Jan Peter Stotz
     */
    protected static class SelectionMenuItem extends JMenuItem implements ActionListener {
        protected transient Collection<? extends OsmPrimitive> sel;

        public SelectionMenuItem(Collection<? extends OsmPrimitive> sel) {
            this.sel = sel;
            int ways = 0;
            int nodes = 0;
            int relations = 0;
            for (OsmPrimitive o : sel) {
                if (!o.isSelectable()) continue; // skip unselectable primitives
                if (o instanceof Way) {
                    ways++;
                } else if (o instanceof Node) {
                    nodes++;
                } else if (o instanceof Relation) {
                    relations++;
                }
            }
            StringBuilder text = new StringBuilder();
            if (ways != 0) {
                text.append(text.length() > 0 ? ", " : "")
                .append(trn("{0} way", "{0} ways", ways, ways));
            }
            if (nodes != 0) {
                text.append(text.length() > 0 ? ", " : "")
                .append(trn("{0} node", "{0} nodes", nodes, nodes));
            }
            if (relations != 0) {
                text.append(text.length() > 0 ? ", " : "")
                .append(trn("{0} relation", "{0} relations", relations, relations));
            }
            if (ways + nodes + relations == 0) {
                text.append(tr("Unselectable now"));
                this.sel = new ArrayList<>(); // empty selection
            }
            DefaultNameFormatter df = DefaultNameFormatter.getInstance();
            if (ways + nodes + relations == 1) {
                text.append(": ");
                for (OsmPrimitive o : sel) {
                    text.append(o.getDisplayName(df));
                }
                setText(text.toString());
            } else {
                setText(tr("Selection: {0}", text));
            }
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            MainApplication.getLayerManager().getActiveDataSet().setSelected(sel);
        }
    }

    /**
     * The popup menu for the JOSM selection history entries
     */
    protected static class SelectionHistoryPopup extends JPopupMenu {
        public static void launch(Component parent, Collection<Collection<? extends OsmPrimitive>> history) {
            if (history == null || history.isEmpty()) return;
            if (parent.isShowing()) {
                JPopupMenu menu = new SelectionHistoryPopup(history);
                Rectangle r = parent.getBounds();
                menu.show(parent, r.x, r.y + r.height);
            }
        }

        public SelectionHistoryPopup(Collection<Collection<? extends OsmPrimitive>> history) {
            for (Collection<? extends OsmPrimitive> sel : history) {
                add(new SelectionMenuItem(sel));
            }
        }
    }

    /**
     * A transfer handler class for drag-and-drop support.
     */
    protected class SelectionTransferHandler extends TransferHandler {

        @Override
        public int getSourceActions(JComponent c) {
            return COPY;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            return new PrimitiveTransferable(PrimitiveTransferData.getData(getSelectedPrimitives()));
        }
    }
}
