// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util.imagery;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test class for {@link CameraPlane}
 */
class CameraPlaneTest {

    private static final int CAMERA_PLANE_WIDTH = 800;
    private static final int CAMERA_PLANE_HEIGHT = 600;

    private CameraPlane cameraPlane;

    @BeforeEach
    void setUp() {
        this.cameraPlane = new CameraPlane(CAMERA_PLANE_WIDTH, CAMERA_PLANE_HEIGHT);
    }

    @Test
    @Disabled("Currently broken")
    void testSetRotation() {
        Vector3D vec = new Vector3D(0, 0, 1);
        cameraPlane.setRotation(vec);
        Vector3D out = cameraPlane.getRotation();
        assertAll(() -> assertEquals(280.0830152838839, out.getRadialDistance(), 0.001),
            () -> assertEquals(0, out.getPolarAngle(), 0.001), () -> assertEquals(0, out.getAzimuthalAngle(), 0.001));
    }

    @Test
    @Disabled("Currently broken")
    void testGetVector3D() {
        Vector3D vec = new Vector3D(0, 0, 1);
        cameraPlane.setRotation(vec);
        Vector3D out = cameraPlane.getVector3D(new Point(CAMERA_PLANE_WIDTH / 2, CAMERA_PLANE_HEIGHT / 2));
        assertAll(() -> assertEquals(0.0, out.getX(), 1.0E-04), () -> assertEquals(0.0, out.getY(), 1.0E-04),
            () -> assertEquals(1.0, out.getZ(), 1.0E-04));
    }

    static Stream<Arguments> testGetVector3DFloat() {
        return Stream
            .of(Arguments.of(new Vector3D(0, 0, 1), new Point(CAMERA_PLANE_WIDTH / 2, CAMERA_PLANE_HEIGHT / 2)));
    }

    /**
     * This tests a method which does not cache, and more importantly, is what is used to create the sphere.
     * The vector is normalized.
     * (0, 0) is the center of the image
     *
     * @param expected The expected vector
     * @param toCheck The point to check
     */
    @ParameterizedTest
    @MethodSource
    void testGetVector3DFloat(final Vector3D expected, final Point toCheck) {
        Vector3D out = cameraPlane.getVector3D(toCheck.getX(), toCheck.getY());
        assertAll(() -> assertEquals(expected.getX(), out.getX(), 1.0E-04),
            () -> assertEquals(expected.getY(), out.getY(), 1.0E-04),
            () -> assertEquals(expected.getZ(), out.getZ(), 1.0E-04), () -> assertEquals(1,
                Math.sqrt(Math.pow(out.getX(), 2) + Math.pow(out.getY(), 2) + Math.pow(out.getZ(), 2)), 1.0E-04));
    }

    @Test
    @Disabled("Currently broken")
    void testMapping() {
        Vector3D vec = new Vector3D(0, 0, 1);
        cameraPlane.setRotation(vec);
        Vector3D out = cameraPlane.getVector3D(new Point(300, 200));
        Point2D map = UVMapping.getTextureCoordinate(out);
        assertAll(() -> assertEquals(0.44542099, map.getX(), 1e-8), () -> assertEquals(0.39674936, map.getY(), 1e-8));
    }
}
