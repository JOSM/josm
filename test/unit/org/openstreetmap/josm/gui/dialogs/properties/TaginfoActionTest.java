// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import javax.swing.Action;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.Tag;

class TaginfoActionTest {
    /**
     * Unit test of {@link TaginfoAction#getTaginfoUrlForTag} and {@link TaginfoAction#getTaginfoUrlForRelationType}
     */
    @Test
    void testTaginfoUrls() {
        TaginfoAction action = new TaginfoAction(() -> null, () -> null);
        assertEquals("https://taginfo.openstreetmap.org/keys/railway", action.getTaginfoUrlForTag(new Tag("railway")));
        assertEquals("https://taginfo.openstreetmap.org/tags/railway=tram", action.getTaginfoUrlForTag(new Tag("railway", "tram")));
        assertEquals("https://taginfo.openstreetmap.org/tags/addr%3Acity=Bassum%3ACity",
                action.getTaginfoUrlForTag(new Tag("addr:city", "Bassum:City")));
        assertEquals("https://taginfo.openstreetmap.org/relations/route", action.getTaginfoUrlForRelationType("route"));
    }

    /**
     * Unit test of {@link TaginfoAction#toTagHistoryAction()}
     */
    @Test
    void testCustomInstance() {
        TaginfoAction action = new TaginfoAction(() -> null, () -> null).withTaginfoUrl("example.com", "https://taginfo.example.com////");
        assertEquals("example.com", action.getValue(Action.NAME));
        assertEquals("https://taginfo.example.com/keys/railway", action.getTaginfoUrlForTag(new Tag("railway")));
    }

    /**
     * Unit test of {@link TaginfoAction#toTagHistoryAction()}
     */
    @Test
    void testTagHistoryUrls() {
        TaginfoAction action = new TaginfoAction(() -> null, () -> null).toTagHistoryAction();
        assertEquals("https://taghistory.raifer.tech/#***/railway/", action.getTaginfoUrlForTag(new Tag("railway")));
        assertEquals("https://taghistory.raifer.tech/#***/railway/tram", action.getTaginfoUrlForTag(new Tag("railway", "tram")));
        assertEquals("https://taghistory.raifer.tech/#***/addr:city/Bassum:City",
                action.getTaginfoUrlForTag(new Tag("addr:city", "Bassum:City")));
        assertNull(action.getTaginfoUrlForRelationType("route"));
    }
}
