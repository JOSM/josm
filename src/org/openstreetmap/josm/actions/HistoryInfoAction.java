//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Shortcut;

public class HistoryInfoAction extends JosmAction {

    public HistoryInfoAction() {
        super(tr("History of Element"), "about",
        tr("Display history information about OSM ways or nodes."),
        Shortcut.registerShortcut("core:history",
        tr("History of Element"), KeyEvent.VK_H, Shortcut.GROUP_HOTKEY), true);
    }

    public void actionPerformed(ActionEvent e) {
        final Collection<Object> sel = new LinkedList<Object>();
        new Visitor() {
            public void visit(Node n) {
                OpenBrowser.displayUrl("http://www.openstreetmap.org/browse/node/" + n.id + "/history");
                sel.add(n);
            }

            public void visit(Way w) {
                OpenBrowser.displayUrl("http://www.openstreetmap.org/browse/way/" + w.id + "/history");
                sel.add(w);
            }

            public void visit(Relation e) {
                OpenBrowser.displayUrl("http://www.openstreetmap.org/browse/relation/" + e.id + "/history");
                sel.add(e);
            }

            public void visitAll() {
                for (OsmPrimitive osm : Main.ds.getSelected())
                    osm.visit(this);
            }
        }.visitAll();

        if (sel.isEmpty()) {
            JOptionPane.showMessageDialog(Main.parent,
            tr("Please select at least one node, way or relation."));
                return;
        }
    }
}
