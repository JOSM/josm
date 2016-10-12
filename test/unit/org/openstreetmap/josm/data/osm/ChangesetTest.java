// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.data.osm.Changeset.MAX_CHANGESET_TAG_LENGTH;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link Changeset}.
 */
public class ChangesetTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of method {@link Changeset#setKeys}.
     */
    @Test
    public void testSetKeys() {
        final Changeset cs = new Changeset();
        // Cannot add null map => IllegalArgumentException
        try {
            cs.setKeys(null);
            Assert.fail("Should have thrown an IllegalArgumentException as we gave a null argument.");
        } catch (IllegalArgumentException e) {
            Main.trace(e);
            // Was expected
        }

        // Add a map with no values
        // => the key list is empty
        Map<String, String> keys = new HashMap<>();

        // Add a map with valid values : null and short texts
        // => all the items are in the keys
        keys.put("empty", null);
        keys.put("test", "test");
        cs.setKeys(keys);
        Assert.assertEquals("Both valid keys should have been put in the ChangeSet.", 2, cs.getKeys().size());

        // Add a map with too long values => IllegalArgumentException
        keys = new HashMap<>();
        StringBuilder b = new StringBuilder(MAX_CHANGESET_TAG_LENGTH + 1);
        for (int i = 0; i < MAX_CHANGESET_TAG_LENGTH + 1; i++) {
           b.append("x");
        }
        keys.put("test", b.toString());
        try {
            cs.setKeys(keys);
            Assert.fail("Should have thrown an IllegalArgumentException as we gave a too long value.");
        } catch (IllegalArgumentException e) {
            Main.trace(e);
            // Was expected
        }
    }
}
