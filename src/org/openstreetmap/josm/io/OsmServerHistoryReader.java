// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.history.HistoryDataSet;
import org.xml.sax.SAXException;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Reads the history of an {@see OsmPrimitive} from the OSM API server.
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
        if (type == null)
            throw new IllegalArgumentException(tr("parameter ''{0}'' must not be null", "type"));
        if (id < 0)
            throw new IllegalArgumentException(tr("parameter ''{0}'' >= 0 expected, got ''{1}''", "id", id));
        this.primitiveType = type;
        this.id = id;
    }

    /**
     * don't use - not implemented!
     * 
     * @exception NotImplementedException
     */
    @Override
    public DataSet parseOsm() throws OsmTransferException {
        throw new NotImplementedException();
    }

    /**
     * Fetches the history from the OSM API and parses it
     * 
     * @return the data set with the parsed history data
     * @throws OsmTransferException thrown, if an exception occurs
     */
    public HistoryDataSet parseHistory() throws OsmTransferException {
        InputStream in = null;
        try {
            Main.pleaseWaitDlg.progress.setValue(0);
            Main.pleaseWaitDlg.currentAction.setText(tr("Contacting OSM Server..."));
            StringBuffer sb = new StringBuffer();
            sb.append(primitiveType.getAPIName())
            .append("/").append(id).append("/history");

            in = getInputStream(sb.toString(), Main.pleaseWaitDlg);
            if (in == null)
                return null;
            Main.pleaseWaitDlg.currentAction.setText(tr("Downloading history..."));
            final OsmHistoryReader reader = new OsmHistoryReader(in);
            HistoryDataSet data = reader.parse(Main.pleaseWaitDlg);
            return data;
        } catch(OsmTransferException e) {
            throw e;
        } catch (Exception e) {
            if (cancel)
                return null;
            throw new OsmTransferException(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch(Exception e) {}
                activeConnection = null;
            }
        }
    }
}
