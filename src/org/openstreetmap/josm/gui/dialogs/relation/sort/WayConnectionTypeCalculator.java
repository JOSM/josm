// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.sort;

import static org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionType.Direction.BACKWARD;
import static org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionType.Direction.FORWARD;
import static org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionType.Direction.NONE;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionType.Direction;
import org.openstreetmap.josm.tools.StreamUtils;
import org.openstreetmap.josm.tools.bugreport.BugReport;

/**
 * This class calculates the {@link WayConnectionType} for a given list of members
 */
public class WayConnectionTypeCalculator {

    private static final int UNCONNECTED = Integer.MIN_VALUE;

    private List<RelationMember> members;

    /**
     * refresh the cache of member WayConnectionTypes
     * @param members relation members
     * @return way connections
     */
    public List<WayConnectionType> updateLinks(List<RelationMember> members) {
        return updateLinks(null, members);
    }

    /**
     * refresh the cache of member WayConnectionTypes
     * @param r relation. Can be null, for plugins compatibility, but really shouldn't
     * @param members relation members
     * @return way connections
     * @since 15696
     */
    public List<WayConnectionType> updateLinks(Relation r, List<RelationMember> members) {
        this.members = members;
        final List<WayConnectionType> con = members.stream()
                .map(ignore -> (WayConnectionType) null)
                .collect(Collectors.toList());

        firstGroupIdx = 0;

        lastForwardWay = UNCONNECTED;
        lastBackwardWay = UNCONNECTED;
        onewayBeginning = false;
        WayConnectionType lastWct = null;

        for (int i = 0; i < members.size(); ++i) {
            try {
                lastWct = updateLinksFor(r, con, lastWct, i);
            } catch (RuntimeException e) {
                int index = i;
                throw BugReport.intercept(e).put("i", i).put("member", () -> members.get(index)).put("con", con)
                    .put("members", members).put("lastWct", lastWct).put("firstGroupIdx", firstGroupIdx);
            }
        }
        if (!isSuperRoute(r)) {
            makeLoopIfNeeded(con, members.size() - 1);
        }

        return con;
    }

    private WayConnectionType updateLinksFor(Relation r, List<WayConnectionType> con, WayConnectionType lastWct, int i) {
        final RelationMember m = members.get(i);
        if (isSuperRoute(r)) {
            final WayConnectionType wct;
            if (i == 0) {
                wct = new WayConnectionType(false);
            } else if (!members.get(i).isRelation() || !members.get(i - 1).isRelation()) {
                wct = new WayConnectionType(true);
            } else {
                final List<RelationMember> previousMembers = members.get(i - 1).getRelation().getMembers();
                final Way previousLastWay = StreamUtils.reversedStream(previousMembers)
                        .filter(x -> x.isWay() && !x.hasRole())
                        .map(RelationMember::getWay)
                        .findFirst().orElse(null);
                final Way currentFirstWay = m.getRelation().getMembers().stream()
                        .filter(x -> x.isWay() && !x.hasRole())
                        .map(RelationMember::getWay)
                        .findFirst().orElse(null);
                final boolean isConnected = isConnected(previousLastWay, currentFirstWay);
                wct = new WayConnectionType(false);
                lastWct.linkNext = wct.linkPrev = isConnected;
            }
            con.set(i, wct);
            return wct;
        } else if (isNoHandleableWay(m)) {
            if (i > 0) {
                makeLoopIfNeeded(con, i-1);
            }
            con.set(i, new WayConnectionType());
            firstGroupIdx = i;
        } else {
            WayConnectionType wct = computeNextWayConnection(r, con, lastWct, i, m);

            if (!wct.linkPrev) {
                if (i > 0) {
                    makeLoopIfNeeded(con, i-1);
                }
                firstGroupIdx = i;
            }
            return wct;
        }
        return lastWct;
    }

    private static boolean isNoHandleableWay(final RelationMember m) {
        return !m.isWay() || m.getWay() == null || m.getWay().isIncomplete();
    }

    private WayConnectionType computeNextWayConnection(Relation r, List<WayConnectionType> con, WayConnectionType lastWct, int i,
            final RelationMember m) {
        WayConnectionType wct = new WayConnectionType(false);
        wct.linkPrev = i > 0 && con.get(i-1) != null && con.get(i-1).isValid();
        wct.direction = NONE;
        wct.ignoreOneway = isOnewayIgnored(r);

        if (!wct.ignoreOneway && RelationSortUtils.isOneway(m)) {
            handleOneway(lastWct, i, wct);
        }

        if (wct.linkPrev) {
            if (lastBackwardWay != UNCONNECTED && lastForwardWay != UNCONNECTED) {
                determineOnewayConnectionType(con, m, i, wct);
                if (!wct.linkPrev) {
                    firstGroupIdx = i;
                }
            }

            if (lastWct != null && !RelationSortUtils.isOneway(m)) {
                wct.direction = determineDirection(i-1, lastWct.direction, i);
                wct.linkPrev = wct.direction != NONE;
            }
        }

        if (!wct.linkPrev) {
            wct.direction = determineDirectionOfFirst(i, m, false);
            if (RelationSortUtils.isOneway(m)) {
                wct.isOnewayLoopForwardPart = true;
                lastForwardWay = i;
            }
        }

        wct.linkNext = false;
        if (lastWct != null) {
            lastWct.linkNext = wct.linkPrev;
        }

        if (!wct.ignoreOneway) {
            handleOnewayFollows(lastWct, i, m, wct);
        }
        con.set(i, wct);
        return wct;
    }

    private boolean isSuperRoute(Relation r) {
        return r != null && r.hasTag("type", "superroute");
    }

    private static boolean isOnewayIgnored(Relation r) {
        return r != null && r.hasTag("type", "boundary", "multipolygon");
    }

    protected void handleOnewayFollows(WayConnectionType lastWct, int i, final RelationMember m,
            WayConnectionType wct) {
        if (lastWct != null && i > 0 && m.getMember() instanceof Way && members.get(i - 1).getMember() instanceof Way
                && (m.getWay().isOneway() != 0 || members.get(i - 1).getWay().isOneway() != 0)) {
            Way way = m.getWay();
            Way previousWay = members.get(i - 1).getWay();
            if (way.isOneway() != 0 && previousWay.isOneway() != 0 &&
                    (way.firstNode(true) != previousWay.lastNode(true) &&
                        way.lastNode(true) != previousWay.firstNode(true))) {
                wct.onewayFollowsPrevious = false;
                lastWct.onewayFollowsNext = false;
            } else if (way.isOneway() != 0 && previousWay.isOneway() == 0 &&
                    previousWay.isFirstLastNode(way.lastNode(true))) {
                wct.onewayFollowsPrevious = false;
            }
        }
    }

    private void handleOneway(WayConnectionType lastWct, int i, WayConnectionType wct) {
        if (lastWct != null && lastWct.isOnewayTail) {
            wct.isOnewayHead = true;
        }
        if (lastBackwardWay == UNCONNECTED && lastForwardWay == UNCONNECTED) { //Beginning of new oneway
            wct.isOnewayHead = true;
            lastForwardWay = i-1;
            lastBackwardWay = i-1;
            onewayBeginning = true;
        }
    }

    private int firstGroupIdx;
    private void makeLoopIfNeeded(final List<WayConnectionType> con, final int i) {
        boolean loop = false;
        if (i == firstGroupIdx) { //is primitive loop
            loop = determineDirection(i, FORWARD, i) == FORWARD;
        } else if (i >= 0) {
            loop = determineDirection(i, con.get(i).direction, firstGroupIdx) == con.get(firstGroupIdx).direction;
        }
        if (loop) {
            for (int j = firstGroupIdx; j <= i; ++j) {
                con.get(j).isLoop = true;
            }
        }
    }

    private Direction determineDirectionOfFirst(final int i, final RelationMember m, boolean reversed) {
        Direction result = RelationSortUtils.roundaboutType(m);
        if (result != NONE)
            return result;

        if (RelationSortUtils.isOneway(m)) {
            if (RelationSortUtils.isBackward(m) != reversed) return BACKWARD;
            else return FORWARD;
        } else { /** guess the direction and see if it fits with the next member */
            if (determineDirection(i, FORWARD, i+1) != NONE) return FORWARD;
            if (determineDirection(i, BACKWARD, i+1) != NONE) return BACKWARD;
        }
        return NONE;
    }

    private int lastForwardWay;
    private int lastBackwardWay;
    private boolean onewayBeginning;

    private void determineOnewayConnectionType(final List<WayConnectionType> con,
            RelationMember m, int i, final WayConnectionType wct) {
        Direction dirFW = determineDirection(lastForwardWay, con.get(lastForwardWay).direction, i);
        Direction dirBW;
        if (onewayBeginning) {
            if (lastBackwardWay < 0) {
                dirBW = determineDirection(firstGroupIdx, reverse(con.get(firstGroupIdx).direction), i, true);
            } else {
                dirBW = determineDirection(lastBackwardWay, con.get(lastBackwardWay).direction, i, true);
            }

            // Support split-start routes. When the current way does
            // not fit as forward or backward and we have no backward
            // ways yet (onewayBeginning) and the most recent oneway
            // head starts a new segment (!linkPrev), instead of
            // disconnecting the current way, make it the start of the
            // backward route. To render properly, unset isOnewayHead on
            // the most recent head (since the current backward way does
            // no longer start there).
            if (dirFW == NONE && dirBW == NONE && RelationSortUtils.isOneway(m) && !wct.isOnewayHead) {
                WayConnectionType prevHead = null;
                for (int j = i - 1; j >= 0; --j) {
                    if (con.get(j).isOnewayHead) {
                        prevHead = con.get(j);
                        break;
                    }
                }

                if (prevHead != null && !prevHead.linkPrev) {
                    dirBW = determineDirectionOfFirst(i, m, true);
                    prevHead.isOnewayHead = false;
                }
            }

            if (dirBW != NONE) {
                onewayBeginning = false;
            }
        } else {
            dirBW = determineDirection(lastBackwardWay, con.get(lastBackwardWay).direction, i, true);
        }

        if (RelationSortUtils.isOneway(m)) {
            if (dirBW != NONE) {
                wct.direction = dirBW;
                lastBackwardWay = i;
                wct.isOnewayLoopBackwardPart = true;
            }
            if (dirFW != NONE) {
                wct.direction = dirFW;
                lastForwardWay = i;
                wct.isOnewayLoopForwardPart = true;
            }
            // Not connected to previous
            if (dirFW == NONE && dirBW == NONE) {
                wct.linkPrev = false;
                wct.isOnewayHead = true;
                lastForwardWay = i-1;
                lastBackwardWay = i-1;
                onewayBeginning = true;
            }

            if (dirFW != NONE && dirBW != NONE) { //End of oneway loop
                if (i+1 < members.size() && determineDirection(i, dirFW, i+1) != NONE) {
                    wct.isOnewayLoopBackwardPart = false;
                    wct.direction = dirFW;
                } else {
                    wct.isOnewayLoopForwardPart = false;
                    wct.direction = dirBW;
                }

                wct.isOnewayTail = true;
            }

        } else {
            lastForwardWay = UNCONNECTED;
            lastBackwardWay = UNCONNECTED;
            if (dirFW == NONE || dirBW == NONE) {
                wct.linkPrev = false;
            }
        }
    }

    private static Direction reverse(final Direction dir) {
        if (dir == FORWARD) return BACKWARD;
        if (dir == BACKWARD) return FORWARD;
        return dir;
    }

    private Direction determineDirection(int refI, Direction refDirection, int k) {
        return determineDirection(refI, refDirection, k, false);
    }

    /**
     * Determines the direction of way {@code k} with respect to the way {@code ref_i}.
     * The way {@code ref_i} is assumed to have the direction {@code ref_direction} and to be the predecessor of {@code k}.
     *
     * If both ways are not linked in any way, NONE is returned.
     *
     * Else the direction is given as follows:
     * Let the relation be a route of oneway streets, and someone travels them in the given order.
     * Direction is FORWARD if it is legal and BACKWARD if it is illegal to do so for the given way.
     * @param refI way key
     * @param refDirection direction of ref_i
     * @param k successor of ref_i
     * @param reversed if {@code true} determine reverse direction
     * @return direction of way {@code k}
     */
    private Direction determineDirection(int refI, final Direction refDirection, int k, boolean reversed) {
        if (members == null || refI < 0 || k < 0 || refI >= members.size() || k >= members.size() || refDirection == NONE)
            return NONE;

        final RelationMember mRef = members.get(refI);
        final RelationMember m = members.get(k);
        Way wayRef = null;
        Way way = null;

        if (mRef.isWay()) {
            wayRef = mRef.getWay();
        }
        if (m.isWay()) {
            way = m.getWay();
        }

        if (wayRef == null || way == null)
            return NONE;

        /** the list of nodes the way k can dock to */
        List<Node> refNodes = new ArrayList<>();

        switch (refDirection) {
        case FORWARD:
            refNodes.add(wayRef.lastNode());
            break;
        case BACKWARD:
            refNodes.add(wayRef.firstNode());
            break;
        case ROUNDABOUT_LEFT:
        case ROUNDABOUT_RIGHT:
            refNodes = wayRef.getNodes();
            break;
        default: // Do nothing
        }

        for (Node n : refNodes) {
            if (n == null) {
                continue;
            }
            if (RelationSortUtils.roundaboutType(members.get(k)) != NONE) {
                for (Node nn : way.getNodes()) {
                    if (n == nn)
                        return RelationSortUtils.roundaboutType(members.get(k));
                }
            } else if (RelationSortUtils.isOneway(m)) {
                if (n == RelationNodeMap.firstOnewayNode(m) && !reversed) {
                    if (RelationSortUtils.isBackward(m))
                        return BACKWARD;
                    else
                        return FORWARD;
                }
                if (reversed && n == RelationNodeMap.lastOnewayNode(m)) {
                    if (RelationSortUtils.isBackward(m))
                        return FORWARD;
                    else
                        return BACKWARD;
                }
            } else {
                if (n == way.firstNode())
                    return FORWARD;
                if (n == way.lastNode())
                    return BACKWARD;
            }
        }
        return NONE;
    }

    /**
     * Free resources.
     */
    public void clear() {
        members = null;
    }

    private boolean isConnected(Way way1, Way way2) {
        return way1 != null && way2 != null && way1.isUsable() && way2.isUsable()
                && (way1.isFirstLastNode(way2.firstNode()) || way1.isFirstLastNode(way2.lastNode()));
    }
}
