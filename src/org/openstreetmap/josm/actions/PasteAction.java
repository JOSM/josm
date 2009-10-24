// License: GPL. Copyright 2007 by Immanuel Scholz and others
// Author: David Earl
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddPrimitivesCommand;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.NodeData;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.PrimitiveDeepCopy;
import org.openstreetmap.josm.data.osm.RelationData;
import org.openstreetmap.josm.data.osm.RelationMemberData;
import org.openstreetmap.josm.data.osm.WayData;
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

    public  void pasteData(PrimitiveDeepCopy pasteBuffer, Layer source, ActionEvent e) {
        /* Find the middle of the pasteBuffer area */
        double maxEast = -1E100, minEast = 1E100, maxNorth = -1E100, minNorth = 1E100;
        for (PrimitiveData data : pasteBuffer.getAll()) {
            if (data instanceof NodeData) {
                NodeData n = (NodeData)data;
                double east = n.getEastNorth().east();
                double north = n.getEastNorth().north();
                if (east > maxEast) { maxEast = east; }
                if (east < minEast) { minEast = east; }
                if (north > maxNorth) { maxNorth = north; }
                if (north < minNorth) { minNorth = north; }
            }
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



        // Make a copy of pasteBuffer and map from old id to copied data id
        List<PrimitiveData> bufferCopy = new ArrayList<PrimitiveData>();
        Map<Long, Long> newIds = new HashMap<Long, Long>();
        for (PrimitiveData data:pasteBuffer.getAll()) {
            PrimitiveData copy = data.makeCopy();
            copy.clearOsmId();
            newIds.put(data.getId(), copy.getId());
            bufferCopy.add(copy);
        }

        // Update references in copied buffer
        for (PrimitiveData data:bufferCopy) {
            if (data instanceof NodeData) {
                NodeData nodeData = (NodeData)data;
                if (Main.map.mapView.getEditLayer() == source) {
                    nodeData.setEastNorth(nodeData.getEastNorth().add(offsetEast, offsetNorth));
                }
            } else if (data instanceof WayData) {
                ListIterator<Long> it = ((WayData)data).getNodes().listIterator();
                while (it.hasNext()) {
                    it.set(newIds.get(it.next()));
                }
            } else if (data instanceof RelationData) {
                ListIterator<RelationMemberData> it = ((RelationData)data).getMembers().listIterator();
                while (it.hasNext()) {
                    RelationMemberData member = it.next();
                    it.set(new RelationMemberData(member.getRole(), member.getMemberType(), newIds.get(member.getMemberId())));
                }
            }
        }

        /* Now execute the commands to add the duplicated contents of the paste buffer to the map */

        Main.main.undoRedo.add(new AddPrimitivesCommand(bufferCopy));
        //getCurrentDataSet().setSelected(osms);
        Main.map.mapView.repaint();
    }

    @Override
    protected void updateEnabledState() {
        if (getCurrentDataSet() == null || Main.pasteBuffer == null) {
            setEnabled(false);
            return;
        }
        setEnabled(!Main.pasteBuffer.isEmpty());
    }
}
