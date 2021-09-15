// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Performance test of {@code WireframeMapRenderer}.
 */
@Projection
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
