// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionType;
import org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionTypeCalculator;

/**
 * Tests for <a href="https://wiki.openstreetmap.org/wiki/Proposed_features/Public_Transport">public transport routes</a>.
 */
public class PublicTransportRouteTest extends Test {

    private final WayConnectionTypeCalculator connectionTypeCalculator = new WayConnectionTypeCalculator();

    /**
     * Constructs a new {@code PublicTransportRouteTest}.
     */
    public PublicTransportRouteTest() {
        super(tr("Public Transport Route"));
    }

    @Override
    public void visit(Relation r) {
        final boolean skip = r.hasIncompleteMembers()
                || !r.hasTag("type", "route")
                || !r.hasKey("route")
                || !r.hasTag("public_transport:version", "2");
        if (skip) {
            return;
        }

        final List<RelationMember> membersToCheck = new ArrayList<>();
        for (RelationMember member : r.getMembers()) {
            if (member.hasRole("forward", "backward")) {
                errors.add(new TestError(this, Severity.WARNING, tr("Route relation contains a ''{0}'' role", "forward/backward"), 3601, r));
                return;
            } else if (member.hasRole("")) {
                membersToCheck.add(member);
            }
        }
        if (membersToCheck.isEmpty()) {
            return;
        }

        final List<WayConnectionType> links = connectionTypeCalculator.updateLinks(membersToCheck);
        for (int i = 0; i < links.size(); i++) {
            final WayConnectionType link = links.get(i);
            final boolean hasError = !(i == 0 || link.linkPrev)
                    || !(i == links.size() - 1 || link.linkNext)
                    || link.direction == null
                    || WayConnectionType.Direction.NONE.equals(link.direction);
            if (hasError) {
                errors.add(new TestError(this, Severity.WARNING, tr("Route relation contains a gap"), 3602, r));
                return;
            }
        }

    }
}
