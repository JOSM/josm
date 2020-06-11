// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.swing.Action;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class TaginfoActionTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of {@link TaginfoAction#getTaginfoUrlForTag} and {@link TaginfoAction#getTaginfoUrlForRelationType}
     */
    @Test
    public void testTaginfoUrls() {
        TaginfoAction action = new TaginfoAction(() -> null, () -> null);
        assertEquals("https://taginfo.openstreetmap.org/keys/railway", action.getTaginfoUrlForTag(new Tag("railway")));
        assertEquals("https://taginfo.openstreetmap.org/tags/railway=tram", action.getTaginfoUrlForTag(new Tag("railway", "tram")));
        assertEquals("https://taginfo.openstreetmap.org/relations/route", action.getTaginfoUrlForRelationType("route"));
    }

    /**
     * Unit test of {@link TaginfoAction#toTagHistoryAction()}
     */
    @Test
    public void testCustomInstance() {
        TaginfoAction action = new TaginfoAction(() -> null, () -> null).withTaginfoUrl("example.com", "https://taginfo.example.com////");
        assertEquals("example.com", action.getValue(Action.NAME));
        assertEquals("https://taginfo.example.com/keys/railway", action.getTaginfoUrlForTag(new Tag("railway")));
    }

    /**
     * Unit test of {@link TaginfoAction#toTagHistoryAction()}
     */
    @Test
    public void testTagHistoryUrls() {
        TaginfoAction action = new TaginfoAction(() -> null, () -> null).toTagHistoryAction();
        assertEquals("https://taghistory.raifer.tech/#***/railway/", action.getTaginfoUrlForTag(new Tag("railway")));
        assertEquals("https://taghistory.raifer.tech/#***/railway/tram", action.getTaginfoUrlForTag(new Tag("railway", "tram")));
        assertNull(action.getTaginfoUrlForRelationType("route"));
    }
}
