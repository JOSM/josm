// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.tags;

import static org.fest.reflect.core.Reflection.field;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link TagMergeModel} class.
 */
@SuppressWarnings("unchecked")
public class TagMergeModelTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    @Test
    public void testAddPropertyChangeListener() {
        TagMergeModel model = new TagMergeModel();
        PropertyChangeListener listener = new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
            }
        };
        model.addPropertyChangeListener(listener);

        Set<?> list = field("listeners").ofType(Set.class)
        .in(model)
        .get();

        assertEquals(1, list.size());
        assertEquals(listener, list.iterator().next());
    }

    @Test
    public void testRemovePropertyChangeListener() {
        TagMergeModel model = new TagMergeModel();
        PropertyChangeListener listener = new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
            }
        };
        model.addPropertyChangeListener(listener);
        model.removePropertyChangeListener(listener);

        Set<?> list = field("listeners")
        .ofType(Set.class)
        .in(model)
        .get();

        assertEquals(0, list.size());
    }

    @Test
    public void testPopulateNoConflichts() {
        Node my = new Node(1);
        Node their = new Node(1);
        TagMergeModel model = new TagMergeModel();
        model.populate(my, their);

        List<TagMergeItem> list = field("tagMergeItems")
        .ofType(List.class)
        .in(model)
        .get();

        assertEquals(0, list.size());
    }

    @Test
    public void testPopulateNoConflicts1() {
        Node my = new Node(1);
        my.put("key", "value");
        Node their = new Node(1);
        their.put("key", "value");
        TagMergeModel model = new TagMergeModel();
        model.populate(my, their);

        List<TagMergeItem> list = field("tagMergeItems")
        .ofType(List.class)
        .in(model)
        .get();

        assertEquals(0, list.size());
    }

    @Test
    public void testPopulateMissingKeyMine() {
        Node my = new Node(1);
        Node their = new Node(1);
        their.put("key", "value");
        TagMergeModel model = new TagMergeModel();
        model.populate(my, their);

        List<TagMergeItem> list = field("tagMergeItems")
        .ofType(List.class)
        .in(model)
        .get();

        assertEquals(1, list.size());
        TagMergeItem item = list.get(0);
        assertEquals(MergeDecisionType.UNDECIDED, item.getMergeDecision());
        assertEquals("key", item.getKey());
        assertNull(item.getMyTagValue());
        assertEquals("value", item.getTheirTagValue());
    }

    @Test
    public void testPopulateMissingKeyTheir() {
        Node my = new Node(1);
        my.put("key", "value");
        Node their = new Node(1);
        TagMergeModel model = new TagMergeModel();
        model.populate(my, their);

        List<TagMergeItem> list = field("tagMergeItems")
        .ofType(List.class)
        .in(model)
        .get();

        assertEquals(1, list.size());
        TagMergeItem item = list.get(0);
        assertEquals(MergeDecisionType.UNDECIDED, item.getMergeDecision());
        assertEquals("key", item.getKey());
        assertNull(item.getTheirTagValue());
        assertEquals("value", item.getMyTagValue());
    }

    @Test
    public void testPopulateConflictingValues() {
        Node my = new Node(1);
        my.put("key", "myvalue");
        Node their = new Node(1);
        their.put("key", "theirvalue");
        TagMergeModel model = new TagMergeModel();
        model.populate(my, their);

        List<TagMergeItem> list = field("tagMergeItems")
        .ofType(List.class)
        .in(model)
        .get();

        assertEquals(1, list.size());
        TagMergeItem item = list.get(0);
        assertEquals(MergeDecisionType.UNDECIDED, item.getMergeDecision());
        assertEquals("key", item.getKey());
        assertEquals("myvalue", item.getMyTagValue());
        assertEquals("theirvalue", item.getTheirTagValue());
    }

    @Test
    public void testAddItem() {
        TagMergeItem item = new TagMergeItem("key", "myvalue", "theirvalue");
        TagMergeModel model = new TagMergeModel();
        model.addItem(item);

        List<TagMergeItem> list = field("tagMergeItems")
        .ofType(List.class)
        .in(model)
        .get();

        assertEquals(1, list.size());
        item = list.get(0);
        assertEquals(MergeDecisionType.UNDECIDED, item.getMergeDecision());
        assertEquals("key", item.getKey());
        assertEquals("myvalue", item.getMyTagValue());
        assertEquals("theirvalue", item.getTheirTagValue());
    }

    @Test
    public void testDecide() {
        TagMergeItem item = new TagMergeItem("key", "myvalue", "theirvalue");
        TagMergeModel model = new TagMergeModel();
        model.addItem(item);

        List<TagMergeItem> list = field("tagMergeItems")
        .ofType(List.class)
        .in(model)
        .get();

        model.decide(0, MergeDecisionType.KEEP_MINE);
        assertEquals(1, list.size());
        item = list.get(0);
        assertEquals(MergeDecisionType.KEEP_MINE, item.getMergeDecision());

        model.decide(0, MergeDecisionType.KEEP_THEIR);
        assertEquals(1, list.size());
        item = list.get(0);
        assertEquals(MergeDecisionType.KEEP_THEIR, item.getMergeDecision());

        model.decide(0, MergeDecisionType.UNDECIDED);
        assertEquals(1, list.size());
        item = list.get(0);
        assertEquals(MergeDecisionType.UNDECIDED, item.getMergeDecision());
    }

    @Test
    public void testDecideMultiple() {

        TagMergeModel model = new TagMergeModel();
        for (int i = 0; i < 10; i++) {
            model.addItem(new TagMergeItem("key-" + i, "myvalue-" + i, "theirvalue-" +i));
        }

        List<TagMergeItem> list = field("tagMergeItems")
        .ofType(List.class)
        .in(model)
        .get();

        assertEquals(10, list.size());

        model.decide(new int[] {0, 3, 5}, MergeDecisionType.KEEP_MINE);
        for (int i = 0; i < 10; i++) {
            TagMergeItem item = list.get(i);
            if (i == 0 || i == 3 || i == 5) {
                assertEquals(MergeDecisionType.KEEP_MINE, item.getMergeDecision());
            } else {
                assertEquals(MergeDecisionType.UNDECIDED, item.getMergeDecision());
            }
        }
    }
}
