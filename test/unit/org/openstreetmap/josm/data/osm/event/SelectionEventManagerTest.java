// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.CommandTest.CommandTestDataWithRelation;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager.FireMode;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tests the {@link SelectionEventManager}
 * @author Michael Zangl
 * @since 12048
 */
public class SelectionEventManagerTest {
    private final class SelectionListener implements SelectionChangedListener {
        private Collection<? extends OsmPrimitive> newSelection;

        @Override
        public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
            this.newSelection = newSelection;
        }
    }

    /**
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Tests that events in the active layer are propagated.
     */
    @Test
    public void test() {
        // automatically adds the layers
        CommandTestDataWithRelation testData1 = new CommandTestDataWithRelation();
        CommandTestDataWithRelation testData2 = new CommandTestDataWithRelation();
        Main.getLayerManager().setActiveLayer(testData1.layer);
        assertEquals(testData1.layer, Main.getLayerManager().getEditLayer());

        SelectionListener listener = new SelectionListener();
        SelectionEventManager.getInstance().addSelectionListener(listener, FireMode.IMMEDIATELY);
        assertNull(listener.newSelection);

        // active layer, should change
        testData1.layer.data.setSelected(testData1.existingNode.getPrimitiveId());
        assertEquals(new HashSet<OsmPrimitive>(Arrays.asList(testData1.existingNode)), listener.newSelection);

        listener.newSelection = null;
        testData1.layer.data.clearSelection(testData1.existingNode.getPrimitiveId());
        assertEquals(new HashSet<OsmPrimitive>(Arrays.asList()), listener.newSelection);

        listener.newSelection = null;
        testData1.layer.data.addSelected(testData1.existingNode2.getPrimitiveId());
        assertEquals(new HashSet<OsmPrimitive>(Arrays.asList(testData1.existingNode2)), listener.newSelection);

        // changing to other dataset should trigger a empty selection
        listener.newSelection = null;
        Main.getLayerManager().setActiveLayer(testData2.layer);
        assertEquals(new HashSet<OsmPrimitive>(Arrays.asList()), listener.newSelection);

        // This should not trigger anything, since the layer is not active any more.
        listener.newSelection = null;
        testData1.layer.data.clearSelection(testData1.existingNode.getPrimitiveId());
        assertNull(listener.newSelection);
    }

}
