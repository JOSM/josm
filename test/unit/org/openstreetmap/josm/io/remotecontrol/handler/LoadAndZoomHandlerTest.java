// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler.RequestHandlerBadRequestException;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.BasicWiremock;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.testutils.annotations.ThreadSync;

/**
 * Unit tests of {@link LoadAndZoomHandler} class.
 */
@BasicPreferences
@BasicWiremock
@ExtendWith(BasicWiremock.OsmApiExtension.class)
class LoadAndZoomHandlerTest {
    private static final String DEFAULT_BBOX_URL = "https://localhost/load_and_zoom?left=0&bottom=0&right=0.001&top=0.001";
    private static final String DEFAULT_BBOX_URL_2 = "https://localhost/load_and_zoom?left=0.00025&bottom=0.00025&right=0.00075&top=0.00125";
    private static LoadAndZoomHandler newHandler(String url) throws RequestHandlerBadRequestException {
        LoadAndZoomHandler req = new LoadAndZoomHandler();
        req.myCommand = LoadAndZoomHandler.command;
        if (url != null)
            req.setUrl(url);
        return req;
    }

    private static void syncThreads() {
        // There are calls to the worker thread and EDT
        new ThreadSync.ThreadSyncExtension().threadSync();
    }

    @BeforeEach
    void setup(WireMockRuntimeInfo wireMockRuntimeInfo) {
        String common = "visible=\"true\" version=\"1\" changeset=\"1\" timestamp=\"2000-01-01T00:00:00Z\" user=\"tsmock\" uid=\"1\"";
        wireMockRuntimeInfo.getWireMock().register(WireMock.get("/api/0.6/map?bbox=0.0,0.0,0.001,0.001")
                .willReturn(WireMock.aResponse().withBody("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<osm version=\"0.6\" generator=\"hand\" copyright=\"JOSM\" attribution=\"\" license=\"\">\n" +
                        " <bounds minlat=\"0\" minlon=\"0\" maxlat=\"0.001\" maxlon=\"0.001\"/>\n" +
                        " <node id=\"1\" " + common + " lat=\"0\" lon=\"0\"/>\n" +
                        " <node id=\"2\" " + common + " lat=\"0.0001\" lon=\"0.0001\"/>\n" +
                        " <node id=\"3\" " + common + " lat=\"0.0002\" lon=\"0.0002\"/>\n" +
                        "</osm>")));
        // The scientific notation is ok server-side.
        wireMockRuntimeInfo.getWireMock().register(WireMock.get("/api/0.6/map?bbox=2.5E-4,0.001,7.5E-4,0.00125")
                .willReturn(WireMock.aResponse().withBody("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<osm version=\"0.6\" generator=\"hand\" copyright=\"JOSM\" attribution=\"\" license=\"\">\n" +
                        " <bounds minlat=\"0.001\" minlon=\"0.00025\" maxlat=\"0.00125\" maxlon=\"0.00075\"/>\n" +
                        " <node id=\"4\" " + common + " lat=\"0.00111\" lon=\"0.00026\"/>\n" +
                        " <node id=\"5\" " + common + " lat=\"0.0011\" lon=\"0.00025\"/>\n" +
                        " <node id=\"6\" " + common + " lat=\"0.0012\" lon=\"0.000251\"/>\n" +
                        "</osm>")));
    }

    /**
     * Unit test for bad request - no param.
     */
    @Test
    void testBadRequestNoParam() {
        final LoadAndZoomHandler handler = assertDoesNotThrow(() -> newHandler(null));
        Exception e = assertThrows(RequestHandlerBadRequestException.class, handler::handle);
        assertEquals("NumberFormatException (empty String)", e.getMessage());
    }

    /**
     * Unit test for bad request - invalid URL.
     */
    @Test
    void testBadRequestInvalidUrl() {
        final LoadAndZoomHandler handler = assertDoesNotThrow(() -> newHandler("invalid_url"));
        Exception e = assertThrows(RequestHandlerBadRequestException.class, handler::handle);
        assertEquals("The following keys are mandatory, but have not been provided: bottom, top, left, right", e.getMessage());
    }

    /**
     * Unit test for bad request - incomplete URL.
     */
    @Test
    void testBadRequestIncompleteUrl() {
        final LoadAndZoomHandler handler = assertDoesNotThrow(() -> newHandler("https://localhost"));
        Exception e = assertThrows(RequestHandlerBadRequestException.class, handler::handle);
        assertEquals("The following keys are mandatory, but have not been provided: bottom, top, left, right", e.getMessage());
    }

    /**
     * Ensure that a download is called and completed
     * @param wireMockRuntimeInfo The wiremock information
     * @throws RequestHandlerBadRequestException If there is an issue with the handler
     */
    @Test
    void testDownload(WireMockRuntimeInfo wireMockRuntimeInfo) throws RequestHandlerBadRequestException {
        LoadAndZoomHandler handler = newHandler(DEFAULT_BBOX_URL);
        assertDoesNotThrow(handler::handle);
        syncThreads();
        final DataSet ds = MainApplication.getLayerManager().getEditDataSet();
        assertNotNull(ds);
        assertAll(() -> assertNotNull(ds.getPrimitiveById(1, OsmPrimitiveType.NODE)),
                () -> assertNotNull(ds.getPrimitiveById(2, OsmPrimitiveType.NODE)),
                () -> assertNotNull(ds.getPrimitiveById(3, OsmPrimitiveType.NODE)),
                () -> assertNull(ds.getPrimitiveById(4, OsmPrimitiveType.NODE)),
                () -> assertTrue(ds.selectionEmpty()));
        wireMockRuntimeInfo.getWireMock().verifyThat(1,
                RequestPatternBuilder.newRequestPattern().withUrl("/api/0.6/map?bbox=0.0,0.0,0.001,0.001"));
    }

    /**
     * Ensure that an area isn't downloaded twice
     * @param wireMockRuntimeInfo The wiremock information
     * @throws RequestHandlerBadRequestException If there is an issue with the handler
     */
    @Test
    void testDoubleDownload(WireMockRuntimeInfo wireMockRuntimeInfo) throws RequestHandlerBadRequestException {
        testDownload(wireMockRuntimeInfo);
        testDownload(wireMockRuntimeInfo);
        // testDownload checks that the URL has been called once. Since it doesn't reset anything, we don't need
        // a specific test here.
    }

    /**
     * Ensure that an overlapping area is trimmed before download
     * @param wireMockRuntimeInfo The wiremock information
     * @throws RequestHandlerBadRequestException If there is an issue with the handler
     */
    @Test
    void testOverlappingArea(WireMockRuntimeInfo wireMockRuntimeInfo) throws RequestHandlerBadRequestException {
        LoadAndZoomHandler handler = newHandler(DEFAULT_BBOX_URL);
        assertDoesNotThrow(handler::handle);
        syncThreads();

        handler = newHandler(DEFAULT_BBOX_URL_2);
        assertDoesNotThrow(handler::handle);
        syncThreads();
        wireMockRuntimeInfo.getWireMock().verifyThat(1, RequestPatternBuilder.newRequestPattern()
                .withUrl("/api/0.6/map?bbox=2.5E-4,0.001,7.5E-4,0.00125"));
        final DataSet ds = MainApplication.getLayerManager().getEditDataSet();
        assertNotNull(ds);
        assertAll(() -> assertNotNull(ds.getPrimitiveById(1, OsmPrimitiveType.NODE)),
                () -> assertNotNull(ds.getPrimitiveById(2, OsmPrimitiveType.NODE)),
                () -> assertNotNull(ds.getPrimitiveById(3, OsmPrimitiveType.NODE)),
                () -> assertNotNull(ds.getPrimitiveById(4, OsmPrimitiveType.NODE)));
    }

    /**
     * Check search and zoom functionality
     * @throws RequestHandlerBadRequestException If there is an issue with the handler
     */
    @Main
    @Projection
    @Test
    void testSearchAndZoom() throws RequestHandlerBadRequestException {
        final LoadAndZoomHandler handler = newHandler(DEFAULT_BBOX_URL + "&search=id:1");
        assertDoesNotThrow(handler::handle);
        syncThreads();
        final DataSet ds = MainApplication.getLayerManager().getEditDataSet();
        Collection<OsmPrimitive> selected = ds.getSelected();
        assertEquals(1, selected.size());
        assertTrue(selected.contains(ds.getPrimitiveById(1, OsmPrimitiveType.NODE)));
        assertTrue(ds.searchNodes(MainApplication.getMap().mapView.getRealBounds().toBBox())
                .contains((Node) ds.getPrimitiveById(1, OsmPrimitiveType.NODE)));
    }

    /**
     * Check select and zoom functionality
     * @throws RequestHandlerBadRequestException If there is an issue with the handler
     */
    @Main
    @Projection
    @Test
    void testSelectAndZoom() throws RequestHandlerBadRequestException {
        final LoadAndZoomHandler handler = newHandler(DEFAULT_BBOX_URL + "&select=n1");
        assertDoesNotThrow(handler::handle);
        syncThreads();
        final DataSet ds = MainApplication.getLayerManager().getEditDataSet();
        Collection<OsmPrimitive> selected = ds.getSelected();
        assertEquals(1, selected.size());
        assertTrue(selected.contains(ds.getPrimitiveById(1, OsmPrimitiveType.NODE)));
        assertTrue(ds.searchNodes(MainApplication.getMap().mapView.getRealBounds().toBBox())
                .contains((Node) ds.getPrimitiveById(1, OsmPrimitiveType.NODE)));
    }

    /**
     * Check changeset tag functionality
     * @throws RequestHandlerBadRequestException If there is an issue with the handler
     */
    @Test
    void testChangesetTags() throws RequestHandlerBadRequestException {
        final String comment = "Add buildings, roads, and other random stuff";
        final String source = "This isn't Bing";
        final String hashtag = "#test-hashcodes";
        final String customTags = "custom=tag|is=here";
        final LoadAndZoomHandler handler = newHandler(DEFAULT_BBOX_URL
                + "&changeset_comment=" + URLEncoder.encode(comment, StandardCharsets.UTF_8)
                + "&changeset_source=" + URLEncoder.encode(source, StandardCharsets.UTF_8)
                + "&changeset_hashtags=" + URLEncoder.encode(hashtag, StandardCharsets.UTF_8)
                + "&changeset_tags=" + URLEncoder.encode(customTags, StandardCharsets.UTF_8));
        assertDoesNotThrow(handler::handle);
        syncThreads();
        final DataSet ds = MainApplication.getLayerManager().getEditDataSet();
        assertEquals(comment, ds.getChangeSetTags().get("comment"));
        assertEquals(source, ds.getChangeSetTags().get("source"));
        assertEquals(hashtag, ds.getChangeSetTags().get("hashtags"));
        assertEquals("tag", ds.getChangeSetTags().get("custom"));
        assertEquals("here", ds.getChangeSetTags().get("is"));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/23821">#23821</a>
     * @throws RequestHandlerBadRequestException If there is an issue with the handler
     */
    @Test
    void testNonRegression23821() throws RequestHandlerBadRequestException {
        final AtomicBoolean block = new AtomicBoolean(false);
        final Runnable runnable = () -> {
            synchronized (block) {
                while (!block.get()) {
                    try {
                        block.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();;
                        throw new RuntimeException(e);
                    }
                }
            }
        };
        final DataSet wrongDataset = new DataSet();
        MainApplication.getLayerManager().addLayer(new OsmDataLayer(wrongDataset,
                "LoadAndZoomHandlerTest#testNonRegression23821", null));
        ForkJoinTask<?> task1;
        ForkJoinTask<?> task2;
        try {
            GuiHelper.runInEDT(runnable);
            MainApplication.worker.submit(runnable);
            // The processor makes a new handler for each request
            // It is single-threaded, so blocking on one handler would fix the problem with the other handler.
            // But we might as well work on multi-threading, since it is easier to test. :)
            final LoadAndZoomHandler handler1 = newHandler(DEFAULT_BBOX_URL + "&new_layer=true&layer_name=OSMData");
            final LoadAndZoomHandler handler2 = newHandler(DEFAULT_BBOX_URL_2 + "&new_layer=false&layer_name=OSMData");
            // Use a separate threads to avoid blocking on this thread
            final ForkJoinPool pool = ForkJoinPool.commonPool();
            task1 = pool.submit(() -> assertDoesNotThrow(handler1::handle));

            // Make certain there is enough time for the first task to block
            Awaitility.await().until(() -> true);
            task2 = pool.submit(() -> assertDoesNotThrow(handler2::handle));
        } finally {
            // Unblock UI/worker threads
            synchronized (block) {
                block.set(true);
                block.notifyAll();
            }
        }

        task1.join();
        task2.join();

        syncThreads();
        assertEquals(2, MainApplication.getLayerManager().getLayers().size());
        final DataSet ds = MainApplication.getLayerManager().getEditDataSet();
        assertNotEquals(wrongDataset, ds);
        assertTrue(wrongDataset.isEmpty());
        assertEquals(6, ds.allPrimitives().size());
    }
}
