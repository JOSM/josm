// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.SelectByInternalPointAction;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;

/**
 * Look up territories ISO3166 codes at a certain place.
 */
public final class Territories {

    private static final String ISO3166_1 = "ISO3166-1:alpha2";
    private static final String ISO3166_2 = "ISO3166-2";

    private static class Iso3166GeoProperty implements GeoProperty<Set<String>> {

        @Override
        public Set<String> get(LatLon ll) {
            Set<String> result = new HashSet<>();
            for (OsmPrimitive surrounding :
                    SelectByInternalPointAction.getSurroundingObjects(dataSet, Main.getProjection().latlon2eastNorth(ll), true)) {
                String iso1 = surrounding.get(ISO3166_1);
                if (iso1 != null) {
                    result.add(iso1);
                }
                String iso2 = surrounding.get(ISO3166_2);
                if (iso2 != null) {
                    result.add(iso2);
                }
            }
            return result;
        }

        @Override
        public Set<String> get(BBox box) {
            return null; // TODO
        }
    }

    private static DataSet dataSet;
    private static final Map<String, OsmPrimitive> iso3166Map = new ConcurrentHashMap<>();

    private static volatile GeoPropertyIndex<Set<String>> iso3166Cache;

    private Territories() {
        // Hide implicit public constructor for utility classes
    }

    /**
     * Get all known ISO3166-1 and ISO3166-2 codes.
     *
     * @return the ISO3166-1 and ISO3166-2 codes for the given location
     */
    public static synchronized Set<String> getKnownIso3166Codes() {
        return iso3166Map.keySet();
    }

    /**
     * Get the ISO3166-1 and ISO3166-2 codes for the given location.
     *
     * @param ll the coordinates of the point
     * @return the ISO3166-1 and ISO3166-2 codes for the given location
     */
    public static synchronized Set<String> getIso3166Codes(LatLon ll) {
        return iso3166Cache.get(ll);
    }

    /**
     * Returns the territories dataset.
     * @return the territories dataset
     */
    public static synchronized DataSet getDataSet() {
        return new DataSet(dataSet);
    }

    /**
     * Initializes territories.
     * TODO: Synchronization can be refined inside the {@link GeoPropertyIndex} as most look-ups are read-only.
     */
    public static synchronized void initialize() {
        iso3166Cache = new GeoPropertyIndex<>(new Iso3166GeoProperty(), 24);
        try (CachedFile cf = new CachedFile("resource://data/boundaries.osm");
                InputStream is = cf.getInputStream()) {
            dataSet = OsmReader.parseDataSet(is, null);
            Collection<OsmPrimitive> candidates = new ArrayList<>(dataSet.getWays());
            candidates.addAll(dataSet.getRelations());
            for (OsmPrimitive osm : candidates) {
                String iso1 = osm.get(ISO3166_1);
                if (iso1 != null) {
                    iso3166Map.put(iso1, osm);
                }
                String iso2 = osm.get(ISO3166_2);
                if (iso2 != null) {
                    iso3166Map.put(iso2, osm);
                }
            }
        } catch (IOException | IllegalDataException ex) {
            throw new RuntimeException(ex);
        }
    }
}
