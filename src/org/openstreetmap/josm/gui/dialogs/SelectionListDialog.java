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
import java.util.Comparator;
import java.util.HashSet;
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
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.relation.DownloadSelectedIncompleteMembersAction;
import org.openstreetmap.josm.actions.relation.EditRelationAction;
import org.openstreetmap.josm.actions.relation.SelectInRelationListAction;
import org.openstreetmap.josm.actions.search.SearchAction;
import org.openstreetmap.josm.actions.search.SearchAction.SearchSetting;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveComparator;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
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
    private JList lstPrimitives;
    private DefaultListSelectionModel selectionModel  = new DefaultListSelectionModel();
    private SelectionListModel model = new SelectionListModel(selectionModel);

    private SelectAction actSelect = new SelectAction();
    private SearchAction actSearch = new SearchAction();
    private ZoomToJOSMSelectionAction actZoomToJOSMSelection = new ZoomToJOSMSelectionAction();
    private ZoomToListSelection actZoomToListSelection = new ZoomToListSelection();
    private SelectInRelationListAction actSetRelationSelection = new SelectInRelationListAction();
    private EditRelationAction actEditRelationSelection = new EditRelationAction();
    private DownloadSelectedIncompleteMembersAction actDownloadSelectedIncompleteMembers = new DownloadSelectedIncompleteMembersAction();

    /** the popup menu and its handler */
    private final ListPopupMenu popupMenu;
    private final PopupMenuHandler popupMenuHandler;

    /**
     * Builds the content panel for this dialog
     */
    protected void buildContentPanel() {
        lstPrimitives = new JList(model);
        lstPrimitives.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        lstPrimitives.setSelectionModel(selectionModel);
        lstPrimitives.setCellRenderer(new OsmPrimitivRenderer());
        lstPrimitives.setTransferHandler(null); // Fix #6290. Drag & Drop is not supported anyway and Copy/Paste is better propagated to main window

        // the select action
        final SideButton selectButton = new SideButton(actSelect);
        lstPrimitives.getSelectionModel().addListSelectionListener(actSelect);
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
            selectButton, searchButton
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
                layer.data.setSelected(Collections.singleton((OsmPrimitive)model.getElementAt(idx)));
            } else if (highlightEnabled && Main.isDisplayingMapView()) {
                if (helper.highlightOnly((OsmPrimitive)model.getElementAt(idx))) {
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
        handler.addAction(actDownloadSelectedIncompleteMembers);
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

        public void updateEnabledState() {
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
    class SelectAction extends AbstractAction implements ListSelectionListener {
        public SelectAction() {
            putValue(NAME, tr("Select"));
            putValue(SHORT_DESCRIPTION,  tr("Set the selected elements on the map to the selected items in the list above."));
            putValue(SMALL_ICON, ImageProvider.get("dialogs","select"));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Collection<OsmPrimitive> sel = model.getSelected();
            if (sel.isEmpty())return;
            OsmDataLayer editLayer = Main.main.getEditLayer();
            if (editLayer == null) return;
            editLayer.data.setSelected(sel);
        }

        public void updateEnabledState() {
            setEnabled(!model.getSelected().isEmpty());
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
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
    class ZoomToListSelection extends AbstractAction implements ListSelectionListener{
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
            Main.map.mapView.recalculateCenterScale(box);
        }

        public void updateEnabledState() {
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
    static private class SelectionListModel extends AbstractListModel implements EditLayerChangeListener, SelectionChangedListener, DataSetListener{

        private static final int SELECTION_HISTORY_SIZE = 10;

        // Variable to store history from currentDataSet()
        private LinkedList<Collection<? extends OsmPrimitive>> history;
        private final List<OsmPrimitive> selection = new ArrayList<OsmPrimitive>();
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
        public String getJOSMSelectionSummary() {
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
        public Object getElementAt(int index) {
            return selection.get(index);
        }

        @Override
        public int getSize() {
            return selection.size();
        }

        /**
         * Replies the collection of OSM primitives currently selected in the view
         * of this model
         *
         * @return choosen elements in the view
         */
        public Collection<OsmPrimitive> getSelected() {
            Set<OsmPrimitive> sel = new HashSet<OsmPrimitive>();
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
        public void setSelected(Collection<OsmPrimitive> sel) {
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
            this.selection.clear();
            if (selection != null) {
                this.selection.addAll(selection);
                sort();
            }
            GuiHelper.runInEDTAndWait(new Runnable() {
                @Override public void run() {
                    fireContentsChanged(this, 0, getSize());
                    if (selection != null) {
                        remember(selection);
                        Main.map.statusLine.setDist(new SubclassFilteredCollection<OsmPrimitive, Way>(selection, OsmPrimitive.wayPredicate));
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
        public void update(Collection<? extends OsmPrimitive> toUpdate) {
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
        public void sort() {
            if (this.selection.size()>Main.pref.getInteger("selection.no_sort_above",100000)) return;
            if (this.selection.size()>Main.pref.getInteger("selection.fast_sort_above",10000)) {
                Collections.sort(this.selection, new OsmPrimitiveQuickComparator());
            } else {
                Collections.sort(this.selection, new OsmPrimitiveComparator());
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
        public void primitivesAdded(PrimitivesAddedEvent event) {/* ignored - handled by SelectionChangeListener */}
        @Override
        public void primitivesRemoved(PrimitivesRemovedEvent event) {/* ignored - handled by SelectionChangeListener*/}
    }

    /**
     * A specialized {@link JMenuItem} for presenting one entry of the search history
     *
     * @author Jan Peter Stotz
     */
    protected static class SearchMenuItem extends JMenuItem implements ActionListener {
        final protected SearchSetting s;

        public SearchMenuItem(SearchSetting s) {
            super(Utils.shortenString(s.toString(), org.openstreetmap.josm.actions.search.SearchAction.MAX_LENGTH_SEARCH_EXPRESSION_DISPLAY));
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
        static public void launch(Component parent) {
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
        final private DefaultNameFormatter df = DefaultNameFormatter.getInstance();
        protected Collection<? extends OsmPrimitive> sel;

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
                this.sel=new ArrayList<OsmPrimitive>(); // empty selection
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
        static public void launch(Component parent, Collection<Collection<? extends OsmPrimitive>> history) {
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

    /** Quicker comparator, comparing just by type and ID's */
    static private class OsmPrimitiveQuickComparator implements Comparator<OsmPrimitive> {

        private int compareId(OsmPrimitive a, OsmPrimitive b) {
            long id_a=a.getUniqueId();
            long id_b=b.getUniqueId();
            if (id_a<id_b) return -1;
            if (id_a>id_b) return 1;
            return 0;
        }

        private int compareType(OsmPrimitive a, OsmPrimitive b) {
            // show ways before relations, then nodes
            if (a.getType().equals(OsmPrimitiveType.WAY)) return -1;
            if (a.getType().equals(OsmPrimitiveType.NODE)) return 1;
            // a is a relation
            if (b.getType().equals(OsmPrimitiveType.WAY)) return 1;
            // b is a node
            return -1;
        }

        @Override
        public int compare(OsmPrimitive a, OsmPrimitive b) {
            if (a.getType().equals(b.getType()))
                return compareId(a, b);
            return compareType(a, b);
        }
    }

}
