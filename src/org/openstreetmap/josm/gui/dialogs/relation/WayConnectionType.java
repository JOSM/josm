// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

public class WayConnectionType {

    /** True, if the corresponding primitive is not a way or the way is incomplete */
    private final boolean invalid;

    /** True, if linked to the previous / next member.  */
    public final boolean linkPrev;
    public final boolean linkNext;

    /** 
     * direction is +1 if the first node of this way is connected to the previous way 
     * and / or the last node of this way is connected to the next way. 
     * direction is -1 if it is the other way around.
     * If this way is neither connected to the previous nor to the next way, then
     * direction has the value 0.
     */
    public final int direction;

    /** True, if the element is part of a closed loop of ways. */
    public boolean isLoop;

    public WayConnectionType(boolean linkPrev, boolean linkNext, int direction) {
        this.linkPrev = linkPrev;
        this.linkNext = linkNext;
        this.isLoop = false;
        this.direction = direction;
        invalid = false;
    }

    /** construct invalid instance */
    public WayConnectionType() {
        this.linkPrev = false;
        this.linkNext = false;
        this.isLoop = false;
        this.direction = 0;
        invalid = true;
    }
    
    public boolean isValid() {
        return !invalid;    
    }

    @Override
    public String toString() {
        return "[P "+linkPrev+" ;N "+linkNext+" ;D "+direction+" ;L "+isLoop+"]";
    }

    public String getToolTip() {
        if (!isValid()) {
            return "";
        }
        else if (linkPrev && linkNext) {
            return tr("way is connected");
        }
        else if (linkPrev) {
            return tr("way is connected to previous relation member");
        }
        else if (linkNext) {
            return tr("way is connected to next relation member");
        }
        else {
            return tr("way is not connected to previous or next relation member");
        }//FIXME: isLoop & direction
    }
}
