// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.data.imagery.ImageryLayerInfo;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Adds an imagery (WMS/TMS) layer. For instance, {@code /imagery?title=...&type=...&url=...}.
 * @since 3715
 */
public class ImageryHandler extends RequestHandler.RawURLParseRequestHandler {

    /**
     * The remote control command name used to add an imagery layer.
     */
    public static final String command = "imagery";

    @Override
    public String getPermissionMessage() {
        return tr("Remote Control has been asked to load an imagery layer from the following URL:")
                + "<br>" + args.get("url");
    }

    @Override
    public String[] getMandatoryParams() {
        return new String[]{"url"};
    }

    @Override
    public String[] getOptionalParams() {
        return new String[] {"title", "type", "cookies", "min_zoom", "max_zoom"};
    }

    @Override
    public PermissionPrefWithDefault getPermissionPref() {
        return PermissionPrefWithDefault.LOAD_IMAGERY;
    }

    protected static ImageryInfo findBingEntry() {
        for (ImageryInfo i : ImageryLayerInfo.instance.getDefaultLayers()) {
            if (ImageryType.BING.equals(i.getImageryType())) {
                return i;
            }
        }
        return null;
    }

    protected ImageryInfo buildImageryInfo() {
        String url = args.get("url");
        String title = args.get("title");
        String type = args.get("type");
        final ImageryInfo bing = ImageryType.BING.getTypeString().equals(type) ? findBingEntry() : null;
        if ((title == null || title.isEmpty()) && bing != null) {
            title = bing.getName();
        }
        if (title == null || title.isEmpty()) {
            title = tr("Remote imagery");
        }
        String cookies = args.get("cookies");
        final ImageryInfo imgInfo = new ImageryInfo(title, url, type, null, cookies);
        if (bing != null) {
            imgInfo.setIcon(bing.getIcon());
        }
        String minZoom = args.get("min_zoom");
        if (minZoom != null && !minZoom.isEmpty()) {
            try {
                imgInfo.setDefaultMinZoom(Integer.parseInt(minZoom));
            } catch (NumberFormatException e) {
                Logging.error(e);
            }
        }
        String maxZoom = args.get("max_zoom");
        if (maxZoom != null && !maxZoom.isEmpty()) {
            try {
                imgInfo.setDefaultMaxZoom(Integer.parseInt(maxZoom));
            } catch (NumberFormatException e) {
                Logging.error(e);
            }
        }
        return imgInfo;
    }

    @Override
    protected void handleRequest() throws RequestHandlerErrorException {
        final ImageryInfo imgInfo = buildImageryInfo();
        if (Main.isDisplayingMapView()) {
            for (ImageryLayer layer : Main.getLayerManager().getLayersOfType(ImageryLayer.class)) {
                if (layer.getInfo().equals(imgInfo)) {
                    Logging.info("Imagery layer already exists: "+imgInfo);
                    return;
                }
            }
        }
        GuiHelper.runInEDT(() -> {
            try {
                Main.getLayerManager().addLayer(ImageryLayer.create(imgInfo));
            } catch (IllegalArgumentException e) {
                Logging.log(Logging.LEVEL_ERROR, e);
            }
        });
    }

    @Override
    protected void validateRequest() throws RequestHandlerBadRequestException {
        String url = args != null ? args.get("url") : null;
        String type = args != null ? args.get("type") : null;
        String cookies = args != null ? args.get("cookies") : null;
        try {
            ImageryLayer.create(new ImageryInfo(null, url, type, null, cookies));
        } catch (IllegalArgumentException e) {
            throw new RequestHandlerBadRequestException(e.getMessage(), e);
        }
    }

    @Override
    public String getUsage() {
        return "adds an imagery layer (e.g. WMS, TMS)";
    }

    @Override
    public String[] getUsageExamples() {
        final String types = Utils.join("|", Utils.transform(Arrays.asList(ImageryInfo.ImageryType.values()),
                ImageryType::getTypeString));
        return new String[] {
            "/imagery?title=osm&type=tms&url=https://a.tile.openstreetmap.org/%7Bzoom%7D/%7Bx%7D/%7By%7D.png",
            "/imagery?title=landsat&type=wms&url=http://irs.gis-lab.info/?" +
                    "layers=landsat&SRS=%7Bproj%7D&WIDTH=%7Bwidth%7D&HEIGHT=%7Bheight%7D&BBOX=%7Bbbox%7D",
            "/imagery?title=...&type={"+types+"}&url=....[&cookies=...][&min_zoom=...][&max_zoom=...]"
            };
    }
}
