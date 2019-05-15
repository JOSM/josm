// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.sort;

import static org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionType.Direction.NONE;
import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * A class used by the {@link RelationSorter} to store if two ways are already connected
 */
public class WayConnectionType {

    /** True, if the corresponding primitive is not a way or the way is incomplete */
    private final boolean invalid;

    /** True, if linked to the previous member.  */
    public boolean linkPrev;
    /** True, if linked to the next member.  */
    public boolean linkNext;

    /**
     * direction is FORWARD if the first node of this way is connected to the previous way
     * and / or the last node of this way is connected to the next way.
     * direction is BACKWARD if it is the other way around.
     * direction has a ONEWAY value, if it is tagged as such and it is connected
     * to the previous / next member.
     * ONEWAY_FORWARD == the first node of the oneway is the last node of the previous way
     * ONEWAY_BACKWARD == the last node of the oneway is the last node of the previous way
     * direction has a ROUNDABOUT value, if it is tagged as such and it is somehow
     * connected to the previous / next member.
     * If there is no connection to the previous or next member, then
     * direction has the value NONE.
     */
    public Direction direction;

    public enum Direction {
        FORWARD, BACKWARD, ROUNDABOUT_LEFT, ROUNDABOUT_RIGHT, NONE;

        public boolean isRoundabout() {
            return this == ROUNDABOUT_RIGHT || this == ROUNDABOUT_LEFT;
        }
    }

    /** True, if the element is part of a closed loop of ways. */
    public boolean isLoop;

    public boolean isOnewayLoopForwardPart;
    public boolean isOnewayLoopBackwardPart;
    public boolean isOnewayHead;
    public boolean isOnewayTail;

    /** False, if the way is oneway and it doesn't follow the flow of the previous member */
    public boolean onewayFollowsPrevious = true;
    /** True, if the way is oneway and it doesn't follow the flow of the next member */
    public boolean onewayFollowsNext = true;

    public WayConnectionType(boolean linkPrev, boolean linkNext, Direction direction) {
        this.linkPrev = linkPrev;
        this.linkNext = linkNext;
        this.isLoop = false;
        this.direction = direction;
        invalid = false;
    }

    public WayConnectionType(boolean invalid) {
        this.invalid = invalid;
    }

    /** construct invalid instance */
    public WayConnectionType() {
        this.linkPrev = false;
        this.linkNext = false;
        this.isLoop = false;
        this.direction = NONE;
        invalid = true;
    }

    public boolean isValid() {
        return !invalid;
    }

    @Override
    public String toString() {
        return "[P "+linkPrev+" ;N "+linkNext+" ;D "+direction+" ;L "+isLoop+
                " ;FP " + isOnewayLoopForwardPart+";BP " + isOnewayLoopBackwardPart+
                ";OH " + isOnewayHead+";OT " + isOnewayTail+']';
    }

    /**
     * Returns the tooltip to display when hovering over the relation member.
     * @return The tooltip, never null.
     * @since 10248
     */
    public String getTooltip() {
        boolean onewayGood = onewayFollowsPrevious && onewayFollowsNext;
        if (!isValid())
            return "";
        else if (linkPrev && linkNext && onewayGood)
            return tr("way is connected");
        else if (linkPrev && linkNext && !onewayGood)
            return tr("way is connected but has a wrong oneway direction");
        else if (linkPrev && onewayGood)
            return tr("way is connected to previous relation member");
        else if (linkPrev && !onewayGood)
            return tr("way is connected to previous relation member but has a wrong oneway direction");
        else if (linkNext && onewayGood)
            return tr("way is connected to next relation member");
        else if (linkNext && !onewayGood)
            return tr("way is connected to next relation member but has a wrong oneway direction");
        else
            return tr("way is not connected to previous or next relation member");
    }
}
