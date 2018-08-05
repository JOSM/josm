// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.nodes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultListSelectionModel;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.conflict.pair.AbstractListMergeModel;
import org.openstreetmap.josm.testutils.DatasetFactory;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Logging;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link NodeListMergeModel}.
 */
public class NodeListMergeModelTest {

    private DatasetFactory my = new DatasetFactory();
    private DatasetFactory their = new DatasetFactory();

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    @SuppressWarnings("unchecked")
    protected List<Node> inspectNodeList(NodeListMergeModel model, String name) {
        try {
            Method getNamedEntries = AbstractListMergeModel.class.getDeclaredMethod("get" + name + "Entries");
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                getNamedEntries.setAccessible(true);
                return null;
            });
            return (List<Node>) getNamedEntries.invoke(model);
        } catch (ReflectiveOperationException | IllegalArgumentException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }

    protected DefaultListSelectionModel inspectListSelectionModel(NodeListMergeModel model, String name) {
        try {
            return (DefaultListSelectionModel) TestUtils.getPrivateField(AbstractListMergeModel.class, model, name);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    protected Set<PropertyChangeListener> getListeners(NodeListMergeModel model) throws ReflectiveOperationException {
        return (Set<PropertyChangeListener>)
                TestUtils.getPrivateField(AbstractListMergeModel.class, model, "listeners");
    }

    protected void ensureSelected(DefaultListSelectionModel model, Object... idx) {
        if (idx == null) return;
        for (int i = 0; i < idx.length; i++) {
            if (idx[i] instanceof Integer) {
                int j = (Integer) idx[i];
                assertTrue("expected row " + j + " to be selected", model.isSelectedIndex(j));
                break;
            }
            int[] rows = (int[]) idx[i];
            if (rows.length != 2) {
                fail("illegal selection range. Either null or not length 2: " + Arrays.toString(rows));
            }
            if (rows[0] > rows[1]) {
                fail("illegal selection range. lower bound > upper bound ");
            }
            for (int j = rows[0]; j <= rows[1]; j++) {
                assertTrue("expected row " + j + " to be selected", model.isSelectedIndex(j));
            }
        }
    }

    @Test
    public void testCopyMyNodesToTop1() {
        NodeListMergeModel model = new NodeListMergeModel();

        Way myWay = my.addWay(1, 1);
        myWay.addNode(my.addNode(2));
        myWay.addNode(my.addNode(3));

        Way theirWay = their.addWay(1);

        model.populate(myWay, theirWay, null);
        model.copyMyToTop(new int[]{0});

        List<Node> mergedNodes = inspectNodeList(model, "Merged");

        assertEquals(1, mergedNodes.size());
        assertEquals(2, mergedNodes.get(0).getId());

        DefaultListSelectionModel mergedSelection = inspectListSelectionModel(model, "mergedEntriesSelectionModel");
        ensureSelected(mergedSelection, 0);
    }

    @Test
    public void testCopyMyNodesToTop2() {
        NodeListMergeModel model = new NodeListMergeModel();

        Way myWay = my.addWay(1, 1);
        myWay.addNode(my.addNode(2));
        myWay.addNode(my.addNode(3));

        Way theirWay = their.addWay(1);

        model.populate(myWay, theirWay, null);

        List<Node> mergedNodes = inspectNodeList(model, "Merged");
        mergedNodes.add(new Node(1));

        model.copyMyToTop(new int[]{0});

        mergedNodes = inspectNodeList(model, "Merged");
        assertEquals(2, mergedNodes.size());
        assertEquals(2, mergedNodes.get(0).getId());
        assertEquals(1, mergedNodes.get(1).getId());

        DefaultListSelectionModel mergedSelection = inspectListSelectionModel(model, "mergedEntriesSelectionModel");
        ensureSelected(mergedSelection, 0);
    }

    @Test
    public void testCopyMyNodesToTop3() {
        NodeListMergeModel model = new NodeListMergeModel();

        Way myWay = my.addWay(1, 1);
        myWay.addNode(my.addNode(2));
        myWay.addNode(my.addNode(3));
        Way theirWay = their.addWay(1);


        model.populate(myWay, theirWay, null);

        List<Node> mergedNodes = inspectNodeList(model, "Merged");
        mergedNodes.add(new Node(1));

        model.copyMyToTop(new int[]{1}); // copy node 3

        mergedNodes = inspectNodeList(model, "Merged");
        assertEquals(2, mergedNodes.size());
        assertEquals(3, mergedNodes.get(0).getId()); // my node 3 at position 0
        assertEquals(1, mergedNodes.get(1).getId()); // already merged node 1 at position 1

        DefaultListSelectionModel mergedSelection = inspectListSelectionModel(model, "mergedEntriesSelectionModel");
        ensureSelected(mergedSelection, 0);
    }

    @Test
    public void testCopyMyNodesToTop4() {
        NodeListMergeModel model = new NodeListMergeModel();

        Way myWay = my.addWay(1, 1);
        myWay.addNode(my.addNode(2));
        myWay.addNode(my.addNode(3));
        myWay.addNode(my.addNode(4));
        Way theirWay = their.addWay(1);

        model.populate(myWay, theirWay, null);

        List<Node> mergedNodes = inspectNodeList(model, "Merged");
        mergedNodes.add(new Node(1));

        model.copyMyToTop(new int[]{1, 2}); // copy node 3 and 4

        mergedNodes = inspectNodeList(model, "Merged");
        assertEquals(3, mergedNodes.size());
        assertEquals(3, mergedNodes.get(0).getId()); // my node 3 at position 0
        assertEquals(4, mergedNodes.get(1).getId()); // my node 4 at position 1
        assertEquals(1, mergedNodes.get(2).getId()); // already merged node 1 at position 2

        DefaultListSelectionModel mergedSelection = inspectListSelectionModel(model, "mergedEntriesSelectionModel");
        ensureSelected(mergedSelection, 0, 1); // first two rows selected
    }

    @Test
    public void testCopyMyNodesToEnd1() {
        NodeListMergeModel model = new NodeListMergeModel();

        Way myWay = my.addWay(1, 1);
        myWay.addNode(my.addNode(2));
        myWay.addNode(my.addNode(3));
        Way theirWay = their.addWay(1);

        model.populate(myWay, theirWay, null);
        model.copyMyToEnd(new int[]{0});

        List<Node> mergedNodes = inspectNodeList(model, "Merged");

        assertEquals(1, mergedNodes.size());
        assertEquals(2, mergedNodes.get(0).getId());

        DefaultListSelectionModel mergedSelection = inspectListSelectionModel(model, "mergedEntriesSelectionModel");
        ensureSelected(mergedSelection, 0);
    }

    @Test
    public void testCopyMyNodesToEnd2() {
        NodeListMergeModel model = new NodeListMergeModel();

        Way myWay = my.addWay(1, 1);
        myWay.addNode(my.addNode(2));
        myWay.addNode(my.addNode(3));
        Way theirWay = their.addWay(1);

        model.populate(myWay, theirWay, null);

        List<Node> mergedNodes = inspectNodeList(model, "Merged");
        mergedNodes.add(new Node(1));

        model.copyMyToEnd(new int[]{0});

        mergedNodes = inspectNodeList(model, "Merged");
        assertEquals(2, mergedNodes.size());
        assertEquals(1, mergedNodes.get(0).getId()); // already merged node 1 at position 0
        assertEquals(2, mergedNodes.get(1).getId()); // copied node 2 at position 1

        DefaultListSelectionModel mergedSelection = inspectListSelectionModel(model, "mergedEntriesSelectionModel");
        ensureSelected(mergedSelection, 1);
    }

    @Test
    public void testCopyMyNodesToEnd3() {
        NodeListMergeModel model = new NodeListMergeModel();


        Way myWay = my.addWay(1, 1);
        myWay.addNode(my.addNode(2));
        myWay.addNode(my.addNode(3));
        Way theirWay = their.addWay(1);

        model.populate(myWay, theirWay, null);

        List<Node> mergedNodes = inspectNodeList(model, "Merged");
        mergedNodes.add(new Node(1));

        model.copyMyToEnd(new int[]{1}); // copy node 3

        mergedNodes = inspectNodeList(model, "Merged");
        assertEquals(2, mergedNodes.size());
        assertEquals(1, mergedNodes.get(0).getId()); // already merged node 1 at position 0
        assertEquals(3, mergedNodes.get(1).getId()); // my node 3 at position 1


        DefaultListSelectionModel mergedSelection = inspectListSelectionModel(model, "mergedEntriesSelectionModel");
        ensureSelected(mergedSelection, 1);
    }

    @Test
    public void testCopyMyNodesToEnd4() {
        NodeListMergeModel model = new NodeListMergeModel();


        Way myWay = my.addWay(1, 1);
        myWay.addNode(my.addNode(2));
        myWay.addNode(my.addNode(3));
        myWay.addNode(my.addNode(4));
        Way theirWay = their.addWay(1);

        model.populate(myWay, theirWay, null);

        List<Node> mergedNodes = inspectNodeList(model, "Merged");
        mergedNodes.add(new Node(1));

        model.copyMyToEnd(new int[]{1, 2}); // copy node 3 and 4

        mergedNodes = inspectNodeList(model, "Merged");
        assertEquals(3, mergedNodes.size());
        assertEquals(1, mergedNodes.get(0).getId()); // already merged node 1 at position 0
        assertEquals(3, mergedNodes.get(1).getId()); // my node 3 at position 1
        assertEquals(4, mergedNodes.get(2).getId()); // my node 4 at position 2


        DefaultListSelectionModel mergedSelection = inspectListSelectionModel(model, "mergedEntriesSelectionModel");
        ensureSelected(mergedSelection, 1, 2); // last two rows selected
    }

    /* ----------------------------------------------------------------------------- */
    /* copyMyNodesBeforeCurrent                                                      */
    /* ----------------------------------------------------------------------------- */

    @Test
    public void testCopyMyNodesBeforeCurrent1() {
        NodeListMergeModel model = new NodeListMergeModel();

        Way myWay = my.addWay(1, 1);
        myWay.addNode(my.addNode(1));
        myWay.addNode(my.addNode(2));
        Way theirWay = their.addWay(1);

        model.populate(myWay, theirWay, null);
        List<Node> mergedNodes = inspectNodeList(model, "Merged");
        mergedNodes.add(new Node(10));
        mergedNodes.add(new Node(11));
        mergedNodes.add(new Node(12));

        model.copyMyBeforeCurrent(new int[]{0}, 1);

        assertEquals(4, mergedNodes.size());
        assertEquals(10, mergedNodes.get(0).getId()); // already merged node
        assertEquals(1, mergedNodes.get(1).getId());  // copied node 1 at position 1
        assertEquals(11, mergedNodes.get(2).getId()); // already merged node
        assertEquals(12, mergedNodes.get(3).getId()); // already merged node

        DefaultListSelectionModel mergedSelection = inspectListSelectionModel(model, "mergedEntriesSelectionModel");
        ensureSelected(mergedSelection, 1); // position 1 selected
    }

    @Test
    public void testCopyMyNodesBeforeCurrent2() {
        NodeListMergeModel model = new NodeListMergeModel();

        Way myWay = my.addWay(1, 1);
        myWay.addNode(my.addNode(1));
        myWay.addNode(my.addNode(2));
        Way theirWay = their.addWay(1);

        model.populate(myWay, theirWay, null);
        List<Node> mergedNodes = inspectNodeList(model, "Merged");
        mergedNodes.add(new Node(10));
        mergedNodes.add(new Node(11));
        mergedNodes.add(new Node(12));

        model.copyMyBeforeCurrent(new int[]{0, 1}, 0);

        assertEquals(5, mergedNodes.size());
        assertEquals(1, mergedNodes.get(0).getId());  // copied node 1 at position 0
        assertEquals(2, mergedNodes.get(1).getId());  // copied node 2 at position 1
        assertEquals(10, mergedNodes.get(2).getId()); // already merged node
        assertEquals(11, mergedNodes.get(3).getId()); // already merged node
        assertEquals(12, mergedNodes.get(4).getId()); // already merged node

        DefaultListSelectionModel mergedSelection = inspectListSelectionModel(model, "mergedEntriesSelectionModel");
        ensureSelected(mergedSelection, 0, 1); // position 0 and 1 selected
    }

    @Test
    public void testCopyMyNodesBeforeCurrent3() {
        NodeListMergeModel model = new NodeListMergeModel();

        Way myWay = my.addWay(1, 1);
        myWay.addNode(my.addNode(1));
        myWay.addNode(my.addNode(2));
        Way theirWay = their.addWay(1);

        model.populate(myWay, theirWay, null);
        List<Node> mergedNodes = inspectNodeList(model, "Merged");
        mergedNodes.add(new Node(10));
        mergedNodes.add(new Node(11));
        mergedNodes.add(new Node(12));

        try {
            model.copyMyBeforeCurrent(new int[]{0, 1}, -1);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
            Logging.trace(e);
        }

        try {
            model.copyMyBeforeCurrent(new int[]{0, 1}, 4);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
            Logging.trace(e);
        }
    }

    /* ----------------------------------------------------------------------------- */
    /* copyMyNodesAfterCurrent                                                       */
    /* ----------------------------------------------------------------------------- */
    @Test
    public void testCopyMyNodesAfterCurrent1() {
        NodeListMergeModel model = new NodeListMergeModel();

        Way myWay = my.addWay(1, 1);
        myWay.addNode(my.addNode(1));
        myWay.addNode(my.addNode(2));
        Way theirWay = their.addWay(1);

        model.populate(myWay, theirWay, null);
        List<Node> mergedNodes = inspectNodeList(model, "Merged");
        mergedNodes.add(new Node(10));
        mergedNodes.add(new Node(11));
        mergedNodes.add(new Node(12));

        model.copyMyAfterCurrent(new int[]{0}, 1);

        assertEquals(4, mergedNodes.size());
        assertEquals(10, mergedNodes.get(0).getId()); // already merged node
        assertEquals(11, mergedNodes.get(1).getId()); // already merged node
        assertEquals(1, mergedNodes.get(2).getId());  // copied node 1 at position 2
        assertEquals(12, mergedNodes.get(3).getId()); // already merged node

        DefaultListSelectionModel mergedSelection = inspectListSelectionModel(model, "mergedEntriesSelectionModel");
        ensureSelected(mergedSelection, 2); // position 1 selected
    }

    @Test
    public void testCopyMyNodesAfterCurrent2() {
        NodeListMergeModel model = new NodeListMergeModel();

        Way myWay = my.addWay(1, 1);
        myWay.addNode(my.addNode(1));
        myWay.addNode(my.addNode(2));
        myWay.addNode(my.addNode(3));
        Way theirWay = their.addWay(1);

        model.populate(myWay, theirWay, null);
        List<Node> mergedNodes = inspectNodeList(model, "Merged");
        mergedNodes.add(new Node(10));
        mergedNodes.add(new Node(11));
        mergedNodes.add(new Node(12));

        model.copyMyAfterCurrent(new int[]{0, 1}, 2);

        assertEquals(5, mergedNodes.size());
        assertEquals(10, mergedNodes.get(0).getId()); // already merged node
        assertEquals(11, mergedNodes.get(1).getId()); // already merged node
        assertEquals(12, mergedNodes.get(2).getId()); // already merged node
        assertEquals(1, mergedNodes.get(3).getId());  // copied node 1 at position 3
        assertEquals(2, mergedNodes.get(4).getId());  // copied node 2 at position 4

        DefaultListSelectionModel mergedSelection = inspectListSelectionModel(model, "mergedEntriesSelectionModel");
        ensureSelected(mergedSelection, 3, 4); // position 3,4 selected
    }

    @Test
    public void testCopyMyNodesAfterCurrent3() {
        NodeListMergeModel model = new NodeListMergeModel();

        Way myWay = my.addWay(1, 1);
        myWay.addNode(my.addNode(1));
        myWay.addNode(my.addNode(2));
        myWay.addNode(my.addNode(3));
        Way theirWay = their.addWay(1);

        model.populate(myWay, theirWay, null);
        List<Node> mergedNodes = inspectNodeList(model, "Merged");
        mergedNodes.add(new Node(10));
        mergedNodes.add(new Node(11));
        mergedNodes.add(new Node(12));

        model.copyMyAfterCurrent(new int[]{0, 2}, 0);

        assertEquals(5, mergedNodes.size());
        assertEquals(10, mergedNodes.get(0).getId()); // already merged node
        assertEquals(1, mergedNodes.get(1).getId());  // copied node 1 at position 1
        assertEquals(3, mergedNodes.get(2).getId());  // copied node 3 at position 2
        assertEquals(11, mergedNodes.get(3).getId()); // already merged node
        assertEquals(12, mergedNodes.get(4).getId()); // already merged node

        DefaultListSelectionModel mergedSelection = inspectListSelectionModel(model, "mergedEntriesSelectionModel");
        ensureSelected(mergedSelection, 1, 2); // position 1,2 selected
    }


    /* ----------------------------------------------------------------------------- */
    /* moveUpMergedNodes                                                       */
    /* ----------------------------------------------------------------------------- */
    @Test
    public void testMoveUpMergedNodes1() {
        NodeListMergeModel model = new NodeListMergeModel();

        Way myWay = new Way(1, 1);
        myWay.addNode(new Node(1));
        myWay.addNode(new Node(2));
        Way theirWay = new Way(1);

        model.populate(myWay, theirWay, null);
        List<Node> mergedNodes = inspectNodeList(model, "Merged");
        mergedNodes.add(new Node(10));
        mergedNodes.add(new Node(11));
        mergedNodes.add(new Node(12));

        model.moveUpMerged(new int[]{1});

        assertEquals(3, mergedNodes.size());
        assertEquals(11, mergedNodes.get(0).getId());
        assertEquals(10, mergedNodes.get(1).getId());
        assertEquals(12, mergedNodes.get(2).getId());

        DefaultListSelectionModel mergedSelection = inspectListSelectionModel(model, "mergedEntriesSelectionModel");
        ensureSelected(mergedSelection, 0); // position 1 selecte0
    }

    @Test
    public void testMoveUpMergedNodes2() {
        NodeListMergeModel model = new NodeListMergeModel();

        Way myWay = new Way(1, 1);
        myWay.addNode(new Node(1));
        myWay.addNode(new Node(2));
        Way theirWay = new Way(1);

        model.populate(myWay, theirWay, null);
        List<Node> mergedNodes = inspectNodeList(model, "Merged");
        mergedNodes.add(new Node(10));
        mergedNodes.add(new Node(11));
        mergedNodes.add(new Node(12));
        mergedNodes.add(new Node(13));
        mergedNodes.add(new Node(14));

        model.moveUpMerged(new int[]{1, 4});

        assertEquals(5, mergedNodes.size());
        assertEquals(11, mergedNodes.get(0).getId());
        assertEquals(10, mergedNodes.get(1).getId());
        assertEquals(12, mergedNodes.get(2).getId());
        assertEquals(14, mergedNodes.get(3).getId());
        assertEquals(13, mergedNodes.get(4).getId());

        DefaultListSelectionModel mergedSelection = inspectListSelectionModel(model, "mergedEntriesSelectionModel");
        ensureSelected(mergedSelection, 0, 3); // position 0 and 3 selecte0
    }

    @Test
    public void testMoveUpMergedNodes3() {
        NodeListMergeModel model = new NodeListMergeModel();

        Way myWay = new Way(1, 1);
        myWay.addNode(new Node(1));
        myWay.addNode(new Node(2));
        Way theirWay = new Way(1);

        model.populate(myWay, theirWay, null);
        List<Node> mergedNodes = inspectNodeList(model, "Merged");
        mergedNodes.add(new Node(10));
        mergedNodes.add(new Node(11));
        mergedNodes.add(new Node(12));
        mergedNodes.add(new Node(13));
        mergedNodes.add(new Node(14));

        model.moveUpMerged(new int[]{1, 2, 3, 4});

        assertEquals(5, mergedNodes.size());
        assertEquals(11, mergedNodes.get(0).getId());
        assertEquals(12, mergedNodes.get(1).getId());
        assertEquals(13, mergedNodes.get(2).getId());
        assertEquals(14, mergedNodes.get(3).getId());
        assertEquals(10, mergedNodes.get(4).getId());

        DefaultListSelectionModel mergedSelection = inspectListSelectionModel(model, "mergedEntriesSelectionModel");
        ensureSelected(mergedSelection, 0, 1, 2, 3);
    }

    /* ----------------------------------------------------------------------------- */
    /* moveDownMergedNodes                                                       */
    /* ----------------------------------------------------------------------------- */
    @Test
    public void testMoveDownMergedNodes1() {
        NodeListMergeModel model = new NodeListMergeModel();

        Way myWay = new Way(1, 1);
        myWay.addNode(new Node(1));
        myWay.addNode(new Node(2));
        Way theirWay = new Way(1);

        model.populate(myWay, theirWay, null);
        List<Node> mergedNodes = inspectNodeList(model, "Merged");
        mergedNodes.add(new Node(10));
        mergedNodes.add(new Node(11));
        mergedNodes.add(new Node(12));

        model.moveDownMerged(new int[]{1});

        assertEquals(3, mergedNodes.size());
        assertEquals(10, mergedNodes.get(0).getId());
        assertEquals(12, mergedNodes.get(1).getId());
        assertEquals(11, mergedNodes.get(2).getId());

        DefaultListSelectionModel mergedSelection = inspectListSelectionModel(model, "mergedEntriesSelectionModel");
        ensureSelected(mergedSelection, 2);
    }

    @Test
    public void testMoveDownMergedNodes2() {
        NodeListMergeModel model = new NodeListMergeModel();

        Way myWay = new Way(1, 1);
        myWay.addNode(new Node(1));
        myWay.addNode(new Node(2));
        Way theirWay = new Way(1);

        model.populate(myWay, theirWay, null);
        List<Node> mergedNodes = inspectNodeList(model, "Merged");
        mergedNodes.add(new Node(10));
        mergedNodes.add(new Node(11));
        mergedNodes.add(new Node(12));
        mergedNodes.add(new Node(13));
        mergedNodes.add(new Node(14));

        model.moveDownMerged(new int[]{1, 3});

        assertEquals(5, mergedNodes.size());
        assertEquals(10, mergedNodes.get(0).getId());
        assertEquals(12, mergedNodes.get(1).getId());
        assertEquals(11, mergedNodes.get(2).getId());
        assertEquals(14, mergedNodes.get(3).getId());
        assertEquals(13, mergedNodes.get(4).getId());

        DefaultListSelectionModel mergedSelection = inspectListSelectionModel(model, "mergedEntriesSelectionModel");
        ensureSelected(mergedSelection, 2, 4);
    }

    @Test
    public void testMoveDownMergedNodes3() {
        NodeListMergeModel model = new NodeListMergeModel();

        Way myWay = new Way(1, 1);
        myWay.addNode(new Node(1));
        myWay.addNode(new Node(2));
        Way theirWay = new Way(1);

        model.populate(myWay, theirWay, null);
        List<Node> mergedNodes = inspectNodeList(model, "Merged");
        mergedNodes.add(new Node(10));
        mergedNodes.add(new Node(11));
        mergedNodes.add(new Node(12));
        mergedNodes.add(new Node(13));
        mergedNodes.add(new Node(14));

        model.moveDownMerged(new int[]{1, 2, 3});

        assertEquals(5, mergedNodes.size());
        assertEquals(10, mergedNodes.get(0).getId());
        assertEquals(14, mergedNodes.get(1).getId());
        assertEquals(11, mergedNodes.get(2).getId());
        assertEquals(12, mergedNodes.get(3).getId());
        assertEquals(13, mergedNodes.get(4).getId());

        DefaultListSelectionModel mergedSelection = inspectListSelectionModel(model, "mergedEntriesSelectionModel");
        ensureSelected(mergedSelection, 2, 3, 4);
    }

    /* ----------------------------------------------------------------------------- */
    /* PropertyChangeListener                                                        */
    /* ----------------------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    @Test
    public void testAddPropertyChangeListener() throws ReflectiveOperationException {
        NodeListMergeModel model = new NodeListMergeModel();

        PropertyChangeListener listener = evt -> {
        };

        model.addPropertyChangeListener(listener);

        Set<PropertyChangeListener> listeners = getListeners(model);

        assertEquals(1, listeners.size());
        assertEquals(listener, listeners.iterator().next());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRemovePropertyChangeListener() throws ReflectiveOperationException {
        NodeListMergeModel model = new NodeListMergeModel();

        PropertyChangeListener listener = evt -> {
        };

        model.addPropertyChangeListener(listener);
        model.removePropertyChangeListener(listener);

        Set<PropertyChangeListener> listeners = getListeners(model);

        assertEquals(0, listeners.size());
    }

    /* ----------------------------------------------------------------------------- */
    /* property frozen                                                               */
    /* ----------------------------------------------------------------------------- */
    @Test
    public void testSetFrozen() {
        NodeListMergeModel model = new NodeListMergeModel();
        model.setFrozen(true);
        assertTrue(model.isFrozen());

        model.setFrozen(false);
        assertFalse(model.isFrozen());
    }

    @Test
    public void testSetFrozenWithPropertyChangeNotification() {
        NodeListMergeModel model = new NodeListMergeModel();

        class MyListener implements PropertyChangeListener {
            public ArrayList<PropertyChangeEvent> events = new ArrayList<>();

            public void propertyChange(PropertyChangeEvent evt) {
                events.add(evt);
            }
        }

        MyListener listener = new MyListener();
        model.addPropertyChangeListener(listener);
        boolean oldValue = model.isFrozen();
        model.setFrozen(!oldValue);
        assertEquals(!oldValue, model.isFrozen());

        assertEquals(1, listener.events.size());
        assertEquals(oldValue, listener.events.get(0).getOldValue());
        assertEquals(!oldValue, listener.events.get(0).getNewValue());
    }
}
