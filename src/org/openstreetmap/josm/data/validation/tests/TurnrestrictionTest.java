// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collections;

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

    public TurnrestrictionTest() {
        super(tr("Turnrestriction"), tr("This test checks if turnrestrictions are valid"));
    }

    @Override
    public void visit(Relation r) {
        if (!"restriction".equals(r.get("type")))
            return;

        Way fromWay = null;
        Way toWay = null;
        OsmPrimitive via = null;

        boolean morefrom = false;
        boolean moreto = false;
        boolean morevia = false;

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
                    if (via != null) {
                        morevia = true;
                    } else {
                        via = w;
                    }
                } else {
                    errors.add(new TestError(this, Severity.WARNING, tr("Unknown role"), UNKNOWN_ROLE,
                            l, Collections.singletonList(m)));
                }
            } else if (m.isNode()) {
                Node n = m.getNode();
                if ("via".equals(m.getRole())) {
                    if (via != null) {
                        morevia = true;
                    } else {
                        via = n;
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
            errors.add(new TestError(this, Severity.ERROR, tr("More than one \"via\" way found"), MORE_VIA, r));
        }

        if (fromWay == null) {
            errors.add(new TestError(this, Severity.ERROR, tr("No \"from\" way found"), NO_FROM, r));
            return;
        }
        if (toWay == null) {
            errors.add(new TestError(this, Severity.ERROR, tr("No \"to\" way found"), NO_TO, r));
            return;
        }
        if (via == null) {
            errors.add(new TestError(this, Severity.ERROR, tr("No \"via\" node or way found"), NO_VIA, r));
            return;
        }

        Node viaNode;
        if (via instanceof Node) {
            viaNode = (Node) via;
            if (!fromWay.isFirstLastNode(viaNode)) {
                errors.add(new TestError(this, Severity.ERROR,
                        tr("The \"from\" way does not start or end at a \"via\" node"), FROM_VIA_NODE, r));
                return;
            }
            if (!toWay.isFirstLastNode(viaNode)) {
                errors.add(new TestError(this, Severity.ERROR,
                        tr("The \"to\" way does not start or end at a \"via\" node"), TO_VIA_NODE, r));
                return;
            }
        } else {
            Way viaWay = (Way) via;
            Node firstNode = viaWay.firstNode();
            Node lastNode = viaWay.lastNode();
            Boolean onewayvia = false;

            String onewayviastr = viaWay.get("oneway");
            if (onewayviastr != null) {
                if ("-1".equals(onewayviastr)) {
                    onewayvia = true;
                    Node tmp = firstNode;
                    firstNode = lastNode;
                    lastNode = tmp;
                } else {
                    onewayvia = OsmUtils.getOsmBoolean(onewayviastr);
                    if (onewayvia == null) {
                        onewayvia = false;
                    }
                }
            }

            if (fromWay.isFirstLastNode(firstNode)) {
                viaNode = firstNode;
            } else if (!onewayvia && fromWay.isFirstLastNode(lastNode)) {
                viaNode = lastNode;
            } else {
                errors.add(new TestError(this, Severity.ERROR,
                        tr("The \"from\" way does not start or end at a \"via\" way."), FROM_VIA_WAY, r));
                return;
            }
            if (!toWay.isFirstLastNode(viaNode == firstNode ? lastNode : firstNode)) {
                errors.add(new TestError(this, Severity.ERROR,
                        tr("The \"to\" way does not start or end at a \"via\" way."), TO_VIA_WAY, r));
            }
        }
    }
}
