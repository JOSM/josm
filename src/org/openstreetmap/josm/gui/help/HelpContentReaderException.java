// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.help;

/**
 * Exception thrown when a problem occurs during help contents fetching.
 * @since 2308
 */
public class HelpContentReaderException extends Exception {

    private final int responseCode;

    /**
     * Constructs a new {@code HelpContentReaderException}.
     * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
     * @param responseCode HTTP response code related to the wiki access exception (0 if not applicable)
     */
    public HelpContentReaderException(String message, int responseCode) {
        super(message);
        this.responseCode = responseCode;
    }

    /**
     * Constructs a new {@code HelpContentReaderException}.
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     *        (A <code>null</code> value is permitted, and indicates that the cause is nonexistent or unknown.)
     * @param responseCode HTTP response code related to the wiki access exception (0 if not applicable)
     */
    public HelpContentReaderException(Throwable cause, int responseCode) {
        super(cause);
        this.responseCode = responseCode;
    }

    /**
     * Replies the HTTP response code related to the wiki access exception.
     * If no HTTP response code is available, 0 is replied.
     *
     * @return the http response code
     */
    public final int getResponseCode() {
        return responseCode;
    }
}
