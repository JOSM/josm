// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType;
import org.openstreetmap.josm.gui.conflict.pair.tags.TagMergeItem;

public class TagMergeItemTest {

    @BeforeClass
    public static void init() {
        Main.initApplicationPreferences();
    }

    @Test
    public void test_TagMergeItem() {
        TagMergeItem item = new TagMergeItem("key", "myvalue", "theirvalue");
        assertEquals("key", item.getKey());
        assertEquals("myvalue", item.getMyTagValue());
        assertEquals("theirvalue", item.getTheirTagValue());
        assertEquals(MergeDecisionType.UNDECIDED, item.getMergeDecision());
    }

    @Test
    public void test_TagMergeItem_2() {
        Node n1 = new Node(1);
        Node n2 = new Node(1);
        n1.put("key", "myvalue");
        n2.put("key", "theirvalue");

        TagMergeItem item = new TagMergeItem("key", n1, n2);
        assertEquals("key", item.getKey());
        assertEquals("myvalue", item.getMyTagValue());
        assertEquals("theirvalue", item.getTheirTagValue());
        assertEquals(MergeDecisionType.UNDECIDED, item.getMergeDecision());
    }

    @Test
    public void test_TagMergeItem_3() {
        Node n1 = new Node(1);
        Node n2 = new Node(1);
        n1.put("key", "myvalue");
        // n2 does not have this key

        TagMergeItem item = new TagMergeItem("key", n1, n2);
        assertEquals("key", item.getKey());
        assertEquals("myvalue", item.getMyTagValue());
        assertNull(item.getTheirTagValue());
        assertEquals(MergeDecisionType.UNDECIDED, item.getMergeDecision());
    }

    @Test
    public void test_TagMergeItem_4() {
        Node n1 = new Node(1);
        Node n2 = new Node(1);
        // n1 does not have this key
        // n1.put("key", "myvalue");
        n2.put("key", "theirvalue");

        TagMergeItem item = new TagMergeItem("key", n1, n2);
        assertEquals("key", item.getKey());
        assertNull(item.getMyTagValue());
        assertEquals("theirvalue",item.getTheirTagValue());
        assertEquals(MergeDecisionType.UNDECIDED, item.getMergeDecision());
    }


    @Test
    public void test_decide() {
        TagMergeItem item = new TagMergeItem("key", "myvalue", "theirvalue");
        item.decide(MergeDecisionType.KEEP_MINE);
        assertEquals(MergeDecisionType.KEEP_MINE, item.getMergeDecision());
    }

    @Test()
    public void test_decide_1() {
        TagMergeItem item = new TagMergeItem("key", "myvalue", "theirvalue");
        try {
            item.decide(null);
            fail("expected IllegalArgumentException not thrown");
        } catch(IllegalArgumentException e) {
            // OK
        }
    }

    @Test()
    public void test_applyToMyPrimitive() {
        TagMergeItem item = new TagMergeItem("key", "myvalue", "theirvalue");
        item.decide(MergeDecisionType.KEEP_MINE);

        Node n1 = new Node(1);
        n1.put("key", "oldvalue");
        item.applyToMyPrimitive(n1);
        assertEquals("myvalue", n1.get("key"));

        n1 = new Node(1);
        item.applyToMyPrimitive(n1);
        assertEquals("myvalue", n1.get("key"));

    }

    @Test()
    public void test_applyToMyPrimitive_2() {
        TagMergeItem item = new TagMergeItem("key", "myvalue", "theirvalue");
        item.decide(MergeDecisionType.KEEP_THEIR);

        Node n1 = new Node(1);
        n1.put("key", "oldvalue");
        item.applyToMyPrimitive(n1);
        assertEquals("theirvalue", n1.get("key"));

        n1 = new Node(1);
        item.applyToMyPrimitive(n1);
        assertEquals("theirvalue", n1.get("key"));
    }

    @Test()
    public void test_applyToMyPrimitive_3() {
        TagMergeItem item = new TagMergeItem("key", "myvalue", "theirvalue");
        // item is undecided
        // item.decide(MergeDecisionType.KEEP_THEIR);

        Node n1 = new Node(1);
        n1.put("key", "oldvalue");
        try {
            item.applyToMyPrimitive(n1);
            fail("expected IllegalStateException");
        } catch(IllegalStateException e) {
            // OK
        }
    }

    @Test()
    public void test_applyToMyPrimitive_4() {
        TagMergeItem item = new TagMergeItem("key", "myvalue", "theirvalue");

        try {
            item.applyToMyPrimitive(null);
            fail("expected IllegalArgumentException");
        } catch(IllegalArgumentException e) {
            // OK
        }
    }
}
