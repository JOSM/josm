// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler.RequestHandlerBadRequestException;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link AddNodeHandler} class.
 */
public class AddNodeHandlerTest {

    /**
     * Rule used for tests throwing exceptions.
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    private static AddNodeHandler newHandler(String url) throws RequestHandlerBadRequestException {
        AddNodeHandler req = new AddNodeHandler();
        if (url != null)
            req.setUrl(url);
        return req;
    }

    /**
     * Unit test for bad request - no layer.
     * @throws Exception if any error occurs
     */
    @Test
    public void testBadRequestNoLayer() throws Exception {
        thrown.expect(RequestHandlerBadRequestException.class);
        thrown.expectMessage("There is no layer opened to add node");
        newHandler("https://localhost?lat=0&lon=0").handle();
    }

    /**
     * Unit test for bad request - no param.
     * @throws Exception if any error occurs
     */
    @Test
    public void testBadRequestNoParam() throws Exception {
        thrown.expect(RequestHandlerBadRequestException.class);
        thrown.expectMessage("NumberFormatException (empty String)");
        OsmDataLayer layer = new OsmDataLayer(new DataSet(), "", null);
        try {
            MainApplication.getLayerManager().addLayer(layer);
            newHandler(null).handle();
        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }

    /**
     * Unit test for bad request - invalid URL.
     * @throws Exception if any error occurs
     */
    @Test
    public void testBadRequestInvalidUrl() throws Exception {
        thrown.expect(RequestHandlerBadRequestException.class);
        thrown.expectMessage("The following keys are mandatory, but have not been provided: lat, lon");
        newHandler("invalid_url").handle();
    }

    /**
     * Unit test for bad request - incomplete URL.
     * @throws Exception if any error occurs
     */
    @Test
    public void testBadRequestIncompleteUrl() throws Exception {
        thrown.expect(RequestHandlerBadRequestException.class);
        thrown.expectMessage("The following keys are mandatory, but have not been provided: lat, lon");
        newHandler("https://localhost").handle();
    }

    /**
     * Unit test for nominal request - local data file.
     * @throws Exception if any error occurs
     */
    @Test
    public void testNominalRequest() throws Exception {
        OsmDataLayer layer = new OsmDataLayer(new DataSet(), "", null);
        try {
            MainApplication.getLayerManager().addLayer(layer);
            newHandler("https://localhost?lat=0&lon=0").handle();
        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }
}
