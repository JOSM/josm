// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicArrowButton;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.search.SearchAction;
import org.openstreetmap.josm.actions.search.SearchAction.SearchSetting;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.Layer.LayerChangeListener;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * A small tool dialog for displaying the current selection. The selection manager
 * respects clicks into the selection list. Ctrl-click will remove entries from
 * the list while single click will make the clicked entry the only selection.
 *
 * @author imi
 */
public class SelectionListDialog extends ToggleDialog implements SelectionChangedListener, LayerChangeListener {

    private static final int SELECTION_HISTORY_SIZE = 10;

    /**
     * The selection's list data.
     */
    private final DefaultListModel list = new DefaultListModel();

    private LinkedList<Collection<? extends OsmPrimitive>> selectionHistory;

    /**
     * The display list.
     */
    private JList displaylist = new JList(list);
    private SideButton selectButton;
    private SideButton searchButton;
    private JPopupMenu popupMenu;
    private JMenuItem zoomToElement;

    /**
     * If the selection changed event is triggered with newSelection equals
     * this element, the newSelection will not be added to the selection history
     */
    private Collection<? extends OsmPrimitive> historyIgnoreSelection = null;

    public SelectionListDialog() {
        super(tr("Current Selection"), "selectionlist", tr("Open a selection list window."),
                Shortcut.registerShortcut("subwindow:selection", tr("Toggle: {0}", tr("Current Selection")), KeyEvent.VK_T, Shortcut.GROUP_LAYER, Shortcut.SHIFT_DEFAULT), 150);

        selectionHistory = new LinkedList<Collection<? extends OsmPrimitive>>();
        popupMenu = new JPopupMenu();
        displaylist.setCellRenderer(new OsmPrimitivRenderer());
        displaylist.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        displaylist.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    updateMap();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                showPopupMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                showPopupMenu(e);
            }

        });

        add(new JScrollPane(displaylist), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));

        selectButton = new SideButton(marktr("Select"), "select", "SelectionList",
                tr("Set the selected elements on the map to the selected items in the list above."),
                new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateMap();
            }
        });
        buttonPanel.add(selectButton);
        BasicArrowButton selectionHistoryMenuButton = createArrowButton(selectButton);
        selectionHistoryMenuButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showSelectionHistoryMenu();
            }
        });
        add(buttonPanel, BorderLayout.SOUTH);

        zoomToElement = new JMenuItem(tr("Zoom to selected element(s)"));
        zoomToElement.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                zoomToSelectedElement();
            }
        });

        buttonPanel.add(new SideButton(marktr("Reload"), "refresh", "SelectionList", tr("Refresh the selection list."),
                new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectionChanged(Main.main.getCurrentDataSet().getSelected());
            }
        }));

        searchButton = new SideButton(marktr("Search"), "search", "SelectionList", tr("Search for objects."),
                Main.main.menu.search);
        buttonPanel.add(searchButton);

        BasicArrowButton searchHistoryMenuButton = createArrowButton(searchButton);
        searchHistoryMenuButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showSearchHistoryMenu();
            }
        });

        popupMenu.add(zoomToElement);
        JMenuItem zoomToSelection = new JMenuItem(tr("Zoom to selection"));
        zoomToSelection.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                zoomToSelection();
            }
        });
        popupMenu.add(zoomToSelection);

        if (Main.main.getCurrentDataSet() != null) {
            selectionChanged(Main.main.getCurrentDataSet().getSelected());
        }

        DataSet.selListeners.add(this);
        Layer.listeners.add(this);
    }

    private BasicArrowButton createArrowButton(SideButton parentButton) {
        BasicArrowButton arrowButton = new BasicArrowButton(SwingConstants.SOUTH, null, null, Color.BLACK, null);
        arrowButton.setBorder(BorderFactory.createEmptyBorder());
        //        selectionHistoryMenuButton.setContentAreaFilled(false);
        //        selectionHistoryMenuButton.setOpaque(false);
        //        selectionHistoryMenuButton.setBorderPainted(false);
        //        selectionHistoryMenuButton.setBackground(null);
        parentButton.setLayout(new BorderLayout());
        parentButton.add(arrowButton, BorderLayout.EAST);
        return arrowButton;
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        if (b && Main.main.getCurrentDataSet() != null) {
            selectionChanged(Main.main.getCurrentDataSet().getSelected());
        }
    }

    protected void showPopupMenu(MouseEvent e) {
        if (e.isPopupTrigger()) {
            zoomToElement.setVisible(displaylist.getSelectedIndex() >= 0);
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    public void zoomToSelection() {
        new AutoScaleAction("selection").actionPerformed(null);
    }

    /**
     * Zooms to the element(s) selected in {@link #displaylist}
     */
    public void zoomToSelectedElement() {
        BoundingXYVisitor box = new BoundingXYVisitor();
        int[] selected = displaylist.getSelectedIndices();
        if (selected.length == 0)
            return;
        for (int i = 0; i < selected.length; i++) {
            Object o = list.get(selected[i]);
            if (o instanceof OsmPrimitive) {
                ((OsmPrimitive) o).visit(box);
            }
        }
        if (box.getBounds() == null)
            return;
        box.enlargeBoundingBox();
        Main.map.mapView.recalculateCenterScale(box);
    }

    private void showSelectionHistoryMenu() {
        if (selectionHistory.size() == 0)
            return;
        JPopupMenu historyMenu = new JPopupMenu();
        for (Collection<? extends OsmPrimitive> sel : selectionHistory) {
            SelectionMenuItem item = new SelectionMenuItem(sel);
            historyMenu.add(item);
        }
        Rectangle r = selectButton.getBounds();
        historyMenu.show(selectButton, r.x, r.y + r.height);
    }

    private void showSearchHistoryMenu() {
        if (SearchAction.searchHistory.size() == 0)
            return;
        JPopupMenu historyMenu = new JPopupMenu();
        for (SearchAction.SearchSetting s : SearchAction.searchHistory) {
            SearchMenuItem item = new SearchMenuItem(s);
            historyMenu.add(item);
        }
        Rectangle r = searchButton.getBounds();
        historyMenu.show(searchButton, r.x, r.y + r.height);
    }

    /**
     * Called when the selection in the dataset changed.
     * @param newSelection The new selection array.
     */
    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        if (list == null || !isVisible())
            return; // selection changed may be received in base class constructor before init
        OsmPrimitive selArr[] = DataSet.sort(newSelection);
        list.setSize(selArr.length);
        int i = 0;
        for (OsmPrimitive osm : selArr) {
            list.setElementAt(osm, i++);
        }
        if (selectionHistory != null && newSelection.size() > 0 && !newSelection.equals(historyIgnoreSelection)) {
            historyIgnoreSelection = null;
            try {
                // Check if the newSelection has already been added to the history
                Collection<? extends OsmPrimitive> first = selectionHistory.getFirst();
                if (first.equals(newSelection))
                    return;
            } catch (NoSuchElementException e) {
            }
            selectionHistory.addFirst(newSelection);
            while (selectionHistory.size() > SELECTION_HISTORY_SIZE) {
                selectionHistory.removeLast();
            }
        }

        int ways = 0;
        int nodes = 0;
        int relations = 0;
        for (OsmPrimitive o : newSelection) {
            if (o instanceof Way) {
                ways++;
            } else if (o instanceof Node) {
                nodes++;
            } else if (o instanceof Relation) {
                relations++;
            }
        }

        if( (nodes+ways+relations) != 0) {
            setTitle(tr("Sel.: Rel.:{0} / Ways:{1} / Nodes:{2}", relations, ways, nodes));
        } else {
            setTitle(tr("Selection"));
        }
    }

    /**
     * Sets the selection of the map to the current selected items.
     */
    public void updateMap() {
        Collection<OsmPrimitive> sel = new LinkedList<OsmPrimitive>();
        for (int i = 0; i < list.getSize(); ++i)
            if (displaylist.isSelectedIndex(i)) {
                sel.add((OsmPrimitive) list.get(i));
            }
        Main.main.getCurrentDataSet().setSelected(sel);
    }

    /**
     * A specialized {@link JMenuItem} for presenting one entry of the selection history
     *
     * @author Jan Peter Stotz
     */
    protected class SelectionMenuItem extends JMenuItem implements ActionListener {
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
            String text = "";
            if(ways != 0) {
                text += (text.length() > 0 ? ", " : "")
                + trn("{0} way", "{0} ways", ways, ways);
            }
            if(nodes != 0) {
                text += (text.length() > 0 ? ", " : "")
                + trn("{0} node", "{0} nodes", nodes, nodes);
            }
            if(relations != 0) {
                text += (text.length() > 0 ? ", " : "")
                + trn("{0} relation", "{0} relations", relations, relations);
            }
            setText(tr("Selection: {0}", text));
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e) {
            historyIgnoreSelection = sel;
            Main.main.getCurrentDataSet().setSelected(sel);
        }

    }

    /**
     * A specialized {@link JMenuItem} for presenting one entry of the search history
     *
     * @author Jan Peter Stotz
     */
    protected class SearchMenuItem extends JMenuItem implements ActionListener {
        protected SearchSetting s;

        public SearchMenuItem(SearchSetting s) {
            super(s.toString());
            this.s = s;
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e) {
            SearchAction.searchWithoutHistory(s);
        }

    }

    public void activeLayerChange(Layer oldLayer, Layer newLayer) {
        if (newLayer instanceof OsmDataLayer) {
            OsmDataLayer dataLayer = (OsmDataLayer)newLayer;
            selectionChanged(dataLayer.data.getSelected());
        } else {
            List<OsmPrimitive> selection = Collections.emptyList();
            selectionChanged(selection);
        }
    }

    public void layerAdded(Layer newLayer) {
        // do nothing
    }

    public void layerRemoved(Layer oldLayer) {
        // do nothing
    }
}
