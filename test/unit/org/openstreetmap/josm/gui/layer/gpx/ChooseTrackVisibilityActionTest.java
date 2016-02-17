// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.io.GpxReaderTest;

/**
 * Unit tests of {@link ChooseTrackVisibilityAction} class.
 */
public class ChooseTrackVisibilityActionTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Test action.
     * @throws Exception if an error occurs
     */
    @Test
    public void testAction() throws Exception {
        final GpxData gpx = GpxReaderTest.parseGpxData(TestUtils.getTestDataRoot() + "minimal.gpx");
        GpxLayer gpxLayer = new GpxLayer(gpx);
        ChooseTrackVisibilityAction action = new ChooseTrackVisibilityAction(gpxLayer);
        action.actionPerformed(null);
    }
}
