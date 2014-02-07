// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.AbstractAction;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
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
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.PopupMenuHandler;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * This dialog displays the {@link ConflictCollection} of the active {@link OsmDataLayer} in a toggle
 * dialog on the right of the main frame.
 *
 */
public final class ConflictDialog extends ToggleDialog implements MapView.EditLayerChangeListener, IConflictListener, SelectionChangedListener{

    /**
     * Replies the color used to paint conflicts.
     *
     * @return the color used to paint conflicts
     * @since 1221
     * @see #paintConflicts
     */
    static public Color getColor() {
        return Main.pref.getColor(marktr("conflict"), Color.gray);
    }

    /** the collection of conflicts displayed by this conflict dialog */
    private ConflictCollection conflicts;

    /** the model for the list of conflicts */
    private ConflictListModel model;
    /** the list widget for the list of conflicts */
    private JList lstConflicts;

    private final JPopupMenu popupMenu = new JPopupMenu();
    private final PopupMenuHandler popupMenuHandler = new PopupMenuHandler(popupMenu);

    private ResolveAction actResolve;
    private SelectAction actSelect;

    /**
     * builds the GUI
     */
    protected void build() {
        model = new ConflictListModel();

        lstConflicts = new JList(model);
        lstConflicts.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        lstConflicts.setCellRenderer(new OsmPrimitivRenderer());
        lstConflicts.addMouseListener(new MouseEventHandler());
        addListSelectionListener(new ListSelectionListener(){
            @Override
            public void valueChanged(ListSelectionEvent e) {
                Main.map.mapView.repaint();
            }
        });

        SideButton btnResolve = new SideButton(actResolve = new ResolveAction());
        addListSelectionListener(actResolve);

        SideButton btnSelect = new SideButton(actSelect = new SelectAction());
        addListSelectionListener(actSelect);

        createLayout(lstConflicts, true, Arrays.asList(new SideButton[] {
            btnResolve, btnSelect
        }));

        popupMenuHandler.addAction(Main.main.menu.autoScaleActions.get("conflict"));
    }

    /**
     * constructor
     */
    public ConflictDialog() {
        super(tr("Conflict"), "conflict", tr("Resolve conflicts."),
                Shortcut.registerShortcut("subwindow:conflict", tr("Toggle: {0}", tr("Conflict")),
                KeyEvent.VK_C, Shortcut.ALT_SHIFT), 100);

        build();
        refreshView();
    }

    @Override
    public void showNotify() {
        DataSet.addSelectionListener(this);
        MapView.addEditLayerChangeListener(this, true);
        refreshView();
    }

    @Override
    public void hideNotify() {
        MapView.removeEditLayerChangeListener(this);
        DataSet.removeSelectionListener(this);
    }

    /**
     * Add a list selection listener to the conflicts list.
     * @param listener the ListSelectionListener
     * @since 5958
     */
    public void addListSelectionListener(ListSelectionListener listener) {
        lstConflicts.getSelectionModel().addListSelectionListener(listener);
    }

    /**
     * Remove the given list selection listener from the conflicts list.
     * @param listener the ListSelectionListener
     * @since 5958
     */
    public void removeListSelectionListener(ListSelectionListener listener) {
        lstConflicts.getSelectionModel().removeListSelectionListener(listener);
    }

    /**
     * Replies the popup menu handler.
     * @return The popup menu handler
     * @since 5958
     */
    public PopupMenuHandler getPopupMenuHandler() {
        return popupMenuHandler;
    }

    /**
     * Launches a conflict resolution dialog for the first selected conflict
     *
     */
    private final void resolve() {
        if (conflicts == null || model.getSize() == 0) return;

        int index = lstConflicts.getSelectedIndex();
        if (index < 0) {
            index = 0;
        }

        Conflict<? extends OsmPrimitive> c = conflicts.get(index);
        ConflictResolutionDialog dialog = new ConflictResolutionDialog(Main.parent);
        dialog.getConflictResolver().populate(c);
        dialog.setVisible(true);

        lstConflicts.setSelectedIndex(index);

        Main.map.mapView.repaint();
    }

    /**
     * refreshes the view of this dialog
     */
    public final void refreshView() {
        OsmDataLayer editLayer =  Main.main.getEditLayer();
        conflicts = (editLayer == null ? new ConflictCollection() : editLayer.getConflicts());
        GuiHelper.runInEDT(new Runnable() {
            @Override
            public void run() {
                model.fireContentChanged();
                updateTitle();
            }
        });
    }

    private void updateTitle() {
        int conflictsCount = conflicts.size();
        if (conflictsCount > 0) {
            setTitle(tr("Conflicts: {0} unresolved", conflicts.size()) +
                    " ("+tr("Rel.:{0} / Ways:{1} / Nodes:{2}",
                            conflicts.getRelationConflicts().size(),
                            conflicts.getWayConflicts().size(),
                            conflicts.getNodeConflicts().size())+")");
        } else {
            setTitle(tr("Conflict"));
        }
    }

    /**
     * Paints all conflicts that can be expressed on the main window.
     *
     * @param g The {@code Graphics} used to paint
     * @param nc The {@code NavigatableComponent} used to get screen coordinates of nodes
     * @since 86
     */
    public void paintConflicts(final Graphics g, final NavigatableComponent nc) {
        Color preferencesColor = getColor();
        if (preferencesColor.equals(Main.pref.getColor(marktr("background"), Color.black)))
            return;
        g.setColor(preferencesColor);
        Visitor conflictPainter = new AbstractVisitor() {
            // Manage a stack of visited relations to avoid infinite recursion with cyclic relations (fix #7938)
            private final Set<Relation> visited = new HashSet<Relation>();
            @Override
            public void visit(Node n) {
                Point p = nc.getPoint(n);
                g.drawRect(p.x-1, p.y-1, 2, 2);
            }
            public void visit(Node n1, Node n2) {
                Point p1 = nc.getPoint(n1);
                Point p2 = nc.getPoint(n2);
                g.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
            @Override
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
            @Override
            public void visit(Relation e) {
                if (!visited.contains(e)) {
                    visited.add(e);
                    try {
                        for (RelationMember em : e.getMembers()) {
                            em.getMember().accept(this);
                        }
                    } finally {
                        visited.remove(e);
                    }
                }
            }
        };
        for (Object o : lstConflicts.getSelectedValues()) {
            if (conflicts == null || !conflicts.hasConflictForMy((OsmPrimitive)o)) {
                continue;
            }
            conflicts.getConflictForMy((OsmPrimitive)o).getTheir().accept(conflictPainter);
        }
    }

    @Override
    public void editLayerChanged(OsmDataLayer oldLayer, OsmDataLayer newLayer) {
        if (oldLayer != null) {
            oldLayer.getConflicts().removeConflictListener(this);
        }
        if (newLayer != null) {
            newLayer.getConflicts().addConflictListener(this);
        }
        refreshView();
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
     * returns the first selected item of the conflicts list
     *
     * @return Conflict
     */
    public Conflict<? extends OsmPrimitive> getSelectedConflict() {
        if (conflicts == null || model.getSize() == 0) return null;

        int index = lstConflicts.getSelectedIndex();
        if (index < 0) return null;

        return conflicts.get(index);
    }

    @Override
    public void onConflictsAdded(ConflictCollection conflicts) {
        refreshView();
    }

    @Override
    public void onConflictsRemoved(ConflictCollection conflicts) {
        Main.info("1 conflict has been resolved.");
        refreshView();
    }

    @Override
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
        return ht("/Dialog/ConflictList");
    }

    class MouseEventHandler extends PopupMenuLauncher {
        public MouseEventHandler() {
            super(popupMenu);
        }
        @Override public void mouseClicked(MouseEvent e) {
            if (isDoubleClick(e)) {
                resolve();
            }
        }
    }

    /**
     * The {@link ListModel} for conflicts
     *
     */
    class ConflictListModel implements ListModel {

        private CopyOnWriteArrayList<ListDataListener> listeners;

        public ConflictListModel() {
            listeners = new CopyOnWriteArrayList<ListDataListener>();
        }

        @Override
        public void addListDataListener(ListDataListener l) {
            if (l != null) {
                listeners.addIfAbsent(l);
            }
        }

        @Override
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
            for (ListDataListener listener : listeners) {
                listener.contentsChanged(evt);
            }
        }

        @Override
        public Object getElementAt(int index) {
            if (index < 0) return null;
            if (index >= getSize()) return null;
            return conflicts.get(index).getMy();
        }

        @Override
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
            putValue("help", ht("/Dialog/ConflictList#ResolveAction"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            resolve();
        }

        @Override
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
            putValue("help", ht("/Dialog/ConflictList#SelectAction"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Collection<OsmPrimitive> sel = new LinkedList<OsmPrimitive>();
            for (Object o : lstConflicts.getSelectedValues()) {
                sel.add((OsmPrimitive)o);
            }
            DataSet ds = Main.main.getCurrentDataSet();
            if (ds != null) { // Can't see how it is possible but it happened in #7942
                ds.setSelected(sel);
            }
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            ListSelectionModel model = (ListSelectionModel)e.getSource();
            boolean enabled = model.getMinSelectionIndex() >= 0
            && model.getMaxSelectionIndex() >= model.getMinSelectionIndex();
            setEnabled(enabled);
        }
    }

    /**
     * Warns the user about the number of detected conflicts
     *
     * @param numNewConflicts the number of detected conflicts
     * @since 5775
     */
    public void warnNumNewConflicts(int numNewConflicts) {
        if (numNewConflicts == 0) return;

        String msg1 = trn(
                "There was {0} conflict detected.",
                "There were {0} conflicts detected.",
                numNewConflicts,
                numNewConflicts
        );

        final StringBuilder sb = new StringBuilder();
        sb.append("<html>").append(msg1).append("</html>");
        if (numNewConflicts > 0) {
            final ButtonSpec[] options = new ButtonSpec[] {
                    new ButtonSpec(
                            tr("OK"),
                            ImageProvider.get("ok"),
                            tr("Click to close this dialog and continue editing"),
                            null /* no specific help */
                    )
            };
            GuiHelper.runInEDT(new Runnable() {
                @Override
                public void run() {
                    HelpAwareOptionPane.showOptionDialog(
                            Main.parent,
                            sb.toString(),
                            tr("Conflicts detected"),
                            JOptionPane.WARNING_MESSAGE,
                            null, /* no icon */
                            options,
                            options[0],
                            ht("/Concepts/Conflict#WarningAboutDetectedConflicts")
                    );
                    unfurlDialog();
                    Main.map.repaint();
                }
            });
        }
    }
}
