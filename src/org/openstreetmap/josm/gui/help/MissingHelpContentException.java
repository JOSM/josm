// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.help;

public class MissingHelpContentException extends HelpContentReaderException {

    public MissingHelpContentException() {
        super();
    }

    public MissingHelpContentException(String message, Throwable cause) {
        super(message, cause);
    }

    public MissingHelpContentException(String message) {
        super(message);
    }

    public MissingHelpContentException(Throwable cause) {
        super(cause);
    }
}
