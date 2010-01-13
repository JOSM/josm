// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

public class IllegalDataException extends Exception{

    public IllegalDataException() {
        super();
    }

    public IllegalDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalDataException(String message) {
        super(message);
    }

    public IllegalDataException(Throwable cause) {
        super(cause);
    }

}
