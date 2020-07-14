// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresets;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetsTest;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.ImageProvider.GetPaddedOptions;

import java.awt.Dimension;
import java.util.EnumSet;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Unit tests of getPadded method of the {@link ImageProvider} class.
 * This unit test is separated because it is the only one that needs a slow initialization.
 */
public class ImageProviderGetPaddedTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().mapStyles().presets();

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link ImageProvider#getPadded}.
     */
    @Test
    public void testGetPadded() {
        TaggingPresetsTest.waitForIconLoading(TaggingPresets.getTaggingPresets());

        final EnumSet<GetPaddedOptions> noDefault = EnumSet.of(GetPaddedOptions.NO_DEFAULT);
        final Dimension iconSize = new Dimension(16, 16);

        assertNull(ImageProvider.getPadded(new Node(), new Dimension(0, 0)));
        assertNotNull(ImageProvider.getPadded(new Node(), iconSize));
        assertNull(ImageProvider.getPadded(new Node(), iconSize, noDefault));
        assertNotNull(ImageProvider.getPadded(OsmUtils.createPrimitive("node amenity=restaurant"), iconSize, noDefault));
        assertNull(ImageProvider.getPadded(OsmUtils.createPrimitive("node barrier=hedge"), iconSize,
                EnumSet.of(GetPaddedOptions.NO_DEFAULT, GetPaddedOptions.NO_DEPRECATED)));
        assertNotNull(ImageProvider.getPadded(OsmUtils.createPrimitive("way waterway=stream"), iconSize, noDefault));
        assertNotNull(ImageProvider.getPadded(OsmUtils.createPrimitive("relation type=route route=railway"), iconSize, noDefault));
    }
}
