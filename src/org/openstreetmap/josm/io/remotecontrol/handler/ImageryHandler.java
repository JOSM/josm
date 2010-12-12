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
    protected String[] getMandatoryParams()
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
        if((title == null) || (title.length() == 0))
        {
            title = tr("Remote imagery");
        }
        String cookies = args.get("cookies");
        ImageryLayer imgLayer = ImageryLayer.create(new ImageryInfo(title, url, cookies));
        Main.main.addLayer(imgLayer);

    }

    @Override
    public void parseArgs() {
        StringTokenizer st = new StringTokenizer(request, "&?");
        HashMap<String, String> args = new HashMap<String, String>();
        // skip first element which is the command
        if(st.hasMoreTokens()) {
            st.nextToken();
        }
        while (st.hasMoreTokens()) {
            String param = st.nextToken();
            int eq = param.indexOf("=");
            if (eq > -1)
            {
                String key = param.substring(0, eq);
                /* "url=" terminates normal parameters
                 * and will be handled separately
                 */
                if("url".equals(key)) {
                    break;
                }

                String value = param.substring(eq + 1);
                // urldecode all normal values
                try {
                    value = URLDecoder.decode(value, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                args.put(key,
                        value);
            }
        }
        // url as second or later parameter
        int urlpos = request.indexOf("&url=");
        // url as first (and only) parameter
        if(urlpos < 0) {
            urlpos = request.indexOf("?url=");
        }
        // url found?
        if(urlpos >= 0) {
            // URL value
            String value = request.substring(urlpos + 5);
            // allow skipping URL decoding with urldecode=false
            String urldecode = args.get("urldecode");
            if((urldecode == null) || (Boolean.valueOf(urldecode) == true))
            {
                try {
                    value = URLDecoder.decode(value, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            args.put("url", value);
        }
        this.args = args;
    }
}
