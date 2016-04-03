// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.help;

public class HelpContentReaderException extends Exception {
    private int responseCode;

    /**
     * Constructs a new {@code HelpContentReaderException}.
     * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
     */
    public HelpContentReaderException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code HelpContentReaderException}.
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     *        (A <tt>null</tt> value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public HelpContentReaderException(Throwable cause) {
        super(cause);
    }

    /**
     * Replies the HTTP response code related to the wiki access exception.
     * If no HTTP response code is available, 0 is replied.
     *
     * @return the http response code
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     * Sets the HTTP response code
     *
     * @param responseCode the response code
     */
    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }
}
