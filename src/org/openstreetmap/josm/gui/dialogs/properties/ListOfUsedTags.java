// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;

public class ListOfUsedTags implements DataSetListener {

    final TreeMap<String, TreeSet<String>> allData = new TreeMap<String, TreeSet<String>>();
    private boolean dirty;

    public Collection<String> getUsedKeys() {
        if (dirty) {
            rebuild();
        }
        return allData.keySet();
    }

    /**
     *
     * @param key
     * @return List of used values or empty list if key is not used
     */
    public Collection<String> getUsedValues(String key) {
        if (dirty) {
            rebuild();
        }
        Collection<String> values = allData.get(key);
        if (values == null)
            return Collections.emptyList();
        else
            return values;
    }

    public void rebuildNecessary() {
        dirty = true;
    }

    private void rebuild() {
        dirty = false;
        allData.clear();
        DataSet currentDataset = Main.main.getCurrentDataSet();
        if (currentDataset != null) {
            addPrimitives(currentDataset.allNonDeletedPrimitives());
        }
    }

    private void addPrimitives(Collection<? extends OsmPrimitive> primitives) {
        for (OsmPrimitive osm : primitives) {
            addPrimitive(osm);
        }
    }

    private void addPrimitive(OsmPrimitive primitive) {
        for (String key: primitive.keySet()) {
            addKey(key, primitive.get(key));
        }
    }

    private void addKey(String key, String value) {
        TreeSet<String> values = allData.get(key);
        if (values == null) {
            values = new TreeSet<String>();
            allData.put(key, values);
        }
        values.add(value);
    }

    public void dataChanged(DataChangedEvent event) {
        rebuild();
    }

    public void nodeMoved(NodeMovedEvent event) {/* ignored */}

    public void otherDatasetChange(AbstractDatasetChangedEvent event) {/* ignored */}

    public void primtivesAdded(PrimitivesAddedEvent event) {
        addPrimitives(event.getPrimitives());
    }

    public void primtivesRemoved(PrimitivesRemovedEvent event) {
        dirty = true;
    }

    public void relationMembersChanged(RelationMembersChangedEvent event) {/* ignored */}

    public void tagsChanged(TagsChangedEvent event) {
        Map<String, String> newKeys = event.getPrimitive().getKeys();
        Map<String, String> oldKeys = event.getOriginalKeys();

        if (!newKeys.keySet().containsAll(oldKeys.keySet())) {
            // Some keys removed, might be the last instance of key, rebuild necessary
            dirty = true;
        } else {
            for (Entry<String, String> oldEntry: oldKeys.entrySet()) {
                if (!oldEntry.getValue().equals(newKeys.get(oldEntry.getKey()))) {
                    // Value changed, might be last instance of value, rebuild necessary
                    dirty = true;
                    return;
                }
            }
            addPrimitive(event.getPrimitive());
        }
    }

    public void wayNodesChanged(WayNodesChangedEvent event) {/* ignored */}
}
