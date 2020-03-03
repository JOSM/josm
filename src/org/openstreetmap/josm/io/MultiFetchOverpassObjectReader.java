// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.tools.Logging;

/**
 * Retrieves a set of {@link org.openstreetmap.josm.data.osm.OsmPrimitive}s from an Overpass API server.
 *
 * @since 9241
 */
public class MultiFetchOverpassObjectReader extends MultiFetchServerObjectReader {

    private static String getPackageString(final OsmPrimitiveType type, Set<Long> idPackage) {
        return idPackage.stream().map(String::valueOf)
                .collect(Collectors.joining(",", type.getAPIName() + (idPackage.size() == 1 ? "(" : "(id:"), ");"));
    }

    /**
     * Create a single query for all elements
     * @return the request string
     */
    protected String buildComplexRequestString() {
        StringBuilder sb = new StringBuilder();
        int countTypes = 0;
        for (Entry<OsmPrimitiveType, Set<Long>> e : primitivesMap.entrySet()) {
            if (!e.getValue().isEmpty()) {
                countTypes++;
                String list = getPackageString(e.getKey(), e.getValue());
                switch (e.getKey()) {
                case MULTIPOLYGON:
                case RELATION:
                    if (recurseDownRelations) {
                        sb.append('(').append(list);
                        sb.setLength(sb.length()-1); // remove semicolon
                        //recurse down only one level, see #18835
                        sb.append("->.r;.r;rel(r);.r;way(r);>;.r;node(r););");
                    } else {
                        sb.append(list);
                    }
                    break;
                case CLOSEDWAY:
                case WAY:
                    sb.append('(').append(list).append(">;);");
                    break;
                case NODE:
                    sb.append(list);
                }
            }
        }
        String query = sb.toString();
        if (countTypes > 1) {
            query = "(" + query + ");";
        }
        query += "out meta;";
        Logging.debug("{0} {1}", "Generated Overpass query:", query);
        return query;
    }

    @Override
    protected String getBaseUrl() {
        return OverpassDownloadReader.OVERPASS_SERVER.get();
    }
}
