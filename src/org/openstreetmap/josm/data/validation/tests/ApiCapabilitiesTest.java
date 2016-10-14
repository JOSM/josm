// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.Capabilities;
import org.openstreetmap.josm.io.OsmApi;

/**
 * Performs validation tests against OSM API capabilities. This class does not test length
 * of key/values (limited to 255 characters) because it's done by {@code TagChecker}.
 * @since 7574
 */
public class ApiCapabilitiesTest extends Test {

    private static final int MAX_WAY_NODES_ERROR = 3401;

    private long maxNodes = -1;

    /**
     * Constructs a new {@code ApiCapabilitiesTest}.
     */
    public ApiCapabilitiesTest() {
        super(tr("API Capabilities"), tr("Checks for errors against API capabilities"));
    }

    @Override
    public void initialize() throws Exception {
        super.initialize();
        OsmApi api = OsmApi.getOsmApi();
        api.initialize(NullProgressMonitor.INSTANCE);
        Capabilities capabilities = api.getCapabilities();
        if (capabilities != null) {
            maxNodes = capabilities.getMaxWayNodes();
        }
    }

    @Override
    public void visit(Way w) {
        if (maxNodes > 1 && w.getNodesCount() > maxNodes) {
            String message;
            if (w.isClosed()) {
                message = tr("Way contains more than {0} nodes. It should be replaced by a multipolygon", maxNodes);
            } else {
                message = tr("Way contains more than {0} nodes. It should be split or simplified", maxNodes);
            }
            errors.add(TestError.builder(this, Severity.ERROR, MAX_WAY_NODES_ERROR)
                    .message(message)
                    .primitives(w)
                    .build());
        }
    }
}
