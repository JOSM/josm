// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.BasicWiremock;
import org.openstreetmap.josm.testutils.annotations.HTTP;
import org.openstreetmap.josm.tools.HttpClient.Response;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.admin.model.ServeEventQuery;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

/**
 * Tests the {@link HttpClient}.
 */
@HTTP
@BasicWiremock
@BasicPreferences
@Timeout(15)
class HttpClientTest {
    /**
     * mocked local http server
     */
    @BasicWiremock
    public WireMockServer localServer;

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
    @BeforeEach
    public void setUp() {
        localServer.resetAll();
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
    void testConstructorGetterSetter() throws IOException {
        final URL localUrl = url("");
        final HttpClient client = HttpClient.create(localUrl);
        assertThat(client.getURL(), is(localUrl));
        assertThat(client.getRequestMethod(), is("GET"));
        assertThat(client.getRequestHeader("Accept"), is("*/*"));
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
    void testGet() throws IOException {
        final UrlPattern pattern = urlEqualTo("/get?foo=bar");
        localServer.stubFor(get(pattern).willReturn(aResponse().withStatusMessage("OK")
                .withHeader("Content-Type", "application/json; encoding=utf-8")));
        final Response response = connect("/get?foo=bar");
        assertThat(response.getRequestMethod(), is("GET"));
        assertThat(response.getResponseCode(), is(200));
        assertThat(response.getResponseMessage(), equalToIgnoringCase("OK"));
        assertThat(response.getContentType(), is("application/json; encoding=utf-8"));
        assertThat(response.getHeaderField("Content-Type"), is("application/json; encoding=utf-8"));
        assertThat(response.getHeaderField("Content-TYPE"), is("application/json; encoding=utf-8"));
        assertThat(response.getHeaderFields().get("Content-Type"), is(Collections.singletonList("application/json; encoding=utf-8")));
        assertThat(response.getHeaderFields().get("Content-TYPE"), is(Collections.singletonList("application/json; encoding=utf-8")));
        localServer.verify(getRequestedFor(pattern)
                .withQueryParam("foo", equalTo("bar"))
                .withoutHeader("Cache-Control")
                .withoutHeader("Pragma"));
    }

    /**
     * Test JOSM User-Agent and the incoming request's HTTP headers.
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testHeaders() throws IOException {
        final UrlPattern pattern = urlEqualTo("/headers");
        localServer.stubFor(get(pattern).willReturn(aResponse()));
        connect("/headers");
        localServer.verify(getRequestedFor(pattern)
                .withHeader("Accept", equalTo("*/*"))
                .withHeader("Accept-Encoding", equalTo("gzip, deflate"))
                .withHeader("User-Agent", equalTo(Version.getInstance().getFullAgentString())));
    }

    /**
     * Test UTF-8 encoded content
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testFetchUtf8Content() throws IOException {
        localServer.stubFor(get(urlEqualTo("/encoding/utf8"))
                .willReturn(aResponse().withBody("∀x∈ℝ: UTF-8 encoded sample plain-text file")));
        final Response response = connect("/encoding/utf8");
        assertThat(response.getResponseCode(), is(200));
        final String content = response.fetchContent();
        assertThat(content, containsString("UTF-8 encoded sample plain-text file"));
        assertThat(content, containsString("\u2200x\u2208\u211d:"));
    }

    /**
     * Test HTTP POST with non-empty body
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testPost() throws IOException {
        final UrlPattern pattern = urlEqualTo("/post");
        localServer.stubFor(post(pattern).willReturn(aResponse()));
        final String text = "Hello World!\nGeetings from JOSM, the Java OpenStreetMap Editor";
        final Response response = HttpClient.create(url("/post"), "POST")
                .setHeader("Content-Type", "text/plain")
                .setRequestBody(text.getBytes(StandardCharsets.UTF_8))
                .setFinishOnCloseOutput(false) // to fix #12583, not sure if it's the best way to do it
                .connect(progress);
        assertThat(response.getResponseCode(), is(200));
        assertThat(response.getRequestMethod(), is("POST"));
        localServer.verify(postRequestedFor(pattern).withRequestBody(equalTo(text)));
    }

    /**
     * Test HTTP POST with empty body
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testPostZero() throws IOException {
        final UrlPattern pattern = urlEqualTo("/post");
        localServer.stubFor(post(pattern).willReturn(aResponse()));
        final byte[] bytes = "".getBytes(StandardCharsets.UTF_8);
        final Response response = HttpClient.create(url("/post"), "POST")
                .setHeader("Content-Type", "text/plain")
                .setRequestBody(bytes)
                .setFinishOnCloseOutput(false) // to fix #12583, not sure if it's the best way to do it
                .connect(progress);
        assertThat(response.getResponseCode(), is(200));
        assertThat(response.getRequestMethod(), is("POST"));
        localServer.verify(postRequestedFor(pattern).withRequestBody(binaryEqualTo(bytes)));
    }

    @Test
    void testRelativeRedirects() throws IOException {
        mockRedirects(false, 3);
        final Response response = connect("/relative-redirect/3");
        assertThat(response.getResponseCode(), is(200));
        assertThat(response.getHeaderField("foo"), is("bar"));
    }

    @Test
    void testAbsoluteRedirects() throws IOException {
        mockRedirects(true, 3);
        final Response response = connect("/absolute-redirect/3");
        assertThat(response.getResponseCode(), is(200));
        assertThat(response.getHeaderField("foo"), is("bar"));
    }

    /**
     * Test maximum number of redirections.
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testTooMuchRedirects() throws IOException {
        mockRedirects(false, 3);
        assertThrows(IOException.class, () -> HttpClient.create(url("/relative-redirect/3")).setMaxRedirects(2).connect(progress));
    }

    /**
     * Ensure that we don't leak authorization headers
     * See <a href="https://josm.openstreetmap.de/ticket/21935">JOSM #21935</a>
     * @param authorization The various authorization configurations to test
     */
    @ParameterizedTest
    @ValueSource(strings = { "Basic dXNlcm5hbWU6cGFzc3dvcmQ=", "Digest username=test_user",
            /* OAuth 1.0 for OSM as implemented in JOSM core */
            "OAuth oauth_consumer_key=\"test_key\", oauth_nonce=\"1234\", oauth_signature=\"test_signature\", "
                    + "oauth_signature_method=\"HMAC-SHA1\", oauth_timestamp=\"0\", oauth_token=\"test_token\", "
                    + "oauth_version=\"1.0\"",
            /* OAuth 2.0, not yet implemented in JOSM core */
            "Bearer some_random_token"
        })
    void testRedirectsToDifferentSite(String authorization) throws IOException {
        final String localhost = "localhost";
        final String localhostIp = "127.0.0.1";
        final String otherServer = this.localServer.baseUrl().contains(localhost) ? localhostIp : localhost;
        final UUID redirect = this.localServer.stubFor(get(urlEqualTo("/redirect/other-site"))
                .willReturn(aResponse().withStatus(302).withHeader(
                        "Location", localServer.url("/same-site/other-site")))).getId();
        final UUID sameSite = this.localServer.stubFor(get(urlEqualTo("/same-site/other-site"))
                .willReturn(aResponse().withStatus(302).withHeader(
                        "Location", localServer.url("/other-site")
                                .replace(otherServer == localhost ? localhostIp : localhost, otherServer)))).getId();
        final UUID otherSite = this.localServer.stubFor(get(urlEqualTo("/other-site"))
                .willReturn(aResponse().withStatus(200).withBody("other-site-here"))).getId();
        final HttpClient client = HttpClient.create(url("/redirect/other-site"));
        client.setHeader("Authorization", authorization);
        try {
            client.connect();
            this.localServer.getServeEvents();
            final ServeEvent first = this.localServer.getServeEvents(ServeEventQuery.forStubMapping(redirect)).getRequests().get(0);
            final ServeEvent second = this.localServer.getServeEvents(ServeEventQuery.forStubMapping(sameSite)).getRequests().get(0);
            final ServeEvent third = this.localServer.getServeEvents(ServeEventQuery.forStubMapping(otherSite)).getRequests().get(0);
            assertAll(() -> assertEquals(3, this.localServer.getServeEvents().getRequests().size()),
                    () -> assertEquals(authorization, first.getRequest().getHeader("Authorization"),
                    "Authorization is expected for the first request: " + first.getRequest().getUrl()),
                    () -> assertEquals(authorization, second.getRequest().getHeader("Authorization"),
                            "Authorization is expected for the second request: " + second.getRequest().getUrl()),
                    () -> assertFalse(third.getRequest().containsHeader("Authorization"),
                    "Authorization is not expected for the third request: " + third.getRequest().getUrl()));
        } finally {
            client.disconnect();
        }
    }

    /**
     * Test HTTP error 418
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testHttp418() throws IOException {
        // https://tools.ietf.org/html/rfc2324
        final Response response = doTestHttp(418, "I'm a teapot!", "I'm a teapot!",
                Collections.singletonMap("X-More-Info", "http://tools.ietf.org/html/rfc2324"));
        assertThat(response.getHeaderField("X-More-Info"), is("http://tools.ietf.org/html/rfc2324"));
    }

    /**
     * Test HTTP error 401
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testHttp401() throws IOException {
        // https://tools.ietf.org/html/rfc2324
        doTestHttp(401, "UNAUTHORIZED", null);
    }

    /**
     * Test HTTP error 402
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testHttp402() throws IOException {
        // https://tools.ietf.org/html/rfc2324
        doTestHttp(402, "PAYMENT REQUIRED", "Fuck you, pay me!");
    }

    /**
     * Test HTTP error 403
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testHttp403() throws IOException {
        // https://tools.ietf.org/html/rfc2324
        doTestHttp(403, "FORBIDDEN", null);
    }

    /**
     * Test HTTP error 404
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testHttp404() throws IOException {
        // https://tools.ietf.org/html/rfc2324
        doTestHttp(404, "NOT FOUND", null);
    }

    /**
     * Test HTTP error 500
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testHttp500() throws IOException {
        // https://tools.ietf.org/html/rfc2324
        doTestHttp(500, "INTERNAL SERVER ERROR", null);
    }

    /**
     * Checks that a slow request is well handled if it completes before the timeout.
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testRequestInTime() throws IOException {
        mockDelay(1);
        final Response response = HttpClient.create(url("/delay/1")).setReadTimeout(2000).connect(progress);
        assertThat(response.getResponseCode(), is(200));
    }

    /**
     * Checks that a slow request results in the expected exception if it exceeds the timeout.
     * @throws IOException always
     */
    @Test
    void testTakesTooLong() throws IOException {
        mockDelay(1);
        assertThrows(IOException.class, () -> HttpClient.create(url("/delay/1")).setReadTimeout(500).connect(progress));
    }

    /**
     * Test reading Gzip-encoded data.
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testGzip() throws IOException {
        localServer.stubFor(get(urlEqualTo("/gzip")).willReturn(aResponse().withBody("foo")));
        final Response response = connect("/gzip");
        assertThat(response.getResponseCode(), is(200));
        assertThat(response.getContentEncoding(), is("gzip"));
        assertThat(response.fetchContent(), is("foo"));
    }

    /**
     * Test of {@link Response#uncompress} method with Gzip compression.
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testOpenUrlGzip() throws IOException {
        final Path path = Paths.get(TestUtils.getTestDataRoot(), "tracks/tracks.gpx.gz");
        final byte[] gpx = Files.readAllBytes(path);
        localServer.stubFor(get(urlEqualTo("/trace/1613906/data"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("content-type", "application/x-gzip")
                        .withBody(gpx)));

        final URL url = new URL(localServer.url("/trace/1613906/data"));
        try (BufferedReader x = HttpClient.create(url).connect().uncompress(true).getContentReader()) {
            assertThat(x.readLine(), startsWith("<?xml version="));
        }
    }

    /**
     * Test of {@link Response#uncompress} method with Bzip compression.
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testOpenUrlBzip() throws IOException {
        final Path path = Paths.get(TestUtils.getTestDataRoot(), "tracks/tracks.gpx.bz2");
        final byte[] gpx = Files.readAllBytes(path);
        localServer.stubFor(get(urlEqualTo("/trace/785544/data"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("content-type", "application/x-bzip2")
                        .withBody(gpx)));

        final URL url = new URL(localServer.url("/trace/785544/data"));
        try (BufferedReader x = HttpClient.create(url).connect().uncompress(true).getContentReader()) {
            assertThat(x.readLine(), startsWith("<?xml version="));
        }
    }

    /**
     * Test of {@link Response#uncompress} method with Bzip compression.
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testOpenUrlBzipAccordingToContentDisposition() throws IOException {
        final Path path = Paths.get(TestUtils.getTestDataRoot(), "tracks/tracks.gpx.bz2");
        final byte[] gpx = Files.readAllBytes(path);
        localServer.stubFor(get(urlEqualTo("/trace/1350010/data"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("content-type", "application/octet-stream")
                        .withHeader("content-disposition", "attachment; filename=\"1350010.gpx.bz2\"")
                        .withBody(gpx)));

        final URL url = new URL(localServer.url("/trace/1350010/data"));
        try (BufferedReader x = HttpClient.create(url).connect()
                .uncompress(true).uncompressAccordingToContentDisposition(true).getContentReader()) {
            assertThat(x.readLine(), startsWith("<?xml version="));
        }
    }

    /**
     * Test that error message sent by Tomcat can be parsed.
     */
    @Test
    void testTomcatErrorMessage() {
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

    private void mockDelay(int seconds) {
        localServer.stubFor(get(urlEqualTo("/delay/" + seconds))
                .willReturn(aResponse().withFixedDelay(1000 * seconds)));
    }

    private void mockRedirects(boolean absolute, int n) {
        final String prefix = absolute ? "absolute" : "relative";
        for (int i = n; i > 0; i--) {
            final String location = "/" + prefix + "-redirect/" + (i-1);
            localServer.stubFor(get(urlEqualTo("/" + prefix + "-redirect/" + i))
                    .willReturn(aResponse().withStatus(302).withHeader(
                            "Location", absolute ? localServer.url(location) : location)));
        }
        localServer.stubFor(get(urlEqualTo("/" + prefix + "-redirect/0"))
                .willReturn(aResponse().withHeader("foo", "bar")));
    }

    private Response doTestHttp(int responseCode, String message, String body) throws IOException {
        return doTestHttp(responseCode, message, body, Collections.emptyMap());
    }

    private Response doTestHttp(int responseCode, String message, String body, Map<String, String> headersMap) throws IOException {
        localServer.stubFor(get(urlEqualTo("/status/" + responseCode))
                .willReturn(aResponse().withStatus(responseCode).withStatusMessage(message).withBody(body).withHeaders(
                        new HttpHeaders(headersMap.entrySet().stream().map(
                                e -> new HttpHeader(e.getKey(), e.getValue())).collect(Collectors.toList())))));
        Response response = connect("/status/" + responseCode);
        assertThat(response.getResponseCode(), is(responseCode));
        assertThat(response.getResponseMessage(), equalToIgnoringCase(message));
        final String content = response.fetchContent();
        assertThat(content, is(body == null ? "" : body));
        assertThat(captured.getMessage(), containsString(body == null ? "Server did not return any body" : body));
        assertThat(captured.getLevel(), is(Logging.LEVEL_DEBUG));
        return response;
    }

    private Response connect(String path) throws IOException {
        return HttpClient.create(url(path)).connect(progress);
    }

    private URL url(String path) throws MalformedURLException {
        return new URL(localServer.url(path));
    }
}
