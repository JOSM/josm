// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveData;

public class AddPrimitivesCommand extends Command {

    private final List<PrimitiveData> data = new ArrayList<PrimitiveData>();

    public AddPrimitivesCommand(List<PrimitiveData> data) {
        this.data.addAll(data);
    }

    @Override public boolean executeCommand() {

        List<OsmPrimitive> createdPrimitives = new ArrayList<OsmPrimitive>(data.size());

        for (PrimitiveData pd:data) {
            createdPrimitives.add(getLayer().data.getPrimitiveById(pd, true));
        }

        // Load nodes first to prevent ways with null coordinates
        for (int i=0; i<createdPrimitives.size(); i++) {
            if (createdPrimitives.get(i) instanceof Node) {
                createdPrimitives.get(i).load(data.get(i));
            }
        }

        // Now load ways and relations
        for (int i=0; i<createdPrimitives.size(); i++) {
            if (!(createdPrimitives.get(i) instanceof Node)) {
                createdPrimitives.get(i).load(data.get(i));
            }
        }

        getLayer().data.setSelected(createdPrimitives);
        return true;
    }

    @Override public void undoCommand() {
        for (PrimitiveData p:data) {
            getLayer().data.removePrimitive(p);
        }
    }

    @Override
    public MutableTreeNode description() {
        return new DefaultMutableTreeNode(
                new JLabel(trn("Added {0} object", "Added {0} objects", data.size(), data.size()), null,
                        JLabel.HORIZONTAL
                )
        );
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added) {
        // Does nothing because we don't want to create OsmPrimitives.
    }

}
