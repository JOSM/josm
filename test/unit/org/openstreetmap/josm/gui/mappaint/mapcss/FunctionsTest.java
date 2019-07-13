// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.junit.Assert.assertEquals;
import static org.openstreetmap.josm.data.osm.OsmPrimitiveType.NODE;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link Functions}.
 */
public class FunctionsTest {

    /**
     * Setup rule
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    private static class EnvBuilder {
        private final OsmPrimitive osm;

        EnvBuilder(OsmPrimitiveType type) {
            switch (type) {
                case NODE : osm = TestUtils.newNode(""); break;
                case WAY : osm = TestUtils.newWay(""); break;
                case RELATION : osm = TestUtils.newRelation(""); break;
                default: throw new IllegalArgumentException();
            }
        }

        EnvBuilder setUser(User user) {
            osm.setUser(user);
            return this;
        }

        Environment build() {
            return new Environment(osm);
        }
    }

    /**
     * Unit test of {@link Functions#osm_user_name}.
     */
    @Test
    public void testOsmUserName() {
        assertEquals("<anonymous>", Functions.osm_user_name(new EnvBuilder(NODE).setUser(User.getAnonymous()).build()));
    }

    /**
     * Unit test of {@link Functions#osm_user_id}.
     */
    @Test
    public void testOsmUserId() {
        assertEquals(-1, Functions.osm_user_id(new EnvBuilder(NODE).setUser(User.getAnonymous()).build()));
    }

    /**
     * Unit test of {@link Functions#osm_version}.
     */
    @Test
    public void testOsmVersion() {
        assertEquals(0, Functions.osm_version(new EnvBuilder(NODE).build()));
    }

    /**
     * Unit test of {@link Functions#osm_changeset_id}.
     */
    @Test
    public void testOsmChangesetId() {
        assertEquals(0, Functions.osm_changeset_id(new EnvBuilder(NODE).build()));
    }

    /**
     * Unit test of {@link Functions#osm_timestamp}.
     */
    @Test
    public void testOsmTimestamp() {
        assertEquals(0, Functions.osm_timestamp(new EnvBuilder(NODE).build()));
    }
}
