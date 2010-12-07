// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.swing.JLabel;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodeData;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Add primitives to a data layer.
 *
 */
public class AddPrimitivesCommand extends Command {

    private List<PrimitiveData> data = new ArrayList<PrimitiveData>();

    // only filled on undo
    private List<OsmPrimitive> createdPrimitives = null;

    public AddPrimitivesCommand(List<PrimitiveData> data) {
        this.data.addAll(data);
    }
    
    public AddPrimitivesCommand(List<PrimitiveData> data, OsmDataLayer layer) {
        super(layer);
        this.data.addAll(data);
    }

    @SuppressWarnings("null")
    @Override public boolean executeCommand() {
        List<OsmPrimitive> newPrimitives;
        if (createdPrimitives == null) { // first time execution
            newPrimitives = new ArrayList<OsmPrimitive>(data.size());

            for (PrimitiveData pd : data) {
                OsmPrimitive primitive = getLayer().data.getPrimitiveById(pd);
                boolean created = primitive == null;
                if (created) {
                    primitive = pd.getType().newInstance(pd.getUniqueId(), true);
                }
                if (pd instanceof NodeData) { // Load nodes immediately because they can't be added to dataset without coordinates
                    primitive.load(pd);
                }
                if (created) {
                    getLayer().data.addPrimitive(primitive);
                }
                newPrimitives.add(primitive);
            }

            //Then load ways and relations
            for (int i=0; i<newPrimitives.size(); i++) {
                if (!(newPrimitives.get(i) instanceof Node)) {
                    newPrimitives.get(i).load(data.get(i));
                }
            }
        } else { // redo
            // When redoing this command, we have to add the same objects, otherwise
            // a subsequent command (e.g. MoveCommand) cannot be redone.
            for (OsmPrimitive osm : createdPrimitives) {
                getLayer().data.addPrimitive(osm);
            }
            newPrimitives = createdPrimitives;
        }

        getLayer().data.setSelected(newPrimitives);
        return true;
    }

    @Override public void undoCommand() {
        DataSet ds = getLayer().data;
        
        if (createdPrimitives == null) {
            createdPrimitives = new ArrayList<OsmPrimitive>(data.size());
            
            for (PrimitiveData p : data) {
                createdPrimitives.add(ds.getPrimitiveById(p));
            }
            createdPrimitives = PurgeCommand.topoSort(createdPrimitives);
            
            for (PrimitiveData p : data) {
                ds.removePrimitive(p);
            }
            data = null;
            
        } else {
            for (OsmPrimitive osm : createdPrimitives) {
                ds.removePrimitive(osm);
            }
        }
    }

    @Override public JLabel getDescription() {
        int size = data != null ? data.size() : createdPrimitives.size();
        return new JLabel(trn("Added {0} object", "Added {0} objects", size, size), null,
                JLabel.HORIZONTAL
        );
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added) {
        // Does nothing because we don't want to create OsmPrimitives.
    }

    @Override
    public Collection<? extends OsmPrimitive> getParticipatingPrimitives() {
        if (createdPrimitives != null)
            return createdPrimitives;
        
        Collection<OsmPrimitive> prims = new HashSet<OsmPrimitive>();
        for (PrimitiveData d : data) {
            OsmPrimitive osm = getLayer().data.getPrimitiveById(d);
            if (osm == null)
                throw new RuntimeException();
            prims.add(osm);
        }
        return prims;
    }
}
