// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.tools.Logging;

/**
 * Retrieves a set of {@link org.openstreetmap.josm.data.osm.OsmPrimitive}s from an Overpass API server.
 *
 * @since 9241
 */
public class MultiFetchOverpassObjectReader extends MultiFetchServerObjectReader {
    private static final List<OsmPrimitiveType> wantedOrder = Arrays.asList(OsmPrimitiveType.RELATION,
            OsmPrimitiveType.WAY, OsmPrimitiveType.NODE);

    private static String getPackageString(final OsmPrimitiveType type, Set<Long> idPackage) {
        return idPackage.stream().map(String::valueOf)
                .collect(Collectors.joining(",", type.getAPIName() + (idPackage.size() == 1 ? "(" : "(id:"), ")"));
    }

    /**
     * Generate single overpass query to retrieve multiple primitives. Can be used to download parents,
     * children, the objects, or any combination of them.
     * @param ids the collection of ids
     * @param includeObjects if false, don't retrieve the primitives (e.g. only the referrers)
     * @param recurseUp if true, referrers (parents) of the objects are downloaded and all nodes of parent ways
     * @param recurseDownRelations true: yes, recurse down to retrieve complete relations
     * @return the overpass query
     * @since xxx
     */
    public static String genOverpassQuery(Collection<? extends PrimitiveId> ids, boolean includeObjects, boolean recurseUp,
            boolean recurseDownRelations) {
        Map<OsmPrimitiveType, Set<Long>> primitivesMap = new LinkedHashMap<>();
        Arrays.asList(OsmPrimitiveType.RELATION, OsmPrimitiveType.WAY, OsmPrimitiveType.NODE)
                .forEach(type -> primitivesMap.put(type, new TreeSet<>()));
        for (PrimitiveId p : ids) {
            primitivesMap.get(p.getType()).add(p.getUniqueId());
        }
        return genOverpassQuery(primitivesMap, includeObjects, recurseUp, recurseDownRelations);
    }

    /**
     * Generate single overpass query to retrieve multiple primitives. Can be used to download parents,
     * children, the objects, or any combination of them.
     * @param primitivesMap map containing the primitives
     * @param includeObjects if false, don't retrieve the primitives (e.g. only the referrers)
     * @param recurseUp if true, referrers (parents) of the objects are downloaded and all nodes of parent ways
     * @param recurseDownRelations true: yes, recurse down to retrieve complete relations
     * @return the overpass query
     */
    protected static String genOverpassQuery(Map<OsmPrimitiveType, Set<Long>> primitivesMap, boolean includeObjects,
            boolean recurseUp, boolean recurseDownRelations) {
        if (!(includeObjects || recurseUp || recurseDownRelations))
            throw new IllegalArgumentException("At least one options must be true");
        StringBuilder sb = new StringBuilder(128);
        StringBuilder setsToInclude = new StringBuilder();
        StringBuilder up = new StringBuilder();
        String down = null;
        for (OsmPrimitiveType type : wantedOrder) {
            Set<Long> set = primitivesMap.get(type);
            if (!set.isEmpty()) {
                sb.append(getPackageString(type, set));
                if (type == OsmPrimitiveType.NODE) {
                    sb.append("->.n;");
                    if (includeObjects) {
                        setsToInclude.append(".n;");
                    }
                    if (recurseUp) {
                        up.append(".n;way(bn)->.wn;.n;rel(bn)->.rn;");
                        setsToInclude.append(".wn;node(w);.rn;");
                    }
                } else if (type == OsmPrimitiveType.WAY) {
                    sb.append("->.w;");
                    if (includeObjects) {
                        setsToInclude.append(".w;>;");
                    }
                    if (recurseUp) {
                        up.append(".w;rel(bw)->.pw;");
                        setsToInclude.append(".pw;");
                    }
                } else {
                    sb.append("->.r;");
                    if (includeObjects) {
                        setsToInclude.append(".r;");
                    }
                    if (recurseUp) {
                        up.append(".r;rel(br)->.pr;");
                        setsToInclude.append(".pr;");
                    }
                    if (recurseDownRelations) {
                        // get complete ways and nodes of the relation and next level of sub relations
                        down = ".r;rel(r)->.rm;";
                        setsToInclude.append(".r;>;.rm;");
                    }
                }
            }
        }
        if (up.length() > 0) {
            sb.append(up);
        }
        if (down != null) {
            sb.append(down);
        }
        sb.append('(').append(setsToInclude).append(");out meta;");

        String query = sb.toString();
        Logging.debug("{0} {1}", "Generated Overpass query:", query);
        return query;
    }

    @Override
    protected String getBaseUrl() {
        return OverpassDownloadReader.OVERPASS_SERVER.get();
    }
}
