// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditor.AddAbortException;
import org.openstreetmap.josm.gui.dialogs.relation.IRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Paste members.
 * @since 9496
 */
public class PasteMembersAction extends AddFromSelectionAction {

    /**
     * Constructs a new {@code PasteMembersAction}.
     * @param memberTableModel member table model
     * @param layer OSM data layer
     * @param editor relation editor
     */
    public PasteMembersAction(MemberTableModel memberTableModel, OsmDataLayer layer, IRelationEditor editor) {
        super(null, memberTableModel, null, null, null, layer, editor);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            List<PrimitiveData> primitives = Main.pasteBuffer.getDirectlyAdded();
            DataSet ds = layer.data;
            List<OsmPrimitive> toAdd = new ArrayList<>();
            boolean hasNewInOtherLayer = false;

            for (PrimitiveData primitive: primitives) {
                OsmPrimitive primitiveInDs = ds.getPrimitiveById(primitive);
                if (primitiveInDs != null) {
                    toAdd.add(primitiveInDs);
                } else if (!primitive.isNew()) {
                    OsmPrimitive p = primitive.getType().newInstance(primitive.getUniqueId(), true);
                    ds.addPrimitive(p);
                    toAdd.add(p);
                } else {
                    hasNewInOtherLayer = true;
                    break;
                }
            }

            if (hasNewInOtherLayer) {
                JOptionPane.showMessageDialog(Main.parent,
                        tr("Members from paste buffer cannot be added because they are not included in current layer"));
                return;
            }

            toAdd = filterConfirmedPrimitives(toAdd);
            int index = memberTableModel.getSelectionModel().getMaxSelectionIndex();
            if (index == -1) {
                index = memberTableModel.getRowCount() - 1;
            }
            memberTableModel.addMembersAfterIdx(toAdd, index);

        } catch (AddAbortException ex) {
            Main.trace(ex);
        }
    }

    @Override
    protected void updateEnabledState() {
        // Do nothing
    }
}
