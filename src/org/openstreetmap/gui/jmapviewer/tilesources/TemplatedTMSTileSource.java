package org.openstreetmap.gui.jmapviewer.tilesources;

import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class TemplatedTMSTileSource extends TMSTileSource {
    
    private Random rand = null;
    private String[] randomParts = null;
    private Map<String, String> headers = new HashMap<String, String>();
    
    public static final String PATTERN_ZOOM    = "\\{zoom([+-]\\d+)?\\}";
    public static final String PATTERN_X       = "\\{x\\}";
    public static final String PATTERN_Y       = "\\{y\\}";
    public static final String PATTERN_Y_YAHOO = "\\{!y\\}";
    public static final String PATTERN_SWITCH  = "\\{switch:[^}]+\\}";
    public static final String PATTERN_HEADER  = "\\{header\\(([^,]+),([^}]+)\\)\\}";
    
    public static final String[] ALL_PATTERNS = {
        PATTERN_HEADER, PATTERN_ZOOM, PATTERN_X, PATTERN_Y, PATTERN_Y_YAHOO, PATTERN_SWITCH
    };
    
    public TemplatedTMSTileSource(String name, String url, int maxZoom) {
        super(name, url, maxZoom);
        handleTemplate();
    }

    public TemplatedTMSTileSource(String name, String url, int minZoom, int maxZoom) {
        super(name, url, minZoom, maxZoom);
        handleTemplate();
    }

    private void handleTemplate() {
        // Capturing group pattern on switch values
        Matcher m = Pattern.compile(".*"+PATTERN_SWITCH+".*").matcher(baseUrl);
        if (m.matches()) {
            rand = new Random();
            randomParts = m.group(1).split(",");
        }
        Pattern pattern = Pattern.compile(PATTERN_HEADER);
        StringBuffer output = new StringBuffer();
        Matcher matcher = pattern.matcher(baseUrl);
        while (matcher.find()) {
            headers.put(matcher.group(1),matcher.group(2));
            matcher.appendReplacement(output, "");
        }
        matcher.appendTail(output);
        baseUrl = output.toString();
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public String getTileUrl(int zoom, int tilex, int tiley) {
        Matcher m = Pattern.compile(".*"+PATTERN_ZOOM+".*").matcher(this.baseUrl);
        if (m.matches() && !m.group(1).isEmpty()) {
            String ofs = m.group(1);
            if(ofs.startsWith("+"))
                ofs = ofs.substring(1);
            zoom += Integer.valueOf(ofs);
        }
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
