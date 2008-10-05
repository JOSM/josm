//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.ShortCut;

public class HistoryInfoAction extends JosmAction {

	public HistoryInfoAction() {
		super(tr("OSM History Information"), "about",tr("Display history information about OSM ways or nodes."),
		ShortCut.registerShortCut("core:history", tr("Display history"), KeyEvent.VK_H, ShortCut.GROUP_HOTKEY), true);
	}

	public void actionPerformed(ActionEvent e) {
                new Visitor() {
                        public void visit(Node n) {
				OpenBrowser.displayUrl("http://www.openstreetmap.org/browse/node/" + n.id + "/history");
			}

                        public void visit(Way w) {
                                OpenBrowser.displayUrl("http://www.openstreetmap.org/browse/way/" + w.id + "/history");
                        }

                        public void visit(Relation e) {
                              OpenBrowser.displayUrl("http://www.openstreetmap.org/browse/relation/" + e.id + "/history");
                        }

                        public void visitAll() {
                                for (OsmPrimitive osm : Main.ds.getSelected())
                                        osm.visit(this);
                        }
                }.visitAll();

	}

}
