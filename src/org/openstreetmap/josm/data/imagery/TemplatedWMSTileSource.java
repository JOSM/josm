// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.gui.jmapviewer.interfaces.TemplatedTileSource;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.layer.WMSLayer;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Utils;

/**
 * Tile Source handling WMS providers
 *
 * @author Wiktor Niesiobędzki
 * @since 8526
 */
public class TemplatedWMSTileSource extends AbstractWMSTileSource implements TemplatedTileSource {
    // CHECKSTYLE.OFF: SingleSpaceSeparator
    private static final Pattern PATTERN_HEADER = Pattern.compile("\\{header\\(([^,]+),([^}]+)\\)\\}");
    private static final Pattern PATTERN_PROJ   = Pattern.compile("\\{proj\\}");
    private static final Pattern PATTERN_WKID   = Pattern.compile("\\{wkid\\}");
    private static final Pattern PATTERN_BBOX   = Pattern.compile("\\{bbox\\}");
    private static final Pattern PATTERN_W      = Pattern.compile("\\{w\\}");
    private static final Pattern PATTERN_S      = Pattern.compile("\\{s\\}");
    private static final Pattern PATTERN_E      = Pattern.compile("\\{e\\}");
    private static final Pattern PATTERN_N      = Pattern.compile("\\{n\\}");
    private static final Pattern PATTERN_WIDTH  = Pattern.compile("\\{width\\}");
    private static final Pattern PATTERN_HEIGHT = Pattern.compile("\\{height\\}");
    private static final Pattern PATTERN_TIME   = Pattern.compile("\\{time\\}"); // Sentinel-2
    private static final Pattern PATTERN_PARAM  = Pattern.compile("\\{([^}]+)\\}");
    // CHECKSTYLE.ON: SingleSpaceSeparator

    private static final NumberFormat LATLON_FORMAT = new DecimalFormat("###0.0000000", new DecimalFormatSymbols(Locale.US));

    private static final Pattern[] ALL_PATTERNS = {
            PATTERN_HEADER, PATTERN_PROJ, PATTERN_WKID, PATTERN_BBOX,
            PATTERN_W, PATTERN_S, PATTERN_E, PATTERN_N,
            PATTERN_WIDTH, PATTERN_HEIGHT, PATTERN_TIME,
    };

    private final Set<String> serverProjections;
    private final Map<String, String> headers = new ConcurrentHashMap<>();
    private final String date;
    private final boolean switchLatLon;

    /**
     * Creates a tile source based on imagery info
     * @param info imagery info
     * @param tileProjection the tile projection
     */
    public TemplatedWMSTileSource(ImageryInfo info, Projection tileProjection) {
        super(info, tileProjection);
        this.serverProjections = new TreeSet<>(info.getServerProjections());
        this.headers.putAll(info.getCustomHttpHeaders());
        this.date = info.getDate();
        handleTemplate();
        initProjection();
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
        // CHECKSTYLE.OFF: LineLength
        // [1] https://www.epsg-registry.org/report.htm?type=selection&entity=urn:ogc:def:crs:EPSG::4326&reportDetail=short&style=urn:uuid:report-style:default-with-code&style_name=OGP%20Default%20With%20Code&title=EPSG:4326
        // CHECKSTYLE.ON: LineLength
        if (baseUrl.toLowerCase(Locale.US).contains("crs=epsg:4326")) {
            switchLatLon = true;
        } else if (baseUrl.toLowerCase(Locale.US).contains("crs=")) {
            // assume WMS 1.3.0
            switchLatLon = ProjectionRegistry.getProjection().switchXY();
        } else {
            switchLatLon = false;
        }
    }

    @Override
    public int getDefaultTileSize() {
        return WMSLayer.PROP_IMAGE_SIZE.get();
    }

    @Override
    public String getTileUrl(int zoom, int tilex, int tiley) {
        String myProjCode = getServerCRS();

        EastNorth nw = getTileEastNorth(tilex, tiley, zoom);
        EastNorth se = getTileEastNorth(tilex + 1, tiley + 1, zoom);

        double w = nw.getX();
        double n = nw.getY();

        double s = se.getY();
        double e = se.getX();

        if ("EPSG:4326".equals(myProjCode) && !serverProjections.contains(myProjCode) && serverProjections.contains("CRS:84")) {
            myProjCode = "CRS:84";
        }

        // Using StringBuffer and generic PATTERN_PARAM matcher gives 2x performance improvement over replaceAll
        StringBuffer url = new StringBuffer(baseUrl.length());
        Matcher matcher = PATTERN_PARAM.matcher(baseUrl);
        while (matcher.find()) {
            String replacement;
            switch (matcher.group(1)) {
            case "proj":
                replacement = myProjCode;
                break;
            case "wkid":
                replacement = myProjCode.startsWith("EPSG:") ? myProjCode.substring(5) : myProjCode;
                break;
            case "bbox":
                replacement = getBbox(zoom, tilex, tiley, switchLatLon);
                break;
            case "w":
                replacement = LATLON_FORMAT.format(w);
                break;
            case "s":
                replacement = LATLON_FORMAT.format(s);
                break;
            case "e":
                replacement = LATLON_FORMAT.format(e);
                break;
            case "n":
                replacement = LATLON_FORMAT.format(n);
                break;
            case "width":
            case "height":
                replacement = String.valueOf(getTileSize());
                break;
            case "time":
                replacement = Utils.encodeUrl(date);
                break;
            default:
                replacement = '{' + matcher.group(1) + '}';
            }
            matcher.appendReplacement(url, replacement);
        }
        matcher.appendTail(url);
        return url.toString().replace(" ", "%20");
    }

    @Override
    public String getTileId(int zoom, int tilex, int tiley) {
        return getTileUrl(zoom, tilex, tiley);
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Checks if url is acceptable by this Tile Source
     * @param url URL to check
     */
    public static void checkUrl(String url) {
        CheckParameterUtil.ensureParameterNotNull(url, "url");
        Matcher m = PATTERN_PARAM.matcher(url);
        while (m.find()) {
            boolean isSupportedPattern = Arrays.stream(ALL_PATTERNS)
                    .anyMatch(pattern -> pattern.matcher(m.group()).matches());
            if (!isSupportedPattern) {
                throw new IllegalArgumentException(
                        tr("{0} is not a valid WMS argument. Please check this server URL:\n{1}", m.group(), url));
            }
        }
    }

    private void handleTemplate() {
        // Capturing group pattern on switch values
        StringBuffer output = new StringBuffer();
        Matcher matcher = PATTERN_HEADER.matcher(this.baseUrl);
        while (matcher.find()) {
            headers.put(matcher.group(1), matcher.group(2));
            matcher.appendReplacement(output, "");
        }
        matcher.appendTail(output);
        this.baseUrl = output.toString();
    }
}
