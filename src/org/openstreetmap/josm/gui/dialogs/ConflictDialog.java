// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.AbstractAction;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.conflict.ConflictCollection;
import org.openstreetmap.josm.data.conflict.IConflictListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.AbstractVisitor;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * This dialog displays the {@see ConflictCollection} of the active {@see OsmDataLayer} in a toggle
 * dialog on the right of the main frame.
 *
 */
public final class ConflictDialog extends ToggleDialog implements MapView.LayerChangeListener, IConflictListener, SelectionChangedListener{

    static public Color getColor() {
        return Main.pref.getColor(marktr("conflict"), Color.gray);
    }

    /** the  collection of conflicts displayed by this conflict dialog*/
    private ConflictCollection conflicts;

    /** the model for the list of conflicts */
    private ConflictListModel model;
    /** the list widget for the list of conflicts */
    private JList lstConflicts;

    private ResolveAction actResolve;
    private SelectAction actSelect;

    private OsmDataLayer layer = null;

    /**
     * builds the GUI
     */
    protected void build() {
        model = new ConflictListModel();

        lstConflicts = new JList(model);
        lstConflicts.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        lstConflicts.setCellRenderer(new OsmPrimitivRenderer());
        lstConflicts.addMouseListener(new MouseAdapter(){
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    resolve();
                }
            }
        });
        lstConflicts.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
            public void valueChanged(ListSelectionEvent e) {
                Main.map.mapView.repaint();
            }
        });

        add(new JScrollPane(lstConflicts), BorderLayout.CENTER);

        SideButton btnResolve = new SideButton(actResolve = new ResolveAction());
        lstConflicts.getSelectionModel().addListSelectionListener(actResolve);

        SideButton btnSelect = new SideButton(actSelect = new SelectAction());
        lstConflicts.getSelectionModel().addListSelectionListener(actSelect);

        JPanel buttonPanel = getButtonPanel(2);
        buttonPanel.add(btnResolve);
        buttonPanel.add(btnSelect);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * constructor
     */
    public ConflictDialog() {
        super(tr("Conflict"), "conflict", tr("Resolve conflicts."),
                Shortcut.registerShortcut("subwindow:conflict", tr("Toggle: {0}", tr("Conflict")), KeyEvent.VK_C, Shortcut.GROUP_LAYER), 100);

        build();
        DataSet.selListeners.add(this);
        MapView.addLayerChangeListener(this);
        refreshView();
    }

    @Override
    public void tearDown() {
        MapView.removeLayerChangeListener(this);
        DataSet.selListeners.remove(this);
    }

    /**
     * Launches a conflict resolution dialog for the first selected conflict
     *
     */
    private final void resolve() {
        if (conflicts == null) return;
        if (conflicts.size() == 1) {
            lstConflicts.setSelectedIndex(0);
        }

        if (lstConflicts.getSelectedIndex() == -1)
            return;

        int [] selectedRows = lstConflicts.getSelectedIndices();
        if (selectedRows == null || selectedRows.length == 0)
            return;
        int row = selectedRows[0];
        Conflict<?> c = conflicts.get(row);
        OsmPrimitive my = c.getMy();
        OsmPrimitive their = c.getTheir();
        ConflictResolutionDialog dialog = new ConflictResolutionDialog(Main.parent);
        dialog.getConflictResolver().populate(my, their);
        dialog.setVisible(true);
        Main.map.mapView.repaint();
    }

    /**
     * refreshes the view of this dialog
     */
    public final void refreshView() {
        model.fireContentChanged();
    }

    /**
     * Paint all conflicts that can be expressed on the main window.
     */
    public void paintConflicts(final Graphics g, final NavigatableComponent nc) {
        Color preferencesColor = getColor();
        if (preferencesColor.equals(Main.pref.getColor(marktr("background"), Color.black)))
            return;
        g.setColor(preferencesColor);
        Visitor conflictPainter = new AbstractVisitor(){
            public void visit(Node n) {
                Point p = nc.getPoint(n);
                g.drawRect(p.x-1, p.y-1, 2, 2);
            }
            public void visit(Node n1, Node n2) {
                Point p1 = nc.getPoint(n1);
                Point p2 = nc.getPoint(n2);
                g.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
            public void visit(Way w) {
                Node lastN = null;
                for (Node n : w.getNodes()) {
                    if (lastN == null) {
                        lastN = n;
                        continue;
                    }
                    visit(lastN, n);
                    lastN = n;
                }
            }
            public void visit(Relation e) {
                for (RelationMember em : e.getMembers()) {
                    em.getMember().visit(this);
                }
            }
        };
        for (Object o : lstConflicts.getSelectedValues()) {
            if (conflicts == null || !conflicts.hasConflictForMy((OsmPrimitive)o)) {
                continue;
            }
            conflicts.getConflictForMy((OsmPrimitive)o).getTheir().visit(conflictPainter);
        }
    }

    /**
     * replies the conflict collection currently held by this dialog; may be null
     *
     * @return the conflict collection currently held by this dialog; may be null
     */
    public ConflictCollection getConflicts() {
        return conflicts;
    }

    /**
     * invoked if the active {@see Layer} changes
     */
    public void activeLayerChange(Layer oldLayer, Layer newLayer) {
        if (oldLayer instanceof OsmDataLayer) {
            this.layer = (OsmDataLayer)oldLayer;
            this.layer.getConflicts().removeConflictListener(this);
        }
        this.layer = null;
        if (newLayer instanceof OsmDataLayer) {
            this.layer = (OsmDataLayer)newLayer;
            layer.getConflicts().addConflictListener(this);
            this.conflicts = layer.getConflicts();
        }
        refreshView();
    }

    public void layerAdded(Layer newLayer) {
        // ignore
    }

    public void layerRemoved(Layer oldLayer) {
        if (this.layer == oldLayer) {
            this.layer = null;
            refreshView();
        }
    }

    public void onConflictsAdded(ConflictCollection conflicts) {
        refreshView();
    }

    public void onConflictsRemoved(ConflictCollection conflicts) {
        refreshView();
    }

    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        lstConflicts.clearSelection();
        for (OsmPrimitive osm : newSelection) {
            if (conflicts != null && conflicts.hasConflictForMy(osm)) {
                int pos = model.indexOf(osm);
                if (pos >= 0) {
                    lstConflicts.addSelectionInterval(pos, pos);
                }
            }
        }
    }

    @Override
    public String helpTopic() {
        return "Dialogs/ConflictListDialog";
    }

    /**
     * The {@see ListModel} for conflicts
     *
     */
    class ConflictListModel implements ListModel {

        private CopyOnWriteArrayList<ListDataListener> listeners;

        public ConflictListModel() {
            listeners = new CopyOnWriteArrayList<ListDataListener>();
        }

        public void addListDataListener(ListDataListener l) {
            if (l != null) {
                listeners.addIfAbsent(l);
            }
        }

        public void removeListDataListener(ListDataListener l) {
            listeners.remove(l);
        }

        protected void fireContentChanged() {
            ListDataEvent evt = new ListDataEvent(
                    this,
                    ListDataEvent.CONTENTS_CHANGED,
                    0,
                    getSize()
            );
            Iterator<ListDataListener> it = listeners.iterator();
            while(it.hasNext()) {
                it.next().contentsChanged(evt);
            }
        }

        public Object getElementAt(int index) {
            if (index < 0) return null;
            if (index >= getSize()) return null;
            return conflicts.get(index).getMy();
        }

        public int getSize() {
            if (conflicts == null) return 0;
            return conflicts.size();
        }

        public int indexOf(OsmPrimitive my) {
            if (conflicts == null) return -1;
            for (int i=0; i < conflicts.size();i++) {
                if (conflicts.get(i).isMatchingMy(my))
                    return i;
            }
            return -1;
        }

        public OsmPrimitive get(int idx) {
            if (conflicts == null) return null;
            return conflicts.get(idx).getMy();
        }
    }

    class ResolveAction extends AbstractAction implements ListSelectionListener {
        public ResolveAction() {
            putValue(NAME, tr("Resolve"));
            putValue(SHORT_DESCRIPTION,  tr("Open a merge dialog of all selected items in the list above."));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "conflict"));
            putValue("help", "Dialogs/ConflictListDialog#ResolveAction");
        }

        public void actionPerformed(ActionEvent e) {
            resolve();
        }

        public void valueChanged(ListSelectionEvent e) {
            ListSelectionModel model = (ListSelectionModel)e.getSource();
            boolean enabled = model.getMinSelectionIndex() >= 0
            && model.getMaxSelectionIndex() >= model.getMinSelectionIndex();
            setEnabled(enabled);
        }
    }

    class SelectAction extends AbstractAction implements ListSelectionListener {
        public SelectAction() {
            putValue(NAME, tr("Select"));
            putValue(SHORT_DESCRIPTION,  tr("Set the selected elements on the map to the selected items in the list above."));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "select"));
            putValue("help", "Dialogs/ConflictListDialog#SelectAction");
        }

        public void actionPerformed(ActionEvent e) {
            Collection<OsmPrimitive> sel = new LinkedList<OsmPrimitive>();
            for (Object o : lstConflicts.getSelectedValues()) {
                sel.add((OsmPrimitive)o);
            }
            Main.main.getCurrentDataSet().setSelected(sel);
        }

        public void valueChanged(ListSelectionEvent e) {
            ListSelectionModel model = (ListSelectionModel)e.getSource();
            boolean enabled = model.getMinSelectionIndex() >= 0
            && model.getMaxSelectionIndex() >= model.getMinSelectionIndex();
            setEnabled(enabled);
        }
    }
}
