// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations.fake_imagery;

import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.testutils.TileSourceRule;

import com.github.tomakehurst.wiremock.WireMockServer;

/**
 * Class defining a tile source for TileSourceRule to mock. Due to the way WireMock is designed, it is far more
 * straightforward to serve a single image in all tile positions
 *
 * Please note that this extends {@link TileSourceRule.ConstSource} for compatibility reasons.
 * {@link TileSourceRule} may be removed in the future.
 * @since xxx
 */
public abstract class ConstSource extends TileSourceRule.ConstSource {
    // This class should have the same body as {@link TileSourceRule.ConstSource}
    // For now, this class solely exists for transitioning off of JUnit 4 (the assumption is that JOSM will
    // eventually remove all JUnit 4 dependent classes, at which point the parent class will be deleted, and its
    // body moved here).

    /**
     * Get the imagery info object for the specified wiremock server (use this instead of the port-based method)
     * @param wireMockServer The wiremock server to use
     * @return The URL for the object
     */
    public ImageryInfo getImageryInfo(final WireMockServer wireMockServer) {
        // When main body is moved here, mark getImageryInfo(int port) as deprecated
        return this.getImageryInfo(wireMockServer.port());
    }
}
