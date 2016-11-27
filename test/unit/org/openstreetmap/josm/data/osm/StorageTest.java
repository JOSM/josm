// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Unit tests for class {@link Storage}.
 */
public class StorageTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of methods {@link Storage#equals} and {@link Storage#hashCode}.
     */
    @Test
    public void testEqualsContract() {
        EqualsVerifier.forClass(Storage.class).usingGetClass()
            /*.withPrefabValues(Collection.class, new HashSet<>(Arrays.asList(1)), new HashSet<>(Arrays.asList(2)))
            .withPrefabValues(AbstractCollection.class, new HashSet<>(Arrays.asList(1)), new HashSet<>(Arrays.asList(2)))
            .withPrefabValues(Set.class, new HashSet<>(Arrays.asList(1)), new HashSet<>(Arrays.asList(2)))
            .withPrefabValues(AbstractSet.class, new HashSet<>(Arrays.asList(1)), new HashSet<>(Arrays.asList(2)))*/
            .withPrefabValues(Hash.class, Storage.<Integer>defaultHash(), Storage.<Boolean>defaultHash())
            .verify();
    }
}
