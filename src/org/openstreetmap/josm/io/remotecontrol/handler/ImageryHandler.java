// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;
import java.util.HashMap;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;
import org.openstreetmap.josm.tools.Utils;

/**
 * Adds an imagery (WMS/TMS) layer. For instance, {@code /imagery?title=...&type=...&url=...}.
 * @since 3715
 */
public class ImageryHandler extends RequestHandler {

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
        return new String[] { "title", "type", "cookies", "min_zoom", "max_zoom"};
    }

    @Override
    public PermissionPrefWithDefault getPermissionPref() {
        return PermissionPrefWithDefault.LOAD_IMAGERY;
    }

    @Override
    protected void handleRequest() throws RequestHandlerErrorException {
        String url = args.get("url");
        String title = args.get("title");
        String type = args.get("type");
        if ((title == null) || (title.isEmpty())) {
            title = tr("Remote imagery");
        }
        String cookies = args.get("cookies");
        final ImageryInfo imgInfo = new ImageryInfo(title, url, type, null, cookies);
        String min_zoom = args.get("min_zoom");
        if (min_zoom != null && !min_zoom.isEmpty()) {
            try {
                imgInfo.setDefaultMinZoom(Integer.parseInt(min_zoom));
            } catch (NumberFormatException e) {
                Main.error(e);
            }
        }
        String max_zoom = args.get("max_zoom");
        if (max_zoom != null && !max_zoom.isEmpty()) {
            try {
                imgInfo.setDefaultMaxZoom(Integer.parseInt(max_zoom));
            } catch (NumberFormatException e) {
                Main.error(e);
            }
        }
        GuiHelper.runInEDT(new Runnable() {
            @Override public void run() {
                Main.main.addLayer(ImageryLayer.create(imgInfo));
            }
        });
    }

    @Override
    protected void parseArgs() {
        HashMap<String, String> args = new HashMap<String, String>();
        if (request.indexOf('?') != -1) {
            String query = request.substring(request.indexOf('?') + 1);
            if (query.indexOf("url=") == 0) {
                args.put("url", decodeParam(query.substring(4)));
            } else {
                int urlIdx = query.indexOf("&url=");
                if (urlIdx != -1) {
                    args.put("url", decodeParam(query.substring(urlIdx + 5)));
                    query = query.substring(0, urlIdx);
                } else {
                    if (query.indexOf('#') != -1) {
                        query = query.substring(0, query.indexOf('#'));
                    }
                }
                String[] params = query.split("&", -1);
                for (String param : params) {
                    int eq = param.indexOf('=');
                    if (eq != -1) {
                        args.put(param.substring(0, eq), decodeParam(param.substring(eq + 1)));
                    }
                }
            }
        }
        this.args = args;
    }
    
    @Override
    protected void validateRequest() throws RequestHandlerBadRequestException {
        // Nothing to do
    }

    @Override
    public String getUsage() {
        return "adds an imagery layer (e.g. WMS, TMS))";
    }

    @Override
    public String[] getUsageExamples() {
        final String types = Utils.join("|", Utils.transform(Arrays.asList(ImageryInfo.ImageryType.values()), new Utils.Function<ImageryInfo.ImageryType, String>() {
            @Override
            public String apply(ImageryInfo.ImageryType x) {
                return x.getTypeString();
            }
        }));
        return new String[] { "/imagery?title=osm&type=tms&url=http://tile.openstreetmap.org/%7Bzoom%7D/%7Bx%7D/%7By%7D.png",
            "/imagery?title=landsat&type=wms&url=http://irs.gis-lab.info/?layers=landsat&SRS=%7Bproj%7D&WIDTH=%7Bwidth%7D&HEIGHT=%7Bheight%7D&BBOX=%7Bbbox%7D",
            "/imagery?title=...&type={"+types+"}&url=....[&cookies=...][&min_zoom=...][&max_zoom=...]"};
    }
}
