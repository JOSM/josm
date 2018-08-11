// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer.importers;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.TransferHandler.TransferSupport;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddPrimitivesCommand;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.NodeData;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.RelationData;
import org.openstreetmap.josm.data.osm.RelationMemberData;
import org.openstreetmap.josm.data.osm.WayData;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.datatransfer.data.PrimitiveTransferData;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.bugreport.BugReport;

/**
 * This transfer support allows us to transfer primitives. This is the default paste action when primitives were copied.
 * @author Michael Zangl
 * @since 10604
 */
public final class PrimitiveDataPaster extends AbstractOsmDataPaster {
    /**
     * Create a new {@link PrimitiveDataPaster}
     */
    public PrimitiveDataPaster() {
        super(PrimitiveTransferData.DATA_FLAVOR);
    }

    @Override
    public boolean importData(TransferSupport support, final OsmDataLayer layer, EastNorth pasteAt)
            throws UnsupportedFlavorException, IOException {
        PrimitiveTransferData pasteBuffer = (PrimitiveTransferData) support.getTransferable().getTransferData(df);
        // Allow to cancel paste if there are incomplete primitives
        if (pasteBuffer.hasIncompleteData() && !confirmDeleteIncomplete()) {
            return false;
        }

        EastNorth center = pasteBuffer.getCenter();
        EastNorth offset = center == null || pasteAt == null ? new EastNorth(0, 0) : pasteAt.subtract(center);

        AddPrimitivesCommand command = createNewPrimitives(pasteBuffer, offset, layer);

        /* Now execute the commands to add the duplicated contents of the paste buffer to the map */
        MainApplication.undoRedo.add(command);
        return true;
    }

    private static AddPrimitivesCommand createNewPrimitives(PrimitiveTransferData pasteBuffer, EastNorth offset, OsmDataLayer layer) {
        // Make a copy of pasteBuffer and map from old id to copied data id
        List<PrimitiveData> bufferCopy = new ArrayList<>();
        List<PrimitiveData> toSelect = new ArrayList<>();
        EnumMap<OsmPrimitiveType, Map<Long, Long>> newIds = generateNewPrimitives(pasteBuffer, bufferCopy, toSelect);

        // Update references in copied buffer
        for (PrimitiveData data : bufferCopy) {
            try {
                if (data instanceof NodeData) {
                    NodeData nodeData = (NodeData) data;
                    nodeData.setEastNorth(nodeData.getEastNorth(ProjectionRegistry.getProjection()).add(offset));
                } else if (data instanceof WayData) {
                    updateNodes(newIds.get(OsmPrimitiveType.NODE), data);
                } else if (data instanceof RelationData) {
                    updateMembers(newIds, data);
                }
            } catch (JosmRuntimeException | IllegalArgumentException | IllegalStateException e) {
                throw BugReport.intercept(e).put("data", data);
            }
        }
        return new AddPrimitivesCommand(bufferCopy, toSelect, layer.getDataSet());
    }

    private static EnumMap<OsmPrimitiveType, Map<Long, Long>> generateNewPrimitives(PrimitiveTransferData pasteBuffer,
            List<PrimitiveData> bufferCopy, List<PrimitiveData> toSelect) {
        EnumMap<OsmPrimitiveType, Map<Long, Long>> newIds = new EnumMap<>(OsmPrimitiveType.class);
        newIds.put(OsmPrimitiveType.NODE, new HashMap<Long, Long>());
        newIds.put(OsmPrimitiveType.WAY, new HashMap<Long, Long>());
        newIds.put(OsmPrimitiveType.RELATION, new HashMap<Long, Long>());

        for (PrimitiveData data : pasteBuffer.getAll()) {
            if (data.isIncomplete() || !data.isVisible()) {
                continue;
            }
            PrimitiveData copy = data.makeCopy();
            // don't know why this is reset, but we need it to not crash on copying incomplete nodes.
            boolean wasIncomplete = copy.isIncomplete();
            copy.clearOsmMetadata();
            copy.setIncomplete(wasIncomplete);
            newIds.get(data.getType()).put(data.getUniqueId(), copy.getUniqueId());

            bufferCopy.add(copy);
            if (pasteBuffer.getDirectlyAdded().contains(data)) {
                toSelect.add(copy);
            }
        }
        return newIds;
    }

    private static void updateMembers(EnumMap<OsmPrimitiveType, Map<Long, Long>> newIds, PrimitiveData data) {
        List<RelationMemberData> newMembers = new ArrayList<>();
        for (RelationMemberData member : ((RelationData) data).getMembers()) {
            OsmPrimitiveType memberType = member.getMemberType();
            Long newId = newIds.get(memberType).get(member.getMemberId());
            if (newId != null) {
                newMembers.add(new RelationMemberData(member.getRole(), memberType, newId));
            }
        }
        ((RelationData) data).setMembers(newMembers);
    }

    private static void updateNodes(Map<Long, Long> newNodeIds, PrimitiveData data) {
        List<Long> newNodes = new ArrayList<>();
        for (Long oldNodeId : ((WayData) data).getNodeIds()) {
            Long newNodeId = newNodeIds.get(oldNodeId);
            if (newNodeId != null) {
                newNodes.add(newNodeId);
            }
        }
        ((WayData) data).setNodeIds(newNodes);
    }

    private static boolean confirmDeleteIncomplete() {
        ExtendedDialog ed = new ExtendedDialog(Main.parent, tr("Delete incomplete members?"),
                tr("Paste without incomplete members"), tr("Cancel"));
        ed.setButtonIcons("dialogs/relation/deletemembers", "cancel");
        ed.setContent(tr(
                "The copied data contains incomplete objects.  " + "When pasting the incomplete objects are removed.  "
                        + "Do you want to paste the data without the incomplete objects?"));
        ed.showDialog();
        return ed.getValue() == 1;
    }
}
