// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

public class WayConnectionType {

    public final boolean connectedToPrevious;
    public final boolean connectedToNext;
    /** Is 1 if way has the same direction as the first way in the set of connected ways. Else it is (-1) */
    public final int direction;
    /** The WayConnectionType is invalid, if the corresponding primitive is not a way or the way is incomplete */
    public final boolean invalid;

    public WayConnectionType(boolean connectedToPrevious, boolean connectedToNext, int direction) {
        this.connectedToPrevious = connectedToPrevious;
        this.connectedToNext = connectedToNext;
        this.direction = direction;
        invalid = false;
    }

    public WayConnectionType() {
        connectedToPrevious = false;
        connectedToNext = false;
        direction = 1;
        invalid = true;
    }

//    @Override
//    public String toString() {
//        return ...
//    }

    public String getToolTip() {
        if (invalid) {
            return "";
        }
        else if (connectedToPrevious && connectedToNext) {
            return tr("way is connected");
        }
        else if (connectedToPrevious) {
            return tr("way is connected to previous relation member");
        }
        else if (connectedToNext) {
            return tr("way is connected to next relation member");
        }
        else {
            return tr("way is not connected to previous or next relation member");
        }
    }
}
