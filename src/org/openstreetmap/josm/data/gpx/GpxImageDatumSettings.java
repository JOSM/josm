// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

/**
 * Image extended exif metadata settings used by {@link GpxImageCorrelationSettings}.
 * @since 19387
 */
public class GpxImageDatumSettings {

    private final boolean setImageGpsDatum;
    private final String imageGpsDatum;
    
    /**
     * Construcs a new {@code GpxImageDatumSettings}.
     * @param setImageGpsDatum determines if images GPS datum must be set
     * @param imageGpsDatum determines the GPS coordinates datum value to be set
     */
    public GpxImageDatumSettings(
        boolean setImageGpsDatum, String imageGpsDatum) {
            this.setImageGpsDatum = setImageGpsDatum;
            this.imageGpsDatum = imageGpsDatum;
        }
    
    /**
     * Determines if image GPS datum must be set
     * @return if the GPS datum must be set
     */
    public boolean isSetImageGpsDatum() {
        return setImageGpsDatum;
    }
    
    /**
     * Return the GPS coordinates datum code.
     * @return the datum code
     */
    public String getImageGpsDatum() {
        return imageGpsDatum;
    }
}
