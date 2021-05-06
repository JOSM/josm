// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.sort;

import static org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionType.Direction.NONE;
import static org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionType.Direction.ROUNDABOUT_LEFT;
import static org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionType.Direction.ROUNDABOUT_RIGHT;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IRelationMember;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionType.Direction;

/**
 * Utility classes for the {@link RelationSorter}.
 */
final class RelationSortUtils {

    private RelationSortUtils() {
        // Hide default constructor for utils classes
    }

    /**
     * determine, if the way i is a roundabout and if yes, what type of roundabout
     * @param member relation member
     * @return roundabout type
     * @since 17862 (generics)
     */
    static Direction roundaboutType(IRelationMember<?> member) {
        if (member == null || !member.isWay()) return NONE;
        return roundaboutType((IWay<?>) member.getWay());
    }

    /**
     * Check if a way is a roundabout type
     * @param w The way to check
     * @param <W> The way type
     * @return The roundabout type
     * @since 17862 (generics)
     */
    static <W extends IWay<?>> Direction roundaboutType(W w) {
        if (w != null && w.hasTag("junction", "circular", "roundabout")) {
            int nodesCount = w.getNodesCount();
            if (nodesCount > 2 && nodesCount < 200) {
                INode n1 = w.getNode(0);
                INode n2 = w.getNode(1);
                INode n3 = w.getNode(2);
                if (n1 != null && n2 != null && n3 != null && w.isClosed()) {
                    /** do some simple determinant / cross product test on the first 3 nodes
                        to see, if the roundabout goes clock wise or ccw */
                    EastNorth en1 = n1.getEastNorth();
                    EastNorth en2 = n2.getEastNorth();
                    EastNorth en3 = n3.getEastNorth();
                    if (en1 != null && en2 != null && en3 != null) {
                        en1 = en2.subtract(en1);
                        en2 = en3.subtract(en2);
                        return en1.north() * en2.east() - en2.north() * en1.east() > 0 ? ROUNDABOUT_LEFT : ROUNDABOUT_RIGHT;
                    }
                }
            }
        }
        return NONE;
    }

    static boolean isBackward(final IRelationMember<?> member) {
        return "backward".equals(member.getRole());
    }

    static boolean isForward(final IRelationMember<?> member) {
        return "forward".equals(member.getRole());
    }

    static boolean isOneway(final IRelationMember<?> member) {
        return isForward(member) || isBackward(member);
    }
}
