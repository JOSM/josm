// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler.RequestHandlerBadRequestException;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link ImageryHandler} class.
 */
public class ImageryHandlerTest {

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

    private static ImageryHandler newHandler(String url) throws RequestHandlerBadRequestException {
        ImageryHandler req = new ImageryHandler();
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
        thrown.expect(RequestHandlerBadRequestException.class);
        thrown.expectMessage("Parameter must not be null");
        newHandler(null).handle();
    }

    /**
     * Unit test for bad request - invalid URL.
     * @throws Exception if any error occurs
     */
    @Test
    public void testBadRequestInvalidUrl() throws Exception {
        thrown.expect(RequestHandlerBadRequestException.class);
        thrown.expectMessage("The following keys are mandatory, but have not been provided: url");
        newHandler("invalid_url").handle();
    }

    /**
     * Unit test for bad request - incomplete URL.
     * @throws Exception if any error occurs
     */
    @Test
    public void testBadRequestIncompleteUrl() throws Exception {
        thrown.expect(RequestHandlerBadRequestException.class);
        thrown.expectMessage("The following keys are mandatory, but have not been provided: url");
        newHandler("https://localhost").handle();
    }

    /**
     * Unit test for nominal request - local data file.
     * @throws Exception if any error occurs
     */
    @Test
    public void testNominalRequest() throws Exception {
        newHandler("https://localhost?url=foo").handle();
    }

    /**
     * Unit test for {@link ImageryHandler#getOptionalParams()}
     * @throws Exception if any error occurs
     */
    @Test
    public void testOptionalParams() throws Exception {
        List<String> optionalParams = Arrays.asList(newHandler("").getOptionalParams());
        assertThat(optionalParams, hasItem("type"));
        assertThat(optionalParams, hasItem("min-zoom"));
        assertThat(optionalParams, hasItem("max-zoom"));
        assertThat(optionalParams, hasItem("category"));
    }

    /**
     * Unit test for {@link ImageryHandler#buildImageryInfo()}
     * @throws Exception if any error occurs
     */
    @Test
    public void testBuildImageryInfo() throws Exception {
        String url = "https://localhost/imagery?title=osm"
                + "&type=tms&min_zoom=3&max_zoom=23&category=osmbasedmap&country_code=XA"
                + "&url=https://a.tile.openstreetmap.org/%7Bzoom%7D/%7Bx%7D/%7By%7D.png";
        ImageryInfo imageryInfo = newHandler(url).buildImageryInfo();
        assertEquals("osm", imageryInfo.getName());
        assertEquals(ImageryInfo.ImageryType.TMS, imageryInfo.getImageryType());
        assertEquals("https://a.tile.openstreetmap.org/{zoom}/{x}/{y}.png", imageryInfo.getUrl());
        assertEquals(3, imageryInfo.getMinZoom());
        assertEquals(23, imageryInfo.getMaxZoom());
        assertEquals(ImageryInfo.ImageryCategory.OSMBASEDMAP, imageryInfo.getImageryCategory());
        assertEquals("XA", imageryInfo.getCountryCode());
    }
}
