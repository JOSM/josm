// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import org.openstreetmap.josm.data.osm.Changeset;

public class OsmChangesetCloseException extends OsmTransferException {
    private Changeset changeset;

    public OsmChangesetCloseException() {
        super();
        // TODO Auto-generated constructor stub
    }

    public OsmChangesetCloseException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

    public OsmChangesetCloseException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    public OsmChangesetCloseException(Throwable cause) {
        super(cause);
        // TODO Auto-generated constructor stub
    }

    public Changeset getChangeset() {
        return changeset;
    }

    public void setChangeset(Changeset changeset) {
        this.changeset = changeset;
    }
}
