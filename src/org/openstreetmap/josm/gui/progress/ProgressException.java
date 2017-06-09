// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.progress;

/**
 * An exception that is thrown by the progress monitor if something went wrong
 */
public class ProgressException extends RuntimeException {

    /**
     * Create a new {@link ProgressException}
     * @param message The message
     * @param args The arguments for the message string
     * @see String#format
     */
    public ProgressException(String message, Object... args) {
        super(String.format(message, args));
    }

}
