package org.openstreetmap.gui.jmapviewer.tilesources;

import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class TemplatedTMSTileSource extends TMSTileSource {
    
    private Random rand = null;
    private String[] randomParts = null;
    
    public static final String PATTERN_ZOOM    = "\\{zoom\\}";
    public static final String PATTERN_X       = "\\{x\\}";
    public static final String PATTERN_Y       = "\\{y\\}";
    public static final String PATTERN_Y_YAHOO = "\\{!y\\}";
    public static final String PATTERN_SWITCH  = "\\{switch:[^}]+\\}";
    
    public static final String[] ALL_PATTERNS = {
        PATTERN_ZOOM, PATTERN_X, PATTERN_Y, PATTERN_Y_YAHOO, PATTERN_SWITCH
    };
    
    public TemplatedTMSTileSource(String name, String url, int maxZoom) {
        super(name, url, maxZoom);
    }

    public TemplatedTMSTileSource(String name, String url, int minZoom, int maxZoom) {
        super(name, url, minZoom, maxZoom);
        // Capturing group pattern on switch values
        Matcher m = Pattern.compile(".*\\{switch:([^}]+)\\}.*").matcher(url);
        if (m.matches()) {
            rand = new Random();
            randomParts = m.group(1).split(",");
        }
    }

    @Override
    public String getTileUrl(int zoom, int tilex, int tiley) {
        String r = this.baseUrl
            .replaceAll(PATTERN_ZOOM, Integer.toString(zoom))
            .replaceAll(PATTERN_X, Integer.toString(tilex))
            .replaceAll(PATTERN_Y, Integer.toString(tiley))
            .replaceAll(PATTERN_Y_YAHOO, Integer.toString((int)Math.pow(2, zoom)-1-tiley));
        if (rand != null) {
            r = r.replaceAll(PATTERN_SWITCH, randomParts[rand.nextInt(randomParts.length)]);
        }
        return r;
    }
}
