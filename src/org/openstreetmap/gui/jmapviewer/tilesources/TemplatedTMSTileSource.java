package org.openstreetmap.gui.jmapviewer.tilesources;

import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class TemplatedTMSTileSource extends TMSTileSource {
    Random rand = null;
    String[] randomParts = null;
    public TemplatedTMSTileSource(String name, String url, int maxZoom) {
        super(name, url, maxZoom);
    }

    public TemplatedTMSTileSource(String name, String url, int minZoom, int maxZoom) {
        super(name, url, minZoom, maxZoom);
        Matcher m = Pattern.compile(".*\\{switch:([^}]+)\\}.*").matcher(url);
        if(m.matches()) {
            rand = new Random();
            randomParts = m.group(1).split(",");
        }
    }

    @Override
    public String getTileUrl(int zoom, int tilex, int tiley) {
        String r = this.baseUrl
        .replaceAll("\\{zoom\\}", Integer.toString(zoom))
        .replaceAll("\\{x\\}", Integer.toString(tilex))
        .replaceAll("\\{y\\}", Integer.toString(tiley))
        .replaceAll("\\{!y\\}", Integer.toString((int)Math.pow(2, zoom)-1-tiley));
        if(rand != null) {
            r = r.replaceAll("\\{switch:[^}]+\\}",
            randomParts[rand.nextInt(randomParts.length)]);
        }
        return r;
    }
}
