// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * Performance test of {@code WireframeMapRenderer}.
 */
class WireframeMapRendererPerformanceTest extends AbstractMapRendererPerformanceTestParent {

    @BeforeAll
    public static void load() throws Exception {
        AbstractMapRendererPerformanceTestParent.load();
    }

    @AfterAll
    public static void clean() throws Exception {
        AbstractMapRendererPerformanceTestParent.clean();
    }

    @Override
    protected Rendering buildRenderer() {
        return new WireframeMapRenderer(g, nc, false);
    }
}
