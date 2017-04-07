// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.io.PrintWriter;
import java.util.Objects;

/**
 * This factory is called by everyone who needs an OsmWriter object,
 * instead of directly calling the OsmWriter constructor.
 *
 * This enables plugins to substitute the original OsmWriter with
 * their own version, altering the way JOSM writes objects to the
 * server, and to disk.
 *
 * @author Frederik Ramm
 *
 */
public class OsmWriterFactory {

    private static volatile OsmWriterFactory theFactory;

    /**
     * Creates new {@code OsmWriter}.
     * @param out print writer
     * @param osmConform if {@code true}, prevents modification attributes to be written to the common part
     * @param version OSM API version (0.6)
     * @return new {@code OsmWriter}
     */
    public static OsmWriter createOsmWriter(PrintWriter out, boolean osmConform, String version) {
        // pre-set factory with this default implementation; can still be overwritten
        // later. note that the default factory may already be used for constructing
        // OsmWriters during the startup process.
        if (theFactory == null) {
            theFactory = new OsmWriterFactory();
        }
        return theFactory.createOsmWriterImpl(out, osmConform, version);
    }

    /**
     * Sets the default factory.
     * @param factory new default factory
     * @since 11851
     */
    public static void setDefaultFactory(OsmWriterFactory factory) {
        theFactory = Objects.requireNonNull(factory);
    }

    /**
     * Creates new {@code OsmWriter}.
     * @param out print writer
     * @param osmConform if {@code true}, prevents modification attributes to be written to the common part
     * @param version OSM API version (0.6)
     * @return new {@code OsmWriter}
     */
    protected OsmWriter createOsmWriterImpl(PrintWriter out, boolean osmConform, String version) {
        return new OsmWriter(out, osmConform, version);
    }
}
