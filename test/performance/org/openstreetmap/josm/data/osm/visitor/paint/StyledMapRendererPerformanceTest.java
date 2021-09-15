// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.testutils.annotations.FullPreferences;
import org.openstreetmap.josm.testutils.annotations.MapStyles;
import org.openstreetmap.josm.testutils.annotations.Projection;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Performance test of {@code StyledMapRenderer}.
 */
@FullPreferences
@MapStyles
@Projection
class StyledMapRendererPerformanceTest extends AbstractMapRendererPerformanceTestParent {

    @BeforeAll
    public static void load() throws Exception {
        AbstractMapRendererPerformanceTestParent.load();
        // TODO Test should have it's own copy of styles because change in style can influence performance
        MapPaintStyles.readFromPreferences();
    }

    @AfterAll
    public static void clean() throws Exception {
        AbstractMapRendererPerformanceTestParent.clean();
    }

    @Override
    protected Rendering buildRenderer() {
        return new StyledMapRenderer(g, nc, false);
    }

    /**
     * run this manually to verify that the rendering is set up properly
     * @throws IOException if any I/O error occurs
     */
    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD")
    private void dumpRenderedImage() throws IOException {
        ImageIO.write(img, "png", new File("test-neubrandenburg.png"));
    }
}
