// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import org.openstreetmap.josm.data.gpx.GpxData;

/**
 * Collection of {@link UrlPattern}s.
 * @since 15784
 */
public final class UrlPatterns {

    private static final String HTTPS = "https?://";
    private static final String COMPRESSED = "(gz|xz|bz2?|zip)";

    private UrlPatterns() {
        // Hide public constructor
    }

    // CHECKSTYLE.OFF: MethodParamPad
    // CHECKSTYLE.OFF: SingleSpaceSeparator

    /**
     * Patterns for Geojson download URLs.
     */
    public enum GeoJsonUrlPattern implements UrlPattern {
        /** URL of remote geojson files, optionally compressed */
        COMPRESSED_FILE(".*/(.*\\.(json|geojson)(\\."+COMPRESSED+")?)"),
        /** URL of generic service providing geojson as output format */
        FORMAT_GEOJSON (".*format=geojson.*");

        private final String urlPattern;

        GeoJsonUrlPattern(String urlPattern) {
            this.urlPattern = HTTPS + urlPattern;
        }

        @Override
        public String pattern() {
            return urlPattern;
        }
    }

    /**
     * Patterns for GPX download URLs.
     */
    public enum GpxUrlPattern implements UrlPattern {
        /** URL of identified GPX trace on OpenStreetMap website */
        TRACE_ID     (".*(osm|openstreetmap).org/trace/\\p{Digit}+/data"),
        /** URL of identified GPX trace belonging to any user on OpenStreetMap website */
        USER_TRACE_ID(".*(osm|openstreetmap).org/user/[^/]+/traces/(\\p{Digit}+)"),
        /** URL of the edit link from the OpenStreetMap trace page */
        EDIT_TRACE_ID(".*(osm|openstreetmap).org/edit/?\\?gpx=(\\p{Digit}+)(#.*)?"),

        /** URL of OSM API trackpoints endpoint */
        TRACKPOINTS_BBOX(".*/api/0.6/trackpoints\\?bbox=.*,.*,.*,.*"),
        /** URL of HOT Tasking Manager (TM) */
        TASKING_MANAGER(".*/api/v\\p{Digit}+/projects?/\\p{Digit}+/(tasks_as_gpx?.*|tasks/queries/gpx/\\?tasks=.*)"),

        /** External GPX script */
        EXTERNAL_GPX_SCRIPT(".*exportgpx.*"),
        /** External GPX file */
        EXTERNAL_GPX_FILE  (".*/(.*\\.gpx)");

        private final String urlPattern;

        GpxUrlPattern(String urlPattern) {
            this.urlPattern = HTTPS + urlPattern;
        }

        @Override
        public String pattern() {
            return urlPattern;
        }

        /**
         * Determines if the given URL denotes an OSM gpx-related API call.
         * @param url The url to check
         * @return true if the url matches "Trace ID" API call or "Trackpoints bbox" API call, false otherwise
         * @see GpxData#fromServer
         */
        public static boolean isGpxFromServer(String url) {
            return TRACE_ID.matches(url) || TRACKPOINTS_BBOX.matches(url);
        }
    }

    /**
     * Patterns for Note download URLs.
     */
    public enum NoteUrlPattern implements UrlPattern {
        /** URL of OSM API Notes endpoint */
        API_URL  (".*/api/0.6/notes.*"),
        /** URL of OSM API Notes compressed dump file */
        DUMP_FILE(".*/(.*\\.osn(\\."+COMPRESSED+")?)");

        private final String urlPattern;

        NoteUrlPattern(String urlPattern) {
            this.urlPattern = HTTPS + urlPattern;
        }

        @Override
        public String pattern() {
            return urlPattern;
        }
    }

    /**
     * Patterns for OsmChange data download URLs.
     */
    public enum OsmChangeUrlPattern implements UrlPattern {
        /** URL of OSM changeset on OpenStreetMap website */
        OSM_WEBSITE             ("www\\.(osm|openstreetmap)\\.org/changeset/(\\p{Digit}+).*"),
        /** URL of OSM API 0.6 changeset */
        OSM_API                 (".*/api/0.6/changeset/\\p{Digit}+/download"),
        /** URL of remote .osc file */
        EXTERNAL_OSC_FILE       (".*/(.*\\.osc)"),
        /** URL of remote compressed osc file */
        EXTERNAL_COMPRESSED_FILE(".*/(.*\\.osc."+COMPRESSED+")");

        private final String urlPattern;

        OsmChangeUrlPattern(String urlPattern) {
            this.urlPattern = HTTPS + urlPattern;
        }

        @Override
        public String pattern() {
            return urlPattern;
        }
    }

    /**
     * Patterns for OSM data download URLs.
     */
    public enum OsmUrlPattern implements UrlPattern {
        /** URL of OSM API */
        OSM_API_URL             (".*/api/0.6/(map|nodes?|ways?|relations?|\\*).*"),
        /** URL of Overpass API */
        OVERPASS_API_URL        (".*/interpreter\\?data=.*"),
        /** URL of Overpass API (XAPI compatibility) */
        OVERPASS_API_XAPI_URL   (".*/xapi(\\?.*\\[@meta\\]|_meta\\?).*"),
        /** URL of remote .osm file */
        EXTERNAL_OSM_FILE       (".*/(.*\\.osm)"),
        /** URL of remote compressed osm file */
        EXTERNAL_COMPRESSED_FILE(".*/(.*\\.osm\\."+COMPRESSED+")");

        private final String urlPattern;

        OsmUrlPattern(String urlPattern) {
            this.urlPattern = HTTPS + urlPattern;
        }

        @Override
        public String pattern() {
            return urlPattern;
        }
    }

    // CHECKSTYLE.ON: SingleSpaceSeparator
    // CHECKSTYLE.ON: MethodParamPad
}
