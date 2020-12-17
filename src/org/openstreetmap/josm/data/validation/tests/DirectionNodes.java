// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;

/**
 * Find nodes with direction tag and invalid number of parent ways or position in way. See #20019.
 * @author Gerd Petermann
 * @since 17349
 */
public class DirectionNodes extends Test {
    private static final int MULTIPLE_WAYS_CODE = 4000;
    private static final int END_NODE_CODE = 4001;
    private static final int NO_WAY_CODE = 4002;
    private static final int NO_SUITABLE_WAY = 4003;

    private static final String INVALID_USE_MSG = tr("Invalid usage of direction on node");
    private static final String DISPUTED_USE_MSG = tr("Disputed usage of direction on node");

    /**
     * Construct a new {@code DirectionNodes} object
     */
    public DirectionNodes() {
        super(tr("Direction nodes"), tr("Check for nodes which have a ''forward'' or ''backward'' direction"));
    }

    @Override
    public void visit(Node n) {
        if (!n.isUsable() || !n.isTagged())
            return;
        for (Entry<String, String> tag : n.getKeys().entrySet()) {
            if (("forward".equals(tag.getValue()) || "backward".equals(tag.getValue()))
                    && ("direction".equals(tag.getKey()) || tag.getKey().endsWith(":direction"))) {
                checkParents(n, tag.toString());
            }
        }
    }

    private static boolean isSuitableParentWay(Way w) {
        return w.hasKey("highway", "railway", "waterway") || w.hasTag("man_made", "pipeline");
    }

    private void checkParents(Node n, String tag) {
        final List<Way> ways = new ArrayList<>();
        int count = 0;
        int countHighWays = 0;
        for (Way w : n.getParentWays()) {
            if (isSuitableParentWay(w)) {
                ways.add(w);
                if (w.hasKey("highway"))
                    countHighWays++;
            }
            count++;
        }

        // ignore minor highways (footway, path etc) if a major highway is found
        if (countHighWays > 1 && (n.hasKey("highway") || n.hasTag("traffic_sign", "city_limit"))) {
            List<Way> minor = ways.stream().filter(w -> !w.hasTag("highway", Highways.CLASSIFIED_HIGHWAYS))
                    .collect(Collectors.toList());
            if (minor.size() != countHighWays) {
                ways.removeAll(minor);
            }
        }
        boolean needsParentWays = n.isNew()
                || (!n.isOutsideDownloadArea() && n.getDataSet().getDataSourceArea() != null);
        TestError.Builder builder = null;
        if (ways.isEmpty() && needsParentWays) {
            if (count == 0) {
                builder = TestError.builder(this, Severity.ERROR, NO_WAY_CODE).message(INVALID_USE_MSG,
                        marktr("Unconnected node with {0}. Use angle or cardinal direction"), tag);
            } else {
                builder = TestError.builder(this, Severity.WARNING, NO_SUITABLE_WAY).message(INVALID_USE_MSG,
                        marktr("Node with {0} should be connected to a linear way"), tag);
            }
        } else if (ways.size() == 1) {
            Way w = ways.get(0);
            if (w.firstNode() == n || w.lastNode() == n) {
                builder = TestError.builder(this, Severity.OTHER, END_NODE_CODE).message(DISPUTED_USE_MSG,
                        marktr("Node with {0} on end of way"), tag);
            }
        } else { // ways.size() > 1
            builder = TestError.builder(this, Severity.OTHER, MULTIPLE_WAYS_CODE).message(DISPUTED_USE_MSG,
                    marktr("Node with {0} on a connection of multiple ways"), tag);
        }
        if (builder != null) {
            List<OsmPrimitive> primitives = new ArrayList<>();
            primitives.add(n);
            primitives.addAll(ways);
            errors.add(builder.primitives(primitives).highlight(n).build());
        }
    }
}
