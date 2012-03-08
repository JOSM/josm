// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

public class DataIntegrityProblemException extends RuntimeException {

    private String htmlMessage;

    public DataIntegrityProblemException(String message) {
        super(message);
    }

    public DataIntegrityProblemException(String message, String htmlMessage) {
        super(message);
        this.htmlMessage = htmlMessage;
    }

    public String getHtmlMessage() {
        return htmlMessage;
    }
}
