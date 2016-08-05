// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer.data;

import java.awt.datatransfer.DataFlavor;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.data.osm.Tagged;

/**
 * This is a special transfer type that only transfers tag data.
 * <p>
 * It contains all tags contained in the selection that was copied. For conflicting tags, any of the values may be used.
 * @author Michael Zangl
 * @since 10604
 */
public class TagTransferData implements Serializable {

    private static final long serialVersionUID = 1;

    /**
     * This is a data flavor added
     */
    public static final DataFlavor FLAVOR = new DataFlavor(TagTransferData.class, "OSM Tags");

    private final TagMap tags = new TagMap();

    /**
     * Creates a new {@link TagTransferData} object for the given objects.
     * @param tagged The tags to transfer.
     */
    public TagTransferData(Collection<? extends Tagged> tagged) {
        for (Tagged t : tagged) {
            tags.putAll(t.getKeys());
        }
    }

    /**
     * Create a new {@link TagTransferData} object with the given tags.
     * @param tags The tags.
     * @since 10637
     */
    public TagTransferData(Map<String, String> tags) {
        this.tags.putAll(tags);
    }

    /**
     * Gets all tags contained in this data.
     * @return The tags.
     */
    public Map<String, String> getTags() {
        return Collections.unmodifiableMap(tags);
    }
}
