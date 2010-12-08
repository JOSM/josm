// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.net.URLDecoder;

import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadTask;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;

/**
 * Handler for import request
 */
public class ImportHandler extends RequestHandler {

    public static final String command = "import";
    public static final String permissionKey = "remotecontrol.permission.import";
    public static final boolean permissionDefault = true;

    @Override
    protected void handleRequest() throws RequestHandlerErrorException {
        try {
            DownloadTask osmTask = new DownloadOsmTask();
            osmTask.loadUrl(false, URLDecoder.decode(args.get("url"), "UTF-8"), null);
        } catch (Exception ex) {
            System.out.println("RemoteControl: Error parsing import remote control request:");
            ex.printStackTrace();
            throw new RequestHandlerErrorException();
        }
    }

    @Override
    protected String[] getMandatoryParams()
    {
        return new String[] { "url" };
    }

    @Override
    public String getPermissionMessage() {
        return tr("Remote Control has been asked to import data from the following URL:") +
        "<br>" + request;
    }

    @Override
    public PermissionPrefWithDefault getPermissionPref()
    {
        return new PermissionPrefWithDefault(permissionKey, permissionDefault,
                "RemoteControl: import forbidden by preferences");
    }
}