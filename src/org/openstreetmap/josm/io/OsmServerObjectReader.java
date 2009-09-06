//License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.xml.sax.SAXException;

public class OsmServerObjectReader extends OsmServerReader {

    long id;
    OsmPrimitiveType type;
    boolean full;

    public OsmServerObjectReader(long id, OsmPrimitiveType type, boolean full) {
        this.id = id;
        this.type = type;
        this.full = full;
    }
    /**
     * Method to download single objects from OSM server. ways, relations, nodes
     * @return the data requested
     * @throws SAXException
     * @throws IOException
     */
    @Override
    public DataSet parseOsm(ProgressMonitor progressMonitor) throws OsmTransferException {
        progressMonitor.beginTask("", 1);
        InputStream in = null;
        try {
            progressMonitor.subTask(tr("Downloading OSM data..."));
            StringBuffer sb = new StringBuffer();
            sb.append(type.getAPIName());
            sb.append("/");
            sb.append(id);
            if (full && ! type.equals(OsmPrimitiveType.NODE)) {
                sb.append("/full");
            }

            in = getInputStream(sb.toString(), progressMonitor.createSubTaskMonitor(1, true));
            if (in == null)
                return null;
            final DataSet data = OsmReader.parseDataSet(in, progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
            return data;
        } catch(OsmTransferException e) {
            if (cancel) return null;
            throw e;
        } catch (Exception e) {
            if (cancel) return null;
            throw new OsmTransferException(e);
        } finally {
            progressMonitor.finishTask();
            if (in!=null) {
                try {
                    in.close();
                } catch(Exception e) {}
            }
            activeConnection = null;
        }
    }

}
