// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler.RequestHandlerBadRequestException;

/**
 * Unit tests of {@link LoadObjectHandler} class.
 */
public class LoadObjectHandlerTest {

    /**
     * Rule used for tests throwing exceptions.
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init(true);
    }

    private static LoadObjectHandler newHandler(String url) throws RequestHandlerBadRequestException {
        LoadObjectHandler req = new LoadObjectHandler();
        if (url != null)
            req.setUrl(url);
        return req;
    }

    /**
     * Unit test for bad request - no param.
     * @throws Exception if any error occurs
     */
    @Test
    public void testBadRequestNoParam() throws Exception {
        newHandler(null).handle();
    }

    /**
     * Unit test for bad request - invalid URL.
     * @throws Exception if any error occurs
     */
    @Test
    public void testBadRequestInvalidUrl() throws Exception {
        thrown.expect(RequestHandlerBadRequestException.class);
        thrown.expectMessage("The following keys are mandatory, but have not been provided: objects");
        newHandler("invalid_url").handle();
    }

    /**
     * Unit test for bad request - incomplete URL.
     * @throws Exception if any error occurs
     */
    @Test
    public void testBadRequestIncompleteUrl() throws Exception {
        thrown.expect(RequestHandlerBadRequestException.class);
        thrown.expectMessage("The following keys are mandatory, but have not been provided: objects");
        newHandler("https://localhost").handle();
    }

    /**
     * Unit test for nominal request - local data file.
     * @throws Exception if any error occurs
     */
    @Test
    public void testNominalRequest() throws Exception {
        newHandler("https://localhost?objects=foo,bar").handle();
    }
}
