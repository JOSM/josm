// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.spi.JsonProvider;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.Version;

/**
 * Tests the {@link HttpClient} using the webservice <a href="https://httpbin.org/">https://httpbin.org/</a>.
 */
public class HttpClientTest {

    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createFunctionalTestFixture().init();
    }

    @Test
    public void testConstructorGetterSetter() throws Exception {
        final HttpClient client = HttpClient.create(new URL("https://httpbin.org/"));
        assertThat(client.getURL(), is(new URL("https://httpbin.org/")));
        assertThat(client.getRequestMethod(), is("GET"));
        assertThat(client.getRequestHeader("Accept"), nullValue());
        client.setAccept("text/html");
        assertThat(client.getRequestHeader("Accept"), is("text/html"));
        assertThat(client.getRequestHeader("ACCEPT"), is("text/html"));
        client.setHeaders(Collections.singletonMap("foo", "bar"));
        assertThat(client.getRequestHeader("foo"), is("bar"));
        client.setHeaders(Collections.singletonMap("foo", "baz"));
        assertThat(client.getRequestHeader("foo"), is("baz"));
        client.setHeaders(Collections.singletonMap("foo", (String) null));
        assertThat(client.getRequestHeader("foo"), nullValue());
    }

    @Test
    public void testGet() throws Exception {
        final HttpClient.Response response = HttpClient.create(new URL("https://httpbin.org/get?foo=bar")).connect();
        assertThat(response.getRequestMethod(), is("GET"));
        assertThat(response.getResponseCode(), is(200));
        assertThat(response.getResponseMessage(), is("OK"));
        assertThat(response.getContentType(), is("application/json"));
        assertThat(response.getHeaderField("Content-Type"), is("application/json"));
        assertThat(response.getHeaderField("Content-TYPE"), is("application/json"));
        assertThat(response.getHeaderFields().get("Content-Type"), is(Collections.singletonList("application/json")));
        assertThat(response.getHeaderFields().get("Content-TYPE"), nullValue());
        try (final InputStream in = response.getContent();
             final JsonReader json = JsonProvider.provider().createReader(in)) {
            final JsonObject root = json.readObject();
            assertThat(root.getJsonObject("args").getString("foo"), is("bar"));
            assertThat(root.getString("url"), is("https://httpbin.org/get?foo=bar"));
        }
    }

    @Test
    public void testUserAgent() throws Exception {
        try (final InputStream in = HttpClient.create(new URL("https://httpbin.org/user-agent")).connect().getContent();
             final JsonReader json = JsonProvider.provider().createReader(in)) {
            assertThat(json.readObject().getString("user-agent"), is(Version.getInstance().getFullAgentString()));
        }
    }

    @Test
    public void testFetchUtf8Content() throws Exception {
        final HttpClient.Response response = HttpClient.create(new URL("https://httpbin.org/encoding/utf8")).connect();
        assertThat(response.getResponseCode(), is(200));
        final String content = response.fetchContent();
        assertThat(content, containsString("UTF-8 encoded sample plain-text file"));
        assertThat(content, containsString("\u2200x\u2208\u211d:"));
    }

    @Test
    public void testPost() throws Exception {
        final String text = "Hello World!\nGeetings from JOSM, the Java OpenStreetMap Editor";
        final HttpClient.Response response = HttpClient.create(new URL("https://httpbin.org/post"), "POST")
                .setHeader("Content-Type", "text/plain")
                .setRequestBody(text.getBytes(StandardCharsets.UTF_8))
                .connect();
        assertThat(response.getResponseCode(), is(200));
        try (final InputStream in = response.getContent();
             final JsonReader json = JsonProvider.provider().createReader(in)) {
            assertThat(json.readObject().getString("data"), is(text));
        }
    }

    @Test
    public void testRelativeRedirects() throws Exception {
        final HttpClient.Response response = HttpClient.create(new URL("https://httpbin.org/relative-redirect/5")).connect();
        assertThat(response.getResponseCode(), is(200));
        assertThat(response.getContentLength() > 100, is(true));
    }

    @Test
    public void testAbsoluteRedirects() throws Exception {
        final HttpClient.Response response = HttpClient.create(new URL("https://httpbin.org/absolute-redirect/5")).connect();
        assertThat(response.getResponseCode(), is(200));
        assertThat(response.getContentLength() > 100, is(true));
    }

    @Test(expected = IOException.class)
    public void testTooMuchRedirects() throws Exception {
        HttpClient.create(new URL("https://httpbin.org/redirect/5")).setMaxRedirects(4).connect();
    }

    @Test
    public void test418() throws Exception {
        // https://tools.ietf.org/html/rfc2324
        final HttpClient.Response response = HttpClient.create(new URL("https://httpbin.org/status/418")).connect();
        assertThat(response.getResponseCode(), is(418));
        assertThat(response.getResponseMessage(), is("I'M A TEAPOT"));
        final String content = response.fetchContent();
        assertThat(content, containsString("-=[ teapot ]=-"));
    }

    @Test
    public void testRequestInTime() throws Exception {
        final HttpClient.Response response = HttpClient.create(new URL("https://httpbin.org/delay/3")).setReadTimeout(3500).connect();
        assertThat(response.getResponseCode(), is(200));
    }

    @Test(expected = IOException.class)
    public void testTakesTooLong() throws Exception {
        HttpClient.create(new URL("https://httpbin.org/delay/3")).setReadTimeout(2500).connect();
    }
}
