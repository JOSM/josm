// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.BeforeClass;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;

/**
 * Performance test of {@code StyledMapRenderer}.
 */
public class StyledMapRendererPerformanceTest extends AbstractMapRendererPerformanceTestParent {

    @BeforeClass
    public static void load() throws Exception {
        AbstractMapRendererPerformanceTestParent.load();
        // TODO Test should have it's own copy of styles because change in style can influence performance
        MapPaintStyles.readFromPreferences();
    }

    @Override
    protected Rendering buildRenderer() {
        return new StyledMapRenderer(g, nc, false);
    }

    /** run this manually to verify that the rendering is set up properly */
    private void dumpRenderedImage() throws IOException {
        File outputfile = new File("test-neubrandenburg.png");
        ImageIO.write(img, "png", outputfile);
    }
}
