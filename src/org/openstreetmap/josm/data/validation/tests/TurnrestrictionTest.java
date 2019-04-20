// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;

/**
 * Checks if turnrestrictions are valid
 * @since 3669
 */
public class TurnrestrictionTest extends Test {

    protected static final int NO_VIA = 1801;
    protected static final int NO_FROM = 1802;
    protected static final int NO_TO = 1803;
    protected static final int MORE_VIA = 1804;
    protected static final int MORE_FROM = 1805;
    protected static final int MORE_TO = 1806;
    protected static final int UNEXPECTED_ROLE = 1807;
    protected static final int UNEXPECTED_TYPE = 1808;
    protected static final int FROM_VIA_NODE = 1809;
    protected static final int TO_VIA_NODE = 1810;
    protected static final int FROM_VIA_WAY = 1811;
    protected static final int TO_VIA_WAY = 1812;
    protected static final int MIX_VIA = 1813;
    protected static final int UNCONNECTED_VIA = 1814;
    protected static final int SUPERFLUOUS = 1815;
    protected static final int FROM_EQUALS_TO = 1816;
    protected static final int UNKNOWN_RESTRICTION = 1817;
    protected static final int TO_CLOSED_WAY = 1818;
    protected static final int FROM_CLOSED_WAY = 1819;

    private static final List<String> SUPPORTED_RESTRICTIONS = Arrays.asList(
            "no_right_turn", "no_left_turn", "no_u_turn", "no_straight_on",
            "only_right_turn", "only_left_turn", "only_straight_on",
            "no_entry", "no_exit"
        );

    /**
     * Constructs a new {@code TurnrestrictionTest}.
     */
    public TurnrestrictionTest() {
        super(tr("Turnrestrictions"), tr("This test checks if turnrestrictions are valid."));
    }

    @Override
    public void visit(Relation r) {
        if (!r.hasTag("type", "restriction"))
            return;

        if (!r.hasTag("restriction", SUPPORTED_RESTRICTIONS)) {
            errors.add(TestError.builder(this, Severity.ERROR, UNKNOWN_RESTRICTION)
                    .message(tr("Unknown restriction"))
                    .primitives(r)
                    .build());
            return;
        }

        Way fromWay = null;
        Way toWay = null;
        List<OsmPrimitive> via = new ArrayList<>();

        boolean morefrom = false;
        boolean moreto = false;
        boolean morevia = false;
        boolean mixvia = false;

        /* find the "from", "via" and "to" elements */
        for (RelationMember m : r.getMembers()) {
            if (m.getMember().isIncomplete())
                return;

            List<OsmPrimitive> l = new ArrayList<>();
            l.add(r);
            l.add(m.getMember());
            if (m.isWay()) {
                Way w = m.getWay();
                if (w.getNodesCount() < 2) {
                    continue;
                }

                switch (m.getRole()) {
                case "from":
                    if (fromWay != null) {
                        morefrom = true;
                    } else {
                        fromWay = w;
                    }
                    break;
                case "to":
                    if (toWay != null) {
                        moreto = true;
                    } else {
                        toWay = w;
                    }
                    break;
                case "via":
                    if (!via.isEmpty() && via.get(0) instanceof Node) {
                        mixvia = true;
                    } else {
                        via.add(w);
                    }
                    break;
                default:
                    errors.add(TestError.builder(this, Severity.WARNING, UNEXPECTED_ROLE)
                            .message(tr("Unexpected role ''{0}'' in restriction", m.getRole()))
                            .primitives(l)
                            .highlight(m.getMember())
                            .build());
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
                    errors.add(TestError.builder(this, Severity.WARNING, UNEXPECTED_ROLE)
                            .message(tr("Unexpected role ''{0}'' in restriction", m.getRole()))
                            .primitives(l)
                            .highlight(m.getMember())
                            .build());
                }
            } else {
                errors.add(TestError.builder(this, Severity.WARNING, UNEXPECTED_TYPE)
                        .message(tr("Unexpected member type in restriction"))
                        .primitives(l)
                        .highlight(m.getMember())
                        .build());
            }
        }
        if (morefrom) {
            errors.add(TestError.builder(this, Severity.ERROR, MORE_FROM)
                    .message(tr("More than one \"from\" way found"))
                    .primitives(r)
                    .build());
            return;
        }
        if (moreto) {
            errors.add(TestError.builder(this, Severity.ERROR, MORE_TO)
                    .message(tr("More than one \"to\" way found"))
                    .primitives(r)
                    .build());
            return;
        }
        if (morevia) {
            errors.add(TestError.builder(this, Severity.ERROR, MORE_VIA)
                    .message(tr("More than one \"via\" node found"))
                    .primitives(r)
                    .build());
            return;
        }
        if (mixvia) {
            errors.add(TestError.builder(this, Severity.ERROR, MIX_VIA)
                    .message(tr("Cannot mix node and way for role \"via\""))
                    .primitives(r)
                    .build());
            return;
        }

        if (fromWay == null) {
            errors.add(TestError.builder(this, Severity.ERROR, NO_FROM)
                    .message(tr("No \"from\" way found"))
                    .primitives(r)
                    .build());
            return;
        } else if (fromWay.isClosed()) {
            errors.add(TestError.builder(this, Severity.ERROR, FROM_CLOSED_WAY)
                    .message(tr("\"from\" way is a closed way"))
                    .primitives(r)
                    .highlight(fromWay)
                    .build());
            return;
        }

        if (toWay == null) {
            errors.add(TestError.builder(this, Severity.ERROR, NO_TO)
                    .message(tr("No \"to\" way found"))
                    .primitives(r)
                    .build());
            return;
        } else if (toWay.isClosed()) {
            errors.add(TestError.builder(this, Severity.ERROR, TO_CLOSED_WAY)
                    .message(tr("\"to\" way is a closed way"))
                    .primitives(r)
                    .highlight(toWay)
                    .build());
            return;
        }
        if (fromWay.equals(toWay)) {
            Severity severity = r.hasTag("restriction", "no_u_turn") ? Severity.OTHER : Severity.WARNING;
            errors.add(TestError.builder(this, severity, FROM_EQUALS_TO)
                    .message(tr("\"from\" way equals \"to\" way"))
                    .primitives(r)
                    .build());
        }
        if (via.isEmpty()) {
            errors.add(TestError.builder(this, Severity.ERROR, NO_VIA)
                    .message(tr("No \"via\" node or way found"))
                    .primitives(r)
                    .build());
            return;
        }

        if (via.get(0) instanceof Node) {
            final Node viaNode = (Node) via.get(0);
            if (isFullOneway(toWay) && viaNode.equals(toWay.lastNode(true))) {
                errors.add(TestError.builder(this, Severity.WARNING, SUPERFLUOUS)
                        .message(tr("Superfluous turnrestriction as \"to\" way is oneway"))
                        .primitives(r)
                        .highlight(toWay)
                        .build());
                return;
            }
            if (isFullOneway(fromWay) && viaNode.equals(fromWay.firstNode(true))) {
                errors.add(TestError.builder(this, Severity.WARNING, SUPERFLUOUS)
                        .message(tr("Superfluous turnrestriction as \"from\" way is oneway"))
                        .primitives(r)
                        .highlight(fromWay)
                        .build());
                return;
            }
            final Way viaPseudoWay = new Way();
            viaPseudoWay.addNode(viaNode);
            checkIfConnected(r, fromWay, viaPseudoWay,
                    tr("The \"from\" way does not start or end at a \"via\" node."), FROM_VIA_NODE);
            checkIfConnected(r, viaPseudoWay, toWay,
                    tr("The \"to\" way does not start or end at a \"via\" node."), TO_VIA_NODE);
        } else {
            if (isFullOneway(toWay) && ((Way) via.get(via.size() - 1)).isFirstLastNode(toWay.lastNode(true))) {
                errors.add(TestError.builder(this, Severity.WARNING, SUPERFLUOUS)
                        .message(tr("Superfluous turnrestriction as \"to\" way is oneway"))
                        .primitives(r)
                        .highlight(toWay)
                        .build());
                return;
            }
            if (isFullOneway(fromWay) && ((Way) via.get(0)).isFirstLastNode(fromWay.firstNode(true))) {
                errors.add(TestError.builder(this, Severity.WARNING, SUPERFLUOUS)
                        .message(tr("Superfluous turnrestriction as \"from\" way is oneway"))
                        .primitives(r)
                        .highlight(fromWay)
                        .build());
                return;
            }
            // check if consecutive ways are connected: from/via[0], via[i-1]/via[i], via[last]/to
            checkIfConnected(r, fromWay, (Way) via.get(0),
                    tr("The \"from\" and the first \"via\" way are not connected."), FROM_VIA_WAY);
            if (via.size() > 1) {
                for (int i = 1; i < via.size(); i++) {
                    Way previous = (Way) via.get(i - 1);
                    Way current = (Way) via.get(i);
                    checkIfConnected(r, previous, current,
                            tr("The \"via\" ways are not connected."), UNCONNECTED_VIA);
                }
            }
            checkIfConnected(r, (Way) via.get(via.size() - 1), toWay,
                    tr("The last \"via\" and the \"to\" way are not connected."), TO_VIA_WAY);
        }
    }

    private static boolean isFullOneway(Way w) {
        return w.isOneway() != 0 && !w.hasTag("oneway:bicycle", "no");
    }

    private void checkIfConnected(Relation r, Way previous, Way current, String msg, int code) {
        boolean c;
        if (isFullOneway(previous) && isFullOneway(current)) {
            // both oneways: end/start node must be equal
            c = previous.lastNode(true).equals(current.firstNode(true));
        } else if (isFullOneway(previous)) {
            // previous way is oneway: end of previous must be start/end of current
            c = current.isFirstLastNode(previous.lastNode(true));
        } else if (isFullOneway(current)) {
            // current way is oneway: start of current must be start/end of previous
            c = previous.isFirstLastNode(current.firstNode(true));
        } else {
            // otherwise: start/end of previous must be start/end of current
            c = current.isFirstLastNode(previous.firstNode()) || current.isFirstLastNode(previous.lastNode());
        }
        if (!c) {
            List<OsmPrimitive> hilite = new ArrayList<>();
            if (previous.getNodesCount() == 1 && previous.isNew())
                hilite.add(previous.firstNode());
            else
                hilite.add(previous);
            if (current.getNodesCount() == 1 && current.isNew())
                hilite.add(current.firstNode());
            else
                hilite.add(current);
            List<OsmPrimitive> primitives = new ArrayList<>();
            primitives.add(r);
            primitives.addAll(hilite);
            errors.add(TestError.builder(this, Severity.ERROR, code)
                    .message(msg)
                    .primitives(primitives)
                    .highlight(hilite)
                    .build());
        }
    }
}
