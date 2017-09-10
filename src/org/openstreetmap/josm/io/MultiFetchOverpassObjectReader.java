// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.util.Set;
import java.util.function.Function;

import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.tools.Utils;

/**
 * Retrieves a set of {@link org.openstreetmap.josm.data.osm.OsmPrimitive}s from an Overpass API server.
 *
 * @since 9241
 */
class MultiFetchOverpassObjectReader extends MultiFetchServerObjectReader {

    @Override
    protected String buildRequestString(final OsmPrimitiveType type, Set<Long> idPackage) {
        final Function<Long, Object> toOverpassExpression = x -> type.getAPIName() + '(' + x + ");>;";
        final String query = '(' + Utils.join("", Utils.transform(idPackage, toOverpassExpression)) + ");out meta;";
        return "interpreter?data=" + Utils.encodeUrl(query);
    }

    @Override
    protected String getBaseUrl() {
        return OverpassDownloadReader.OVERPASS_SERVER.get();
    }

    @Override
    protected boolean recursesDown() {
        // see https://wiki.openstreetmap.org/wiki/Overpass_API/Overpass_QL#Recurse_down_.28.3E.29 for documentation
        // accomplished using >; in the query string above
        return true;
    }
}
