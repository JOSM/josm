// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.sort;

import static org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionType.Direction.NONE;
import static org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionType.Direction.ROUNDABOUT_LEFT;
import static org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionType.Direction.ROUNDABOUT_RIGHT;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionType.Direction;

final class RelationSortUtils {

    private RelationSortUtils() {
        // Hide default constructor for utils classes
    }
    
    /**
     * determine, if the way i is a roundabout and if yes, what type of roundabout
     */
    static Direction roundaboutType(RelationMember member) {
        if (member == null || !member.isWay()) return NONE;
        Way w = member.getWay();
        return roundaboutType(w);
    }

    static Direction roundaboutType(Way w) {
        if (w != null &&
                "roundabout".equals(w.get("junction")) &&
                w.getNodesCount() < 200 &&
                w.getNodesCount() > 2 &&
                w.getNode(0) != null &&
                w.getNode(1) != null &&
                w.getNode(2) != null &&
                w.firstNode() == w.lastNode()) {
            /** do some simple determinant / cross pruduct test on the first 3 nodes
                to see, if the roundabout goes clock wise or ccw */
            EastNorth en1 = w.getNode(0).getEastNorth();
            EastNorth en2 = w.getNode(1).getEastNorth();
            EastNorth en3 = w.getNode(2).getEastNorth();
            if (en1 != null && en2 != null && en3 != null) {
                en1 = en1.sub(en2);
                en2 = en2.sub(en3);
                return en1.north() * en2.east() - en2.north() * en1.east() > 0 ? ROUNDABOUT_LEFT : ROUNDABOUT_RIGHT;
            }
        }
        return NONE;
    }

    static boolean isBackward(final RelationMember member){
        return member.getRole().equals("backward");
    }

    static boolean isForward(final RelationMember member){
        return member.getRole().equals("forward");
    }

    static boolean isOneway(final RelationMember member){
        return isForward(member) || isBackward(member);
    }

}
