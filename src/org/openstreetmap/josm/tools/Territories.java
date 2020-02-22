// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * Look up territories ISO3166 codes at a certain place.
 */
public final class Territories {

    /** Internal OSM filename */
    public static final String FILENAME = "boundaries.osm";

    private static final String ISO3166_1 = "ISO3166-1:alpha2";
    private static final String ISO3166_2 = "ISO3166-2";
    private static final String ISO3166_1_LC = ISO3166_1.toLowerCase(Locale.ENGLISH);
    private static final String ISO3166_2_LC = ISO3166_2.toLowerCase(Locale.ENGLISH);
    private static final String TAGINFO = "taginfo";

    private static DataSet dataSet;

    private static volatile Map<String, GeoPropertyIndex<Boolean>> iso3166Cache;
    private static volatile Map<String, TaginfoRegionalInstance> taginfoCache;
    private static volatile Map<String, TaginfoRegionalInstance> taginfoGeofabrikCache;

    private Territories() {
        // Hide implicit public constructor for utility classes
    }

    /**
     * Get all known ISO3166-1 and ISO3166-2 codes.
     *
     * @return the ISO3166-1 and ISO3166-2 codes for the given location
     */
    public static synchronized Set<String> getKnownIso3166Codes() {
        return iso3166Cache.keySet();
    }

    /**
     * Returns the {@link GeoPropertyIndex} for the given ISO3166-1 or ISO3166-2 code.
     * @param code the ISO3166-1 or ISO3166-2 code
     * @return the {@link GeoPropertyIndex} for the given {@code code}
     * @since 14484
     */
    public static GeoPropertyIndex<Boolean> getGeoPropertyIndex(String code) {
        return iso3166Cache.get(code);
    }

    /**
     * Determine, if a point is inside a territory with the given ISO3166-1
     * or ISO3166-2 code.
     *
     * @param code the ISO3166-1 or ISO3166-2 code
     * @param ll the coordinates of the point
     * @return true, if the point is inside a territory with the given code
     */
    public static synchronized boolean isIso3166Code(String code, LatLon ll) {
        GeoPropertyIndex<Boolean> gpi = iso3166Cache.get(code);
        if (gpi == null) {
            Logging.warn(tr("Unknown territory id: {0}", code));
            return false;
        }
        return Boolean.TRUE.equals(gpi.get(ll)); // avoid NPE, see #16491
    }

    /**
     * Returns the original territories dataset. Be extra cautious when manipulating it!
     * @return the original territories dataset
     * @since 15565
     */
    public static synchronized DataSet getOriginalDataSet() {
        return dataSet;
    }

    /**
     * Returns a copy of the territories dataset.
     * @return a copy of the territories dataset
     */
    public static synchronized DataSet getDataSet() {
        return new DataSet(dataSet);
    }

    /**
     * Initializes territories.
     * TODO: Synchronization can be refined inside the {@link GeoPropertyIndex} as most look-ups are read-only.
     */
    public static synchronized void initialize() {
        initializeInternalData();
        initializeExternalData();
    }

    private static void initializeInternalData() {
        iso3166Cache = new HashMap<>();
        taginfoCache = new TreeMap<>();
        try (CachedFile cf = new CachedFile("resource://data/" + FILENAME);
                InputStream is = cf.getInputStream()) {
            dataSet = OsmReader.parseDataSet(is, null);
            Collection<OsmPrimitive> candidates = new ArrayList<>(dataSet.getWays());
            candidates.addAll(dataSet.getRelations());
            for (OsmPrimitive osm : candidates) {
                String iso1 = osm.get(ISO3166_1);
                String iso2 = osm.get(ISO3166_2);
                if (iso1 != null || iso2 != null) {
                    GeoProperty<Boolean> gp;
                    if (osm instanceof Way) {
                        gp = new DefaultGeoProperty(Collections.singleton((Way) osm));
                    } else {
                        gp = new DefaultGeoProperty((Relation) osm);
                    }
                    GeoPropertyIndex<Boolean> gpi = new GeoPropertyIndex<>(gp, 24);
                    if (iso1 != null) {
                        iso3166Cache.put(iso1, gpi);
                        String taginfo = osm.get(TAGINFO);
                        if (taginfo != null) {
                            taginfoCache.put(iso1, new TaginfoRegionalInstance(taginfo, Collections.singleton(iso1)));
                        }
                    }
                    if (iso2 != null) {
                        iso3166Cache.put(iso2, gpi);
                    }
                }
            }
        } catch (IOException | IllegalDataException ex) {
            throw new JosmRuntimeException(ex);
        }
    }

    private static void initializeExternalData() {
        taginfoGeofabrikCache = new TreeMap<>();
        try (CachedFile cf = new CachedFile(Config.getUrls().getJOSMWebsite() + "/remote/geofabrik-index-v1-nogeom.json");
                InputStream is = cf.getInputStream();
                JsonParser json = Json.createParser(is)) {
            while (json.hasNext()) {
                Event event = json.next();
                if (event == Event.START_OBJECT) {
                    for (JsonValue feature : json.getObject().getJsonArray("features")) {
                        JsonObject props = feature.asJsonObject().getJsonObject("properties");
                        if (props != null) {
                            JsonObject urls = props.getJsonObject("urls");
                            if (urls != null) {
                                String taginfo = urls.getString(TAGINFO);
                                if (taginfo != null) {
                                    JsonArray iso1 = props.getJsonArray(ISO3166_1_LC);
                                    JsonArray iso2 = props.getJsonArray(ISO3166_2_LC);
                                    if (iso1 != null) {
                                        readExternalTaginfo(taginfo, iso1);
                                    } else if (iso2 != null) {
                                        readExternalTaginfo(taginfo, iso2);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new JosmRuntimeException(e);
        }
    }

    private static void readExternalTaginfo(String taginfo, JsonArray jsonCodes) {
        Set<String> isoCodes = jsonCodes.getValuesAs(JsonString.class).stream().map(JsonString::getString).collect(Collectors.toSet());
        isoCodes.forEach(s -> taginfoGeofabrikCache.put(s, new TaginfoRegionalInstance(taginfo, isoCodes, "Geofabrik")));
    }

    /**
     * Returns regional taginfo instances for the given location.
     * @param ll lat/lon where to look.
     * @return regional taginfo instances for the given location (code / url)
     * @since 15876
     */
    public static Stream<TaginfoRegionalInstance> getRegionalTaginfoUrls(LatLon ll) {
        if (iso3166Cache == null) {
            return Stream.empty();
        }
        return iso3166Cache.entrySet().parallelStream().distinct()
                .filter(e -> Boolean.TRUE.equals(e.getValue().get(ll)))
                .map(Entry<String, GeoPropertyIndex<Boolean>>::getKey)
                .distinct()
                .flatMap(code -> Stream.of(taginfoCache, taginfoGeofabrikCache).map(cache -> cache.get(code)))
                .filter(Objects::nonNull);
    }
}
