// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.HashSet;
import java.util.Set;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;

/**
 * Warn when a node has the same tags as its parent way. The check is rather
 * conservative: it warns only when the tags are identical and important (i.e.,
 * no warning for a way and a node that only have a "source=PGS" tag).
 * <p>
 * See JOSM ticket #7639 for the original request.
 *
 * @author Mrwojo
 */
public class NodesDuplicatingWayTags extends Test {

    protected static final int NODE_DUPING_PARENT_WAY_TAGS = 2401;

    public NodesDuplicatingWayTags() {
        super(tr("Nodes duplicating way tags"),
                tr("Checks for nodes that have the same tags as their parent way."));
    }

    @Override
    public void visit(Way way) {
        // isTagged represents interesting tags (not "source", "created_by", ...)
        if (!way.isUsable() || !way.isTagged())
            return;

        // Use a set so you don't report the same node of an area/loop more than once.
        Set<OsmPrimitive> dupedWayTags = new HashSet<OsmPrimitive>();

        // Check for nodes in the way that have tags identical to the way's tags.
        for (Node node : way.getNodes()) {
            if (way.hasSameTags(node)) {
                dupedWayTags.add(node);
            }
        }

        if (!dupedWayTags.isEmpty()) {
            // Add the way for the warning.
            dupedWayTags.add(way);

            errors.add(new TestError(this, Severity.WARNING, tr("Nodes duplicating parent way tags"),
                    NODE_DUPING_PARENT_WAY_TAGS, dupedWayTags));
        }
    }
}
