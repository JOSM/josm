// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Performance test of {@code WireframeMapRenderer}.
 */
public class WireframeMapRendererPerformanceTest extends AbstractMapRendererPerformanceTestParent {

    @BeforeClass
    public static void load() throws Exception {
        AbstractMapRendererPerformanceTestParent.load();
    }

    @AfterClass
    public static void clean() throws Exception {
        AbstractMapRendererPerformanceTestParent.clean();
    }

    @Override
    protected Rendering buildRenderer() {
        return new WireframeMapRenderer(g, nc, false);
    }
}
