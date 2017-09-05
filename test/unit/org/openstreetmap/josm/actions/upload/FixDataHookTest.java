// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.upload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.command.PseudoCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link FixDataHook}.
 */
public class FixDataHookTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().main();

    /**
     * Test of {@link FixDataHook#checkUpload} method.
     */
    @Test
    public void testCheckUpload() {
        // Empty data set
        MainApplication.undoRedo.commands.clear();
        new FixDataHook().checkUpload(new APIDataSet());
        assertTrue(MainApplication.undoRedo.commands.isEmpty());

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

        MainApplication.undoRedo.commands.clear();
        new FixDataHook().checkUpload(ads);
        assertEquals(1, MainApplication.undoRedo.commands.size());

        SequenceCommand seq = (SequenceCommand) MainApplication.undoRedo.commands.iterator().next();
        Collection<? extends OsmPrimitive> prims = seq.getParticipatingPrimitives();
        assertNotNull(prims);
        assertEquals(9, prims.size());
        for (OsmPrimitive o : Arrays.asList(w1, w2, w3, w4, w5, w6, w7, r1, r2)) {
            assertTrue(o.toString(), prims.contains(o));
        }
        Collection<PseudoCommand> cmds = seq.getChildren();
        assertNotNull(cmds);
        assertEquals(9, cmds.size());

        assertTrue(seq.executeCommand());

        assertFalse(w1.hasKey("color"));
        assertTrue(w1.hasKey("colour"));

        assertFalse(w2.hasKey("highway"));
        assertTrue(w2.hasKey("ford"));

        assertFalse("false".equals(w3.get("oneway")));
        assertTrue("no".equals(w3.get("oneway")));

        assertFalse("0".equals(w4.get("oneway")));
        assertTrue("no".equals(w4.get("oneway")));

        assertFalse("true".equals(w5.get("oneway")));
        assertTrue("yes".equals(w5.get("oneway")));

        assertFalse("1".equals(w6.get("oneway")));
        assertTrue("yes".equals(w6.get("oneway")));

        assertFalse(w7.hasKey("highway"));
        assertTrue(w7.hasKey("barrier"));

        assertFalse("multipolygon".equals(r1.get("type")));
        assertTrue("boundary".equals(r1.get("type")));

        assertTrue("space_end".equals(r2.get("foo")));
        assertTrue("space_begin".equals(r2.get("bar")));
        assertTrue("space_both".equals(r2.get("baz")));
        assertFalse(r2.hasKey(" space_begin"));
        assertFalse(r2.hasKey("space_end "));
        assertFalse(r2.hasKey(" space_both "));
        assertTrue(r2.hasKey("space_begin"));
        assertTrue(r2.hasKey("space_end"));
        assertTrue(r2.hasKey("space_both"));
    }
}
