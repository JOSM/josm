// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.gui.layer.imagery.ImageryFilterSettings;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link ImageryLayer} class.
 */
public class ImageryLayerTest {

    /**
     * For creating layers
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Unit test of {@link ImageryLayer#getFilterSettings()}
     */
    @Test
    public void testHasSettings() {
        ImageryLayer layer = TMSLayerTest.createTmsLayer();
        ImageryFilterSettings settings = layer.getFilterSettings();
        assertNotNull(settings);
        assertSame(settings, layer.getFilterSettings());
    }
}
