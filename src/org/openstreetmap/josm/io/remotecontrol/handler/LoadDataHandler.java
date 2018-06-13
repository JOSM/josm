// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadParams;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;
import org.openstreetmap.josm.tools.Utils;

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
     * Holds the parsed data set
     */
    private DataSet dataSet;

    @Override
    protected void handleRequest() throws RequestHandlerErrorException {
        MainApplication.worker.submit(new LoadDataTask(getDownloadParams(), dataSet, args.get("layer_name")));
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
        return new String[]{
                "/load_data?layer_name=extra_layer&new_layer=true&data=" +
                    Utils.encodeUrl("<osm version='0.6'><node id='-1' lat='1' lon='2' /></osm>")};
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
    protected void validateRequest() throws RequestHandlerBadRequestException {
        this.data = args.get("data");
        /**
         * Holds the mime type. Currently only OSM_MIME_TYPE is supported
         * But it could be extended to text/csv, application/gpx+xml, ... or even binary encoded data
         */
        final String mimeType = Utils.firstNonNull(args.get("mime_type"), OSM_MIME_TYPE);
        try {
            if (OSM_MIME_TYPE.equals(mimeType)) {
                final ByteArrayInputStream in = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
                dataSet = OsmReader.parseDataSet(in, null);
            } else {
                dataSet = new DataSet();
            }
        } catch (IllegalDataException e) {
            throw new RequestHandlerBadRequestException("Failed to parse " + data + ": " + e.getMessage(), e);
        }
    }

    protected static class LoadDataTask extends DownloadOsmTask.AbstractInternalTask {

        protected final String layerName;

        /**
         * Constructs a new {@code LoadDataTask}.
         * @param settings download settings
         * @param dataSet data set
         * @param layerName layer name
         * @since 13927
         */
        public LoadDataTask(DownloadParams settings, DataSet dataSet, String layerName) {
            super(settings, tr("Loading data"), false, true);
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
