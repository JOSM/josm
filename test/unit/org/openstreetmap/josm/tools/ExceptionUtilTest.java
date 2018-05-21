// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.TimeZone;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.io.ChangesetClosedException;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.MissingOAuthAccessTokenException;
import org.openstreetmap.josm.io.OfflineAccessException;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmApiException;
import org.openstreetmap.josm.io.OsmApiInitializationException;
import org.openstreetmap.josm.io.auth.CredentialsManager;
import org.openstreetmap.josm.tools.date.DateUtils;
import org.openstreetmap.josm.tools.date.DateUtilsTest;

/**
 * Unit tests of {@link ExceptionUtil} class.
 */
public class ExceptionUtilTest {

    private static String baseUrl;
    private static String serverUrl;
    private static String host;
    private static String user;

    /**
     * Setup test.
     * @throws Exception in case of error
     */
    @BeforeClass
    public static void setUp() throws Exception {
        JOSMFixture.createUnitTestFixture().init();
        OsmApi api = OsmApi.getOsmApi();
        api.initialize(null);
        baseUrl = api.getBaseUrl();
        serverUrl = api.getServerUrl();
        host = new URL(serverUrl).getHost();
        user = CredentialsManager.getInstance().getUsername();
        DateUtils.PROP_ISO_DATES.put(Boolean.TRUE);
    }

    /**
     * Test of {@link ExceptionUtil#explainBadRequest} method.
     */
    @Test
    public void testExplainBadRequest() {
        assertEquals("<html>The OSM server '"+baseUrl+"' reported a bad request.<br></html>",
                ExceptionUtil.explainBadRequest(new OsmApiException("")));

        assertEquals("<html>The OSM server '"+baseUrl+"' reported a bad request.<br><br>"+
                "Error message(untranslated): header</html>",
                ExceptionUtil.explainBadRequest(new OsmApiException(HttpURLConnection.HTTP_BAD_REQUEST, "header", "")));

        assertEquals("<html>The OSM server '"+baseUrl+"' reported a bad request.<br><br>"+
                "Error message(untranslated): header</html>",
                ExceptionUtil.explainBadRequest(new OsmApiException(HttpURLConnection.HTTP_BAD_REQUEST, "header", "", "invalid_url")));

        assertEquals("<html>The OSM server '"+host+"' reported a bad request.<br><br>"+
                "Error message(untranslated): header</html>",
                ExceptionUtil.explainBadRequest(new OsmApiException(HttpURLConnection.HTTP_BAD_REQUEST, "header", "", baseUrl)));

        assertEquals("<html>The OSM server '"+baseUrl+"' reported a bad request.<br><br>"+
                "The area you tried to download is too big or your request was too large.<br>"+
                "Either request a smaller area or use an export file provided by the OSM community.</html>",
                ExceptionUtil.explainBadRequest(new OsmApiException(HttpURLConnection.HTTP_BAD_REQUEST, "The maximum bbox", "")));

        assertEquals("<html>The OSM server '"+baseUrl+"' reported a bad request.<br><br>"+
                "The area you tried to download is too big or your request was too large.<br>"+
                "Either request a smaller area or use an export file provided by the OSM community.</html>",
                ExceptionUtil.explainBadRequest(new OsmApiException(HttpURLConnection.HTTP_BAD_REQUEST, "You requested too many nodes", "")));
    }

    /**
     * Test of {@link ExceptionUtil#explainBandwidthLimitExceeded} method.
     */
    @Test
    public void testExplainBandwidthLimitExceeded() {
        assertEquals("<html>Communication with the OSM server '"+baseUrl+"'failed. "+
                "The server replied<br>the following error code and the following error message:<br>"+
                "<strong>Error code:<strong> 0<br><strong>Error message (untranslated)</strong>: no error message available</html>",
                ExceptionUtil.explainBandwidthLimitExceeded(new OsmApiException("")));
    }

    /**
     * Test of {@link ExceptionUtil#explainChangesetClosedException} method.
     */
    @Test
    public void testExplainChangesetClosedException() {
        assertEquals("<html>Failed to upload to changeset <strong>0</strong><br>because it has already been closed on ?.",
                ExceptionUtil.explainChangesetClosedException(new ChangesetClosedException("")));

        assertEquals("<html>Failed to upload to changeset <strong>1</strong><br>because it has already been closed on 2016-01-01 00:00:00.",
                ExceptionUtil.explainChangesetClosedException(new ChangesetClosedException(1, DateUtils.fromString("2016-01-01"), null)));
    }

    /**
     * Test of {@link ExceptionUtil#explainClientTimeout} method.
     */
    @Test
    public void testExplainClientTimeout() {
        assertEquals("<html>Communication with the OSM server '"+baseUrl+"' timed out. Please retry later.</html>",
                ExceptionUtil.explainClientTimeout(new OsmApiException("")));
    }

    /**
     * Test of {@link ExceptionUtil#explainConflict} method.
     */
    @Test
    public void testExplainConflict() {
        int code = HttpURLConnection.HTTP_CONFLICT;
        assertEquals("<html>The server reported that it has detected a conflict.</html>",
                ExceptionUtil.explainConflict(new OsmApiException("")));
        assertEquals("<html>The server reported that it has detected a conflict.<br>Error message (untranslated):<br>header</html>",
                ExceptionUtil.explainConflict(new OsmApiException(code, "header", "")));
        assertEquals("<html>Closing of changeset <strong>1</strong> failed <br>because it has already been closed.",
                ExceptionUtil.explainConflict(new OsmApiException(code, "The changeset 1 was closed at xxx", "")));
        assertEquals("<html>Closing of changeset <strong>1</strong> failed<br> because it has already been closed on 2016-01-01 12:34:56.",
                ExceptionUtil.explainConflict(new OsmApiException(code, "The changeset 1 was closed at 2016-01-01 12:34:56 UTC", "")));
        DateUtilsTest.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
        assertEquals("<html>Closing of changeset <strong>1</strong> failed<br> because it has already been closed on 2016-01-01 13:34:56.",
                ExceptionUtil.explainConflict(new OsmApiException(code, "The changeset 1 was closed at 2016-01-01 12:34:56 UTC", "")));
    }

    /**
     * Test of {@link ExceptionUtil#explainException} method.
     */
    @Test
    public void testExplainException() {
        assertEquals("ResponseCode=0",
                ExceptionUtil.explainException(new OsmApiException("")));
        assertEquals("java.lang.Exception: ",
                ExceptionUtil.explainException(new Exception("")));
        assertEquals("java.lang.Exception",
                ExceptionUtil.explainException(new Exception(null, null)));
        assertEquals("test",
                ExceptionUtil.explainException(new Exception("test")));
    }

    /**
     * Test of {@link ExceptionUtil#explainFailedAuthorisation} method.
     */
    @Test
    public void testExplainFailedAuthorisation() {
        assertEquals("<html>Authorisation at the OSM server failed.<br></html>",
                ExceptionUtil.explainFailedAuthorisation(new OsmApiException("")));
        assertEquals("<html>Authorisation at the OSM server failed.<br>The server reported the following error:<br>'header'</html>",
                ExceptionUtil.explainFailedAuthorisation(new OsmApiException(HttpURLConnection.HTTP_FORBIDDEN, "header", null)));
        assertEquals("<html>Authorisation at the OSM server failed.<br>The server reported the following error:<br>'header (body)'</html>",
                ExceptionUtil.explainFailedAuthorisation(new OsmApiException(HttpURLConnection.HTTP_FORBIDDEN, "header", "body")));
        assertEquals("<html>Authorisation at the OSM server failed.<br>The server reported the following error:<br>'body'</html>",
                ExceptionUtil.explainFailedAuthorisation(new OsmApiException(HttpURLConnection.HTTP_FORBIDDEN, null, "body")));
    }

    /**
     * Test of {@link ExceptionUtil#explainFailedOAuthAuthorisation} method.
     */
    @Test
    public void testExplainFailedOAuthAuthorisation() {
        assertEquals("<html>Authorisation at the OSM server with the OAuth token 'null' failed.<br>"+
                "The token is not authorised to access the protected resource<br>'unknown'.<br>"+
                "Please launch the preferences dialog and retrieve another OAuth token.</html>",
                ExceptionUtil.explainFailedOAuthAuthorisation(new OsmApiException("")));
        assertEquals("<html>Authorisation at the OSM server with the OAuth token 'null' failed.<br>"+
                "The token is not authorised to access the protected resource<br>'"+baseUrl+"'.<br>"+
                "Please launch the preferences dialog and retrieve another OAuth token.</html>",
                ExceptionUtil.explainFailedOAuthAuthorisation(new OsmApiException(HttpURLConnection.HTTP_FORBIDDEN, "", "", baseUrl)));
    }

    /**
     * Test of {@link ExceptionUtil#explainFailedBasicAuthentication} method.
     */
    @Test
    public void testExplainFailedBasicAuthentication() {
        assertEquals("<html>Authentication at the OSM server with the username '"+user+"' failed.<br>"+
                "Please check the username and the password in the JOSM preferences.</html>",
                ExceptionUtil.explainFailedBasicAuthentication(new OsmApiException("")));
    }

    /**
     * Test of {@link ExceptionUtil#explainFailedOAuthAuthentication} method.
     */
    @Test
    public void testExplainFailedOAuthAuthentication() {
        assertEquals("<html>Authentication at the OSM server with the OAuth token 'null' failed.<br>"+
                "Please launch the preferences dialog and retrieve another OAuth token.</html>",
                ExceptionUtil.explainFailedOAuthAuthentication(new OsmApiException("")));
    }

    /**
     * Test of {@link ExceptionUtil#explainGenericOsmApiException} method.
     */
    @Test
    public void testExplainGenericOsmApiException() {
        assertEquals("<html>Communication with the OSM server '"+baseUrl+"'failed. The server replied<br>"+
                "the following error code and the following error message:<br><strong>Error code:<strong> 0<br>"+
                "<strong>Error message (untranslated)</strong>: no error message available</html>",
                ExceptionUtil.explainGenericOsmApiException(new OsmApiException("")));

        assertEquals("<html>Communication with the OSM server '"+baseUrl+"'failed. The server replied<br>"+
                "the following error code and the following error message:<br><strong>Error code:<strong> 500<br>"+
                "<strong>Error message (untranslated)</strong>: header</html>",
                ExceptionUtil.explainGenericOsmApiException(new OsmApiException(HttpURLConnection.HTTP_INTERNAL_ERROR, "header", null)));

        assertEquals("<html>Communication with the OSM server '"+baseUrl+"'failed. The server replied<br>"+
                "the following error code and the following error message:<br><strong>Error code:<strong> 500<br>"+
                "<strong>Error message (untranslated)</strong>: body</html>",
                ExceptionUtil.explainGenericOsmApiException(new OsmApiException(HttpURLConnection.HTTP_INTERNAL_ERROR, null, "body")));
    }

    /**
     * Test of {@link ExceptionUtil#explainGoneForUnknownPrimitive} method.
     */
    @Test
    public void testExplainGoneForUnknownPrimitive() {
        assertEquals("<html>The server reports that an object is deleted.<br>"+
                "<strong>Uploading failed</strong> if you tried to update or delete this object.<br> "+
                "<strong>Downloading failed</strong> if you tried to download this object.<br><br>"+
                "The error message is:<br>ResponseCode=0</html>",
                ExceptionUtil.explainGoneForUnknownPrimitive(new OsmApiException("")));
    }

    /**
     * Test of {@link ExceptionUtil#explainInternalServerError} method.
     */
    @Test
    public void testExplainInternalServerError() {
        assertEquals("<html>The OSM server<br>'"+baseUrl+"'<br>reported an internal server error.<br>"+
                "This is most likely a temporary problem. Please try again later.</html>",
                ExceptionUtil.explainInternalServerError(new OsmApiException("")));
    }

    /**
     * Test of {@link ExceptionUtil#explainMissingOAuthAccessTokenException} method.
     */
    @Test
    public void testExplainMissingOAuthAccessTokenException() {
        assertEquals("<html>Failed to authenticate at the OSM server 'https://api06.dev.openstreetmap.org/api'.<br>"+
                "You are using OAuth to authenticate but currently there is no<br>OAuth Access Token configured.<br>"+
                "Please open the Preferences Dialog and generate or enter an Access Token.</html>",
                ExceptionUtil.explainMissingOAuthAccessTokenException(new MissingOAuthAccessTokenException()));
    }

    /**
     * Test of {@link ExceptionUtil#explainNestedIllegalDataException} method.
     */
    @Test
    public void testExplainNestedIllegalDataException() {
        assertEquals("<html>Failed to download data. Its format is either unsupported, ill-formed, and/or inconsistent.<br><br>"+
                "Details (untranslated): null</html>",
                ExceptionUtil.explainNestedIllegalDataException(new OsmApiException("")));

        assertEquals("<html>Failed to download data. Its format is either unsupported, ill-formed, and/or inconsistent.<br><br>"+
                "Details (untranslated): test</html>",
                ExceptionUtil.explainNestedIllegalDataException(new OsmApiException(new IllegalDataException("test"))));
    }

    /**
     * Test of {@link ExceptionUtil#explainNestedIOException} method.
     */
    @Test
    public void testExplainNestedIOException() {
        assertEquals("<html>Failed to upload data to or download data from<br>'"+baseUrl+"'<br>"+
                "due to a problem with transferring data.<br>Details (untranslated): null</html>",
                ExceptionUtil.explainNestedIOException(new OsmApiException("")));

        assertEquals("<html>Failed to upload data to or download data from<br>'"+baseUrl+"'<br>"+
                "due to a problem with transferring data.<br>Details (untranslated): test</html>",
                ExceptionUtil.explainNestedIOException(new OsmApiException(new IOException("test"))));
    }

    /**
     * Test of {@link ExceptionUtil#explainNestedSocketException} method.
     */
    @Test
    public void testExplainNestedSocketException() {
        assertEquals("<html>Failed to open a connection to the remote server<br>'"+baseUrl+"'.<br>"+
                "Please check your internet connection.</html>",
                ExceptionUtil.explainNestedSocketException(new OsmApiException("")));
    }

    /**
     * Test of {@link ExceptionUtil#explainNestedUnknownHostException} method.
     */
    @Test
    public void testExplainNestedUnknownHostException() {
        assertEquals("<html>Failed to open a connection to the remote server<br>'"+baseUrl+"'.<br>"+
                "Host name '"+host+"' could not be resolved. <br>"+
                "Please check the API URL in your preferences and your internet connection.</html>",
                ExceptionUtil.explainNestedUnknownHostException(new OsmApiException("")));
    }

    /**
     * Test of {@link ExceptionUtil#explainNotFound} method.
     */
    @Test
    public void testExplainNotFound() {
        assertEquals("<html>The OSM server '"+baseUrl+"' does not know about an object<br>"+
                "you tried to read, update, or delete. Either the respective object<br>"+
                "does not exist on the server or you are using an invalid URL to access<br>"+
                "it. Please carefully check the server's address '"+baseUrl+"' for typos.</html>",
                ExceptionUtil.explainNotFound(new OsmApiException("")));
    }

    /**
     * Test of {@link ExceptionUtil#explainOfflineAccessException} method.
     */
    @Test
    public void testExplainOfflineAccessException() {
        assertEquals("<html>Failed to download data.<br><br>Details: null</html>",
                ExceptionUtil.explainOfflineAccessException(new OsmApiException("")));
        assertEquals("<html>Failed to download data.<br><br>Details: test</html>",
                ExceptionUtil.explainOfflineAccessException(new OsmApiException(new OfflineAccessException("test"))));
    }

    /**
     * Test of {@link ExceptionUtil#explainOsmApiInitializationException} method.
     */
    @Test
    public void testExplainOsmApiInitializationException() {
        assertEquals("<html>Failed to initialize communication with the OSM server "+serverUrl+".<br>"+
                "Check the server URL in your preferences and your internet connection.</html>",
                ExceptionUtil.explainOsmApiInitializationException(new OsmApiInitializationException("")));
    }

    /**
     * Test of {@link ExceptionUtil#explainOsmTransferException} method.
     */
    @Test
    public void testExplainOsmTransferException() {
        assertEquals("<html>Failed to open a connection to the remote server<br>'"+baseUrl+"'<br>"+
                "for security reasons. This is most likely because you are running<br>"+
                "in an applet and because you did not load your applet from '"+host+"'.</html>",
                ExceptionUtil.explainOsmTransferException(new OsmApiException(new SecurityException("test"))));

        assertEquals("<html>Failed to open a connection to the remote server<br>'"+baseUrl+"'.<br>"+
                "Please check your internet connection.</html>",
                ExceptionUtil.explainOsmTransferException(new OsmApiException(new SocketException("test"))));

        assertEquals("<html>Failed to open a connection to the remote server<br>'"+baseUrl+"'.<br>"+
                "Host name '"+host+"' could not be resolved. <br>"+
                "Please check the API URL in your preferences and your internet connection.</html>",
                ExceptionUtil.explainOsmTransferException(new OsmApiException(new UnknownHostException("test"))));

        assertEquals("<html>Failed to upload data to or download data from<br>'"+baseUrl+"'<br>"+
                "due to a problem with transferring data.<br>Details (untranslated): test</html>",
                ExceptionUtil.explainOsmTransferException(new OsmApiException(new IOException("test"))));

        assertEquals("<html>Failed to initialize communication with the OSM server "+serverUrl+".<br>"+
                "Check the server URL in your preferences and your internet connection.</html>",
                ExceptionUtil.explainOsmTransferException(new OsmApiInitializationException("")));

        assertEquals("<html>Failed to upload to changeset <strong>0</strong><br>because it has already been closed on ?.",
                ExceptionUtil.explainOsmTransferException(new ChangesetClosedException("")));

        assertEquals("<html>Uploading to the server <strong>failed</strong> because your current<br>"+
                "dataset violates a precondition.<br>The error message is:<br>ResponseCode=412</html>",
                ExceptionUtil.explainOsmTransferException(new OsmApiException(HttpURLConnection.HTTP_PRECON_FAILED, "", "")));

        assertEquals("<html>The server reports that an object is deleted.<br>"+
                "<strong>Uploading failed</strong> if you tried to update or delete this object.<br> "+
                "<strong>Downloading failed</strong> if you tried to download this object.<br><br>"+
                "The error message is:<br>ResponseCode=410</html>",
                ExceptionUtil.explainOsmTransferException(new OsmApiException(HttpURLConnection.HTTP_GONE, "", "")));

        assertEquals("<html>The OSM server<br>'"+baseUrl+"'<br>reported an internal server error.<br>"+
                "This is most likely a temporary problem. Please try again later.</html>",
                ExceptionUtil.explainOsmTransferException(new OsmApiException(HttpURLConnection.HTTP_INTERNAL_ERROR, "", "")));

        assertEquals("<html>The OSM server '"+baseUrl+"' reported a bad request.<br><br>Error message(untranslated): </html>",
                ExceptionUtil.explainOsmTransferException(new OsmApiException(HttpURLConnection.HTTP_BAD_REQUEST, "", "")));

        assertEquals("<html>Communication with the OSM server '"+baseUrl+"'failed. The server replied<br>"+
                "the following error code and the following error message:<br><strong>Error code:<strong> 509<br>"+
                "<strong>Error message (untranslated)</strong>: </html>",
                ExceptionUtil.explainOsmTransferException(new OsmApiException(509, "", "")));

        assertEquals("ResponseCode=0",
                ExceptionUtil.explainOsmTransferException(new OsmApiException("")));
    }

    /**
     * Test of {@link ExceptionUtil#explainPreconditionFailed} method.
     */
    @Test
    public void testExplainPreconditionFailed() {
        int code = HttpURLConnection.HTTP_PRECON_FAILED;
        assertEquals("<html>Uploading to the server <strong>failed</strong> because your current<br>dataset violates a precondition.<br>"+
                "The error message is:<br>ResponseCode=0</html>",
                ExceptionUtil.explainPreconditionFailed(new OsmApiException("")));

        assertEquals("<html>Uploading to the server <strong>failed</strong> because your current<br>dataset violates a precondition.<br>"+
                "The error message is:<br>ResponseCode=412, Error Header=&lt;test&gt;</html>",
                ExceptionUtil.explainPreconditionFailed(new OsmApiException(code, "test", "")));

        assertEquals("<html><strong>Failed</strong> to delete <strong>node 1</strong>. It is still referred to by relation 1.<br>"+
                "Please load the relation, remove the reference to the node, and upload again.</html>",
                ExceptionUtil.explainPreconditionFailed(new OsmApiException(code, "Node 1 is still used by relation 1", "")));

        assertEquals("<html><strong>Failed</strong> to delete <strong>node 1</strong>. It is still referred to by way 1.<br>"+
                "Please load the way, remove the reference to the node, and upload again.</html>",
                ExceptionUtil.explainPreconditionFailed(new OsmApiException(code, "Node 1 is still used by way 1", "")));

        assertEquals("<html><strong>Failed</strong> to delete <strong>relation 1</strong>. It is still referred to by relation 2.<br>"+
                "Please load the relation, remove the reference to the relation, and upload again.</html>",
                ExceptionUtil.explainPreconditionFailed(new OsmApiException(code, "The relation 1 is used in relation 2", "")));

        assertEquals("<html><strong>Failed</strong> to delete <strong>way 1</strong>. It is still referred to by relation 1.<br>"+
                "Please load the relation, remove the reference to the way, and upload again.</html>",
                ExceptionUtil.explainPreconditionFailed(new OsmApiException(code, "Way 1 is still used by relation 1", "")));

        assertEquals("<html><strong>Failed</strong> to delete <strong>way 1</strong>. It is still referred to by nodes [1, 2].<br>"+
                "Please load the nodes, remove the reference to the way, and upload again.</html>",
                ExceptionUtil.explainPreconditionFailed(new OsmApiException(code, "Way 1 requires the nodes with id in 1,2", "")));
    }
}
