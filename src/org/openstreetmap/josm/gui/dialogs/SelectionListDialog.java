// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicArrowButton;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.search.SearchAction.SearchSetting;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.NameFormatter;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.SelectionEventManager;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager.FireMode;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.MapView.EditLayerChangeListener;
import org.openstreetmap.josm.gui.dialogs.relation.DownloadRelationMemberTask;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * A small tool dialog for displaying the current selection.
 * 
 */
public class SelectionListDialog extends ToggleDialog  {

    private JList lstPrimitives;
    private SelectionListModel model;

    private SelectAction actSelect;
    private SearchAction actSearch;
    private ZoomToJOSMSelectionAction actZoomToJOSMSelection;
    private ZoomToListSelection actZoomToListSelection;
    private DownloadSelectedIncompleteMembersAction actDownloadSelectedIncompleteMembers;

    /**
     * Builds the panel with the list of selected OSM primitives
     * 
     * @return the panel with the list of selected OSM primitives
     */
    protected JPanel buildListPanel() {
        JPanel pnl = new JPanel(new BorderLayout());
        DefaultListSelectionModel selectionModel  = new DefaultListSelectionModel();
        model = new SelectionListModel(selectionModel);
        lstPrimitives = new JList(model);
        lstPrimitives.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        lstPrimitives.setSelectionModel(selectionModel);
        lstPrimitives.setCellRenderer(new OsmPrimitivRenderer());
        pnl.add(new JScrollPane(lstPrimitives), BorderLayout.CENTER);

        return pnl;
    }

    /**
     * Builds the row of action buttons at the bottom of this dialog
     * 
     * @return the panel
     */
    protected JPanel buildActionPanel() {
        JPanel pnl = new  JPanel(new GridLayout(1,2));

        // the select action
        final JButton selectButton = new SideButton(actSelect = new SelectAction());
        lstPrimitives.getSelectionModel().addListSelectionListener(actSelect);
        pnl.add(selectButton);
        BasicArrowButton selectionHistoryMenuButton = createArrowButton(selectButton);
        selectionHistoryMenuButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SelectionHistoryPopup.launch(selectButton, model.getSelectionHistory());
            }
        });

        // the search button
        final JButton searchButton = new SideButton(actSearch = new SearchAction());
        pnl.add(searchButton);

        BasicArrowButton searchHistoryMenuButton = createArrowButton(searchButton);
        searchHistoryMenuButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SearchPopupMenu.launch(searchButton);
            }
        });

        return pnl;
    }

    /**
     * Builds the content panel for this dialog
     * 
     * @return the content panel
     */
    protected JPanel buildContentPanel() {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.add(buildListPanel(), BorderLayout.CENTER);
        pnl.add(buildActionPanel(), BorderLayout.SOUTH);
        return pnl;
    }

    public SelectionListDialog() {
        super(tr("Current Selection"), "selectionlist", tr("Open a selection list window."),
                Shortcut.registerShortcut("subwindow:selection", tr("Toggle: {0}", tr("Current Selection")), KeyEvent.VK_T, Shortcut.GROUP_LAYER, Shortcut.SHIFT_DEFAULT),
                150, // default height
                true // default is "show dialog"
        );

        add(buildContentPanel(), BorderLayout.CENTER);
        model.addListDataListener(new TitleUpdater());
        actZoomToJOSMSelection = new ZoomToJOSMSelectionAction();
        model.addListDataListener(actZoomToJOSMSelection);

        actZoomToListSelection = new ZoomToListSelection();
        lstPrimitives.getSelectionModel().addListSelectionListener(actZoomToListSelection);

        actDownloadSelectedIncompleteMembers = new DownloadSelectedIncompleteMembersAction();
        lstPrimitives.getSelectionModel().addListSelectionListener(actDownloadSelectedIncompleteMembers);

        lstPrimitives.addMouseListener(new SelectionPopupMenuLauncher());
    }

    @Override
    public void showNotify() {
        MapView.addEditLayerChangeListener(model);
        SelectionEventManager.getInstance().addSelectionListener(model, FireMode.IN_EDT_CONSOLIDATED);
        DatasetEventManager.getInstance().addDatasetListener(model, FireMode.IN_EDT);
        MapView.addEditLayerChangeListener(actSearch);
        if (Main.map.mapView.getEditLayer() != null) {
            model.setJOSMSelection(Main.map.mapView.getEditLayer().data.getSelected());
        }
    }

    @Override
    public void hideNotify() {
        MapView.removeEditLayerChangeListener(actSearch);
        MapView.removeEditLayerChangeListener(model);
        SelectionEventManager.getInstance().removeSelectionListener(model);
        DatasetEventManager.getInstance().removeDatasetListener(model);
    }

    private BasicArrowButton createArrowButton(JButton parentButton) {
        BasicArrowButton arrowButton = new BasicArrowButton(SwingConstants.SOUTH, null, null, Color.BLACK, null);
        arrowButton.setBorder(BorderFactory.createEmptyBorder());
        parentButton.setLayout(new BorderLayout());
        parentButton.add(arrowButton, BorderLayout.EAST);
        return arrowButton;
    }

    /**
     * The popup menu launcher
     */
    class SelectionPopupMenuLauncher extends PopupMenuLauncher {
        private SelectionPopup popup = new SelectionPopup();

        @Override
        public void launch(MouseEvent evt) {
            if (model.getSelected().isEmpty()) {
                int idx = lstPrimitives.locationToIndex(evt.getPoint());
                if (idx < 0) return;
                model.setSelected(Collections.singleton((OsmPrimitive)model.getElementAt(idx)));
            }
            popup.show(lstPrimitives, evt.getX(), evt.getY());
        }
    }

    /**
     * The popup menu for the selection list
     */
    class SelectionPopup extends JPopupMenu {
        public SelectionPopup() {
            add(actZoomToJOSMSelection);
            add(actZoomToListSelection);
            addSeparator();
            add(actDownloadSelectedIncompleteMembers);
        }
    }

    /**
     * Updates the dialog title with a summary of the current JOSM selection
     */
    class TitleUpdater implements ListDataListener {
        protected void updateTitle() {
            setTitle(model.getJOSMSelectionSummary());
        }

        public void contentsChanged(ListDataEvent e) {
            updateTitle();
        }

        public void intervalAdded(ListDataEvent e) {
            updateTitle();
        }

        public void intervalRemoved(ListDataEvent e) {
            updateTitle();
        }
    }

    /**
     * Launches the search dialog
     */
    class SearchAction extends AbstractAction implements EditLayerChangeListener {
        public SearchAction() {
            putValue(NAME, tr("Search"));
            putValue(SHORT_DESCRIPTION,   tr("Search for objects"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs","select"));
            updateEnabledState();
        }

        public void actionPerformed(ActionEvent e) {
            if (!isEnabled()) return;
            org.openstreetmap.josm.actions.search.SearchAction.search();
        }

        public void updateEnabledState() {
            setEnabled(Main.main != null && Main.main.getEditLayer() != null);
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
            if (Main.map == null || Main.map.mapView == null || Main.map.mapView.getEditLayer() == null) return;
            Main.map.mapView.getEditLayer().data.setSelected(sel);
        }

        public void updateEnabledState() {
            setEnabled(!model.getSelected().isEmpty());
        }

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
            new AutoScaleAction("selection").autoScale();
        }

        public void updateEnabledState() {
            setEnabled(model.getSize() > 0);
        }

        public void contentsChanged(ListDataEvent e) {
            updateEnabledState();
        }

        public void intervalAdded(ListDataEvent e) {
            updateEnabledState();
        }

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

        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    /**
     * The list model for the list of OSM primitives in the current JOSM selection.
     * 
     * The model also maintains a history of the last {@see SelectionListModel#SELECTION_HISTORY_SIZE}
     * JOSM selection.
     * 
     */
    static private class SelectionListModel extends AbstractListModel implements EditLayerChangeListener, SelectionChangedListener, DataSetListener{

        private static final int SELECTION_HISTORY_SIZE = 10;

        private final LinkedList<Collection<? extends OsmPrimitive>> history = new LinkedList<Collection<? extends OsmPrimitive>>();
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
            if (history.isEmpty()) {
                history.add(selection);
                return;
            }
            if (history.getFirst().equals(selection)) return;
            history.addFirst(selection);
            while (history.size() > SELECTION_HISTORY_SIZE) {
                history.removeLast();
            }
        }

        /**
         * Replies the history of JOSM selections
         * 
         * @return
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
         * @return
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
        public void setJOSMSelection(Collection<? extends OsmPrimitive> selection) {
            this.selection.clear();
            if (selection == null) {
                fireContentsChanged(this, 0, getSize());
                return;
            }
            this.selection.addAll(selection);
            sort();
            fireContentsChanged(this, 0, getSize());
            remember(selection);
        }

        /**
         * Sorts the primitives in the list
         */
        public void sort() {
            Collections.sort(
                    this.selection,
                    new Comparator<OsmPrimitive>() {
                        NameFormatter nf = DefaultNameFormatter.getInstance();
                        @Override
                        public int compare(OsmPrimitive o1, OsmPrimitive o2) {

                            if (o1.getType() != o2.getType())
                                return o1.getType().compareTo(o2.getType());
                            return o1.getDisplayName(nf).compareTo(o2.getDisplayName(nf));
                        }
                    }
            );
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
         * Replies the list of selected relations with incomplete members
         * 
         * @return the list of selected relations with incomplete members
         */
        public List<Relation> getSelectedRelationsWithIncompleteMembers() {
            List<Relation> ret = new LinkedList<Relation>();
            for(int i=0; i<getSize(); i++) {
                if (!selectionModel.isSelectedIndex(i)) {
                    continue;
                }
                OsmPrimitive p = selection.get(i);
                if (! (p instanceof Relation)) {
                    continue;
                }
                if (p.isNew()) {
                    continue;
                }
                Relation r = (Relation)p;
                if (r.hasIncompleteMembers()) {
                    ret.add(r);
                }
            }
            return ret;
        }

        /* ------------------------------------------------------------------------ */
        /* interface EditLayerChangeListener                                        */
        /* ------------------------------------------------------------------------ */
        public void editLayerChanged(OsmDataLayer oldLayer, OsmDataLayer newLayer) {
            if (newLayer == null) {
                setJOSMSelection(null);
            } else {
                setJOSMSelection(newLayer.data.getSelected());
            }
        }

        /* ------------------------------------------------------------------------ */
        /* interface SelectionChangeListener                                        */
        /* ------------------------------------------------------------------------ */
        public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
            setJOSMSelection(newSelection);
        }

        /* ------------------------------------------------------------------------ */
        /* interface DataSetListener                                                */
        /* ------------------------------------------------------------------------ */
        public void dataChanged(DataChangedEvent event) {
            // refresh the whole list
            fireContentsChanged(this, 0, getSize());
        }

        public void nodeMoved(NodeMovedEvent event) {
            // may influence the display name of primitives, update the data
            update(event.getPrimitives());
        }

        public void otherDatasetChange(AbstractDatasetChangedEvent event) {
            // may influence the display name of primitives, update the data
            update(event.getPrimitives());
        }

        public void relationMembersChanged(RelationMembersChangedEvent event) {
            // may influence the display name of primitives, update the data
            update(event.getPrimitives());
        }

        public void tagsChanged(TagsChangedEvent event) {
            // may influence the display name of primitives, update the data
            update(event.getPrimitives());
        }

        public void wayNodesChanged(WayNodesChangedEvent event) {
            // may influence the display name of primitives, update the data
            update(event.getPrimitives());
        }

        @Override
        public void primtivesAdded(PrimitivesAddedEvent event) {/* ignored - handled by SelectionChangeListener */}
        @Override
        public void primtivesRemoved(PrimitivesRemovedEvent event) {/* ignored - handled by SelectionChangeListener*/}
    }

    /**
     * A specialized {@link JMenuItem} for presenting one entry of the search history
     *
     * @author Jan Peter Stotz
     */
    protected static class SearchMenuItem extends JMenuItem implements ActionListener {
        protected SearchSetting s;

        public SearchMenuItem(SearchSetting s) {
            super(s.toString());
            this.s = s;
            addActionListener(this);
        }

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
            if (org.openstreetmap.josm.actions.search.SearchAction.searchHistory.isEmpty())
                return;
            JPopupMenu menu = new SearchPopupMenu();
            Rectangle r = parent.getBounds();
            menu.show(parent, r.x, r.y + r.height);
        }

        public SearchPopupMenu() {
            for (SearchSetting ss: org.openstreetmap.josm.actions.search.SearchAction.searchHistory) {
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
        protected Collection<? extends OsmPrimitive> sel;

        public SelectionMenuItem(Collection<? extends OsmPrimitive> sel) {
            super();
            this.sel = sel;
            int ways = 0;
            int nodes = 0;
            int relations = 0;
            for (OsmPrimitive o : sel) {
                if (o instanceof Way) {
                    ways++;
                } else if (o instanceof Node) {
                    nodes++;
                } else if (o instanceof Relation) {
                    relations++;
                }
            }
            StringBuffer text = new StringBuffer();
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
            setText(tr("Selection: {0}", text));
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e) {
            Main.main.getCurrentDataSet().setSelected(sel);
        }
    }

    /**
     * The popup menue for the JOSM selection history entries
     *
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
}
