// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

/**
 * A link to an external resource (Web page, digital photo, video clip, etc) with additional information.
 * @since 444
 */
public class GpxLink {

    /** External resource URI */
    public String uri;

    /** Text to display on the hyperlink */
    public String text;

    /** Link type */
    public String type;

    /**
     * Constructs a new {@code GpxLink}.
     * @param uri External resource URI
     */
    public GpxLink(String uri) {
        this.uri = uri;
    }
}
