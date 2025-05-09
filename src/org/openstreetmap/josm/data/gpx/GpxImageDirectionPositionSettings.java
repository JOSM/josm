// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

/**
 * Image direction / position modification settings used by {@link GpxImageCorrelationSettings}.
 * @since 18061
 */
public class GpxImageDirectionPositionSettings {

    private final boolean setImageDirection;
    private final double imageDirectionAngleOffset;
    private final boolean setGpxTrackDirection;
    private final double shiftImageX;
    private final double shiftImageY;
    private final double elevationShift;

    /**
     * Constructs a new {@code GpxImageDirectionPositionSettings}.
     * @param setImageDirection determines if image direction must be set towards the next GPX waypoint
     * @param imageDirectionAngleOffset direction angle offset in degrees
     * @param setGpxTrackDirection determines if image course direction must be set
     * @param shiftImageX image shift on X axis relative to the direction in meters
     * @param shiftImageY image shift on Y axis relative to the direction in meters
     * @param elevationShift image elevation shift in meters
     * @since 19387 @setGpsTrackDirection was added
     */
    public GpxImageDirectionPositionSettings(
            boolean setImageDirection, double imageDirectionAngleOffset, boolean setGpxTrackDirection, 
            double shiftImageX, double shiftImageY, double elevationShift) {
        this.setImageDirection = setImageDirection;
        this.imageDirectionAngleOffset = imageDirectionAngleOffset;
        this.setGpxTrackDirection = setGpxTrackDirection;
        this.shiftImageX = shiftImageX;
        this.shiftImageY = shiftImageY;
        this.elevationShift = elevationShift;
    }

    /**
     * Determines if image direction must be set towards the next GPX waypoint
     * @return {@code true} if image direction must be set towards the next GPX waypoint
     */
    public boolean isSetImageDirection() {
        return setImageDirection;
    }

    /**
     * Returns direction angle offset in degrees
     * @return direction angle offset in degrees
     */
    public double getImageDirectionAngleOffset() {
        return imageDirectionAngleOffset;
    }

    /**
     * Determines if GPS course direction must be set. Angle value is from the gpx trace.
     * @return {@code true} if image GPS course must be set
     * @since 19387
     */
    public boolean isSetGpxTrackDirection() {
        return setGpxTrackDirection;
    }

    /**
     * Returns image shift on X axis relative to the direction in meters
     * @return image shift on X axis relative to the direction in meters
     */
    public double getShiftImageX() {
        return shiftImageX;
    }

    /**
     * Returns image shift on Y axis relative to the direction in meters
     * @return image shift on Y axis relative to the direction in meters
     */
    public double getShiftImageY() {
        return shiftImageY;
    }

    /**
     * Returns image elevation shift in meters
     * @return image elevation shift in meters
     */
    public double getElevationShift() {
        return elevationShift;
    }

    @Override
    public String toString() {
        return "[setImageDirection=" + setImageDirection
                + ", imageDirectionAngleOffset=" + imageDirectionAngleOffset
                + ", setImageGpxTrackDirection=" + setGpxTrackDirection
                + ", shiftImageX=" + shiftImageX
                + ", shiftImageY=" + shiftImageY
                + ", elevationShift=" + elevationShift + ']';
    }
}
