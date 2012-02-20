// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;

public class ImageryHandler extends RequestHandler {
    public static final String command = "imagery";
    public static final String permissionKey = "remotecontrol.permission.imagery";
    public static final boolean permissionDefault = true;

    @Override
    public String getPermissionMessage() {
        return tr("Remote Control has been asked to load an imagery layer from the following URL:") +
        "<br>" + args.get("url");
    }

    @Override
    public String[] getMandatoryParams()
    {
        return new String[] { "url" };
    }

    @Override
    public PermissionPrefWithDefault getPermissionPref()
    {
        return new PermissionPrefWithDefault(permissionKey, permissionDefault,
        "RemoteControl: import forbidden by preferences");
    }

    @Override
    protected void handleRequest() throws RequestHandlerErrorException {
        if (Main.map == null) //Avoid exception when creating ImageryLayer with null MapFrame
            throw new RequestHandlerErrorException();
        String url = args.get("url");
        String title = args.get("title");
        String type = args.get("type");
        if((title == null) || (title.length() == 0))
        {
            title = tr("Remote imagery");
        }
        String cookies = args.get("cookies");
        ImageryLayer imgLayer = ImageryLayer.create(new ImageryInfo(title, url, type, null, cookies));
        Main.main.addLayer(imgLayer);
    }

    @Override
    protected void parseArgs() {
        HashMap<String, String> args = new HashMap<String, String>();
        if (request.indexOf('?') != -1) {
            String query = request.substring(request.indexOf('?') + 1);
            if (query.indexOf("url=") == 0) {
                args.put("url", decodeURL(query.substring(4)));
            } else {
                int urlIdx = query.indexOf("&url=");
                if (urlIdx != -1) {
                    String url = query.substring(urlIdx + 1);
                    args.put("url", decodeURL(query.substring(urlIdx + 5)));
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
                        args.put(param.substring(0, eq), param.substring(eq + 1));
                    }
                }
            }
        }
        this.args = args;
    }

    private String decodeURL(String url) {
        try {
            return URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException();
        }
    }
}
