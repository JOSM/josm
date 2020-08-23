// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * OsmServerObjectReader reads an individual object from the OSM server.
 *
 * It can either download the object including or not including its immediate children.
 * The former case is called a "full download".
 *
 * It can also download a specific version of the object (however, "full" download is not possible
 * in that case).
 *
 */
public class OsmServerObjectReader extends OsmServerReader {
    /** the id of the object to download */
    private final PrimitiveId id;
    /** true if a full download is required, i.e. a download including the immediate children */
    private final boolean full;
    /** the specific version number, if required (incompatible with full), or -1 else */
    private final int version;

    /**
     * Creates a new server object reader for a given id and a primitive type.
     *
     * @param id the object id. &gt; 0 required.
     * @param type the type. Must not be null.
     * @param full true, if a full download is requested (i.e. a download including
     * immediate children); false, otherwise
     * @throws IllegalArgumentException if id &lt;= 0
     * @throws IllegalArgumentException if type is null
     */
    public OsmServerObjectReader(long id, OsmPrimitiveType type, boolean full) {
        this(id, type, full, -1);
    }

    /**
     * Creates a new server object reader for a given id and a primitive type.
     *
     * @param id the object id. &gt; 0 required.
     * @param type the type. Must not be null.
     * @param version the specific version number, if required; -1, otherwise
     * @throws IllegalArgumentException if id &lt;= 0
     * @throws IllegalArgumentException if type is null
     */
    public OsmServerObjectReader(long id, OsmPrimitiveType type, int version) {
        this(id, type, false, version);
    }

    protected OsmServerObjectReader(long id, OsmPrimitiveType type, boolean full, int version) {
        if (id <= 0)
            throw new IllegalArgumentException(MessageFormat.format("Expected value > 0 for parameter ''{0}'', got {1}", "id", id));
        CheckParameterUtil.ensureParameterNotNull(type, "type");
        this.id = new SimplePrimitiveId(id, type);
        this.full = full;
        this.version = version;
    }

    /**
     * Creates a new server object reader for an object with the given <code>id</code>
     *
     * @param id the object id. Must not be null. Unique id &gt; 0 required.
     * @param full true, if a full download is requested (i.e. a download including
     * immediate children); false, otherwise
     * @throws IllegalArgumentException if id is null
     * @throws IllegalArgumentException if id.getUniqueId() &lt;= 0
     */
    public OsmServerObjectReader(PrimitiveId id, boolean full) {
        this(id, full, -1);
    }

    /**
     * Creates a new server object reader for an object with the given <code>id</code>
     *
     * @param id the object id. Must not be null. Unique id &gt; 0 required.
     * @param version the specific version number, if required; -1, otherwise
     * @throws IllegalArgumentException if id is null
     * @throws IllegalArgumentException if id.getUniqueId() &lt;= 0
     */
    public OsmServerObjectReader(PrimitiveId id, int version) {
        this(id, false, version);
    }

    protected OsmServerObjectReader(PrimitiveId id, boolean full, int version) {
        CheckParameterUtil.ensureThat(id.getUniqueId() > 0, "id > 0");
        this.id = id;
        this.full = full;
        this.version = version;
    }

    /**
     * Downloads and parses the data.
     *
     * @param progressMonitor the progress monitor. Set to {@link NullProgressMonitor#INSTANCE} if null
     * @return the downloaded data
     * @throws OsmTransferException if any error occurs during dialog with OSM API
     */
    @Override
    public DataSet parseOsm(ProgressMonitor progressMonitor) throws OsmTransferException {
        if (progressMonitor == null) {
            progressMonitor = NullProgressMonitor.INSTANCE;
        }
        progressMonitor.beginTask("", 1);
        try {
            progressMonitor.indeterminateSubTask(tr("Downloading OSM data..."));
            StringBuilder sb = new StringBuilder();
            sb.append(id.getType().getAPIName())
              .append('/')
              .append(id.getUniqueId());
            if (full && id.getType() != OsmPrimitiveType.NODE) {
                sb.append("/full");
            } else if (version > 0) {
                sb.append('/').append(version);
            }

            try (InputStream in = getInputStream(sb.toString(), progressMonitor.createSubTaskMonitor(1, true))) {
                if (in == null)
                    return null;
                return OsmReader.parseDataSet(in, progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
            }
        } catch (OsmTransferException e) {
            if (cancel) return null;
            throw e;
        } catch (IOException | IllegalDataException e) {
            if (cancel) return null;
            throw new OsmTransferException(e);
        } finally {
            progressMonitor.finishTask();
            activeConnection = null;
        }
    }
}
