// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer.data;

import java.awt.datatransfer.DataFlavor;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.TagCollection;

/**
 * This is a variant of {@link TagTransferData} that holds tags that were copied from a collection of primitives.
 * @author Michael Zangl
 * @since 10737
 */
public class PrimitiveTagTransferData implements Serializable {

    private static final long serialVersionUID = 1;

    /**
     * This is a data flavor added
     */
    public static final DataFlavor FLAVOR = new DataFlavor(TagTransferData.class, "OSM Primitive Tags");

    private final EnumMap<OsmPrimitiveType, TagCollection> tags = new EnumMap<>(OsmPrimitiveType.class);
    private final EnumMap<OsmPrimitiveType, Integer> counts = new EnumMap<>(OsmPrimitiveType.class);

    /**
     * Create a new {@link PrimitiveTagTransferData}
     * @param source The primitives to initialize this object with.
     */
    public PrimitiveTagTransferData(Collection<? extends PrimitiveData> source) {
        for (OsmPrimitiveType type : OsmPrimitiveType.dataValues()) {
            tags.put(type, new TagCollection());
        }

        for (PrimitiveData primitive : source) {
            tags.get(primitive.getType()).add(TagCollection.from(primitive));
            counts.merge(primitive.getType(), 1, Integer::sum);
        }
    }

    /**
     * Create a new {@link PrimitiveTagTransferData}
     * @param data The primitives to initialize this object with.
     */
    public PrimitiveTagTransferData(PrimitiveTransferData data) {
        this(data.getDirectlyAdded());
    }

    /**
     * Determines if the source for tag pasting is heterogeneous, i.e. if it doesn't consist of
     * {@link OsmPrimitive}s of exactly one type
     * @return true if the source for tag pasting is heterogeneous
     */
    public boolean isHeterogeneousSource() {
        return counts.size() > 1;
    }

    /**
     * Gets the tags used for this primitive type.
     * @param type The primitive type
     * @return The tags as collection. Empty if no such type was copied
     */
    public TagCollection getForPrimitives(OsmPrimitiveType type) {
        return tags.get(type);
    }

    /**
     * Gets the number of source primitives for the given type.
     * @param type The type
     * @return The number of source primitives of that type
     */
    public int getSourcePrimitiveCount(OsmPrimitiveType type) {
        return counts.getOrDefault(type, 0);
    }

    /**
     * Gets the statistics of the source primitive counts. May contain no entries for unused types.
     * @return The statistics as map
     */
    public Map<OsmPrimitiveType, Integer> getStatistics() {
        return Collections.unmodifiableMap(counts);
    }
}
