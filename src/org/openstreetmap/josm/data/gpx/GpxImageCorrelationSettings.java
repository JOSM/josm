// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.util.Objects;

/**
 * Correlation settings used by {@link GpxImageCorrelation}.
 * @since 18061
 */
public class GpxImageCorrelationSettings {

    private final long offset;
    private final boolean forceTags;
    private final TimeSource imgTimeSource;
    private final GpxImageDirectionPositionSettings directionPositionSettings;
    private final GpxImageDatumSettings datumSettings;

    /**
     * Constructs a new {@code GpxImageCorrelationSettings}.
     * @param offset offset in milliseconds
     * @param forceTags force tagging of all photos, otherwise prefs are used
     */
    public GpxImageCorrelationSettings(long offset, boolean forceTags) {
        this(offset, forceTags, TimeSource.EXIFCAMTIME,
        new GpxImageDirectionPositionSettings(false, 0, false, 0, 0, 0),
        new GpxImageDatumSettings(false, null)
        );
    }

    /**
     * Constructs a new {@code GpxImageCorrelationSettings}.
     * @param offset offset in milliseconds
     * @param forceTags force tagging of all photos, otherwise prefs are used
     * @param imgTimeSource select image clock source: 
     *                      "exifCamTime" for camera internal clock
     *                      "exifGpsTime for the GPS clock of the camera
     * @param directionPositionSettings direction/position settings
     * @since 19426 @imgTimeSource was added
     */
    public GpxImageCorrelationSettings(long offset, boolean forceTags, TimeSource imgTimeSource,
            GpxImageDirectionPositionSettings directionPositionSettings) {
        this(offset, forceTags, imgTimeSource, directionPositionSettings,
        new GpxImageDatumSettings(false, null));
    }

    /**
     * Constructs a new {@code GpxImageCorrelationSettings}.
     * @param offset offset in milliseconds
     * @param forceTags force tagging of all photos, otherwise prefs are used
     * @param imgTimeSource select image clock source: 
     *                      "exifCamTime" for camera internal clock
     *                      "exifGpsTime for the GPS clock of the camera
     * @param directionPositionSettings direction/position settings
     * @param datumSettings GPS datum settings
     * @since 19387 @datumSettings was added
     * @since 19426 @imgTimeSource was added
     */
    public GpxImageCorrelationSettings(long offset, boolean forceTags, TimeSource imgTimeSource,
            GpxImageDirectionPositionSettings directionPositionSettings,
            GpxImageDatumSettings datumSettings) {
        this.offset = offset;
        this.forceTags = forceTags;
        this.imgTimeSource = imgTimeSource;
        this.directionPositionSettings = Objects.requireNonNull(directionPositionSettings);
        this.datumSettings = Objects.requireNonNull(datumSettings);
    }
    
    /**
     * Returns the offset in milliseconds.
     * @return the offset in milliseconds
     */
    public long getOffset() {
        return offset;
    }

    /**
     * Determines if tagging of all photos must be forced, otherwise prefs are used
     * @return {@code true} if tagging of all photos must be forced, otherwise prefs are used
     */
    public boolean isForceTags() {
        return forceTags;
    }

    /**
     * Return the selected image clock source, which is camera internal time, or GPS time
     * @return the clock source
     * @since 19426
     */
    public TimeSource getImgTimeSource() {
        return imgTimeSource;
    }

    /**
     * Returns the direction/position settings.
     * @return the direction/position settings
     */
    public GpxImageDirectionPositionSettings getDirectionPositionSettings() {
        return directionPositionSettings;
    }

    /**
     * Returns the EXIF metadata datum settings.
     * @return the EXIF metadata datum settings
     * @since 19387
     */
    public GpxImageDatumSettings getDatumSettings() {
        return datumSettings;
    }

    @Override
    public String toString() {
        return "[offset=" + offset + ", forceTags=" + forceTags
                + ", clock source=" + imgTimeSource
                + ", directionPositionSettings=" + directionPositionSettings
                + ", datumSettings=" + datumSettings + ']';
    }
}
