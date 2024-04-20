// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.openstreetmap.josm.tools.Territories;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.SAXException;

/**
 * Indicates that this object may be specific to a region
 * @since 18918
 */
public interface RegionSpecific {
    /**
     * Get the regions for the item
     * @return The regions that the item is valid for
     * @apiNote This is not {@code getRegions} just in case we decide to make the {@link RegionSpecific} record classes.
     */
    Collection<String> regions();

    /**
     * Set the regions for the preset
     * @param regions The region list (comma delimited)
     * @throws SAXException if an unknown ISO 3166-2 is found
     */
    default void setRegions(String regions) throws SAXException {
        Set<String> regionSet = Collections.unmodifiableSet(Arrays.stream(regions.split(","))
                .map(Utils::intern).collect(Collectors.toSet()));
        for (String region : regionSet) {
            if (!Territories.getKnownIso3166Codes().contains(region)) {
                throw new SAXException(tr("Unknown ISO-3166 Code: {0}", region));
            }
        }
        this.realSetRegions(regionSet);
    }

    /**
     * Set the regions for the preset
     * @param regions The region collection
     */
    void realSetRegions(Collection<String> regions);

    /**
     * Get the exclude_regions for the preset
     * @apiNote This is not {@code getExclude_regions} just in case we decide to make {@link RegionSpecific} a record class.
     * @return {@code true} if the meaning of {@link #regions()} should be inverted
     */
    boolean exclude_regions();

    /**
     * Set if the preset should not be used in the given region
     * @param excludeRegions if true the function of regions is inverted
     */
    void setExclude_regions(boolean excludeRegions);
}
