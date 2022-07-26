// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler.RequestHandlerBadRequestException;
import org.openstreetmap.josm.testutils.annotations.AssertionsInEDT;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.LayerEnvironment;

/**
 * Unit tests of {@link AddNodeHandler} class.
 */
@AssertionsInEDT
@BasicPreferences
@LayerEnvironment
class AddNodeHandlerTest {
    private static AddNodeHandler newHandler(String url) throws RequestHandlerBadRequestException {
        AddNodeHandler req = new AddNodeHandler();
        if (url != null)
            req.setUrl(url);
        return req;
    }

    /**
     * Unit test for bad request - no layer.
     */
    @Test
    void testBadRequestNoLayer() {
        Exception e = assertThrows(RequestHandlerBadRequestException.class, () -> newHandler("https://localhost?lat=0&lon=0").handle());
        assertEquals("There is no layer opened to add node", e.getMessage());
    }

    /**
     * Unit test for bad request - no param.
     */
    @Test
    void testBadRequestNoParam() {
        OsmDataLayer layer = new OsmDataLayer(new DataSet(), "", null);
        MainApplication.getLayerManager().addLayer(layer);
        Exception e = assertThrows(RequestHandlerBadRequestException.class, () -> newHandler(null).handle());
        assertEquals("NumberFormatException (empty String)", e.getMessage());
    }

    /**
     * Unit test for bad request - invalid URL.
     */
    @Test
    void testBadRequestInvalidUrl() {
        Exception e = assertThrows(RequestHandlerBadRequestException.class, () -> newHandler("invalid_url").handle());
        assertEquals("The following keys are mandatory, but have not been provided: lat, lon", e.getMessage());
    }

    /**
     * Unit test for bad request - incomplete URL.
     */
    @Test
    void testBadRequestIncompleteUrl() {
        Exception e = assertThrows(RequestHandlerBadRequestException.class, () -> newHandler("https://localhost").handle());
        assertEquals("The following keys are mandatory, but have not been provided: lat, lon", e.getMessage());
    }

    /**
     * Unit test for nominal request - local data file.
     */
    @Test
    void testNominalRequest() {
        OsmDataLayer layer = new OsmDataLayer(new DataSet(), "", null);
        MainApplication.getLayerManager().addLayer(layer);
        assertDoesNotThrow(() -> newHandler("https://localhost?lat=0&lon=0").handle());
    }
}
