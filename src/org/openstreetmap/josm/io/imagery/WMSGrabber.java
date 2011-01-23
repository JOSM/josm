// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
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
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.imagery.GeorefImage.State;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.projection.Mercator;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.WMSLayer;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.io.ProgressInputStream;


public class WMSGrabber extends Grabber {

    protected String baseURL;
    private final boolean urlWithPatterns;

    public WMSGrabber(MapView mv, WMSLayer layer) {
        super(mv, layer);
        this.baseURL = layer.getInfo().getURL();
        /* URL containing placeholders? */
        urlWithPatterns = ImageryInfo.isUrlWithPatterns(baseURL);
    }

    @Override
    void fetch(WMSRequest request, int attempt) throws Exception{
        URL url = null;
        try {
            url = getURL(
                    b.min.east(), b.min.north(),
                    b.max.east(), b.max.north(),
                    width(), height());
            request.finish(State.IMAGE, grab(url, attempt));

        } catch(Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage() + "\nImage couldn't be fetched: " + (url != null ? url.toString() : ""));
        }
    }

    public static final NumberFormat latLonFormat = new DecimalFormat("###0.0000000",
            new DecimalFormatSymbols(Locale.US));

    protected URL getURL(double w, double s,double e,double n,
            int wi, int ht) throws MalformedURLException {
        String myProj = Main.proj.toCode();
        if(Main.proj instanceof Mercator) // don't use mercator code directly
        {
            LatLon sw = Main.proj.eastNorth2latlon(new EastNorth(w, s));
            LatLon ne = Main.proj.eastNorth2latlon(new EastNorth(e, n));
            myProj = "EPSG:4326";
            s = sw.lat();
            w = sw.lon();
            n = ne.lat();
            e = ne.lon();
        }

        String str = baseURL;
        String bbox = latLonFormat.format(w) + ","
        + latLonFormat.format(s) + ","
        + latLonFormat.format(e) + ","
        + latLonFormat.format(n);

        if (urlWithPatterns) {
            str = str.replaceAll("\\{proj\\}", myProj)
            .replaceAll("\\{bbox\\}", bbox)
            .replaceAll("\\{w\\}", latLonFormat.format(w))
            .replaceAll("\\{s\\}", latLonFormat.format(s))
            .replaceAll("\\{e\\}", latLonFormat.format(e))
            .replaceAll("\\{n\\}", latLonFormat.format(n))
            .replaceAll("\\{width\\}", String.valueOf(wi))
            .replaceAll("\\{height\\}", String.valueOf(ht));
        } else {
            str += "bbox=" + bbox
            + getProjection(baseURL, false)
            + "&width=" + wi + "&height=" + ht;
            if (!(baseURL.endsWith("&") || baseURL.endsWith("?"))) {
                System.out.println(tr("Warning: The base URL ''{0}'' for a WMS service doesn't have a trailing '&' or a trailing '?'.", baseURL));
                System.out.println(tr("Warning: Fetching WMS tiles is likely to fail. Please check you preference settings."));
                System.out.println(tr("Warning: The complete URL is ''{0}''.", str));
            }
        }
        return new URL(str.replace(" ", "%20"));
    }

    static public String getProjection(String baseURL, Boolean warn)
    {
        String projname = Main.proj.toCode();
        if(Main.proj instanceof Mercator) {
            projname = "EPSG:4326";
        }
        String res = "";
        try
        {
            Matcher m = Pattern.compile(".*srs=([a-z0-9:]+).*").matcher(baseURL.toLowerCase());
            if(m.matches())
            {
                projname = projname.toLowerCase();
                if(!projname.equals(m.group(1)) && warn)
                {
                    JOptionPane.showMessageDialog(Main.parent,
                            tr("The projection ''{0}'' in URL and current projection ''{1}'' mismatch.\n"
                                    + "This may lead to wrong coordinates.",
                                    m.group(1), projname),
                                    tr("Warning"),
                                    JOptionPane.WARNING_MESSAGE);
                }
            } else {
                res ="&srs="+projname;
            }
        }
        catch(Exception e)
        {
        }
        return res;
    }

    @Override
    public boolean loadFromCache(WMSRequest request) {
        URL url = null;
        try{
            url = getURL(
                    b.min.east(), b.min.north(),
                    b.max.east(), b.max.north(),
                    width(), height());
        } catch(Exception e) {
            return false;
        }
        BufferedImage cached = cache.getImg(url.toString());
        if((!request.isReal() && !layer.hasAutoDownload()) || cached != null){
            if(cached == null){
                request.finish(State.NOT_IN_CACHE, null);
                return true;
            }
            request.finish(State.IMAGE, cached);
            return true;
        }
        return false;
    }

    protected BufferedImage grab(URL url, int attempt) throws IOException, OsmTransferException {
        System.out.println("Grabbing WMS " + (attempt > 1? "(attempt " + attempt + ") ":"") + url);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        if(layer.getInfo().getCookies() != null && !layer.getInfo().getCookies().equals("")) {
            conn.setRequestProperty("Cookie", layer.getInfo().getCookies());
        }
        conn.setRequestProperty("User-Agent", Main.pref.get("imagery.wms.user_agent", Version.getInstance().getAgentString()));
        conn.setConnectTimeout(Main.pref.getInteger("imagery.wms.timeout.connect", 30) * 1000);
        conn.setReadTimeout(Main.pref.getInteger("imagery.wms.timeout.read", 30) * 1000);

        String contentType = conn.getHeaderField("Content-Type");
        if( conn.getResponseCode() != 200
                || contentType != null && !contentType.startsWith("image") )
            throw new IOException(readException(conn));

        InputStream is = new ProgressInputStream(conn, null);
        BufferedImage img = layer.normalizeImage(ImageIO.read(is));
        is.close();

        cache.saveImg(url.toString(), img);
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
            br.close();
        }
    }
}
