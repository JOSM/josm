// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadTask;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;

/**
 * Handler for import request
 */
public class ImportHandler extends RequestHandler {

    /**
     * The remote control command name used to import data.
     */
    public static final String command = "import";
    
    private URL url; // parameter 'url'
    private Boolean new_layer; // parameter 'nl'
    private Collection<DownloadTask> suitableDownloadTasks;

    @Override
    protected void handleRequest() throws RequestHandlerErrorException {
        try {
            if (suitableDownloadTasks != null && !suitableDownloadTasks.isEmpty()) {
                // TODO: handle multiple suitable download tasks ?
                suitableDownloadTasks.iterator().next().loadUrl(new_layer, url.toExternalForm(), null);
            }
        } catch (Exception ex) {
            System.out.println("RemoteControl: Error parsing import remote control request:");
            ex.printStackTrace();
            throw new RequestHandlerErrorException();
        }
    }

    @Override
    public String[] getMandatoryParams() {
        return new String[]{"url"};
    }

    @Override
    public String getPermissionMessage() {
        // URL can be any suitable URL giving back OSM data, including OSM API calls, even if calls to the main API
        // should rather be passed to LoadAndZoomHandler or LoadObjectHandler.
        // Other API instances will however use the import handler to force JOSM to make requests to this API instance.
        // (Example with OSM-FR website that makes calls to the OSM-FR API)
        // For user-friendliness, let's try to decode these OSM API calls to give a better confirmation message.
        String taskMessage = null;
        if (suitableDownloadTasks != null && !suitableDownloadTasks.isEmpty()) {
            // TODO: handle multiple suitable download tasks ?
            taskMessage = suitableDownloadTasks.iterator().next().getConfirmationMessage(url);
        }
        return tr("Remote Control has been asked to import data from the following URL:")
                + "<br>" + (taskMessage == null ? url.toString() : taskMessage);
    }

    @Override
    public PermissionPrefWithDefault getPermissionPref() {
        return PermissionPrefWithDefault.IMPORT_DATA;
    }

    @Override
    protected void parseArgs() {
        String req = decodeParam(this.request);
        HashMap<String, String> args = new HashMap<String, String>();
        if (request.indexOf('?') != -1) {
			String query = req.substring(req.indexOf('?') + 1);
			if (query.indexOf('#') != -1) {
				query = query.substring(0, query.indexOf('#'));
			}
			String[] params = query.split("&", -1);
			for (String param : params) {
				int eq = param.indexOf('=');
				if (eq != -1) {
					args.put(param.substring(0, eq), param.substring(eq + 1));
				}
			}
        }
        this.args = args;
    }

    @Override
    protected void validateRequest() throws RequestHandlerBadRequestException {
        final String urlString = args.get("url");
        try {
            // Ensure the URL is valid
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new RequestHandlerBadRequestException("MalformedURLException: "+e.getMessage());
        }
		
		// See if there is a new-layer specified.
		if(args.containsKey("nl") &&
			args.get("nl").equals("yes"))
		{ 
			new_layer = true;
		}
		else
		{ 
			new_layer = false;
		}
		
        // Find download tasks for the given URL
        suitableDownloadTasks = Main.main.menu.openLocation.findDownloadTasks(urlString);
        if (suitableDownloadTasks.isEmpty()) {
            // It should maybe be better to reject the request in that case ?
            // For compatibility reasons with older instances of JOSM, arbitrary choice of DownloadOsmTask
            suitableDownloadTasks.add(new DownloadOsmTask());
        }
    }
}
