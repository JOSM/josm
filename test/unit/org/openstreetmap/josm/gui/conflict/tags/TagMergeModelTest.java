// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.fest.reflect.core.Reflection.field;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType;
import org.openstreetmap.josm.gui.conflict.pair.tags.TagMergeItem;
import org.openstreetmap.josm.gui.conflict.pair.tags.TagMergeModel;

@SuppressWarnings("unchecked")
public class TagMergeModelTest {

    @BeforeClass
    public static void init() {
        Main.initApplicationPreferences();
    }

    @Test
    public void addPropertyChangeListener() {
        TagMergeModel model = new TagMergeModel();
        PropertyChangeListener listener = new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
            }
        };
        model.addPropertyChangeListener(listener);

        List<?> list = field("listeners").ofType(List.class)
        .in(model)
        .get();

        assertEquals(1, list.size());
        assertEquals(listener, list.get(0));
    }

    @Test
    public void removePropertyChangeListener() {
        TagMergeModel model = new TagMergeModel();
        PropertyChangeListener listener = new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
            }
        };
        model.addPropertyChangeListener(listener);
        model.removePropertyChangeListener(listener);

        List<?> list = field("listeners")
        .ofType(List.class)
        .in(model)
        .get();

        assertEquals(0, list.size());
    }

    @Test
    public void populateNoConflichts() {
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
    public void populateNoConflicts1() {
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
    public void populateMissingKeyMine() {
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
    public void populateMissingKeyTheir() {
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
    public void populateConflictingValues() {
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
    public void addItem() {
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
    public void decide() {
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
    public void decideMultiple() {

        TagMergeModel model = new TagMergeModel();
        for (int i=0; i < 10; i++) {
            model.addItem(new TagMergeItem("key-" + i, "myvalue-" + i, "theirvalue-" +i));
        }

        List<TagMergeItem> list = field("tagMergeItems")
        .ofType(List.class)
        .in(model)
        .get();

        assertEquals(10, list.size());

        model.decide(new int[] {0, 3, 5}, MergeDecisionType.KEEP_MINE);
        for (int i = 0; i< 10; i++) {
            TagMergeItem item = list.get(i);
            if (i == 0 || i == 3 || i == 5) {
                assertEquals(MergeDecisionType.KEEP_MINE, item.getMergeDecision());
            } else {
                assertEquals(MergeDecisionType.UNDECIDED, item.getMergeDecision());
            }
        }
    }
}
