// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.command.PseudoCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testutils.annotations.Main;

/**
 * Unit tests for class {@link FixDataHook}.
 */
@Main
class FixDataHookTest {
    /**
     * Test of {@link FixDataHook#checkUpload} method.
     */
    @Test
    void testCheckUpload() {
        // Empty data set
        UndoRedoHandler.getInstance().clean();
        new FixDataHook().checkUpload(new APIDataSet());
        assertTrue(UndoRedoHandler.getInstance().getUndoCommands().isEmpty());

        // Complete data set (except empty node which cannot be tested anymore)
        Way emptyWay = new Way();
        Relation emptyRelation = new Relation();
        Way w1 = new Way();
        w1.put("color", "test");
        Way w2 = new Way();
        w2.put("highway", "ford");
        Way w3 = new Way();
        w3.put("oneway", "false");
        Way w4 = new Way();
        w4.put("oneway", "0");
        Way w5 = new Way();
        w5.put("oneway", "true");
        Way w6 = new Way();
        w6.put("oneway", "1");
        Way w7 = new Way();
        w7.put("highway", "stile");
        Relation r1 = new Relation();
        r1.put("type", "multipolygon");
        r1.put("boundary", "administrative");
        Relation r2 = new Relation();
        r2.put("foo", "space_end ");
        r2.put("bar", " space_begin ");
        r2.put("baz", " space_both ");
        r2.put(" space_begin", "test");
        r2.put("space_end ", "test");
        r2.put(" space_both ", "test");
        APIDataSet ads = new APIDataSet();
        ads.init(new DataSet(emptyWay, emptyRelation, w1, w2, w3, w4, w5, w6, w7, r1, r2));

        assertEquals(0, UndoRedoHandler.getInstance().getUndoCommands().size());
        new FixDataHook().checkUpload(ads);
        assertEquals(1, UndoRedoHandler.getInstance().getUndoCommands().size());

        SequenceCommand seq = (SequenceCommand) UndoRedoHandler.getInstance().getUndoCommands().iterator().next();
        Collection<? extends OsmPrimitive> prims = seq.getParticipatingPrimitives();
        assertNotNull(prims);
        assertEquals(9, prims.size());
        for (OsmPrimitive o : Arrays.asList(w1, w2, w3, w4, w5, w6, w7, r1, r2)) {
            assertTrue(prims.contains(o), o.toString());
        }
        Collection<PseudoCommand> cmds = seq.getChildren();
        assertNotNull(cmds);
        assertEquals(9, cmds.size());

        assertTrue(seq.executeCommand());

        assertFalse(w1.hasKey("color"));
        assertTrue(w1.hasKey("colour"));

        assertFalse(w2.hasKey("highway"));
        assertTrue(w2.hasKey("ford"));

        assertNotEquals("false", w3.get("oneway"));
        assertEquals("no", w3.get("oneway"));

        assertNotEquals("0", w4.get("oneway"));
        assertEquals("no", w4.get("oneway"));

        assertNotEquals("true", w5.get("oneway"));
        assertEquals("yes", w5.get("oneway"));

        assertNotEquals("1", w6.get("oneway"));
        assertEquals("yes", w6.get("oneway"));

        assertFalse(w7.hasKey("highway"));
        assertTrue(w7.hasKey("barrier"));

        assertNotEquals("multipolygon", r1.get("type"));
        assertEquals("boundary", r1.get("type"));

        assertEquals("space_end", r2.get("foo"));
        assertEquals("space_begin", r2.get("bar"));
        assertEquals("space_both", r2.get("baz"));
        assertFalse(r2.hasKey(" space_begin"));
        assertFalse(r2.hasKey("space_end "));
        assertFalse(r2.hasKey(" space_both "));
        assertTrue(r2.hasKey("space_begin"));
        assertTrue(r2.hasKey("space_end"));
        assertTrue(r2.hasKey("space_both"));
    }
}
