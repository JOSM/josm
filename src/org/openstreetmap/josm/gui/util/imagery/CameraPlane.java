// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util.imagery;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferInt;
import java.util.stream.IntStream;
import javax.annotation.Nullable;

/**
 * The plane that the camera appears on and rotates around.
 * @since 18246
 */
public class CameraPlane {
    /** The field of view for the panorama at 0 zoom */
    static final double PANORAMA_FOV = Math.toRadians(110);

    /** This determines the yaw direction. We may want to make it a config option, but maybe not */
    private static final byte YAW_DIRECTION = -1;

    /** The width of the image */
    private final int width;
    /** The height of the image */
    private final int height;

    private final Vector3D[][] vectors;
    private Vector3D rotation;

    public static final double HALF_PI = Math.PI / 2;
    public static final double TWO_PI = 2 * Math.PI;

    /**
     * Create a new CameraPlane with the default FOV (110 degrees).
     *
     * @param width The width of the image
     * @param height The height of the image
     */
    public CameraPlane(int width, int height) {
        this(width, height, (width / 2d) / Math.tan(PANORAMA_FOV / 2));
    }

    /**
     * Create a new CameraPlane
     *
     * @param width The width of the image
     * @param height The height of the image
     * @param distance The radial distance of the photosphere
     */
    private CameraPlane(int width, int height, double distance) {
        this.width = width;
        this.height = height;
        this.rotation = new Vector3D(Vector3D.VectorType.RPA, distance, 0, 0);
        this.vectors = new Vector3D[width][height];
        IntStream.range(0, this.height).parallel().forEach(y -> IntStream.range(0, this.width).parallel()
            .forEach(x -> this.vectors[x][y] = this.getVector3D((double) x, y)));
    }

    /**
     * Get the width of the image
     * @return The width of the image
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * Get the height of the image
     * @return The height of the image
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * Get the point for a vector
     *
     * @param vector the vector for which the corresponding point on the camera plane will be returned
     * @return the point on the camera plane to which the given vector is mapped, nullable
     */
    @Nullable
    public Point getPoint(final Vector3D vector) {
        final Vector3D rotatedVector = rotate(vector);
        // Currently set to false due to change in painting
        if (rotatedVector.getZ() < 0) {
            // Ignores any points "behind the back", so they don't get painted a second time on the other
            // side of the sphere
            return null;
        }
        // This is a slightly faster than just doing the (brute force) method of Math.max(Math.min)). Reduces if
        // statements by 1 per call.
        final long x = Math
            .round((rotatedVector.getX() / rotatedVector.getZ()) * this.rotation.getRadialDistance() + width / 2d);
        final long y = Math
            .round((rotatedVector.getY() / rotatedVector.getZ()) * this.rotation.getRadialDistance() + height / 2d);

        try {
            return new Point(Math.toIntExact(x), Math.toIntExact(y));
        } catch (ArithmeticException e) {
            return new Point((int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, x)),
                (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, y)));
        }
    }

    /**
     * Convert a point to a 3D vector
     *
     * @param p The point to convert
     * @return The vector
     */
    public Vector3D getVector3D(final Point p) {
        return this.getVector3D(p.x, p.y);
    }

    /**
     * Convert a point to a 3D vector (vectors are cached)
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @return The vector
     */
    public Vector3D getVector3D(final int x, final int y) {
        Vector3D res;
        try {
            res = rotate(vectors[x][y]);
        } catch (Exception e) {
            res = Vector3D.DEFAULT_VECTOR_3D;
        }
        return res;
    }

    /**
     * Convert a point to a 3D vector. Warning: This method does not cache.
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @return The vector (the middle of the image is 0, 0)
     */
    public Vector3D getVector3D(final double x, final double y) {
        return new Vector3D(x - width / 2d, y - height / 2d, this.rotation.getRadialDistance()).normalize();
    }

    /**
     * Set camera plane rotation by current plane position.
     *
     * @param p Point within current plane.
     */
    public void setRotation(final Point p) {
        setRotation(getVector3D(p));
    }

    /**
     * Set the rotation from the difference of two points
     *
     * @param from The originating point
     * @param to The new point
     */
    public void setRotationFromDelta(final Point from, final Point to) {
        // Bound check (bounds are essentially the image viewer component)
        if (from.x < 0 || from.y < 0 || to.x < 0 || to.y < 0
            || from.x > this.vectors.length || from.y > this.vectors[0].length
            || to.x > this.vectors.length || to.y > this.vectors[0].length) {
            return;
        }
        Vector3D f1 = this.vectors[from.x][from.y];
        Vector3D t1 = this.vectors[to.x][to.y];
        double deltaPolarAngle = f1.getPolarAngle() - t1.getPolarAngle();
        double deltaAzimuthalAngle = t1.getAzimuthalAngle() - f1.getAzimuthalAngle();
        double polarAngle = this.rotation.getPolarAngle() + deltaPolarAngle;
        double azimuthalAngle = this.rotation.getAzimuthalAngle() + deltaAzimuthalAngle;
        this.setRotation(azimuthalAngle, polarAngle);
    }

    /**
     * Set camera plane rotation by spherical vector.
     *
     * @param vec vector pointing new view position.
     */
    public void setRotation(Vector3D vec) {
        setRotation(vec.getPolarAngle(), vec.getAzimuthalAngle());
    }

    public Vector3D getRotation() {
        return this.rotation;
    }

    synchronized void setRotation(double azimuthalAngle, double polarAngle) {
        // Note: Something, somewhere, is switching the two.
        // FIXME: Figure out what is switching them and why
        // Prevent us from going much outside 2pi
        if (polarAngle < 0) {
            polarAngle = polarAngle + TWO_PI;
        } else if (polarAngle > TWO_PI) {
            polarAngle = polarAngle - TWO_PI;
        }
        // Avoid flipping the camera
        if (azimuthalAngle > HALF_PI) {
            azimuthalAngle = HALF_PI;
        } else if (azimuthalAngle < -HALF_PI) {
            azimuthalAngle = -HALF_PI;
        }
        this.rotation = new Vector3D(Vector3D.VectorType.RPA, this.rotation.getRadialDistance(), polarAngle, azimuthalAngle);
    }

    /**
     * Rotate a vector using the current rotation
     * @param vec The vector to rotate
     * @return A rotated vector
     */
    private Vector3D rotate(final Vector3D vec) {
        // @formatting:off
        /* Full rotation matrix for a yaw-pitch-roll
         * yaw = alpha, pitch = beta, roll = gamma (typical representations)
         * [cos(alpha), -sin(alpha), 0 ]   [cos(beta), 0, sin(beta) ]   [1,     0     ,     0      ]   [x]   [x1]
         * |sin(alpha), cos(alpha), 0  | . |0        , 1, 0         | . |0, cos(gamma), -sin(gamma)| . |y| = |y1|
         * [0         ,       0    , 1 ]   [-sin(beta), 0, cos(beta)]   [0, sin(gamma), cos(gamma) ]   [z]   [z1]
         * which becomes
         * x1 = y(cos(alpha)sin(beta)sin(gamma) - sin(alpha)cos(gamma)) + z(cos(alpha)sin(beta)cos(gamma) + sin(alpha)sin(gamma))
         *      + x cos(alpha)cos(beta)
         * y1 = y(sin(alpha)sin(beta)sin(gamma) + cos(alpha)cos(gamma)) + z(sin(alpha)sin(beta)cos(gamma) - cos(alpha)sin(gamma))
         *      + x sin(alpha)cos(beta)
         * z1 = y cos(beta)sin(gamma) + z cos(beta)cos(gamma) - x sin(beta)
         */
        // @formatting:on
        double vecX;
        double vecY;
        double vecZ;
        // We only do pitch/roll (we specifically do not do roll -- this would lead to tilting the image)
        // So yaw (alpha) -> azimuthalAngle, pitch (beta) -> polarAngle, roll (gamma) -> 0 (sin(gamma) -> 0, cos(gamma) -> 1)
        // gamma is set here just to make it slightly easier to tilt images in the future -- we just have to set the gamma somewhere else.
        // Ironically enough, the alpha (yaw) and gama (roll) got reversed somewhere. TODO figure out where and fix this.
        final int gamma = 0;
        final double sinAlpha = Math.sin(gamma);
        final double cosAlpha = Math.cos(gamma);
        final double cosGamma = this.rotation.getAzimuthalAngleCos();
        final double sinGamma = this.rotation.getAzimuthalAngleSin();
        final double cosBeta = this.rotation.getPolarAngleCos();
        final double sinBeta = this.rotation.getPolarAngleSin();
        final double x = vec.getX();
        final double y = YAW_DIRECTION * vec.getY();
        final double z = vec.getZ();
        vecX = y * (cosAlpha * sinBeta * sinGamma - sinAlpha * cosGamma)
                + z * (cosAlpha * sinBeta * cosGamma + sinAlpha * sinGamma) + x * cosAlpha * cosBeta;
        vecY = y * (sinAlpha * sinBeta * sinGamma + cosAlpha * cosGamma)
                + z * (sinAlpha * sinBeta * cosGamma - cosAlpha * sinGamma) + x * sinAlpha * cosBeta;
        vecZ = y * cosBeta * sinGamma + z * cosBeta * cosGamma - x * sinBeta;
        return new Vector3D(vecX, YAW_DIRECTION * vecY, vecZ);
    }

    public void mapping(BufferedImage sourceImage, BufferedImage targetImage) {
        DataBuffer sourceBuffer = sourceImage.getRaster().getDataBuffer();
        DataBuffer targetBuffer = targetImage.getRaster().getDataBuffer();
        // Faster mapping
        if (sourceBuffer.getDataType() == DataBuffer.TYPE_INT && targetBuffer.getDataType() == DataBuffer.TYPE_INT) {
            int[] sourceImageBuffer = ((DataBufferInt) sourceImage.getRaster().getDataBuffer()).getData();
            int[] targetImageBuffer = ((DataBufferInt) targetImage.getRaster().getDataBuffer()).getData();
            IntStream.range(0, targetImage.getHeight()).parallel()
                    .forEach(y -> IntStream.range(0, targetImage.getWidth()).forEach(x -> {
                        final Point2D.Double p = mapPoint(x, y);
                        int tx = (int) (p.x * (sourceImage.getWidth() - 1));
                        int ty = (int) (p.y * (sourceImage.getHeight() - 1));
                        int color = sourceImageBuffer[ty * sourceImage.getWidth() + tx];
                        targetImageBuffer[y * targetImage.getWidth() + x] = color;
                    }));
        } else if (sourceBuffer.getDataType() == DataBuffer.TYPE_DOUBLE && targetBuffer.getDataType() == DataBuffer.TYPE_DOUBLE) {
            double[] sourceImageBuffer = ((DataBufferDouble) sourceImage.getRaster().getDataBuffer()).getData();
            double[] targetImageBuffer = ((DataBufferDouble) targetImage.getRaster().getDataBuffer()).getData();
            IntStream.range(0, targetImage.getHeight()).parallel()
                    .forEach(y -> IntStream.range(0, targetImage.getWidth()).forEach(x -> {
                        final Point2D.Double p = mapPoint(x, y);
                        int tx = (int) (p.x * (sourceImage.getWidth() - 1));
                        int ty = (int) (p.y * (sourceImage.getHeight() - 1));
                        double color = sourceImageBuffer[ty * sourceImage.getWidth() + tx];
                        targetImageBuffer[y * targetImage.getWidth() + x] = color;
                    }));
        } else {
            IntStream.range(0, targetImage.getHeight()).parallel()
                .forEach(y -> IntStream.range(0, targetImage.getWidth()).parallel().forEach(x -> {
                    final Point2D.Double p = mapPoint(x, y);
                    targetImage.setRGB(x, y, sourceImage.getRGB((int) (p.x * (sourceImage.getWidth() - 1)),
                        (int) (p.y * (sourceImage.getHeight() - 1))));
                }));
        }
    }

    /**
     * Map a real point to the displayed point. This method uses cached vectors.
     * @param x The original x coordinate
     * @param y The original y coordinate
     * @return The scaled (0-1) point in the image. Use {@code p.x * (image.getWidth() - 1)} or {@code p.y * image.getHeight() - 1}.
     */
    public final Point2D.Double mapPoint(final int x, final int y) {
        final Vector3D vec = getVector3D(x, y);
        return UVMapping.getTextureCoordinate(vec);
    }

    /**
     * Map a real point to the displayed point. This function does not use cached vectors.
     * @param x The original x coordinate
     * @param y The original y coordinate
     * @return The scaled (0-1) point in the image. Use {@code p.x * (image.getWidth() - 1)} or {@code p.y * image.getHeight() - 1}.
     */
    public final Point2D.Double mapPoint(final double x, final double y) {
        final Vector3D vec = getVector3D(x, y);
        return UVMapping.getTextureCoordinate(vec);
    }
}
