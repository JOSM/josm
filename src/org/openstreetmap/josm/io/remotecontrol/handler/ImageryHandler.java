// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;

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
    public PermissionPrefWithDefault getPermissionPref() {
        return PermissionPrefWithDefault.LOAD_IMAGERY;
    }

    @Override
    protected void handleRequest() throws RequestHandlerErrorException {
        if (Main.map == null) //Avoid exception when creating ImageryLayer with null MapFrame
        {
            throw new RequestHandlerErrorException();
        }
        String url = args.get("url");
        String title = args.get("title");
        String type = args.get("type");
        if ((title == null) || (title.isEmpty())) {
            title = tr("Remote imagery");
        }
        String cookies = args.get("cookies");
        ImageryInfo imgInfo = new ImageryInfo(title, url, type, null, cookies);
        String min_zoom = args.get("min_zoom");
        if (min_zoom != null && !min_zoom.isEmpty()) {
            try {
                imgInfo.setDefaultMinZoom(Integer.parseInt(min_zoom));
            } catch (NumberFormatException e) {
                System.err.println(e.getMessage());
            }
        }
        String max_zoom = args.get("max_zoom");
        if (max_zoom != null && !max_zoom.isEmpty()) {
            try {
                imgInfo.setDefaultMaxZoom(Integer.parseInt(max_zoom));
            } catch (NumberFormatException e) {
                System.err.println(e.getMessage());
            }
        }
        Main.main.addLayer(ImageryLayer.create(imgInfo));
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

    private String decodeParam(String param) {
        try {
            return URLDecoder.decode(param, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException();
        }
    }
}
