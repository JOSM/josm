// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.history.HistoryDataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.xml.sax.SAXException;

/**
 * Reads the history of an {@link org.openstreetmap.josm.data.osm.OsmPrimitive} from the OSM API server.
 *
 */
public class OsmServerHistoryReader extends OsmServerReader {

    private final OsmPrimitiveType primitiveType;
    private final long id;

    /**
     * constructor
     *
     * @param type the type of the primitive whose history is to be fetched from the server.
     *   Must not be null.
     * @param id the id of the primitive
     *
     *  @throws IllegalArgumentException if type is null
     */
    public OsmServerHistoryReader(OsmPrimitiveType type, long id) {
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
     * @param progressMonitor progress monitor
     *
     * @return the data set with the parsed history data
     * @throws OsmTransferException if an exception occurs
     */
    public HistoryDataSet parseHistory(ProgressMonitor progressMonitor) throws OsmTransferException {
        progressMonitor.beginTask("");
        try {
            final String urlStr = primitiveType.getAPIName() + '/' + id + "/history";
            progressMonitor.indeterminateSubTask(tr("Contacting OSM Server for {0}", urlStr));

            try (InputStream in = getInputStream(urlStr, progressMonitor.createSubTaskMonitor(1, true))) {
                if (in == null)
                    return null;
                progressMonitor.indeterminateSubTask(tr("Downloading history..."));
                OsmHistoryReader reader = new OsmHistoryReader(in);
                return reader.parse(progressMonitor.createSubTaskMonitor(1, true));
            }
        } catch (OsmTransferException e) {
            throw e;
        } catch (IOException | SAXException e) {
            if (cancel)
                return null;
            throw new OsmTransferException(e);
        } finally {
            progressMonitor.finishTask();
            activeConnection = null;
        }
    }
}
