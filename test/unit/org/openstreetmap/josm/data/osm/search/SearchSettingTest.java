// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.search;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

/**
 * Test class for {@link SearchSetting}
 */
class SearchSettingTest {
    /**
     * Check that equality and hashCode meet specifications.
     */
    @Test
    void testEqualsHashcode() {
        EqualsVerifier.simple().forClass(SearchSetting.class).verify();
    }

    /**
     * The hashCode return is used for shortcut preferences (see #22515 for details).
     * As such, the hashCode must be stable between JVM restarts.
     * <br>
     * Note: The hashCode of enums <i>are not stable</i> across JVM restarts, as they
     * <i>must</i> use the default hashCode (which is usually {@link System#identityHashCode(Object)}),
     * This tends to be fairly stable in JUnit tests, but is not stable in the real world!
     */
    @Test
    void testStableHashcode() {
        SearchSetting testWayReplace = new SearchSetting();
        testWayReplace.text = "type:way";
        testWayReplace.mode = SearchMode.replace;
        SearchSetting testWayAdd = new SearchSetting(testWayReplace);
        testWayAdd.mode = SearchMode.add;
        SearchSetting testWayNull = new SearchSetting(testWayReplace);
        testWayNull.mode = null;
        SearchSetting testNodeReplace = new SearchSetting(testWayReplace);
        SearchSetting testNodeAdd = new SearchSetting(testWayAdd);
        SearchSetting testNodeNull = new SearchSetting(testWayNull);
        testNodeReplace.text = "type:node";
        testNodeAdd.text = "type:node";
        testNodeNull.text = "type:node";
        assertAll(() -> assertEquals(837851780, testWayReplace.hashCode()),
                () -> assertEquals(822151923, testWayAdd.hashCode()),
                () -> assertEquals(762123058, testWayNull.hashCode()),
                () -> assertEquals(-620395823, testNodeReplace.hashCode()),
                () -> assertEquals(-636095680, testNodeAdd.hashCode()),
                () -> assertEquals(-696124545, testNodeNull.hashCode())
                );
    }
}
