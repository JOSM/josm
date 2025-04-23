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
    private final GpxImageDirectionPositionSettings directionPositionSettings;
    private final GpxImageDatumSettings datumSettings;

    /**
     * Constructs a new {@code GpxImageCorrelationSettings}.
     * @param offset offset in milliseconds
     * @param forceTags force tagging of all photos, otherwise prefs are used
     */
    public GpxImageCorrelationSettings(long offset, boolean forceTags) {
        this(offset, forceTags,
        new GpxImageDirectionPositionSettings(false, 0, false, 0, 0, 0),
        new GpxImageDatumSettings(false, null)
        );
    }

    /**
     * Constructs a new {@code GpxImageCorrelationSettings}.
     * @param offset offset in milliseconds
     * @param forceTags force tagging of all photos, otherwise prefs are used
     * @param directionPositionSettings direction/position settings
     */
    public GpxImageCorrelationSettings(long offset, boolean forceTags,
            GpxImageDirectionPositionSettings directionPositionSettings) {
        this(offset, forceTags, directionPositionSettings,
        new GpxImageDatumSettings(false, null));
    }

    /**
     * Constructs a new {@code GpxImageCorrelationSettings}.
     * @param offset offset in milliseconds
     * @param forceTags force tagging of all photos, otherwise prefs are used
     * @param directionPositionSettings direction/position settings
     * @param datumSettings GPS datum settings
     * @since 19387 @datumSettings was added
     */
    public GpxImageCorrelationSettings(long offset, boolean forceTags,
            GpxImageDirectionPositionSettings directionPositionSettings,
            GpxImageDatumSettings datumSettings) {
        this.offset = offset;
        this.forceTags = forceTags;
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
                + ", directionPositionSettings=" + directionPositionSettings
                + ", datumSettings=" + datumSettings + ']';
    }
}
