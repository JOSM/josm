// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

public class OsmApiInitializationException extends OsmTransferException {

    public OsmApiInitializationException() {
        super();
    }

    public OsmApiInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public OsmApiInitializationException(String message) {
        super(message);
    }

    public OsmApiInitializationException(Throwable cause) {
        super(cause);
    }
}
