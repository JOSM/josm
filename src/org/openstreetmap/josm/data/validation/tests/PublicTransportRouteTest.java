// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
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
        final Set<Node> routeNodes = new HashSet<>();
        for (RelationMember member : r.getMembers()) {
            if (member.hasRole("forward", "backward", "alternate")) {
                errors.add(TestError.builder(this, Severity.ERROR, 3601)
                        .message(tr("Route relation contains a ''{0}'' role", "forward/backward/alternate"))
                        .primitives(r)
                        .build());
                return;
            } else if (member.hasRole("", "hail_and_ride") && OsmPrimitiveType.WAY == member.getType()) {
                membersToCheck.add(member);
                routeNodes.addAll(member.getWay().getNodes());
            }
        }
        if (membersToCheck.isEmpty()) {
            return;
        }

        final List<WayConnectionType> links = connectionTypeCalculator.updateLinks(r, membersToCheck);
        for (int i = 0; i < links.size(); i++) {
            final WayConnectionType link = links.get(i);
            final boolean hasError = !(i == 0 || link.linkPrev)
                    || !(i == links.size() - 1 || link.linkNext)
                    || link.direction == null
                    || WayConnectionType.Direction.NONE == link.direction;
            if (hasError) {
                errors.add(TestError.builder(this, Severity.WARNING, 3602)
                        .message(tr("Route relation contains a gap"))
                        .primitives(r)
                        .build());
                return;
            }
        }

        for (RelationMember member : r.getMembers()) {
            if (member.hasRole("stop", "stop_exit_only", "stop_entry_only")
                    && OsmPrimitiveType.NODE == member.getType()
                    && !routeNodes.contains(member.getNode())) {
                errors.add(TestError.builder(this, Severity.WARNING, 3603)
                        .message(tr("Stop position not part of route"))
                        .primitives(member.getMember(), r)
                        .build());
            }
        }
    }

    @Override
    public void clear() {
        connectionTypeCalculator.clear();
        super.clear();
    }
}
