//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.AbstractVisitor;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Shortcut;

public class HistoryInfoAction extends JosmAction {

    public HistoryInfoAction() {
        super(tr("History of Element"), "about",
        tr("Display history information about OSM ways or nodes."),
        Shortcut.registerShortcut("core:history",
        tr("History of Element"), KeyEvent.VK_H, Shortcut.GROUP_HOTKEY), true);
    }

    /**
     * replies the base URL for browsing the the history of an OSM primitive
     *
     * @return the base URL, i.e. http://api.openstreetmap.org/browse
     */
    protected String getBaseURL() {
        String baseUrl = Main.pref.get("osm-server.url", "http://api.openstreetmap.org/api");
        Pattern pattern = Pattern.compile("/api/?$");
        String ret =  pattern.matcher(baseUrl).replaceAll("/browse");
        if (ret.equals(baseUrl)) {
            System.out.println("WARNING: unexpected format of API base URL. Redirection to history page for OSM primitive will probably fail. API base URL is: " + baseUrl);
        }
        return ret;
    }

    public void actionPerformed(ActionEvent e) {
        final Collection<Object> sel = new LinkedList<Object>();
        final String baseUrl  = getBaseURL();
        new AbstractVisitor() {
            public void visit(Node n) {
                if(n.id <= 0) return;
                OpenBrowser.displayUrl(baseUrl + "/node/" + n.id + "/history");
                sel.add(n);
            }

            public void visit(Way w) {
                if(w.id <= 0) return;
                OpenBrowser.displayUrl(baseUrl + "/way/" + w.id + "/history");
                sel.add(w);
            }

            public void visit(Relation e) {
                if(e.id <= 0) return;
                OpenBrowser.displayUrl(baseUrl + "/relation/" + e.id + "/history");
                sel.add(e);
            }

            public void visitAll() {
                for (OsmPrimitive osm : Main.ds.getSelected())
                    osm.visit(this);
            }
        }.visitAll();

        if (sel.isEmpty()) {
            JOptionPane.showMessageDialog(Main.parent,
            tr("Please select at least one node, way or relation. Only already uploaded elements have a history."));
                return;
        }
    }
}
