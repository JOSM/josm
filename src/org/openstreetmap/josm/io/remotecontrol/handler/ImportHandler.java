// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadTask;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Handler for import request
 */
public class ImportHandler extends RequestHandler.RawURLParseRequestHandler {

    /**
     * The remote control command name used to import data.
     */
    public static final String command = "import";

    private URL url;
    private Collection<DownloadTask> suitableDownloadTasks;

    @Override
    protected void handleRequest() throws RequestHandlerErrorException {
        try {
            if (suitableDownloadTasks.isEmpty()) {
                // It should maybe be better to reject the request in that case ?
                // For compatibility reasons with older instances of JOSM, arbitrary choice of DownloadOsmTask
                // As of 2015-04, Overpass Turbo requires this branch of code ...
                Logging.debug("Remote control, /import: defaulting to DownloadOsmTask");
                new DownloadOsmTask().loadUrl(getDownloadParams(), url.toExternalForm(), null);
            } else if (Config.getPref().getBoolean("remotecontrol.import.interactive", true)) {
                // OpenLocationAction queries the user if more than one task is suitable
                MainApplication.getMenu().openLocation.openUrl(getDownloadParams(), url.toExternalForm());
            } else {
                // Otherwise perform all tasks
                for (DownloadTask task : suitableDownloadTasks) {
                    task.loadUrl(getDownloadParams(), url.toExternalForm(), null);
                }
            }
        } catch (RuntimeException ex) { // NOPMD
            Logging.warn("RemoteControl: Error parsing import remote control request:");
            Logging.error(ex);
            throw new RequestHandlerErrorException(ex);
        }
    }

    @Override
    public String[] getMandatoryParams() {
        return new String[]{"url"};
    }

    @Override
    public String[] getOptionalParams() {
        return new String[] {"new_layer", "layer_name", "layer_locked", "download_policy", "upload_policy"};
    }

    @Override
    public String getUsage() {
        return "downloads the specified OSM file and adds it to the current data set";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[] {"/import?url=" + Utils.encodeUrl(
                Main.getJOSMWebsite()+"/browser/josm/trunk/data_nodist/direction-arrows.osm?format=txt")};
    }

    @Override
    public String getPermissionMessage() {
        // URL can be any suitable URL giving back OSM data, including OSM API calls, even if calls to the main API
        // should rather be passed to LoadAndZoomHandler or LoadObjectHandler.
        // Other API instances will however use the import handler to force JOSM to make requests to this API instance.
        // (Example with OSM-FR website that makes calls to the OSM-FR API)
        // For user-friendliness, let's try to decode these OSM API calls to give a better confirmation message.
        Set<String> taskMessages = new LinkedHashSet<>();
        if (suitableDownloadTasks != null && !suitableDownloadTasks.isEmpty()) {
            for (DownloadTask task : suitableDownloadTasks) {
                taskMessages.add(Utils.firstNonNull(task.getConfirmationMessage(url), url.toString()));
            }
        }
        return tr("Remote Control has been asked to import data from the following URL:")
                + Utils.joinAsHtmlUnorderedList(taskMessages);
    }

    @Override
    public PermissionPrefWithDefault getPermissionPref() {
        return PermissionPrefWithDefault.IMPORT_DATA;
    }

    @Override
    protected void validateRequest() throws RequestHandlerBadRequestException {
        validateDownloadParams();
        String urlString = args != null ? args.get("url") : null;
        if (Config.getPref().getBoolean("remotecontrol.importhandler.fix_url_query", true)) {
            urlString = Utils.fixURLQuery(urlString);
        }
        try {
            // Ensure the URL is valid
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new RequestHandlerBadRequestException("MalformedURLException: "+e.getMessage(), e);
        }
        // Find download tasks for the given URL
        suitableDownloadTasks = MainApplication.getMenu().openLocation.findDownloadTasks(urlString, true);
    }
}
