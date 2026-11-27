// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link TextUtils}.
 */
class TextUtilsTest {

    /**
     * Test of {@link TextUtils#wrapLongUrl}.
     */
    @Test
    void testWrapLongUrl() {
        assertEquals("https:/​/​example.org/​x?a=1&​b=2", TextUtils.wrapLongUrl("https://example.org/x?a=1&b=2"));
    }

    /**
     * API keys and tokens in a URL are stripped, as before; other parameters are kept.
     */
    @Test
    void testStripUrl() {
        assertEquals("https://example.org/{z}/{x}/{y}.png?access_token=...stripped...&style=dark",
                TextUtils.stripUrl("https://example.org/{z}/{x}/{y}.png?access_token=abc&style=dark"));
        assertEquals("https://example.org/tiles?apikey=...stripped...&connectId=...stripped...",
                TextUtils.stripUrl("https://example.org/tiles?apikey=abc&connectId=def"));
        assertEquals("https://example.org/search?q=Main+Street&postcode=AB1",
                TextUtils.stripUrl("https://example.org/search?q=Main+Street&postcode=AB1"));
    }

    /**
     * The OAuth 2.0 authorization code in the redirect received by Remote Control is stripped.
     */
    @Test
    void testStripUrlAuthorizationCode() {
        assertEquals("GET /oauth_authorization?code=...stripped...&state=a-state HTTP/1.1",
                TextUtils.stripUrl("GET /oauth_authorization?code=a-code&state=a-state HTTP/1.1"));
        assertEquals("GET /oauth_authorization?state=a-state&code=...stripped... HTTP/1.1",
                TextUtils.stripUrl("GET /oauth_authorization?state=a-state&code=a-code HTTP/1.1"));
    }

    /**
     * The credentials of an OAuth 2.0 token request are stripped from its form-encoded body; the other parameters are
     * kept, among them the grant type, whose value may be {@code refresh_token}.
     */
    @Test
    void testStripUrlFormBody() {
        assertEquals("grant_type=authorization_code&client_id=an-id&client_secret=...stripped...&redirect_uri=http://x"
                + "&code=...stripped...&code_verifier=...stripped...",
                TextUtils.stripUrl("grant_type=authorization_code&client_id=an-id&client_secret=a-secret"
                + "&redirect_uri=http://x&code=a-code&code_verifier=a-verifier"));
        assertEquals("grant_type=refresh_token&refresh_token=...stripped...",
                TextUtils.stripUrl("grant_type=refresh_token&refresh_token=a-refresh-token"));
        assertEquals("username=someone&password=...stripped...", TextUtils.stripUrl("username=someone&password=secret"));
    }

    /**
     * The values of the headers which are credentials are stripped, but for the scheme of an authorization; the
     * other headers are kept as they are, in the same order.
     */
    @Test
    void testStripHeaders() {
        final Map<String, String> request = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        request.put("Accept", "application/xml");
        request.put("authorization", "Bearer a-token");
        request.put("Proxy-Authorization", "Basic dXNlcjpwYXNzd29yZA==");
        request.put("Cookie", "a-cookie=1; other=2");
        assertEquals("{Accept=application/xml, authorization=Bearer ...stripped..., Cookie=...stripped..., "
                + "Proxy-Authorization=Basic ...stripped...}", TextUtils.stripHeaders(request).toString());

        final Map<String, List<String>> response = new LinkedHashMap<>();
        response.put(null, Collections.singletonList("HTTP/1.1 200 OK"));
        response.put("Set-Cookie", Arrays.asList("_osm_session=a-cookie; path=/; HttpOnly", "other=1"));
        response.put("Content-Type", Collections.singletonList("application/json"));
        assertEquals("{null=[HTTP/1.1 200 OK], Set-Cookie=[...stripped..., ...stripped...], Content-Type=[application/json]}",
                TextUtils.stripHeaders(response).toString());
    }
}
