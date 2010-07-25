// License: GPL. Copyright 2007 by Immanuel Scholz and others
// Author: David Earl
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddPrimitivesCommand;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.NodeData;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.PrimitiveDeepCopy;
import org.openstreetmap.josm.data.osm.RelationData;
import org.openstreetmap.josm.data.osm.RelationMemberData;
import org.openstreetmap.josm.data.osm.WayData;
import org.openstreetmap.josm.data.osm.PrimitiveDeepCopy.PasteBufferChangedListener;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.Shortcut;

public final class PasteAction extends JosmAction implements PasteBufferChangedListener {

    public PasteAction() {
        super(tr("Paste"), "paste", tr("Paste contents of paste buffer."),
                Shortcut.registerShortcut("system:paste", tr("Edit: {0}", tr("Paste")), KeyEvent.VK_V, Shortcut.GROUP_MENU), true);
        putValue("help", ht("/Action/Paste"));
        Main.pasteBuffer.addPasteBufferChangedListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        pasteData(Main.pasteBuffer, Main.pasteSource, e);
    }

    public  void pasteData(PrimitiveDeepCopy pasteBuffer, Layer source, ActionEvent e) {
        /* Find the middle of the pasteBuffer area */
        double maxEast = -1E100, minEast = 1E100, maxNorth = -1E100, minNorth = 1E100;
        boolean incomplete = false;
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
            if (data.isIncomplete()) {
                incomplete = true;
            }
        }

        // Allow to cancel paste if there are incomplete primitives
        if (incomplete) {
            if (!confirmDeleteIncomplete()) return;
        }

        EastNorth mPosition;
        if((e.getModifiers() & ActionEvent.CTRL_MASK) ==0){
            /* adjust the coordinates to the middle of the visible map area */
            mPosition = Main.map.mapView.getCenter();
        } else {
            if (Main.map.mapView.lastMEvent != null) {
                mPosition = Main.map.mapView.getEastNorth(Main.map.mapView.lastMEvent.getX(), Main.map.mapView.lastMEvent.getY());
            } else {
                mPosition = Main.map.mapView.getCenter();
            }
        }

        double offsetEast  = mPosition.east() - (maxEast + minEast)/2.0;
        double offsetNorth = mPosition.north() - (maxNorth + minNorth)/2.0;

        // Make a copy of pasteBuffer and map from old id to copied data id
        List<PrimitiveData> bufferCopy = new ArrayList<PrimitiveData>();
        Map<Long, Long> newNodeIds = new HashMap<Long, Long>();
        Map<Long, Long> newWayIds = new HashMap<Long, Long>();
        Map<Long, Long> newRelationIds = new HashMap<Long, Long>();
        for (PrimitiveData data: pasteBuffer.getAll()) {
            if (data.isIncomplete()) {
                continue;
            }
            PrimitiveData copy = data.makeCopy();
            copy.clearOsmId();
            if (data instanceof NodeData) {
                newNodeIds.put(data.getUniqueId(), copy.getUniqueId());
            } else if (data instanceof WayData) {
                newWayIds.put(data.getUniqueId(), copy.getUniqueId());
            } else if (data instanceof RelationData) {
                newRelationIds.put(data.getUniqueId(), copy.getUniqueId());
            }
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
                List<Long> newNodes = new ArrayList<Long>();
                for (Long oldNodeId: ((WayData)data).getNodes()) {
                    Long newNodeId = newNodeIds.get(oldNodeId);
                    if (newNodeId != null) {
                        newNodes.add(newNodeId);
                    }
                }
                ((WayData)data).setNodes(newNodes);
            } else if (data instanceof RelationData) {
                List<RelationMemberData> newMembers = new ArrayList<RelationMemberData>();
                for (RelationMemberData member: ((RelationData)data).getMembers()) {
                    OsmPrimitiveType memberType = member.getMemberType();
                    Long newId = null;
                    switch (memberType) {
                    case NODE:
                        newId = newNodeIds.get(member.getMemberId());
                        break;
                    case WAY:
                        newId = newWayIds.get(member.getMemberId());
                        break;
                    case RELATION:
                        newId = newRelationIds.get(member.getMemberId());
                        break;
                    }
                    if (newId != null) {
                        newMembers.add(new RelationMemberData(member.getRole(), memberType, newId));
                    }
                }
                ((RelationData)data).setMembers(newMembers);
            }
        }

        /* Now execute the commands to add the duplicated contents of the paste buffer to the map */

        Main.main.undoRedo.add(new AddPrimitivesCommand(bufferCopy));
        Main.map.mapView.repaint();
    }

    protected boolean confirmDeleteIncomplete() {
        ExtendedDialog ed = new ExtendedDialog(Main.parent,
                tr("Delete incomplete members?"),
                new String[] {tr("Paste without incomplete members"), tr("Cancel")});
        ed.setButtonIcons(new String[] {"dialogs/relation/deletemembers.png", "cancel.png"});
        ed.setContent(tr("The copied data contains incomplete primitives.  "
                + "When pasting the incomplete primitives are removed.  "
                + "Do you want to paste the data without the incomplete primitives?"));
        ed.showDialog();
        return ed.getValue() == 1;
    }

    @Override
    protected void updateEnabledState() {
        if (getCurrentDataSet() == null || Main.pasteBuffer == null) {
            setEnabled(false);
            return;
        }
        setEnabled(!Main.pasteBuffer.isEmpty());
    }

    @Override
    public void pasteBufferChanged(PrimitiveDeepCopy pasteBuffer) {
        updateEnabledState();
    }
}
