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

    /**
     * Constructs a new {@code GpxImageCorrelationSettings}.
     * @param offset offset in milliseconds
     * @param forceTags force tagging of all photos, otherwise prefs are used
     */
    public GpxImageCorrelationSettings(long offset, boolean forceTags) {
        this(offset, forceTags, new GpxImageDirectionPositionSettings(false, 0, 0, 0, 0));
    }

    /**
     * Constructs a new {@code GpxImageCorrelationSettings}.
     * @param offset offset in milliseconds
     * @param forceTags force tagging of all photos, otherwise prefs are used
     * @param directionPositionSettings direction/position settings
     */
    public GpxImageCorrelationSettings(long offset, boolean forceTags,
            GpxImageDirectionPositionSettings directionPositionSettings) {
        this.offset = offset;
        this.forceTags = forceTags;
        this.directionPositionSettings = Objects.requireNonNull(directionPositionSettings);
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

    @Override
    public String toString() {
        return "[offset=" + offset + ", forceTags=" + forceTags
                + ", directionPositionSettings=" + directionPositionSettings + ']';
    }
}
