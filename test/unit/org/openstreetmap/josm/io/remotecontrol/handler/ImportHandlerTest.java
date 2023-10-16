// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler.RequestHandlerBadRequestException;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.tools.Utils;

/**
 * Unit tests of {@link ImportHandler} class.
 */
@Main
class ImportHandlerTest {
    private static ImportHandler newHandler(String url) throws RequestHandlerBadRequestException {
        ImportHandler req = new ImportHandler();
        if (url != null)
            req.setUrl(url);
        return req;
    }

    /**
     * Non-regression test for bug #7434.
     * @throws Exception if any error occurs
     */
    @Test
    void testTicket7434() throws Exception {
        ImportHandler req = newHandler("http://localhost:8111/import?url=http://localhost:8888/relations?relations=19711&mode=recursive");
        assertEquals("http://localhost:8888/relations?relations=19711&mode=recursive", req.args.get("url"));
    }

    /**
     * Unit test for bad request - no param.
     * @throws Exception if any error occurs
     */
    @Test
    void testBadRequestNoParam() throws Exception {
        final ImportHandler handler = newHandler(null);
        Exception e = assertThrows(RequestHandlerBadRequestException.class, handler::handle);
        // This has differing behavior after Java 15
        if ("MalformedURLException: null".length() == e.getMessage().length()) {
            assertEquals("MalformedURLException: null", e.getMessage());
        } else {
            assertEquals("MalformedURLException: Cannot invoke \"String.length()\" because \"spec\" is null", e.getMessage());
        }
    }

    /**
     * Unit test for bad request - invalid URL.
     * @throws Exception if any error occurs
     */
    @Test
    void testBadRequestInvalidUrl() throws Exception {
        final ImportHandler handler = newHandler("https://localhost?url=invalid_url");
        Exception e = assertThrows(RequestHandlerBadRequestException.class, handler::handle);
        assertEquals("MalformedURLException: no protocol: invalid_url", e.getMessage());
    }

    /**
     * Unit test for bad request - incomplete URL.
     * @throws Exception if any error occurs
     */
    @Test
    void testBadRequestIncompleteUrl() throws Exception {
        final ImportHandler handler = newHandler("https://localhost?lat=0&lon=0");
        Exception e = assertThrows(RequestHandlerBadRequestException.class, handler::handle);
        assertEquals("The following keys are mandatory, but have not been provided: url", e.getMessage());
    }

    /**
     * Unit test for nominal request - local data file.
     * @throws Exception if any error occurs
     */
    @Test
    void testNominalRequest() throws Exception {
        String url = new File(TestUtils.getRegressionDataFile(11957, "data.osm")).toURI().toURL().toExternalForm();
        try {
            assertDoesNotThrow(() -> newHandler("https://localhost?url=" + Utils.encodeUrl(url)).handle());
        } finally {
            for (OsmDataLayer layer : MainApplication.getLayerManager().getLayersOfType(OsmDataLayer.class)) {
                MainApplication.getLayerManager().removeLayer(layer);
            }
        }
    }
}
