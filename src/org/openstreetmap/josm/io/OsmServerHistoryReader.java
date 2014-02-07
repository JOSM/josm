// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.InputStream;
import java.text.MessageFormat;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.history.HistoryDataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Utils;

/**
 * Reads the history of an {@link org.openstreetmap.josm.data.osm.OsmPrimitive} from the OSM API server.
 *
 */
public class OsmServerHistoryReader extends OsmServerReader {

    private OsmPrimitiveType primitiveType;
    private long id;

    /**
     * constructor
     *
     * @param type the type of the primitive whose history is to be fetched from the server.
     *   Must not be null.
     * @param id the id of the primitive
     *
     *  @exception IllegalArgumentException thrown, if type is null
     */
    public OsmServerHistoryReader(OsmPrimitiveType type, long id) throws IllegalArgumentException {
        CheckParameterUtil.ensureParameterNotNull(type, "type");
        if (id < 0)
            throw new IllegalArgumentException(MessageFormat.format("Parameter ''{0}'' >= 0 expected. Got ''{1}''.", "id", id));
        this.primitiveType = type;
        this.id = id;
    }

    /**
     * don't use - not implemented!
     *
     */
    @Override
    public DataSet parseOsm(ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
    }

    /**
     * Fetches the history from the OSM API and parses it
     *
     * @return the data set with the parsed history data
     * @throws OsmTransferException thrown, if an exception occurs
     */
    public HistoryDataSet parseHistory(ProgressMonitor progressMonitor) throws OsmTransferException {
        InputStream in = null;
        progressMonitor.beginTask("");
        try {
            progressMonitor.indeterminateSubTask(tr("Contacting OSM Server..."));
            StringBuilder sb = new StringBuilder();
            sb.append(primitiveType.getAPIName())
            .append("/").append(id).append("/history");

            in = getInputStream(sb.toString(), progressMonitor.createSubTaskMonitor(1, true));
            if (in == null)
                return null;
            progressMonitor.indeterminateSubTask(tr("Downloading history..."));
            final OsmHistoryReader reader = new OsmHistoryReader(in);
            return reader.parse(progressMonitor.createSubTaskMonitor(1, true));
        } catch(OsmTransferException e) {
            throw e;
        } catch (Exception e) {
            if (cancel)
                return null;
            throw new OsmTransferException(e);
        } finally {
            progressMonitor.finishTask();
            Utils.close(in);
            activeConnection = null;
        }
    }
}
