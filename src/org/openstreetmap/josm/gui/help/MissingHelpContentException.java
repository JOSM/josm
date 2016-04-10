// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.help;

/**
 * Exception thrown when help content is missing.
 * @since 2308
 */
public class MissingHelpContentException extends HelpContentReaderException {

    /**
     * Constructs a new {@code MissingHelpContentException}.
     * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
     */
    public MissingHelpContentException(String message) {
        super(message, 0);
    }
}
