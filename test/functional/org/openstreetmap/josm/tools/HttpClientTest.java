// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.spi.JsonProvider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tests the {@link HttpClient} using the webservice <a href="https://httpbin.org/">https://httpbin.org/</a>.
 */
public class HttpClientTest {

    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    private ProgressMonitor progress;

    @Before
    public void setUp() {
        progress = TestUtils.newTestProgressMonitor();
    }

    @Test
    public void testConstructorGetterSetter() throws IOException {
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
    public void testGet() throws IOException {
        final HttpClient.Response response = HttpClient.create(new URL("https://httpbin.org/get?foo=bar")).connect(progress);
        assertThat(response.getRequestMethod(), is("GET"));
        assertThat(response.getResponseCode(), is(200));
        assertThat(response.getResponseMessage(), is("OK"));
        assertThat(response.getContentType(), is("application/json"));
        assertThat(response.getHeaderField("Content-Type"), is("application/json"));
        assertThat(response.getHeaderField("Content-TYPE"), is("application/json"));
        assertThat(response.getHeaderFields().get("Content-Type"), is(Collections.singletonList("application/json")));
        assertThat(response.getHeaderFields().get("Content-TYPE"), is(Collections.singletonList("application/json")));
        try (InputStream in = response.getContent();
             JsonReader json = JsonProvider.provider().createReader(in)) {
            final JsonObject root = json.readObject();
            assertThat(root.getJsonObject("args").getString("foo"), is("bar"));
            assertThat(root.getString("url"), is("https://httpbin.org/get?foo=bar"));
        }
    }

    @Test
    public void testUserAgent() throws IOException {
        try (InputStream in = HttpClient.create(new URL("https://httpbin.org/user-agent")).connect(progress).getContent();
             JsonReader json = JsonProvider.provider().createReader(in)) {
            assertThat(json.readObject().getString("user-agent"), is(Version.getInstance().getFullAgentString()));
        }
    }

    @Test
    public void testFetchUtf8Content() throws IOException {
        final HttpClient.Response response = HttpClient.create(new URL("https://httpbin.org/encoding/utf8")).connect(progress);
        assertThat(response.getResponseCode(), is(200));
        final String content = response.fetchContent();
        assertThat(content, containsString("UTF-8 encoded sample plain-text file"));
        assertThat(content, containsString("\u2200x\u2208\u211d:"));
    }

    @Test
    public void testPost() throws IOException {
        final String text = "Hello World!\nGeetings from JOSM, the Java OpenStreetMap Editor";
        final HttpClient.Response response = HttpClient.create(new URL("https://httpbin.org/post"), "POST")
                .setHeader("Content-Type", "text/plain")
                .setRequestBody(text.getBytes(StandardCharsets.UTF_8))
                .setFinishOnCloseOutput(false) // to fix #12583, not sure if it's the best way to do it
                .connect(progress);
        assertThat(response.getResponseCode(), is(200));
        try (InputStream in = response.getContent();
             JsonReader json = JsonProvider.provider().createReader(in)) {
            assertThat(json.readObject().getString("data"), is(text));
        }
    }

    @Test
    public void testPostZero() throws IOException {
        final HttpClient.Response response = HttpClient.create(new URL("https://httpbin.org/post"), "POST")
                .setHeader("Content-Type", "text/plain")
                .setRequestBody("".getBytes(StandardCharsets.UTF_8))
                .setFinishOnCloseOutput(false) // to fix #12583, not sure if it's the best way to do it
                .connect(progress);
        assertThat(response.getResponseCode(), is(200));
        try (InputStream in = response.getContent();
             JsonReader json = JsonProvider.provider().createReader(in)) {
            assertThat(json.readObject().getString("data"), is(""));
        }
    }

    @Test
    public void testRelativeRedirects() throws IOException {
        final HttpClient.Response response = HttpClient.create(new URL("https://httpbin.org/relative-redirect/5")).connect(progress);
        assertThat(response.getResponseCode(), is(200));
        assertThat(response.getContentLength() > 100, is(true));
    }

    @Test
    public void testAbsoluteRedirects() throws IOException {
        final HttpClient.Response response = HttpClient.create(new URL("https://httpbin.org/absolute-redirect/5")).connect(progress);
        assertThat(response.getResponseCode(), is(200));
        assertThat(response.getContentLength() > 100, is(true));
    }

    @Test(expected = IOException.class)
    public void testTooMuchRedirects() throws IOException {
        HttpClient.create(new URL("https://httpbin.org/redirect/5")).setMaxRedirects(4).connect(progress);
    }

    @Test
    public void testHttp418() throws IOException {
        // https://tools.ietf.org/html/rfc2324
        final HttpClient.Response response = HttpClient.create(new URL("https://httpbin.org/status/418")).connect(progress);
        assertThat(response.getResponseCode(), is(418));
        assertThat(response.getResponseMessage(), is("I'M A TEAPOT"));
        final String content = response.fetchContent();
        assertThat(content, containsString("-=[ teapot ]=-"));
    }

    @Test
    public void testRequestInTime() throws IOException {
        final HttpClient.Response response = HttpClient.create(new URL("https://httpbin.org/delay/3")).setReadTimeout(3500).connect(progress);
        assertThat(response.getResponseCode(), is(200));
    }

    @Test(expected = IOException.class)
    public void testTakesTooLong() throws IOException {
        HttpClient.create(new URL("https://httpbin.org/delay/3")).setReadTimeout(2500).connect(progress);
    }

    /**
     * Test of {@link HttpClient.Response#uncompress} method with Gzip compression.
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testOpenUrlGzip() throws IOException {
        final URL url = new URL("https://www.openstreetmap.org/trace/1613906/data");
        try (BufferedReader x = HttpClient.create(url).connect().uncompress(true).getContentReader()) {
            Assert.assertTrue(x.readLine().startsWith("<?xml version="));
        }
    }

    /**
     * Test of {@link HttpClient.Response#uncompress} method with Bzip compression.
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testOpenUrlBzip() throws IOException {
        final URL url = new URL("https://www.openstreetmap.org/trace/785544/data");
        try (BufferedReader x = HttpClient.create(url).connect().uncompress(true).getContentReader()) {
            Assert.assertTrue(x.readLine().startsWith("<?xml version="));
        }
    }

    /**
     * Test of {@link HttpClient.Response#uncompress} method with Bzip compression.
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testTicket9660() throws IOException {
        final URL url = new URL("http://www.openstreetmap.org/trace/1350010/data");
        try (BufferedReader x = HttpClient.create(url).connect()
                .uncompress(true).uncompressAccordingToContentDisposition(true).getContentReader()) {
            Assert.assertTrue(x.readLine().startsWith("<?xml version="));
        }
    }
}
