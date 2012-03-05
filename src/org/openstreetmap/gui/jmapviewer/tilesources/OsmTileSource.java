package org.openstreetmap.gui.jmapviewer.tilesources;

public class OsmTileSource {

    public static final String MAP_MAPNIK = "http://tile.openstreetmap.org";

    public static class Mapnik extends AbstractOsmTileSource {
        public Mapnik() {
            super("Mapnik", MAP_MAPNIK);
        }

        public TileUpdate getTileUpdate() {
            return TileUpdate.IfNoneMatch;
        }
    }

    public static class CycleMap extends AbstractOsmTileSource {

        private static final String PATTERN = "http://%s.tile.opencyclemap.org/cycle";

        private static final String[] SERVER = { "a", "b", "c" };

        private int SERVER_NUM = 0;

        public CycleMap() {
            super("OSM Cycle Map", PATTERN);
        }

        @Override
        public String getBaseUrl() {
            String url = String.format(this.baseUrl, new Object[] { SERVER[SERVER_NUM] });
            SERVER_NUM = (SERVER_NUM + 1) % SERVER.length;
            return url;
        }

        @Override
        public int getMaxZoom() {
            return 17;
        }

        public TileUpdate getTileUpdate() {
            return TileUpdate.LastModified;
        }
    }
}
