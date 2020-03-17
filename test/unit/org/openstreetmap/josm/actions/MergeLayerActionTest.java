// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collections;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.LayerManagerTest.TestLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.mockers.ExtendedDialogMocker;
import org.openstreetmap.josm.testutils.mockers.JOptionPaneSimpleMocker;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link MergeLayerAction}.
 */
public class MergeLayerActionTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().main().projection();

    /**
     * MergeLayerExtendedDialog mocker.
     */
    public static class MergeLayerExtendedDialogMocker extends ExtendedDialogMocker {
        @Override
        protected void act(final ExtendedDialog instance) {
            ((JosmComboBox<?>) ((JPanel) this.getContent(instance)).getComponent(1)).setSelectedIndex(0);
        }

        @Override
        protected String getString(final ExtendedDialog instance) {
            return ((JLabel) ((JPanel) this.getContent(instance)).getComponent(0)).getText();
        }
    }

    private MergeLayerAction action;

    /**
     * Setup test.
     */
    @Before
    public void setUp() {
        if (action == null) {
            action = new MergeLayerAction();
        }
        for (TestLayer testLayer : MainApplication.getLayerManager().getLayersOfType(TestLayer.class)) {
            MainApplication.getLayerManager().removeLayer(testLayer);
        }
    }

    /**
     * Tests that no error occurs when no source layer exists.
     */
    @Test
    public void testMergeNoSourceLayer() {
        assertNull(MainApplication.getLayerManager().getActiveLayer());
        action.actionPerformed(null);
        assertEquals(0, MainApplication.getLayerManager().getLayers().size());
    }

    /**
     * Tests that no error occurs when no target layer exists.
     */
    @Test
    public void testMergeNoTargetLayer() {
        TestUtils.assumeWorkingJMockit();
        final JOptionPaneSimpleMocker jopsMocker = new JOptionPaneSimpleMocker(
            Collections.singletonMap("<html>There are no layers the source layer<br>'onion'<br>could be merged to.</html>", 0)
        );

        OsmDataLayer layer = new OsmDataLayer(new DataSet(), "onion", null);
        MainApplication.getLayerManager().addLayer(layer);
        assertEquals(1, MainApplication.getLayerManager().getLayers().size());
        assertNull(action.merge(layer));
        assertEquals(1, MainApplication.getLayerManager().getLayers().size());

        assertEquals(1, jopsMocker.getInvocationLog().size());
        Object[] invocationLogEntry = jopsMocker.getInvocationLog().get(0);
        assertEquals(0, (int) invocationLogEntry[0]);
        assertEquals("No target layers", invocationLogEntry[2]);
    }

    /**
     * Tests that the merge is done with two empty layers.
     * @throws Exception if any error occurs
     */
    @Test
    public void testMergeTwoEmptyLayers() throws Exception {
        TestUtils.assumeWorkingJMockit();
        final MergeLayerExtendedDialogMocker edMocker = new MergeLayerExtendedDialogMocker();
        edMocker.getMockResultMap().put("Please select the target layer.", "Merge layer");

        OsmDataLayer layer1 = new OsmDataLayer(new DataSet(), "1", null);
        OsmDataLayer layer2 = new OsmDataLayer(new DataSet(), "2", null);
        MainApplication.getLayerManager().addLayer(layer1);
        MainApplication.getLayerManager().addLayer(layer2);
        assertEquals(2, MainApplication.getLayerManager().getLayers().size());
        action.merge(layer2).get();
        assertEquals(1, MainApplication.getLayerManager().getLayers().size());

        assertEquals(1, edMocker.getInvocationLog().size());
        Object[] invocationLogEntry = edMocker.getInvocationLog().get(0);
        assertEquals(1, (int) invocationLogEntry[0]);
        assertEquals("Select target layer", invocationLogEntry[2]);
    }
}
