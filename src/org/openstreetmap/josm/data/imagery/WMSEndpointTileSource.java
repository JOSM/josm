// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.openstreetmap.gui.jmapviewer.interfaces.TemplatedTileSource;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.layer.WMSLayer;
import org.openstreetmap.josm.io.imagery.WMSImagery;
import org.openstreetmap.josm.io.imagery.WMSImagery.WMSGetCapabilitiesException;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Class representing ImageryType.WMS_ENDPOINT tile source.
 * It differs from standard WMS tile source that this tile source fetches GetCapabilities from server and
 * uses most of the parameters from there
 *
 * @author Wiktor Niesiobedzki
 * @since 13733
 */
public class WMSEndpointTileSource extends AbstractWMSTileSource implements TemplatedTileSource {

    private final WMSImagery wmsi;
    private final List<DefaultLayer> layers;
    private final String urlPattern;
    private static final Pattern PATTERN_PARAM = Pattern.compile("\\{([^}]+)\\}");
    private final Map<String, String> headers = new ConcurrentHashMap<>();

    /**
     * Create WMSEndpointTileSource tile source
     * @param info WMS_ENDPOINT ImageryInfo
     * @param tileProjection server projection that should be used by this tile source
     */
    public WMSEndpointTileSource(ImageryInfo info, Projection tileProjection) {
        super(info, tileProjection);
        CheckParameterUtil.ensureThat(info.getImageryType() == ImageryType.WMS_ENDPOINT, "imageryType");
        try {
            wmsi = new WMSImagery(info.getUrl(), info.getCustomHttpHeaders());
        } catch (IOException | WMSGetCapabilitiesException e) {
            throw new IllegalArgumentException(e);
        }
        layers = info.getDefaultLayers();
        initProjection();
        urlPattern = wmsi.buildGetMapUrl(layers, info.isTransparent());
        this.headers.putAll(info.getCustomHttpHeaders());
    }

    @Override
    public int getDefaultTileSize() {
        return WMSLayer.PROP_IMAGE_SIZE.get();
    }

    @Override
    public String getTileUrl(int zoom, int tilex, int tiley) {
        // Using StringBuffer and generic PATTERN_PARAM matcher gives 2x performance improvement over replaceAll
        StringBuffer url = new StringBuffer(urlPattern.length());
        Matcher matcher = PATTERN_PARAM.matcher(urlPattern);
        while (matcher.find()) {
            String replacement;
            switch (matcher.group(1)) {
            case "proj":
                replacement = getServerCRS();
                break;
            case "bbox":
                replacement = getBbox(zoom, tilex, tiley, !wmsi.belowWMS130() && getTileProjection().switchXY());
                break;
            case "width":
            case "height":
                replacement = String.valueOf(getTileSize());
                break;
            default:
                replacement = '{' + matcher.group(1) + '}';
            }
            matcher.appendReplacement(url, replacement);
        }
        matcher.appendTail(url);
        return url.toString();
    }

    /**
     * Returns list of EPSG codes that current layer selection supports.
     * @return list of EPSG codes that current layer selection supports (this may differ from layer to layer)
     */
    public List<String> getServerProjections() {
        return wmsi.getLayers(layers).stream().flatMap(x -> x.getCrs().stream()).distinct().collect(Collectors.toList());
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }
}
