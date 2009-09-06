// License: GPL. Copyright 2007 by Immanuel Scholz and others
// Author: David Earl
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.Shortcut;

public final class PasteAction extends JosmAction {

    public PasteAction() {
        super(tr("Paste"), "paste", tr("Paste contents of paste buffer."),
                Shortcut.registerShortcut("system:paste", tr("Edit: {0}", tr("Paste")), KeyEvent.VK_V, Shortcut.GROUP_MENU), true);
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        pasteData(Main.pasteBuffer, Main.pasteSource, e);
    }

    public  void pasteData(DataSet pasteBuffer, Layer source, ActionEvent e) {
        /* Find the middle of the pasteBuffer area */
        double maxEast = -1E100, minEast = 1E100, maxNorth = -1E100, minNorth = 1E100;
        for (Node n : pasteBuffer.nodes) {
            double east = n.getEastNorth().east();
            double north = n.getEastNorth().north();
            if (east > maxEast) { maxEast = east; }
            if (east < minEast) { minEast = east; }
            if (north > maxNorth) { maxNorth = north; }
            if (north < minNorth) { minNorth = north; }
        }

        EastNorth mPosition;
        if((e.getModifiers() & ActionEvent.CTRL_MASK) ==0){
            /* adjust the coordinates to the middle of the visible map area */
            mPosition = Main.map.mapView.getCenter();
        } else {
            mPosition = Main.map.mapView.getEastNorth(Main.map.mapView.lastMEvent.getX(), Main.map.mapView.lastMEvent.getY());
        }

        double offsetEast  = mPosition.east() - (maxEast + minEast)/2.0;
        double offsetNorth = mPosition.north() - (maxNorth + minNorth)/2.0;

        HashMap<OsmPrimitive,OsmPrimitive> map = new HashMap<OsmPrimitive,OsmPrimitive>();
        /* temporarily maps old nodes to new so we can do a true deep copy */

        /* do the deep copy of the paste buffer contents, leaving the pasteBuffer unchanged */
        for (Node n : pasteBuffer.nodes) {
            Node nnew = new Node(n);
            nnew.clearOsmId();
            if (Main.map.mapView.getEditLayer() == source) {
                nnew.setEastNorth(nnew.getEastNorth().add(offsetEast, offsetNorth));
            }
            map.put(n, nnew);
        }
        for (Way w : pasteBuffer.ways) {
            Way wnew = new Way();
            wnew.cloneFrom(w);
            wnew.clearOsmId();
            /* make sure we reference the new nodes corresponding to the old ones */
            List<Node> nodes = new ArrayList<Node>();
            for (Node n : w.getNodes()) {
                nodes.add((Node)map.get(n));
            }
            wnew.setNodes(nodes);
            map.put(w, wnew);
        }
        for (Relation r : pasteBuffer.relations) {
            Relation rnew = new Relation(r);
            r.clearOsmId();
            List<RelationMember> members = new ArrayList<RelationMember>();
            for (RelationMember m : r.getMembers()) {
                OsmPrimitive mo = map.get(m.getMember());
                if(mo != null) /* FIXME - This only prevents illegal data, but kills the relation */
                {
                    RelationMember mnew = new RelationMember(m.getRole(), map.get(m.getMember()));
                    members.add(mnew);
                }
            }
            rnew.setMembers(members);
            map.put(r, rnew);
        }

        /* Now execute the commands to add the dupicated contents of the paste buffer to the map */
        Collection<OsmPrimitive> osms = map.values();
        Collection<Command> clist = new LinkedList<Command>();
        for (OsmPrimitive osm : osms) {
            clist.add(new AddCommand(osm));
        }

        Main.main.undoRedo.add(new SequenceCommand(tr("Paste"), clist));
        getCurrentDataSet().setSelected(osms);
        Main.map.mapView.repaint();
    }

    @Override
    protected void updateEnabledState() {
        if (getCurrentDataSet() == null || Main.pasteBuffer == null) {
            setEnabled(false);
            return;
        }
        setEnabled(
                !Main.pasteBuffer.nodes.isEmpty()
                || !Main.pasteBuffer.ways.isEmpty()
                || !Main.pasteBuffer.relations.isEmpty()
        );
    }
}
