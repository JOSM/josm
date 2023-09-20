// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.Capabilities;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmApiInitializationException;
import org.openstreetmap.josm.io.OsmTransferCanceledException;
import org.openstreetmap.josm.io.OsmTransferException;

/**
 * A fake OSM API server. It is used to test again.
 * <p>
 * It provides only basic features.
 * <p>
 * These image servers are blacklisted:
 * <ul>
 * <li>.*blacklisted.*</li>
 * <li>^(invalid|bad).*</li>
 * </ul>
 *
 * @author Michael Zangl
 */
public class FakeOsmApi extends OsmApi {

    private static FakeOsmApi instance;

    private boolean initialized = false;

    protected FakeOsmApi() {
        super("http://fake.xxx/api");
    }

    @Override
    public void initialize(ProgressMonitor monitor, boolean fastFail)
            throws OsmTransferCanceledException, OsmApiInitializationException {
        // we do not connect to any server so we do not need that.
        initialized = true;
    }

    @Override
    public synchronized Capabilities getCapabilities() {
        if (!initialized) {
            return null;
        } else {
            Capabilities capabilities = new Capabilities();
            capabilities.put("blacklist", "regex", ".*blacklisted.*");
            capabilities.put("blacklist", "regex", "^https?://(invalid|bad).*");
            capabilities.put("version", "minimum", "0.6");
            capabilities.put("version", "maximum", "0.6");
            return capabilities;
        }
    }

    @Override
    public void createPrimitive(IPrimitive osm, ProgressMonitor monitor) throws OsmTransferException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void modifyPrimitive(IPrimitive osm, ProgressMonitor monitor) throws OsmTransferException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void deletePrimitive(OsmPrimitive osm, ProgressMonitor monitor) throws OsmTransferException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void openChangeset(Changeset changeset, ProgressMonitor progressMonitor) throws OsmTransferException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void updateChangeset(Changeset changeset, ProgressMonitor monitor) throws OsmTransferException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void closeChangeset(Changeset changeset, ProgressMonitor monitor) throws OsmTransferException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Note createNote(LatLon latlon, String text, ProgressMonitor monitor) throws OsmTransferException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Note addCommentToNote(Note note, String comment, ProgressMonitor monitor) throws OsmTransferException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Note closeNote(Note note, String closeMessage, ProgressMonitor monitor) throws OsmTransferException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Note reopenNote(Note note, String reactivateMessage, ProgressMonitor monitor) throws OsmTransferException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Gets and caches an instance of this API.
     * @return The API instance. Always the same object.
     */
    public static synchronized FakeOsmApi getInstance() {
        if (instance == null) {
            instance = new FakeOsmApi();
        }
        cacheInstance(instance);
        return instance;
    }
}
