// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.io.PrintWriter;

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

    public static OsmWriterFactory theFactory;
    public static OsmWriter createOsmWriter(PrintWriter out, boolean osmConform, String version) {
        // pre-set factory with this default implementation; can still be overwritten
        // later. note that the default factory may already be used for constructing
        // OsmWriters during the startup process.
        if (theFactory == null) {
            theFactory = new OsmWriterFactory();
        }
        return theFactory.createOsmWriterImpl(out, osmConform, version);
    }
    protected OsmWriter createOsmWriterImpl(PrintWriter out, boolean osmConform, String version) {
        return new OsmWriter(out, osmConform, version);
    }
}
