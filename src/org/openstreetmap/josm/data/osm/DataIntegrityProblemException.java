// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

public class DataIntegrityProblemException extends RuntimeException {

    private final String htmlMessage;

    public DataIntegrityProblemException(String message) {
        this(message, null);
    }

    public DataIntegrityProblemException(String message, String htmlMessage) {
        super(message);
        this.htmlMessage = htmlMessage;
    }

    public String getHtmlMessage() {
        return htmlMessage;
    }
}
