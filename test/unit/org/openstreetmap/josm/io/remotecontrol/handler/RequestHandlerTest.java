// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;

/**
 * Unit tests of {@link RequestHandler} class.
 */
public class RequestHandlerTest {

    Map<String, String> getRequestParameter(String url) {
        final RequestHandler req = new RequestHandler() {
            @Override
            protected void validateRequest() throws RequestHandlerBadRequestException {
            }

            @Override
            protected void handleRequest() throws RequestHandlerErrorException, RequestHandlerBadRequestException {
            }

            @Override
            public String getPermissionMessage() {
                return null;
            }

            @Override
            public PermissionPrefWithDefault getPermissionPref() {
                return null;
            }

            @Override
            public String[] getMandatoryParams() {
                return new String[0];
            }
        };
        req.setUrl(url);
        return req.args;
    }

    /**
     * Test request parameter - case 1
     */
    @Test
    public void testRequestParameter1() {
        final Map<String, String> expected = new HashMap<>();
        expected.put("query", "a");
        expected.put("b", "=c");
        assertEquals(expected, getRequestParameter("http://example.com/?query=a&b==c"));
    }

    /**
     * Test request parameter - case 2
     */
    @Test
    public void testRequestParameter2() {
        assertEquals(Collections.singletonMap("query", "a&b==c"),
                getRequestParameter("http://example.com/?query=a%26b==c"));
    }

    /**
     * Test request parameter - case 3
     */
    @Test
    public void testRequestParameter3() {
        assertEquals(Collections.singleton("blue+light blue"),
                getRequestParameter("http://example.com/blue+light%20blue?blue%2Blight+blue").keySet());
    }

    /**
     * Test request parameter - case 4
     * @see <a href="http://blog.lunatech.com/2009/02/03/what-every-web-developer-must-know-about-url-encoding">
     *      What every web developer must know about URL encoding</a>
     */
    @Test
    public void testRequestParameter4() {
        assertEquals(Collections.singletonMap("/?:@-._~!$'()* ,;", "/?:@-._~!$'()* ,;=="), getRequestParameter(
                // CHECKSTYLE.OFF: LineLength
                "http://example.com/:@-._~!$&'()*+,=;:@-._~!$&'()*+,=:@-._~!$&'()*+,==?/?:@-._~!$'()*+,;=/?:@-._~!$'()*+,;==#/?:@-._~!$&'()*+,;="));
                // CHECKSTYLE.ON: LineLength
    }

    /**
     * Test request parameter - case 5
     */
    @Test
    public void testRequestParameter5() {
        final Map<String, String> expected = new HashMap<>();
        expected.put("space", " ");
        expected.put("tab", "\t");
        assertEquals(expected, getRequestParameter("http://example.com/?space=%20&tab=%09"));
    }
}
