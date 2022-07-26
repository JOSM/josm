// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.oauth.SignpostAdapters.HttpRequest;
import org.openstreetmap.josm.data.oauth.SignpostAdapters.HttpResponse;
import org.openstreetmap.josm.data.oauth.SignpostAdapters.OAuthConsumer;
import org.openstreetmap.josm.testutils.annotations.HTTP;
import org.openstreetmap.josm.testutils.annotations.HTTPS;
import org.openstreetmap.josm.tools.HttpClient;

import net.trajano.commons.testing.UtilityClassTestUtil;

/**
 * Unit tests for class {@link SignpostAdapters}.
 */
@HTTP
@HTTPS
class SignpostAdaptersTest {
    private static HttpClient newClient() throws MalformedURLException {
        return HttpClient.create(new URL("https://www.openstreetmap.org"));
    }

    /**
     * Tests that {@code SignpostAdapters} satisfies utility class criteria.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(SignpostAdapters.class);
    }

    /**
     * Unit test of method {@link SignpostAdapters.OAuthConsumer#wrap}.
     * @throws MalformedURLException never
     */
    @Test
    void testOAuthConsumerWrap() throws MalformedURLException {
        assertNotNull(new OAuthConsumer("", "").wrap(newClient()));
    }

    /**
     * Unit test of method {@link SignpostAdapters.HttpRequest#getMessagePayload}.
     * @throws IOException never
     */
    @Test
    void testHttpRequestGetMessagePayload() throws IOException {
        assertNull(new HttpRequest(newClient()).getMessagePayload());
    }

    /**
     * Unit test of method {@link SignpostAdapters.HttpRequest#setRequestUrl}.
     */
    @Test
    void testHttpRequestSetRequestUrl() {
        assertThrows(IllegalStateException.class, () -> new HttpRequest(newClient()).setRequestUrl(null));
    }

    /**
     * Unit test of method {@link SignpostAdapters.HttpRequest#getAllHeaders}.
     */
    @Test
    void testHttpRequestGetAllHeaders() {
        assertThrows(IllegalStateException.class, () -> new HttpRequest(newClient()).getAllHeaders());
    }

    /**
     * Unit test of method {@link SignpostAdapters.HttpRequest#unwrap}.
     */
    @Test
    void testHttpRequestUnwrap() {
        assertThrows(IllegalStateException.class, () -> new HttpRequest(newClient()).unwrap());
    }

    /**
     * Unit test of method {@link SignpostAdapters.HttpResponse#getReasonPhrase()}.
     * @throws Exception never
     */
    @Test
    void testHttpResponseGetReasonPhrase() throws Exception {
        assertEquals("OK", new HttpResponse(new HttpRequest(newClient()).request.connect()).getReasonPhrase());
    }

    /**
     * Unit test of method {@link SignpostAdapters.HttpResponse#unwrap}.
     */
    @Test
    void testHttpResponseUnwrap() {
        assertThrows(IllegalStateException.class, () -> new HttpResponse(new HttpRequest(newClient()).request.connect()).unwrap());
    }
}
