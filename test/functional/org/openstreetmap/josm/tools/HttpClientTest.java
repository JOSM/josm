// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;

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

    /**
     * Setup test
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().timeout(15000);

    private ProgressMonitor progress;

    private LogRecord captured;
    private final Handler handler = new Handler() {

        @Override
        public void publish(LogRecord record) {
            captured = record;
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    };

    /**
     * Setup test.
     */
    @Before
    public void setUp() {
        progress = TestUtils.newTestProgressMonitor();
        captured = null;
        Logging.getLogger().addHandler(handler);
        Logging.getLogger().setLevel(Logging.LEVEL_DEBUG);
    }

    /**
     * Test constructor, getters and setters
     * @throws IOException if an I/O error occurs
     */
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

    /**
     * Test HTTP GET
     * @throws IOException if an I/O error occurs
     */
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
            assertThat(root.getJsonObject("headers").get("Cache-Control"), nullValue());
            assertThat(root.getJsonObject("headers").get("Pragma"), nullValue());
        }
    }

    /**
     * Test JOSM User-Agent
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testUserAgent() throws IOException {
        try (InputStream in = HttpClient.create(new URL("https://httpbin.org/user-agent")).connect(progress).getContent();
             JsonReader json = JsonProvider.provider().createReader(in)) {
            assertThat(json.readObject().getString("user-agent"), is(Version.getInstance().getFullAgentString()));
        }
    }

    /**
     * Test UTF-8 encoded content
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testFetchUtf8Content() throws IOException {
        final HttpClient.Response response = HttpClient.create(new URL("https://httpbin.org/encoding/utf8")).connect(progress);
        assertThat(response.getResponseCode(), is(200));
        final String content = response.fetchContent();
        assertThat(content, containsString("UTF-8 encoded sample plain-text file"));
        assertThat(content, containsString("\u2200x\u2208\u211d:"));
    }

    /**
     * Test HTTP POST
     * @throws IOException if an I/O error occurs
     */
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
        final HttpClient.Response response = HttpClient.create(new URL("https://httpbin.org/relative-redirect/3")).connect(progress);
        assertThat(response.getResponseCode(), is(200));
        assertThat(response.getContentLength() > 100, is(true));
    }

    @Test
    public void testAbsoluteRedirects() throws IOException {
        final HttpClient.Response response = HttpClient.create(new URL("https://httpbin.org/absolute-redirect/3")).connect(progress);
        assertThat(response.getResponseCode(), is(200));
        assertThat(response.getContentLength() > 100, is(true));
    }

    /**
     * Test maximum number of redirections.
     * @throws IOException if an I/O error occurs
     */
    @Test(expected = IOException.class)
    public void testTooMuchRedirects() throws IOException {
        HttpClient.create(new URL("https://httpbin.org/redirect/3")).setMaxRedirects(2).connect(progress);
    }

    /**
     * Test HTTP error 418
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testHttp418() throws IOException {
        // https://tools.ietf.org/html/rfc2324
        final HttpClient.Response response = HttpClient.create(new URL("https://httpbin.org/status/418")).connect(progress);
        assertThat(response.getResponseCode(), is(418));
        assertThat(response.getResponseMessage(), is("I'M A TEAPOT"));
        final String content = response.fetchContent();
        assertThat(content, containsString("-=[ teapot ]=-"));
        assertThat(captured.getMessage(), containsString("-=[ teapot ]=-"));
        assertThat(captured.getLevel(), is(Logging.LEVEL_DEBUG));
    }

    /**
     * Test HTTP error 401
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testHttp401() throws IOException {
        // https://tools.ietf.org/html/rfc2324
        final HttpClient.Response response = HttpClient.create(new URL("https://httpbin.org/status/401")).connect(progress);
        assertThat(response.getResponseCode(), is(401));
        assertThat(response.getResponseMessage(), is("UNAUTHORIZED"));
        final String content = response.fetchContent();
        assertThat(content, is(""));
        assertThat(captured.getMessage(), containsString("Server did not return any body"));
        assertThat(captured.getLevel(), is(Logging.LEVEL_DEBUG));
    }

    /**
     * Test HTTP error 402
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testHttp402() throws IOException {
        // https://tools.ietf.org/html/rfc2324
        final HttpClient.Response response = HttpClient.create(new URL("https://httpbin.org/status/402")).connect(progress);
        assertThat(response.getResponseCode(), is(402));
        assertThat(response.getResponseMessage(), is("PAYMENT REQUIRED"));
        final String content = response.fetchContent();
        assertThat(content, containsString("Fuck you, pay me!"));
        assertThat(captured.getMessage(), containsString("Fuck you, pay me!"));
        assertThat(captured.getLevel(), is(Logging.LEVEL_DEBUG));
    }

    /**
     * Test HTTP error 403
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testHttp403() throws IOException {
        // https://tools.ietf.org/html/rfc2324
        final HttpClient.Response response = HttpClient.create(new URL("https://httpbin.org/status/403")).connect(progress);
        assertThat(response.getResponseCode(), is(403));
        assertThat(response.getResponseMessage(), is("FORBIDDEN"));
        final String content = response.fetchContent();
        assertThat(content, is(""));
        assertThat(captured.getMessage(), containsString("Server did not return any body"));
        assertThat(captured.getLevel(), is(Logging.LEVEL_DEBUG));
    }

    /**
     * Test HTTP error 404
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testHttp404() throws IOException {
        // https://tools.ietf.org/html/rfc2324
        final HttpClient.Response response = HttpClient.create(new URL("https://httpbin.org/status/404")).connect(progress);
        assertThat(response.getResponseCode(), is(404));
        assertThat(response.getResponseMessage(), is("NOT FOUND"));
        final String content = response.fetchContent();
        assertThat(content, is(""));
        assertThat(captured.getMessage(), containsString("Server did not return any body"));
        assertThat(captured.getLevel(), is(Logging.LEVEL_DEBUG));
    }

    /**
     * Test HTTP error 500
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testHttp500() throws IOException {
        // https://tools.ietf.org/html/rfc2324
        final HttpClient.Response response = HttpClient.create(new URL("https://httpbin.org/status/500")).connect(progress);
        assertThat(response.getResponseCode(), is(500));
        assertThat(response.getResponseMessage(), is("INTERNAL SERVER ERROR"));
        final String content = response.fetchContent();
        assertThat(content, containsString(""));
        assertThat(captured.getMessage(), containsString("Server did not return any body"));
        assertThat(captured.getLevel(), is(Logging.LEVEL_DEBUG));
    }

    /**
     * Checks that a slow request is well handled if it completes before the timeout.
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testRequestInTime() throws IOException {
        final HttpClient.Response response = HttpClient.create(new URL("https://httpbin.org/delay/1")).setReadTimeout(2000).connect(progress);
        assertThat(response.getResponseCode(), is(200));
    }

    /**
     * Checks that a slow request results in the expected exception if it exceeds the timeout.
     * @throws IOException always
     */
    @Test(expected = IOException.class)
    public void testTakesTooLong() throws IOException {
        HttpClient.create(new URL("https://httpbin.org/delay/1")).setReadTimeout(500).connect(progress);
    }

    /**
     * Test reading Deflate-encoded data.
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testDeflate() throws IOException {
        final HttpClient.Response response = HttpClient.create(new URL("https://httpbin.org/deflate")).connect(progress);
        assertThat(response.getResponseCode(), is(200));
        try (InputStream in = response.getContent();
             JsonReader json = JsonProvider.provider().createReader(in)) {
            assertThat(json.readObject().getBoolean("deflated"), is(true));
        }
    }

    /**
     * Test reading Gzip-encoded data.
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testGzip() throws IOException {
        final HttpClient.Response response = HttpClient.create(new URL("https://httpbin.org/gzip")).connect(progress);
        assertThat(response.getResponseCode(), is(200));
        try (InputStream in = response.getContent();
             JsonReader json = JsonProvider.provider().createReader(in)) {
            assertThat(json.readObject().getBoolean("gzipped"), is(true));
        }
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

    /**
     * Test that error message sent by Tomcat can be parsed.
     */
    @Test
    public void testTomcatErrorMessage() {
        Matcher m = HttpClient.getTomcatErrorMatcher(
            "<html><head><title>Apache Tomcat/DGFiP - Rapport d''erreur</title><style><!--"+
                "H1 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:22px;} "+
                "H2 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:16px;} "+
                "H3 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:14px;} "+
                "BODY {font-family:Tahoma,Arial,sans-serif;color:black;background-color:white;} "+
                "B {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;} "+
                "P {font-family:Tahoma,Arial,sans-serif;background:white;color:black;font-size:12px;}"+
                "A {color : black;}A.name {color : black;}HR {color : #525D76;}"+
            "--></style> </head><body><h1>Etat HTTP 400 - La commune demandée n'existe pas ou n'est pas accessible.</h1>"+
            "<HR size=\"1\" noshade=\"noshade\">"+
            "<p><b>type</b> Rapport d''état</p><p><b>message</b> <u>La commune demandée n'existe pas ou n'est pas accessible.</u></p>"+
            "<p><b>description</b> <u>La requête envoyée par le client était syntaxiquement incorrecte.</u></p>"+
            "<HR size=\"1\" noshade=\"noshade\"><h3>Apache Tomcat/DGFiP</h3></body></html>");
        assertTrue(m.matches());
        assertEquals("La commune demandée n'existe pas ou n'est pas accessible.", m.group(1));
    }
}
