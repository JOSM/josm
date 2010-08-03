// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.properties;

public class OperationCancelledException extends Exception {

    public OperationCancelledException() {
        super();
    }

    public OperationCancelledException(String message, Throwable cause) {
        super(message, cause);
    }

    public OperationCancelledException(String message) {
        super(message);
    }

    public OperationCancelledException(Throwable cause) {
        super(cause);
    }

}
