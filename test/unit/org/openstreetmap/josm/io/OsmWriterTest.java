// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.openstreetmap.josm.data.osm.NodeData;
import org.openstreetmap.josm.tools.Utils;

/**
 * Unit tests of {@link OsmWriter} class.
 */
public class OsmWriterTest {

    /**
     * Unit test of {@link OsmWriter#byIdComparator}.
     */
    @Test
    public void testByIdComparator() {

        final List<NodeData> ids = new ArrayList<>();
        for (Long id : Arrays.asList(12L, Long.MIN_VALUE, 65L, -12L, 2L, 0L, -3L, -20L, Long.MAX_VALUE)) {
            final NodeData n = new NodeData();
            n.setId(id);
            ids.add(n);
        }

        Collections.sort(ids, OsmWriter.byIdComparator);

        final String idsAsString = Utils.transform(ids, new Utils.Function<NodeData, Object>() {
            @Override
            public Object apply(NodeData x) {
                return x.getUniqueId();
            }
        }).toString();

        assertEquals("[-3, -12, -20, -9223372036854775808, 0, 2, 12, 65, 9223372036854775807]", idsAsString);
    }
}
