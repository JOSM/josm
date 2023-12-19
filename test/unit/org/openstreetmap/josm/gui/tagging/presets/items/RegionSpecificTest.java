// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.Territories;
import org.xml.sax.SAXException;

/**
 * Test class for {@link RegionSpecific}
 */
@Territories
interface RegionSpecificTest {
    /**
     * Get the test instance
     * @return The instance to test
     */
    RegionSpecific getInstance();

    @Test
    default void testSetRegions() throws SAXException {
        final RegionSpecific regionSpecific = getInstance();
        if ("java.lang.Record".equals(regionSpecific.getClass().getSuperclass().getCanonicalName())) {
            assertThrows(UnsupportedOperationException.class, () -> regionSpecific.setRegions("US"));
        } else {
            assertFalse(regionSpecific.regions() != null && regionSpecific.regions().contains("US"),
                    "Using US as the test region for setting regions");
            regionSpecific.setRegions("US");
            assertAll(() -> assertEquals(1, regionSpecific.regions().size()),
                    () -> assertEquals("US", regionSpecific.regions().iterator().next()));
        }
    }

    @Test
    default void testSetExcludeRegions() {
        final RegionSpecific regionSpecific = getInstance();
        if ("java.lang.Record".equals(regionSpecific.getClass().getSuperclass().getCanonicalName())) {
            assertThrows(UnsupportedOperationException.class, () -> regionSpecific.setExclude_regions(true));
        } else {
            final boolean oldExclude = regionSpecific.exclude_regions();
            regionSpecific.setExclude_regions(!oldExclude);
            assertNotEquals(oldExclude, regionSpecific.exclude_regions());
        }
    }
}
