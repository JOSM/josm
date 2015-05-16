// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
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
import javax.swing.DefaultListSelectionModel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AbstractSelectAction;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.relation.DownloadSelectedIncompleteMembersAction;
import org.openstreetmap.josm.actions.relation.EditRelationAction;
import org.openstreetmap.josm.actions.relation.SelectInRelationListAction;
import org.openstreetmap.josm.actions.search.SearchAction.SearchSetting;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
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
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.EditLayerChangeListener;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.PopupMenuHandler;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.history.HistoryBrowserDialogManager;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.HighlightHelper;
import org.openstreetmap.josm.gui.widgets.ListPopupMenu;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;
import org.openstreetmap.josm.tools.Utils;

/**
 * A small tool dialog for displaying the current selection.
 * @since 8
 */
public class SelectionListDialog extends ToggleDialog  {
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
        lstPrimitives.setCellRenderer(new OsmPrimitivRenderer());
        // Fix #6290. Drag & Drop is not supported anyway and Copy/Paste is better propagated to main window
        lstPrimitives.setTransferHandler(null);

        lstPrimitives.getSelectionModel().addListSelectionListener(actSelect);
        lstPrimitives.getSelectionModel().addListSelectionListener(actShowHistory);

        // the select action
        final SideButton selectButton = new SideButton(actSelect);
        selectButton.createArrow(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SelectionHistoryPopup.launch(selectButton, model.getSelectionHistory());
            }
        });

        // the search button
        final SideButton searchButton = new SideButton(actSearch);
        searchButton.createArrow(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SearchPopupMenu.launch(searchButton);
            }
        });

        createLayout(lstPrimitives, true, Arrays.asList(new SideButton[] {
            selectButton, searchButton, new SideButton(actShowHistory)
        }));
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

        lstPrimitives.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                actZoomToListSelection.valueChanged(e);
                popupMenuHandler.setPrimitives(model.getSelected());
            }
        });

        lstPrimitives.addMouseListener(new MouseEventHandler());

        InputMapUtils.addEnterAction(lstPrimitives, actZoomToListSelection);
    }

    @Override
    public void showNotify() {
        MapView.addEditLayerChangeListener(model);
        SelectionEventManager.getInstance().addSelectionListener(actShowHistory, FireMode.IN_EDT_CONSOLIDATED);
        SelectionEventManager.getInstance().addSelectionListener(model, FireMode.IN_EDT_CONSOLIDATED);
        DatasetEventManager.getInstance().addDatasetListener(model, FireMode.IN_EDT);
        MapView.addEditLayerChangeListener(actSearch);
        // editLayerChanged also gets the selection history of the level
        OsmDataLayer editLayer = Main.main.getEditLayer();
        model.editLayerChanged(null, editLayer);
        if (editLayer != null) {
            model.setJOSMSelection(editLayer.data.getAllSelected());
        }
        actSearch.updateEnabledState();
    }

    @Override
    public void hideNotify() {
        MapView.removeEditLayerChangeListener(actSearch);
        MapView.removeEditLayerChangeListener(model);
        SelectionEventManager.getInstance().removeSelectionListener(actShowHistory);
        SelectionEventManager.getInstance().removeSelectionListener(model);
        DatasetEventManager.getInstance().removeDatasetListener(model);
    }

    /**
     * Responds to double clicks on the list of selected objects and launches the popup menu
     */
    class MouseEventHandler extends PopupMenuLauncher {
        private final HighlightHelper helper = new HighlightHelper();
        private boolean highlightEnabled = Main.pref.getBoolean("draw.target-highlight", true);
        public MouseEventHandler() {
            super(popupMenu);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            int idx = lstPrimitives.locationToIndex(e.getPoint());
            if (idx < 0) return;
            if (isDoubleClick(e)) {
                OsmDataLayer layer = Main.main.getEditLayer();
                if (layer == null) return;
                OsmPrimitive osm = model.getElementAt(idx);
                Collection<OsmPrimitive> sel = layer.data.getSelected();
                if (sel.size() != 1 || !sel.iterator().next().equals(osm)) {
                    // Select primitive if it's not the whole current selection
                    layer.data.setSelected(Collections.singleton(osm));
                } else if (osm instanceof Relation) {
                    // else open relation editor if applicable
                    actEditRelationSelection.actionPerformed(null);
                }
            } else if (highlightEnabled && Main.isDisplayingMapView()) {
                if (helper.highlightOnly(model.getElementAt(idx))) {
                    Main.map.mapView.repaint();
                }
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
    static class SearchAction extends AbstractAction implements EditLayerChangeListener {
        /**
         * Constructs a new {@code SearchAction}.
         */
        public SearchAction() {
            putValue(NAME, tr("Search"));
            putValue(SHORT_DESCRIPTION,   tr("Search for objects"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs","search"));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isEnabled()) return;
            org.openstreetmap.josm.actions.search.SearchAction.search();
        }

        protected void updateEnabledState() {
            setEnabled(Main.main != null && Main.main.hasEditLayer());
        }

        @Override
        public void editLayerChanged(OsmDataLayer oldLayer, OsmDataLayer newLayer) {
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
        public SelectAction() {
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Collection<OsmPrimitive> sel = model.getSelected();
            if (sel.isEmpty())return;
            OsmDataLayer editLayer = Main.main.getEditLayer();
            if (editLayer == null) return;
            editLayer.data.setSelected(sel);
            model.selectionModel.setSelectionInterval(0, sel.size()-1);
        }

        protected void updateEnabledState() {
            setEnabled(!model.getSelected().isEmpty());
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    /**
     * The action for showing history information of the current history item.
     */
    class ShowHistoryAction extends AbstractAction implements ListSelectionListener, SelectionChangedListener {
        /**
         * Constructs a new {@code ShowHistoryAction}.
         */
        public ShowHistoryAction() {
            putValue(NAME, tr("History"));
            putValue(SHORT_DESCRIPTION, tr("Display the history of the selected objects."));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "history"));
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
            setEnabled(!model.getSelected().isEmpty() || osmSelectionSize == 1);
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState(model.getSize());
        }

        @Override
        public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
            updateEnabledState(newSelection.size());
        }
    }

    /**
     * The action for zooming to the primitives in the current JOSM selection
     *
     */
    class ZoomToJOSMSelectionAction extends AbstractAction implements ListDataListener {

        public ZoomToJOSMSelectionAction() {
            putValue(NAME,tr("Zoom to selection"));
            putValue(SHORT_DESCRIPTION, tr("Zoom to selection"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs/autoscale", "selection"));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            AutoScaleAction.autoScale("selection");
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
        public ZoomToListSelection() {
            putValue(NAME, tr("Zoom to selected element(s)"));
            putValue(SHORT_DESCRIPTION, tr("Zoom to selected element(s)"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs/autoscale", "selection"));
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
            Main.map.mapView.zoomTo(box);
        }

        protected void updateEnabledState() {
            setEnabled(!model.getSelected().isEmpty());
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
    private static class SelectionListModel extends AbstractListModel<OsmPrimitive> implements EditLayerChangeListener, SelectionChangedListener, DataSetListener{

        private static final int SELECTION_HISTORY_SIZE = 10;

        // Variable to store history from currentDataSet()
        private LinkedList<Collection<? extends OsmPrimitive>> history;
        private final transient List<OsmPrimitive> selection = new ArrayList<>();
        private DefaultListSelectionModel selectionModel;

        /**
         * Constructor
         * @param selectionModel the selection model used in the list
         */
        public SelectionListModel(DefaultListSelectionModel selectionModel) {
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
            if (selection == null)return;
            if (selection.isEmpty())return;
            if (history == null) return;
            if (history.isEmpty()) {
                history.add(selection);
                return;
            }
            if (history.getFirst().equals(selection)) return;
            history.addFirst(selection);
            for(int i = 1; i < history.size(); ++i) {
                if(history.get(i).equals(selection)) {
                    history.remove(i);
                    break;
                }
            }
            int maxsize = Main.pref.getInteger("select.history-size", SELECTION_HISTORY_SIZE);
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
         * Replies the collection of OSM primitives currently selected in the view
         * of this model
         *
         * @return choosen elements in the view
         */
        public synchronized Collection<OsmPrimitive> getSelected() {
            Set<OsmPrimitive> sel = new HashSet<>();
            for(int i=0; i< getSize();i++) {
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
            selectionModel.clearSelection();
            if (sel == null) return;
            for (OsmPrimitive p: sel){
                int i = selection.indexOf(p);
                if (i >= 0){
                    selectionModel.addSelectionInterval(i, i);
                }
            }
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
                        if (selection.size() == 2) {
                            Iterator<? extends OsmPrimitive> it = selection.iterator();
                            OsmPrimitive n1 = it.next();
                            OsmPrimitive n2 = it.next();
                            // show distance between two selected nodes with coordinates
                            if (n1 instanceof Node && n2 instanceof Node) {
                                LatLon c1 = ((Node) n1).getCoor();
                                LatLon c2 = ((Node) n2).getCoor();
                                if (c1 != null && c2 != null) {
                                    Main.map.statusLine.setDist(c1.greatCircleDistance(c2));
                                    return;
                                }
                            }
                        }
                        Main.map.statusLine.setDist(
                                new SubclassFilteredCollection<OsmPrimitive, Way>(selection, OsmPrimitive.wayPredicate));
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
            for (OsmPrimitive p: toUpdate){
                int i = selection.indexOf(p);
                if (i >= 0) {
                    super.fireContentsChanged(this, i,i);
                }
            }
            setSelected(sel);
        }

        /**
         * Sorts the current elements in the selection
         */
        public synchronized void sort() {
            if (this.selection.size() <= Main.pref.getInteger("selection.no_sort_above", 100000)) {
                boolean quick = this.selection.size() > Main.pref.getInteger("selection.fast_sort_above", 10000);
                Collections.sort(this.selection, new OsmPrimitiveComparator(quick, false));
            }
        }

        /* ------------------------------------------------------------------------ */
        /* interface EditLayerChangeListener                                        */
        /* ------------------------------------------------------------------------ */
        @Override
        public void editLayerChanged(OsmDataLayer oldLayer, OsmDataLayer newLayer) {
            if (newLayer == null) {
                setJOSMSelection(null);
                history = null;
            } else {
                history = newLayer.data.getSelectionHistory();
                setJOSMSelection(newLayer.data.getAllSelected());
            }
        }

        /* ------------------------------------------------------------------------ */
        /* interface SelectionChangeListener                                        */
        /* ------------------------------------------------------------------------ */
        @Override
        public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
            setJOSMSelection(newSelection);
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
            JPopupMenu menu = new SearchPopupMenu();
            Rectangle r = parent.getBounds();
            menu.show(parent, r.x, r.y + r.height);
        }

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
        private final transient DefaultNameFormatter df = DefaultNameFormatter.getInstance();
        protected transient Collection<? extends OsmPrimitive> sel;

        public SelectionMenuItem(Collection<? extends OsmPrimitive> sel) {
            super();
            this.sel = sel;
            int ways = 0;
            int nodes = 0;
            int relations = 0;
            for (OsmPrimitive o : sel) {
                if (! o.isSelectable()) continue; // skip unselectable primitives
                if (o instanceof Way) {
                    ways++;
                } else if (o instanceof Node) {
                    nodes++;
                } else if (o instanceof Relation) {
                    relations++;
                }
            }
            StringBuilder text = new StringBuilder();
            if(ways != 0) {
                text.append(text.length() > 0 ? ", " : "")
                .append(trn("{0} way", "{0} ways", ways, ways));
            }
            if(nodes != 0) {
                text.append(text.length() > 0 ? ", " : "")
                .append(trn("{0} node", "{0} nodes", nodes, nodes));
            }
            if(relations != 0) {
                text.append(text.length() > 0 ? ", " : "")
                .append(trn("{0} relation", "{0} relations", relations, relations));
            }
            if(ways + nodes + relations == 0) {
                text.append(tr("Unselectable now"));
                this.sel=new ArrayList<>(); // empty selection
            }
            if(ways + nodes + relations == 1)
            {
                text.append(": ");
                for(OsmPrimitive o : sel) {
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
            Main.main.getCurrentDataSet().setSelected(sel);
        }
    }

    /**
     * The popup menu for the JOSM selection history entries
     */
    protected static class SelectionHistoryPopup extends JPopupMenu {
        public static void launch(Component parent, Collection<Collection<? extends OsmPrimitive>> history) {
            if (history == null || history.isEmpty()) return;
            JPopupMenu menu = new SelectionHistoryPopup(history);
            Rectangle r = parent.getBounds();
            menu.show(parent, r.x, r.y + r.height);
        }

        public SelectionHistoryPopup(Collection<Collection<? extends OsmPrimitive>> history) {
            for (Collection<? extends OsmPrimitive> sel : history) {
                add(new SelectionMenuItem(sel));
            }
        }
    }
}
