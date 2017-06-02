// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

/**
 * Exception thrown when a primitive or data set does not pass its integrity checks.
 * @since 2399
 */
public class DataIntegrityProblemException extends RuntimeException {

    private final String htmlMessage;

    /**
     * Constructs a new {@code DataIntegrityProblemException}.
     * @param message the detail message
     */
    public DataIntegrityProblemException(String message) {
        this(message, null);
    }

    /**
     * Constructs a new {@code DataIntegrityProblemException}.
     * @param message the detail message
     * @param htmlMessage HTML-formatted error message. Can be null
     */
    public DataIntegrityProblemException(String message, String htmlMessage) {
        super(message);
        this.htmlMessage = htmlMessage;
    }

    /**
     * Returns the HTML-formatted error message.
     * @return the HTML-formatted error message, or null
     */
    public String getHtmlMessage() {
        return htmlMessage;
    }
}
