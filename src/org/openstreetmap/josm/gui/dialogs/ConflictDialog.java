// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ConflictResolveCommand;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.AbstractVisitor;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.gui.ConflictResolver;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.tools.Shortcut;

public final class ConflictDialog extends ToggleDialog {

    public final Map<OsmPrimitive, OsmPrimitive> conflicts = new HashMap<OsmPrimitive, OsmPrimitive>();
    private final DefaultListModel model = new DefaultListModel();
    private final JList displaylist = new JList(model);

    private final SideButton sbSelect = new SideButton(marktr("Select"), "select", "Conflict",
            tr("Set the selected elements on the map to the selected items in the list above."), new ActionListener(){
        public void actionPerformed(ActionEvent e) {
            Collection<OsmPrimitive> sel = new LinkedList<OsmPrimitive>();
            for (Object o : displaylist.getSelectedValues()) {
                sel.add((OsmPrimitive)o);
            }
            Main.ds.setSelected(sel);
        }
    });
    private final SideButton sbResolve = new SideButton(marktr("Resolve"), "conflict", "Conflict",
            tr("Open a merge dialog of all selected items in the list above."), new ActionListener(){
        public void actionPerformed(ActionEvent e) {
            resolve();
        }
    });

    public ConflictDialog() {
        super(tr("Conflict"), "conflict", tr("Merging conflicts."),
                Shortcut.registerShortcut("subwindow:conflict", tr("Toggle: {0}", tr("Conflict")), KeyEvent.VK_C, Shortcut.GROUP_LAYER), 100);
        displaylist.setCellRenderer(new OsmPrimitivRenderer());
        displaylist.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        displaylist.addMouseListener(new MouseAdapter(){
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    resolve();
                }
            }
        });
        add(new JScrollPane(displaylist), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(1,2));
        buttonPanel.add(sbResolve);
        buttonPanel.add(sbSelect);
        add(buttonPanel, BorderLayout.SOUTH);

        DataSet.selListeners.add(new SelectionChangedListener(){
            public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
                displaylist.clearSelection();
                for (OsmPrimitive osm : newSelection) {
                    if (conflicts.containsKey(osm)) {
                        int pos = model.indexOf(osm);
                        displaylist.addSelectionInterval(pos, pos);
                    }
                }
            }
        });
        displaylist.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
            public void valueChanged(ListSelectionEvent e) {
                Main.map.mapView.repaint();
            }
        });

        rebuildList();
    }

    private final void resolve() {
        String method = Main.pref.get("conflict.resolution", "extended");
        method = method.trim().toLowerCase();
        if (method.equals("traditional")) {
            resolveTraditional();
        } else if (method.equals("extended")) {
            resolveExtended();
        } else {
            System.out.println(tr("WARNING: unexpected value for preference conflict.resolution, got " + method));
            resolveTraditional();
        }
    }


    private final void resolveExtended() {
        if(model.size() == 1) {
            displaylist.setSelectedIndex(0);
        }

        if (displaylist.getSelectedIndex() == -1)
            return;

        int [] selectedRows = displaylist.getSelectedIndices();
        if (selectedRows == null || selectedRows.length == 0)
            return;
        int row = selectedRows[0];
        OsmPrimitive my = (OsmPrimitive)model.get(row);
        OsmPrimitive their = conflicts.get(my);
        ConflictResolutionDialog dialog = new ConflictResolutionDialog(Main.parent);
        dialog.getConflictResolver().populate(my, their);
        dialog.setVisible(true);
        Main.map.mapView.repaint();
    }


    private final void resolveTraditional() {
        if(model.size() == 1) {
            displaylist.setSelectedIndex(0);
        }

        if (displaylist.getSelectedIndex() == -1)
            return;
        Map<OsmPrimitive, OsmPrimitive> sel = new HashMap<OsmPrimitive, OsmPrimitive>();
        for (int i : displaylist.getSelectedIndices()) {
            OsmPrimitive s = (OsmPrimitive)model.get(i);
            sel.put(s, conflicts.get(s));
        }
        ConflictResolver resolver = new ConflictResolver(sel);
        int answer = new ExtendedDialog(Main.parent,
                tr("Resolve Conflicts"),
                resolver,
                new String[] { tr("Solve Conflict"), tr("Cancel") },
                new String[] { "dialogs/conflict.png", "cancel.png"}
        ).getValue();

        if (answer != 1)
            return;
        Main.main.undoRedo.add(new ConflictResolveCommand(resolver.conflicts, sel));
        Main.map.mapView.repaint();
    }

    public final void rebuildList() {
        model.removeAllElements();
        for (OsmPrimitive osm : this.conflicts.keySet()) {
            model.addElement(osm);
        }

        if(model.size() != 0) {
            setTitle(tr("Conflicts: {0}", model.size()), true);
        } else {
            setTitle(tr("Conflicts"), false);
        }

        sbSelect.setEnabled(model.size() > 0);
        sbResolve.setEnabled(model.size() > 0);
    }

    public final void add(Map<OsmPrimitive, OsmPrimitive> conflicts) {
        this.conflicts.putAll(conflicts);
        rebuildList();
    }


    /**
     * removes a conflict registered for {@see OsmPrimitive} <code>my</code>
     *
     * @param my the {@see OsmPrimitive} for which a conflict is registered
     *   with this dialog
     */
    public void removeConflictForPrimitive(OsmPrimitive my) {
        if (! conflicts.keySet().contains(my))
            return;
        conflicts.remove(my);
        rebuildList();
        repaint();
    }

    /**
     * registers a conflict with this dialog. The conflict is represented
     * by a pair of {@see OsmPrimitive} with differences in their tag sets,
     * their node lists (for {@see Way}s) or their member lists (for {@see Relation}s)
     *
     * @param my  my version of the {@see OsmPrimitive}
     * @param their their version of the {@see OsmPrimitive}
     */
    public void addConflict(OsmPrimitive my, OsmPrimitive their) {
        conflicts.put(my, their);
        rebuildList();
        repaint();
    }

    static public Color getColor()
    {
        return Main.pref.getColor(marktr("conflict"), Color.gray);
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
                Point p = nc.getPoint(n.getEastNorth());
                g.drawRect(p.x-1, p.y-1, 2, 2);
            }
            public void visit(Node n1, Node n2) {
                Point p1 = nc.getPoint(n1.getEastNorth());
                Point p2 = nc.getPoint(n2.getEastNorth());
                g.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
            public void visit(Way w) {
                Node lastN = null;
                for (Node n : w.nodes) {
                    if (lastN == null) {
                        lastN = n;
                        continue;
                    }
                    visit(lastN, n);
                    lastN = n;
                }
            }
            public void visit(Relation e) {
                for (RelationMember em : e.members) {
                    em.member.visit(this);
                }
            }
        };
        for (Object o : displaylist.getSelectedValues()) {
            if (conflicts.get(o) == null) {
                continue;
            }
            conflicts.get(o).visit(conflictPainter);
        }
    }
}
