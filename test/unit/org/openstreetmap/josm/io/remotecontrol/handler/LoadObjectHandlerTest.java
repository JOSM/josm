// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler.RequestHandlerBadRequestException;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link LoadObjectHandler} class.
 */
public class LoadObjectHandlerTest {
    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    private static LoadObjectHandler newHandler(String url) throws RequestHandlerBadRequestException {
        LoadObjectHandler req = new LoadObjectHandler();
        if (url != null)
            req.setUrl(url);
        return req;
    }

    /**
     * Unit test for bad request - no param.
     */
    @Test
    public void testBadRequestNoParam() {
        assertDoesNotThrow(() -> newHandler(null).handle());
    }

    /**
     * Unit test for bad request - invalid URL.
     */
    @Test
    public void testBadRequestInvalidUrl() {
        Exception e = assertThrows(RequestHandlerBadRequestException.class, () -> newHandler("invalid_url").handle());
        assertEquals("The following keys are mandatory, but have not been provided: objects", e.getMessage());
    }

    /**
     * Unit test for bad request - incomplete URL.
     */
    @Test
    public void testBadRequestIncompleteUrl() {
        Exception e = assertThrows(RequestHandlerBadRequestException.class, () -> newHandler("https://localhost").handle());
        assertEquals("The following keys are mandatory, but have not been provided: objects", e.getMessage());
    }

    /**
     * Unit test for nominal request - local data file.
     */
    @Test
    public void testNominalRequest() {
        assertDoesNotThrow(() -> newHandler("https://localhost?objects=foo,bar").handle());
    }
}
