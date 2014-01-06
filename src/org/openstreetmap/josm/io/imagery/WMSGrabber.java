// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.imagery;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.imagery.GeorefImage.State;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.WMSLayer;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.io.ProgressInputStream;
import org.openstreetmap.josm.tools.Utils;


public class WMSGrabber extends Grabber {

    protected String baseURL;
    private ImageryInfo info;
    private Map<String, String> props = new HashMap<String, String>();

    public WMSGrabber(MapView mv, WMSLayer layer, boolean localOnly) {
        super(mv, layer, localOnly);
        this.info = layer.getInfo();
        this.baseURL = info.getUrl();
        if(layer.getInfo().getCookies() != null && !layer.getInfo().getCookies().isEmpty()) {
            props.put("Cookie", layer.getInfo().getCookies());
        }
        Pattern pattern = Pattern.compile("\\{header\\(([^,]+),([^}]+)\\)\\}");
        StringBuffer output = new StringBuffer();
        Matcher matcher = pattern.matcher(this.baseURL);
        while (matcher.find()) {
            props.put(matcher.group(1),matcher.group(2));
            matcher.appendReplacement(output, "");
        }
        matcher.appendTail(output);
        this.baseURL = output.toString();
    }

    @Override
    void fetch(WMSRequest request, int attempt) throws Exception{
        URL url = null;
        try {
            url = getURL(
                    b.minEast, b.minNorth,
                    b.maxEast, b.maxNorth,
                    width(), height());
            request.finish(State.IMAGE, grab(request, url, attempt));

        } catch(Exception e) {
            Main.error(e);
            throw new Exception(e.getMessage() + "\nImage couldn't be fetched: " + (url != null ? url.toString() : ""), e);
        }
    }

    public static final NumberFormat latLonFormat = new DecimalFormat("###0.0000000",
            new DecimalFormatSymbols(Locale.US));

    protected URL getURL(double w, double s,double e,double n,
            int wi, int ht) throws MalformedURLException {
        String myProj = Main.getProjection().toCode();
        if (!info.getServerProjections().contains(myProj) && "EPSG:3857".equals(Main.getProjection().toCode())) {
            LatLon sw = Main.getProjection().eastNorth2latlon(new EastNorth(w, s));
            LatLon ne = Main.getProjection().eastNorth2latlon(new EastNorth(e, n));
            myProj = "EPSG:4326";
            s = sw.lat();
            w = sw.lon();
            n = ne.lat();
            e = ne.lon();
        }
        if (myProj.equals("EPSG:4326") && !info.getServerProjections().contains(myProj) && info.getServerProjections().contains("CRS:84")) {
            myProj = "CRS:84";
        }

        // Bounding box coordinates have to be switched for WMS 1.3.0 EPSG:4326.
        //
        // Background:
        //
        // bbox=x_min,y_min,x_max,y_max
        //
        //      SRS=... is WMS 1.1.1
        //      CRS=... is WMS 1.3.0
        //
        // The difference:
        //      For SRS x is east-west and y is north-south
        //      For CRS x and y are as specified by the EPSG
        //          E.g. [1] lists lat as first coordinate axis and lot as second, so it is switched for EPSG:4326.
        //          For most other EPSG code there seems to be no difference.
        // [1] http://www.epsg-registry.org/report.htm?type=selection&entity=urn:ogc:def:crs:EPSG::4326&reportDetail=short&style=urn:uuid:report-style:default-with-code&style_name=OGP%20Default%20With%20Code&title=EPSG:4326
        boolean switchLatLon = false;
        if (baseURL.toLowerCase().contains("crs=epsg:4326")) {
            switchLatLon = true;
        } else if (baseURL.toLowerCase().contains("crs=") && myProj.equals("EPSG:4326")) {
            switchLatLon = true;
        }
        String bbox;
        if (switchLatLon) {
            bbox = String.format("%s,%s,%s,%s", latLonFormat.format(s), latLonFormat.format(w), latLonFormat.format(n), latLonFormat.format(e));
        } else {
            bbox = String.format("%s,%s,%s,%s", latLonFormat.format(w), latLonFormat.format(s), latLonFormat.format(e), latLonFormat.format(n));
        }
        return new URL(baseURL.replaceAll("\\{proj(\\([^})]+\\))?\\}", myProj)
                .replaceAll("\\{bbox\\}", bbox)
                .replaceAll("\\{w\\}", latLonFormat.format(w))
                .replaceAll("\\{s\\}", latLonFormat.format(s))
                .replaceAll("\\{e\\}", latLonFormat.format(e))
                .replaceAll("\\{n\\}", latLonFormat.format(n))
                .replaceAll("\\{width\\}", String.valueOf(wi))
                .replaceAll("\\{height\\}", String.valueOf(ht))
                .replace(" ", "%20"));
    }

    @Override
    public boolean loadFromCache(WMSRequest request) {
        BufferedImage cached = layer.cache.getExactMatch(Main.getProjection(), request.getPixelPerDegree(), b.minEast, b.minNorth);

        if (cached != null) {
            request.finish(State.IMAGE, cached);
            return true;
        } else if (request.isAllowPartialCacheMatch()) {
            BufferedImage partialMatch = layer.cache.getPartialMatch(Main.getProjection(), request.getPixelPerDegree(), b.minEast, b.minNorth);
            if (partialMatch != null) {
                request.finish(State.PARTLY_IN_CACHE, partialMatch);
                return true;
            }
        }

        if((!request.isReal() && !layer.hasAutoDownload())){
            request.finish(State.NOT_IN_CACHE, null);
            return true;
        }

        return false;
    }

    protected BufferedImage grab(WMSRequest request, URL url, int attempt) throws IOException, OsmTransferException {
        Main.info("Grabbing WMS " + (attempt > 1? "(attempt " + attempt + ") ":"") + url);

        HttpURLConnection conn = Utils.openHttpConnection(url);
        for(Entry<String, String> e : props.entrySet()) {
            conn.setRequestProperty(e.getKey(), e.getValue());
        }
        conn.setConnectTimeout(Main.pref.getInteger("socket.timeout.connect",15) * 1000);
        conn.setReadTimeout(Main.pref.getInteger("socket.timeout.read", 30) * 1000);

        String contentType = conn.getHeaderField("Content-Type");
        if( conn.getResponseCode() != 200
                || contentType != null && !contentType.startsWith("image") )
            throw new IOException(readException(conn));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream is = new ProgressInputStream(conn, null);
        try {
            Utils.copyStream(is, baos);
        } finally {
            Utils.close(is);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        BufferedImage img = layer.normalizeImage(ImageIO.read(bais));
        bais.reset();
        layer.cache.saveToCache(layer.isOverlapEnabled()?img:null, bais, Main.getProjection(), request.getPixelPerDegree(), b.minEast, b.minNorth);
        return img;
    }

    protected String readException(URLConnection conn) throws IOException {
        StringBuilder exception = new StringBuilder();
        InputStream in = conn.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        try {
            String line = null;
            while( (line = br.readLine()) != null) {
                // filter non-ASCII characters and control characters
                exception.append(line.replaceAll("[^\\p{Print}]", ""));
                exception.append('\n');
            }
            return exception.toString();
        } finally {
            Utils.close(br);
        }
    }
}
