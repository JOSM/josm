// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery.street_level;

/**
 * Projections for street level imagery
 * @author Taylor Smock
 * @since 18246
 */
public enum Projections {
    /** This is the image type from most cameras */
    PERSPECTIVE(1),
    /** This will probably not be seen often in JOSM, but someone might have a synchronized pair of fisheye camers */
    FISHEYE(1),
    /** 360 imagery using the equirectangular method (single image) */
    EQUIRECTANGULAR(1),
    /** 360 imagery using a cube map */
    CUBE_MAP(6),
    // Additional known projections: Equi-Angular Cubemap (EAC) from Google and the Pyramid format from Facebook
    // Neither are particularly well-documented at this point, although I believe the Pyramid format uses 30 images.
    /** In the event that we have no clue what the projection should be. Defaults to perspective viewing. */
    UNKNOWN(Integer.MAX_VALUE);

    private final int expectedImages;

    /**
     * Create a new Projections enum
     * @param expectedImages The maximum images for the projection type
     */
    Projections(final int expectedImages) {
        this.expectedImages = expectedImages;
    }

    /**
     * Get the maximum number of expected images for the projection
     * @return The number of expected images
     */
    public int getExpectedImages() {
        return this.expectedImages;
    }
}
