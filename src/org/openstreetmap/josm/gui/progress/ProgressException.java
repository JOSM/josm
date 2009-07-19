// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.progress;

public class ProgressException extends RuntimeException {

    public ProgressException(String message, Object... args) {
        super(String.format(message, args));
    }

}
