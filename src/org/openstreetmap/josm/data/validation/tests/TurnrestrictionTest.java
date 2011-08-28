// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;

public class TurnrestrictionTest extends Test {

    protected static final int NO_VIA = 1801;
    protected static final int NO_FROM = 1802;
    protected static final int NO_TO = 1803;
    protected static final int MORE_VIA = 1804;
    protected static final int MORE_FROM = 1805;
    protected static final int MORE_TO = 1806;
    protected static final int UNKNOWN_ROLE = 1807;
    protected static final int UNKNOWN_TYPE = 1808;
    protected static final int FROM_VIA_NODE = 1809;
    protected static final int TO_VIA_NODE = 1810;
    protected static final int FROM_VIA_WAY = 1811;
    protected static final int TO_VIA_WAY = 1812;
    protected static final int MIX_VIA = 1813;
    protected static final int UNCONNECTED_VIA = 1814;

    public TurnrestrictionTest() {
        super(tr("Turnrestriction"), tr("This test checks if turnrestrictions are valid"));
    }

    @Override
    public void visit(Relation r) {
        if (!"restriction".equals(r.get("type")))
            return;

        Way fromWay = null;
        Way toWay = null;
        List<OsmPrimitive> via = new ArrayList<OsmPrimitive>();

        boolean morefrom = false;
        boolean moreto = false;
        boolean morevia = false;
        boolean mixvia = false;

        /* find the "from", "via" and "to" elements */
        for (RelationMember m : r.getMembers()) {
            if (m.getMember().isIncomplete())
                return;

            ArrayList<OsmPrimitive> l = new ArrayList<OsmPrimitive>();
            l.add(r);
            l.add(m.getMember());
            if (m.isWay()) {
                Way w = m.getWay();
                if (w.getNodesCount() < 2) {
                    continue;
                }

                if ("from".equals(m.getRole())) {
                    if (fromWay != null) {
                        morefrom = true;
                    } else {
                        fromWay = w;
                    }
                } else if ("to".equals(m.getRole())) {
                    if (toWay != null) {
                        moreto = true;
                    } else {
                        toWay = w;
                    }
                } else if ("via".equals(m.getRole())) {
                    if (!via.isEmpty() && via.get(0) instanceof Node) {
                        mixvia = true;
                    } else {
                        via.add(w);
                    }
                } else {
                    errors.add(new TestError(this, Severity.WARNING, tr("Unknown role"), UNKNOWN_ROLE,
                            l, Collections.singletonList(m)));
                }
            } else if (m.isNode()) {
                Node n = m.getNode();
                if ("via".equals(m.getRole())) {
                    if (!via.isEmpty()) {
                        if (via.get(0) instanceof Node) {
                            morevia = true;
                        } else {
                            mixvia = true;
                        }
                    } else {
                        via.add(n);
                    }
                } else {
                    errors.add(new TestError(this, Severity.WARNING, tr("Unknown role"), UNKNOWN_ROLE,
                            l, Collections.singletonList(m)));
                }
            } else {
                errors.add(new TestError(this, Severity.WARNING, tr("Unknown member type"), UNKNOWN_TYPE,
                        l, Collections.singletonList(m)));
            }
        }
        if (morefrom) {
            errors.add(new TestError(this, Severity.ERROR, tr("More than one \"from\" way found"), MORE_FROM, r));
        }
        if (moreto) {
            errors.add(new TestError(this, Severity.ERROR, tr("More than one \"to\" way found"), MORE_TO, r));
        }
        if (morevia) {
            errors.add(new TestError(this, Severity.ERROR, tr("More than one \"via\" node found"), MORE_VIA, r));
        }
        if (mixvia) {
            errors.add(new TestError(this, Severity.ERROR, tr("Cannot mix node and way for role \"via\""), MIX_VIA, r));
        }

        if (fromWay == null) {
            errors.add(new TestError(this, Severity.ERROR, tr("No \"from\" way found"), NO_FROM, r));
            return;
        }
        if (toWay == null) {
            errors.add(new TestError(this, Severity.ERROR, tr("No \"to\" way found"), NO_TO, r));
            return;
        }
        if (via.isEmpty()) {
            errors.add(new TestError(this, Severity.ERROR, tr("No \"via\" node or way found"), NO_VIA, r));
            return;
        }

        Node viaNode;
        if (via.get(0) instanceof Node) {
            viaNode = (Node) via.get(0);
            Way viaPseudoWay = new Way();
            viaPseudoWay.addNode(viaNode);
            checkIfConnected(fromWay, viaPseudoWay,
                    tr("The \"from\" way does not start or end at a \"via\" node"), FROM_VIA_NODE);
            checkIfConnected(viaPseudoWay, toWay,
                    tr("The \"to\" way does not start or end at a \"via\" node"), TO_VIA_NODE);
        } else {
            // check if consecutive ways are connected: from/via[0], via[i-1]/via[i], via[last]/to
            checkIfConnected(fromWay, (Way) via.get(0), 
                    tr("The \"from\" and the first \"via\" way are not connected."), FROM_VIA_WAY);
            if (via.size() > 1) {
                for (int i = 1; i < via.size(); i++) {
                    Way previous = (Way) via.get(i - 1);
                    Way current = (Way) via.get(i);
                    checkIfConnected(previous, current, 
                            tr("The \"via\" ways are not connected."), UNCONNECTED_VIA);
                }
            }
            checkIfConnected((Way) via.get(via.size() - 1), toWay, 
                    tr("The last \"via\" and the \"to\" way are not connected."), TO_VIA_WAY);

        }
    }

    private void checkIfConnected(Way previous, Way current, String msg, int code) {
        int onewayPrevious = isOneway(previous);
        int onewayCurrent = isOneway(current);
        Node endPrevious = onewayPrevious != -1 ? previous.lastNode() : previous.firstNode();
        Node startCurrent = onewayCurrent != -1 ? current.firstNode() : current.lastNode();
        //System.out.println(previous.getUniqueId() + " -- " + current.getUniqueId() + ": " + onewayPrevious + "/" + onewayCurrent + " " + endPrevious.getUniqueId() + "/" + startCurrent.getUniqueId());
        boolean c;
        if (onewayPrevious != 0 && onewayCurrent != 0) {
            // both oneways: end/start node must be equal
            c = endPrevious.equals(startCurrent);
        } else if (onewayPrevious != 0) {
            // previous way is oneway: end of previous must be start/end of current
            c = current.isFirstLastNode(endPrevious);
        } else if (onewayCurrent != 0) {
            // current way is oneway: start of current must be start/end of previous
            c = previous.isFirstLastNode(startCurrent);
        } else {
            // otherwise: start/end of previous must be start/end of current
            c = current.isFirstLastNode(previous.firstNode()) || current.isFirstLastNode(previous.lastNode());
        }
        if (!c) {
            errors.add(new TestError(this, Severity.ERROR, msg, code, Arrays.asList(previous, current)));
        }
    }

    private static int isOneway(Way w) {
        String onewayviastr = w.get("oneway");
        if (onewayviastr != null) {
            if ("-1".equals(onewayviastr)) {
                return -1;
            } else {
                Boolean onewayvia = OsmUtils.getOsmBoolean(onewayviastr);
                if (onewayvia != null && onewayvia) {
                    return 1;
                }
            }
        }
        return 0;
    }
}
