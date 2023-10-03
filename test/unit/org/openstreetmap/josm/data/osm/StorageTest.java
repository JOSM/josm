// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests for class {@link Storage}.
 */
class StorageTest {
    /**
     * Unit test of methods {@link Storage#equals} and {@link Storage#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(Storage.class).usingGetClass()
            .suppress(Warning.NONFINAL_FIELDS)
            .withIgnoredFields("arrayCopyNecessary", "hash", "mask", "modCount", "safeIterator", "size")
            .withPrefabValues(Hash.class, Storage.<Integer>defaultHash(), Storage.<Boolean>defaultHash())
            .verify();
    }
}
