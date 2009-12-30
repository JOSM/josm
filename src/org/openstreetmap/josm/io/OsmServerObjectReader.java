//License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.xml.sax.SAXException;

/**
 * OsmServerObjectReader reads an individual object from the OSM server.
 *
 * It can either download the object including or not including its immediate children.
 * The former case is called a "full download".
 *
 */
public class OsmServerObjectReader extends OsmServerReader {
    /** the id of the object to download */
    private PrimitiveId id;
    /** true if a full download is required, i.e. a download including the immediate children */
    private boolean full;

    /**
     * Creates a new server object reader for a given id and a primitive type.
     *
     * @param id the object id. > 0 required.
     * @param type the type. Must not be null.
     * @param full true, if a full download is requested (i.e. a download including
     * immediate children); false, otherwise
     * @throws IllegalArgumentException thrown if id <= 0
     * @throws IllegalArgumentException thrown if type is null
     */
    public OsmServerObjectReader(long id, OsmPrimitiveType type, boolean full) throws IllegalArgumentException {
        if (id <= 0)
            throw new IllegalArgumentException(tr("Expected value > 0 for parameter ''{0}'', got {1}", "id", id));
        if (type == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null", "type"));
        this.id = new SimplePrimitiveId(id, type);
        this.full = full;
    }

    /**
     * Creates a new server object reader for an object with the given <code>id</code>
     *
     * @param id the object id. Must not be null. Unique id > 0 required.
     * @param full true, if a full download is requested (i.e. a download including
     * immediate children); false, otherwise
     * @throws IllegalArgumentException thrown if id is null
     * @throws IllegalArgumentException thrown if id.getUniqueId() <= 0
     */
    public OsmServerObjectReader(PrimitiveId id, boolean full) {
        if (id == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null", "id"));
        if (id.getUniqueId() <= 0)
            throw new IllegalArgumentException(tr("Expected value > 0 for parameter ''{0}'', got {1}", "id.getUniqueId()", id.getUniqueId()));
        this.id = id;
        this.full = full;
    }

    /**
     * Downloads and parses the data.
     *
     * @param progressMonitor the progress monitor. Set to {@see NullProgressMonitor#INSTANCE} if
     * null
     * @return the downloaded data
     * @throws SAXException
     * @throws IOException
     */
    @Override
    public DataSet parseOsm(ProgressMonitor progressMonitor) throws OsmTransferException {
        if (progressMonitor == null) {
            progressMonitor = NullProgressMonitor.INSTANCE;
        }
        progressMonitor.beginTask("", 1);
        InputStream in = null;
        try {
            progressMonitor.indeterminateSubTask(tr("Downloading OSM data..."));
            StringBuffer sb = new StringBuffer();
            sb.append(id.getType().getAPIName());
            sb.append("/");
            sb.append(id.getUniqueId());
            if (full && ! id.getType().equals(OsmPrimitiveType.NODE)) {
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
                } catch(Exception e) {/* ignore this exception */}
            }
            activeConnection = null;
        }
    }
}
