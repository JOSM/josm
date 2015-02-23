// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;

/**
 * Handler to load data directly from the URL.
 * @since 7636
 */
public class LoadDataHandler extends RequestHandler {

    private static final String OSM_MIME_TYPE = "application/x-osm+xml";

    /**
     * The remote control command name used to import data.
     */
    public static final String command = "load_data";

    /**
     * Holds the data input string
     */
    private String data;

    /**
     * Holds the mime type. Currently only OSM_MIME_TYPE is supported
     * But it could be extended to text/csv, application/gpx+xml, ... or even binary encoded data
     */
    private String mimeType;

    @Override
    protected void handleRequest() throws RequestHandlerErrorException {
        try {
            // Transform data string to inputstream
            InputStream source = new ByteArrayInputStream(data.getBytes("UTF-8"));
            DataSet dataSet = new DataSet();
            if (mimeType != null && mimeType.contains(OSM_MIME_TYPE))
                dataSet = OsmReader.parseDataSet(source, null);
            Main.worker.submit(new LoadDataTask(isLoadInNewLayer(), dataSet, args.get("layer_name")));
        } catch (Exception e) {
            Main.warn("Problem with data: " + data);
            throw new RequestHandlerErrorException(e);
        }
    }

    @Override
    public String[] getMandatoryParams() {
        return new String[]{"data"};
    }

    @Override
    public String[] getOptionalParams() {
        return new String[] {"new_layer", "mime_type", "layer_name"};
    }

    @Override
    public String getUsage() {
        return "Reads data encoded directly in the URL and adds it to the current data set";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[] {
                "/load_data?layer_name=extra_layer&new_layer=true&data=%3Cosm%3E%3Cnode%3E...%3C%2Fnode%3E%3C%2Fosm%3E" };
    }

    @Override
    public String getPermissionMessage() {
        return tr("Remote Control has been asked to load the following data:")
                + "<br>" + data;
    }

    @Override
    public PermissionPrefWithDefault getPermissionPref() {
        // Same permission as the import data, as the difference from a user pov is minimal
        return PermissionPrefWithDefault.IMPORT_DATA;
    }

    @Override
    protected void parseArgs() {
        if (request.indexOf('?') == -1)
            return; // nothing to do

        Map<String, String> args = new HashMap<>();

        // The data itself shouldn't contain any &, = or ? chars.
        // Those are reserved for the URL parsing
        // and should be URL encoded as %26, %3D or %3F
        String query = request.substring(request.indexOf('?') + 1);
        String[] params = query.split("&");
        for (String param : params) {
            String[] kv = param.split("=");
            if (kv.length == 2)
                args.put(kv[0], kv[1]);
        }
        this.args = args;
    }

    @Override
    protected void validateRequest() throws RequestHandlerBadRequestException {
        if (args.get("data") == null)
            throw new RequestHandlerBadRequestException("RemoteControl: No data defined in URL");
        try {
            data = URLDecoder.decode(args.get("data"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RequestHandlerBadRequestException("RemoteControl: UnsupportedEncodingException: " + e.getMessage(), e);
        }
        mimeType = args.get("mime_type");
        if (mimeType == null) {
            mimeType = OSM_MIME_TYPE;
        }
    }

    protected class LoadDataTask extends DownloadOsmTask.AbstractInternalTask {

        protected final String layerName;

        public LoadDataTask(boolean newLayer, DataSet dataSet, String layerName) {
            super(newLayer, tr("Loading data"), false);
            this.dataSet = dataSet;
            this.layerName = layerName;
        }

        @Override
        public void realRun() {
            // No real run, the data is already loaded
        }

        @Override
        protected void cancel() {
            // No Cancel, would be hard without a real run
        }

        @Override
        protected void finish() {
            loadData(layerName, null);
        }
    }
}
