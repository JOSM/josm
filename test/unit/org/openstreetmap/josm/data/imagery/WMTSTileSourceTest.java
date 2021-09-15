// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openstreetmap.gui.jmapviewer.FeatureAdapter;
import org.openstreetmap.gui.jmapviewer.TileXY;
import org.openstreetmap.gui.jmapviewer.tilesources.TemplatedTMSTileSource;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.data.imagery.WMTSTileSource.WMTSGetCapabilitiesException;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.BasicWiremock;
import org.openstreetmap.josm.testutils.annotations.Projection;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

/**
 * Unit tests for class {@link WMTSTileSource}.
 */
@BasicPreferences
@BasicWiremock
@Projection
@Timeout(value = 5, unit = TimeUnit.MINUTES)
class WMTSTileSourceTest {
    @BasicWiremock
    WireMockServer tileServer;

    private final ImageryInfo testImageryTMS = new ImageryInfo("test imagery", "http://localhost", "tms", null, null);
    private final ImageryInfo testImageryPSEUDO_MERCATOR = getImagery(TestUtils.getTestDataRoot() + "wmts/getcapabilities-pseudo-mercator.xml");
    private final ImageryInfo testImageryTOPO_PL = getImagery(TestUtils.getTestDataRoot() + "wmts/getcapabilities-TOPO.xml");
    private final ImageryInfo testImageryORTO_PL = getImagery(TestUtils.getTestDataRoot() + "wmts/getcapabilities-ORTO.xml");
    private final ImageryInfo testImageryWIEN = getImagery(TestUtils.getTestDataRoot() + "wmts/getCapabilities-wien.xml");
    private final ImageryInfo testImageryWALLONIE = getImagery(TestUtils.getTestDataRoot() + "wmts/WMTSCapabilities-Wallonie.xml");
    private final ImageryInfo testImageryOntario = getImagery(TestUtils.getTestDataRoot() + "wmts/WMTSCapabilities-Ontario.xml");
    private final ImageryInfo testImageryGeoAdminCh = getImagery(TestUtils.getTestDataRoot() + "wmts/WMTSCapabilities-GeoAdminCh.xml");
    private final ImageryInfo testImagery12168 = getImagery(TestUtils.getTestDataRoot() + "wmts/bug12168-WMTSCapabilities.xml");
    private final ImageryInfo testImageryORT2LT = getImagery(TestUtils.getTestDataRoot() + "wmts/WMTSCapabilities-Lithuania.xml");
    private final ImageryInfo testLotsOfLayers = getImagery(TestUtils.getTestDataRoot() + "wmts/getCapabilities-lots-of-layers.xml");
    private final ImageryInfo testDuplicateTags = getImagery(TestUtils.getTestDataRoot() + "wmts/bug12573-wmts-identifier.xml");
    private final ImageryInfo testMissingStyleIdentifier = getImagery(TestUtils.getTestDataRoot() +
            "wmts/bug12573-wmts-missing-style-identifier.xml");
    private final ImageryInfo testMultipleTileMatrixForLayer = getImagery(TestUtils.getTestDataRoot() +
            "wmts/bug13975-multiple-tile-matrices-for-one-layer-projection.xml");
    private final ImageryInfo testImageryGisKtnGvAt = getImagery(TestUtils.getTestDataRoot() + "wmts/gis.ktn.gv.at.xml");

    private static ImageryInfo getImagery(String path) {
        try {
            ImageryInfo ret = new ImageryInfo(
                    "test",
                    new File(path).toURI().toURL().toString()
                    );
            ret.setImageryType(ImageryType.WMTS);
            return ret;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Test
    void testPseudoMercator() throws IOException, WMTSGetCapabilitiesException {
        ProjectionRegistry.setProjection(Projections.getProjectionByCode("EPSG:3857"));
        WMTSTileSource testSource = new WMTSTileSource(testImageryPSEUDO_MERCATOR);
        testSource.initProjection(ProjectionRegistry.getProjection());

        verifyMercatorTile(testSource, 0, 0, 1);
        verifyMercatorTile(testSource, 0, 0, 2);
        verifyMercatorTile(testSource, 1, 1, 2);
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                verifyMercatorTile(testSource, x, y, 3);
            }
        }
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 4; y++) {
                verifyMercatorTile(testSource, x, y, 4);
            }
        }

        verifyMercatorTile(testSource, 2 << 9 - 1, 2 << 8 - 1, 10);

        assertEquals(1, testSource.getTileXMax(0), "TileXMax");
        assertEquals(1, testSource.getTileYMax(0), "TileYMax");
        assertEquals(2, testSource.getTileXMax(1), "TileXMax");
        assertEquals(2, testSource.getTileYMax(1), "TileYMax");
        assertEquals(4, testSource.getTileXMax(2), "TileXMax");
        assertEquals(4, testSource.getTileYMax(2), "TileYMax");
    }

    @Test
    void testWALLONIE() throws IOException, WMTSGetCapabilitiesException {
        ProjectionRegistry.setProjection(Projections.getProjectionByCode("EPSG:31370"));
        WMTSTileSource testSource = new WMTSTileSource(testImageryWALLONIE);
        testSource.initProjection(ProjectionRegistry.getProjection());

        assertEquals("http://geoservices.wallonie.be/arcgis/rest/services/DONNEES_BASE/FOND_PLAN_ANNOTATIONS_2012_RW_NB/"
                + "MapServer/WMTS/tile/1.0.0/DONNEES_BASE_FOND_PLAN_ANNOTATIONS_2012_RW_NB/default/default028mm/5/1219/1063.png",
                testSource.getTileUrl(5, 1063, 1219));

        // +bounds=2.54,49.51,6.4,51.5
        Bounds wallonieBounds = new Bounds(
                new LatLon(49.485372459967245, 2.840548314430268),
                new LatLon(50.820959517561256, 6.427849693016202)
                );
        verifyBounds(wallonieBounds, testSource, 5, 1063, 1219);
        verifyBounds(wallonieBounds, testSource, 10, 17724, 20324);
    }

    @Test
    @Disabled("disable this test, needs further working") // XXX
    void testWALLONIENoMatrixDimension() throws IOException, WMTSGetCapabilitiesException {
        ProjectionRegistry.setProjection(Projections.getProjectionByCode("EPSG:31370"));
        WMTSTileSource testSource = new WMTSTileSource(getImagery("test/data/wmts/WMTSCapabilities-Wallonie-nomatrixdimension.xml"));
        testSource.initProjection(ProjectionRegistry.getProjection());

        Bounds wallonieBounds = new Bounds(
                new LatLon(49.485372459967245, 2.840548314430268),
                new LatLon(50.820959517561256, 6.427849693016202)
                );

        verifyBounds(wallonieBounds, testSource, 6, 1063, 1219);
        verifyBounds(wallonieBounds, testSource, 11, 17724, 20324);
    }

    private void verifyBounds(Bounds bounds, WMTSTileSource testSource, int z, int x, int y) {
        LatLon ret = CoordinateConversion.coorToLL(testSource.tileXYToLatLon(x, y, z));
        assertTrue(bounds.contains(ret), ret.toDisplayString() + " doesn't lie within: " + bounds);
        int tileXmax = testSource.getTileXMax(z);
        int tileYmax = testSource.getTileYMax(z);
        assertTrue(tileXmax >= x, "tile x: " + x + " is greater than allowed max: " + tileXmax);
        assertTrue(tileYmax >= y, "tile y: " + y + " is greater than allowed max: " + tileYmax);
    }

    @Test
    void testWIEN() throws IOException, WMTSGetCapabilitiesException {
        ProjectionRegistry.setProjection(Projections.getProjectionByCode("EPSG:3857"));
        WMTSTileSource testSource = new WMTSTileSource(testImageryWIEN);
        testSource.initProjection(ProjectionRegistry.getProjection());
        int zoomOffset = 10;

        verifyMercatorTile(testSource, 0, 0, 1, zoomOffset);
        verifyMercatorTile(testSource, 1105, 709, 2, zoomOffset);
        verifyMercatorTile(testSource, 1, 1, 1, zoomOffset);
        verifyMercatorTile(testSource, 2, 2, 1, zoomOffset);
        verifyMercatorTile(testSource, 0, 0, 2, zoomOffset);
        verifyMercatorTile(testSource, 1, 1, 2, zoomOffset);

        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                verifyMercatorTile(testSource, x, y, 3, zoomOffset);
            }
        }
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 4; y++) {
                verifyMercatorTile(testSource, x, y, 4, zoomOffset);
            }
        }

        verifyMercatorTile(testSource, 2 << 9 - 1, 2 << 8 - 1, 2, zoomOffset);

        verifyMercatorMax(testSource, 1, zoomOffset);
        verifyMercatorMax(testSource, 2, zoomOffset);
        verifyMercatorMax(testSource, 3, zoomOffset);
    }

    private void verifyMercatorMax(WMTSTileSource testSource, int zoom, int zoomOffset) {
        TemplatedTMSTileSource verifier = new TemplatedTMSTileSource(testImageryTMS);
        int result = testSource.getTileXMax(zoom);
        int expected = verifier.getTileXMax(zoom + zoomOffset);
        assertTrue(Math.abs(result - expected) < 5, "TileXMax expected: " + expected + " got: " + result);
        result = testSource.getTileYMax(zoom);
        expected = verifier.getTileYMax(zoom + zoomOffset);
        assertTrue(Math.abs(result - expected) < 5, "TileYMax expected: " + expected + " got: " + result);
    }

    @Test
    void testGeoportalTOPOPL() throws IOException, WMTSGetCapabilitiesException {
        ProjectionRegistry.setProjection(Projections.getProjectionByCode("EPSG:4326"));
        WMTSTileSource testSource = new WMTSTileSource(testImageryTOPO_PL);
        testSource.initProjection(ProjectionRegistry.getProjection());
        verifyTile(new LatLon(56, 12), testSource, 0, 0, 1);
        verifyTile(new LatLon(56, 12), testSource, 0, 0, 2);
        verifyTile(new LatLon(51.13231917844218, 16.867680821557823), testSource, 1, 1, 1);

        assertEquals(2, testSource.getTileXMax(0), "TileXMax");
        assertEquals(1, testSource.getTileYMax(0), "TileYMax");
        assertEquals(3, testSource.getTileXMax(1), "TileXMax");
        assertEquals(2, testSource.getTileYMax(1), "TileYMax");
        assertEquals(6, testSource.getTileXMax(2), "TileXMax");
        assertEquals(4, testSource.getTileYMax(2), "TileYMax");
        assertEquals(
                "http://mapy.geoportal.gov.pl/wss/service/WMTS/guest/wmts/TOPO?SERVICE=WMTS&REQUEST=GetTile&"
                + "VERSION=1.0.0&LAYER=MAPA TOPOGRAFICZNA&STYLE=default&FORMAT=image/jpeg&tileMatrixSet=EPSG:4326&"
                + "tileMatrix=EPSG:4326:0&tileRow=1&tileCol=1",
                testSource.getTileUrl(0, 1, 1));
    }

    @Test
    void testGeoportalORTOPL4326() throws IOException, WMTSGetCapabilitiesException {
        ProjectionRegistry.setProjection(Projections.getProjectionByCode("EPSG:4326"));
        WMTSTileSource testSource = new WMTSTileSource(testImageryORTO_PL);
        testSource.initProjection(ProjectionRegistry.getProjection());
        verifyTile(new LatLon(53.60205873528009, 19.552206794646956), testSource, 12412, 3941, 13);
        verifyTile(new LatLon(49.79005619189761, 22.778262259134397), testSource, 17714, 10206, 13);
    }

    @Test
    void testGeoportalORTOPL2180() throws IOException, WMTSGetCapabilitiesException {
        ProjectionRegistry.setProjection(Projections.getProjectionByCode("EPSG:2180"));
        WMTSTileSource testSource = new WMTSTileSource(testImageryORTO_PL);
        testSource.initProjection(ProjectionRegistry.getProjection());

        verifyTile(new LatLon(53.59940948387726, 19.560544913270064), testSource, 6453, 3140, 13);
        verifyTile(new LatLon(49.782984840526055, 22.790064966993445), testSource, 9932, 9305, 13);
    }

    @Test
    void testTicket12168() throws IOException, WMTSGetCapabilitiesException {
        ProjectionRegistry.setProjection(Projections.getProjectionByCode("EPSG:3857"));
        WMTSTileSource testSource = new WMTSTileSource(testImagery12168);
        testSource.initProjection(ProjectionRegistry.getProjection());
        assertEquals(
                "http://www.ngi.be/cartoweb/1.0.0/topo/default/3857/7/1/1.png",
                testSource.getTileUrl(0, 1, 1));
    }

    @Test
    void testProjectionWithENUAxis() throws IOException, WMTSGetCapabilitiesException {
        ProjectionRegistry.setProjection(Projections.getProjectionByCode("EPSG:3346"));
        WMTSTileSource testSource = new WMTSTileSource(testImageryORT2LT);
        testSource.initProjection(ProjectionRegistry.getProjection());
        TileXY tileXY0 = testSource.latLonToTileXY(55.31083718860799, 22.172052608196587, 0);
        double delta = 1e-10;
        assertEquals(27.09619727782481, tileXY0.getX(), delta);
        assertEquals(19.03524443532604, tileXY0.getY(), delta);
        TileXY tileXY2 = testSource.latLonToTileXY(55.31083718860799, 22.172052608196587, 2);
        assertEquals(81.28859183347444, tileXY2.getX(), delta);
        assertEquals(57.10573330597811, tileXY2.getY(), delta);
    }

    @Test
    void testTwoTileSetsForOneProjection() throws Exception {
        ProjectionRegistry.setProjection(Projections.getProjectionByCode("EPSG:3857"));
        ImageryInfo ontario = getImagery(TestUtils.getTestDataRoot() + "wmts/WMTSCapabilities-Ontario.xml");
        ontario.setDefaultLayers(Arrays.asList(new DefaultLayer[] {
                new DefaultLayer(ImageryType.WMTS, "Basemap_Imagery_2014", null, "default028mm")
        }));
        WMTSTileSource testSource = new WMTSTileSource(ontario);
        testSource.initProjection(ProjectionRegistry.getProjection());
        assertEquals(
                "http://maps.ottawa.ca/arcgis/rest/services/Basemap_Imagery_2014/MapServer/WMTS/tile/1.0.0/Basemap_Imagery_2014/default/"
                + "default028mm/4/2932/2371.jpg",
                testSource.getTileUrl(4, 2371, 2932));
        verifyTile(new LatLon(45.4601306, -75.7617187), testSource, 2372, 2932, 4);
        verifyTile(new LatLon(45.4602510, -75.7617187), testSource, 607232, 750591, 12);
    }

    @Test
    void testTwoTileSetsForOneProjectionSecondLayer() throws Exception {
        ProjectionRegistry.setProjection(Projections.getProjectionByCode("EPSG:3857"));
        ImageryInfo ontario = getImagery(TestUtils.getTestDataRoot() + "wmts/WMTSCapabilities-Ontario.xml");
        ontario.setDefaultLayers(Arrays.asList(new DefaultLayer[] {
                new DefaultLayer(ImageryType.WMTS, "Basemap_Imagery_2014", null, "GoogleMapsCompatible")
        }));
        WMTSTileSource testSource = new WMTSTileSource(ontario);
        testSource.initProjection(ProjectionRegistry.getProjection());
        assertEquals(
                "http://maps.ottawa.ca/arcgis/rest/services/Basemap_Imagery_2014/MapServer/WMTS/tile/1.0.0/Basemap_Imagery_2014/default/"
                + "GoogleMapsCompatible/4/2932/2371.jpg",
                testSource.getTileUrl(4, 2371, 2932));
        verifyMercatorTile(testSource, 74, 91, 8);
        verifyMercatorTile(testSource, 37952, 46912, 17);
    }

    @Test
    void testManyLayersScrollbars() throws Exception {
        ProjectionRegistry.setProjection(Projections.getProjectionByCode("EPSG:3857"));
        WMTSTileSource testSource = new WMTSTileSource(testLotsOfLayers);
        testSource.initProjection(ProjectionRegistry.getProjection());
    }

    @Test
    void testParserForDuplicateTags() throws Exception {
        ProjectionRegistry.setProjection(Projections.getProjectionByCode("EPSG:3857"));
        WMTSTileSource testSource = new WMTSTileSource(testDuplicateTags);
        testSource.initProjection(ProjectionRegistry.getProjection());
        assertEquals(
                "http://tile.informatievlaanderen.be/ws/raadpleegdiensten/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=grb_bsk&"
                        + "STYLE=&FORMAT=image/png&tileMatrixSet=GoogleMapsVL&tileMatrix=1&tileRow=1&tileCol=1",
                testSource.getTileUrl(1, 1, 1)
                );
    }

    @Test
    void testParserForMissingStyleIdentifier() throws Exception {
        ProjectionRegistry.setProjection(Projections.getProjectionByCode("EPSG:3857"));
        WMTSTileSource testSource = new WMTSTileSource(testMissingStyleIdentifier);
        testSource.initProjection(ProjectionRegistry.getProjection());
    }

    @Test
    void testForMultipleTileMatricesForOneLayerProjection() throws Exception {
        ProjectionRegistry.setProjection(Projections.getProjectionByCode("EPSG:3857"));
        ImageryInfo copy = new ImageryInfo(testMultipleTileMatrixForLayer);
        List<DefaultLayer> defaultLayers = new ArrayList<>(1);
        defaultLayers.add(new DefaultLayer(ImageryType.WMTS, "Mashhad_BaseMap_1", null, "default028mm"));
        copy.setDefaultLayers(defaultLayers);
        WMTSTileSource testSource = new WMTSTileSource(copy);
        testSource.initProjection(ProjectionRegistry.getProjection());
        assertEquals(
                "http://188.253.0.155:6080/arcgis/rest/services/Mashhad_BaseMap_1/MapServer/WMTS/tile/1.0.0/Mashhad_BaseMap_1"
                        + "/default/default028mm/1/3/2",
                testSource.getTileUrl(1, 2, 3)
                );
    }

    /**
     * Test WMTS dimension.
     * @throws IOException if any I/O error occurs
     * @throws WMTSGetCapabilitiesException if any error occurs
     */
    @Test
    void testDimension() throws IOException, WMTSGetCapabilitiesException {
        ProjectionRegistry.setProjection(Projections.getProjectionByCode("EPSG:21781"));
        ImageryInfo info = new ImageryInfo(testImageryGeoAdminCh);
        List<DefaultLayer> defaultLayers = new ArrayList<>(1);
        defaultLayers.add(new DefaultLayer(ImageryType.WMTS, "ch.are.agglomerationen_isolierte_staedte", null, "21781_26"));
        info.setDefaultLayers(defaultLayers);
        WMTSTileSource testSource = new WMTSTileSource(info);
        testSource.initProjection(ProjectionRegistry.getProjection());
        assertEquals(
                "http://wmts.geo.admin.ch/1.0.0/ch.are.agglomerationen_isolierte_staedte/default/20140101/21781/1/3/2.png",
                testSource.getTileUrl(1, 2, 3)
                );
    }

    @Test
    void testDefaultLayer() throws Exception {
        // https://gibs.earthdata.nasa.gov/wmts/epsg3857/best/1.0.0/WMTSCapabilities.xml
        // do not use withFileBody as it needs different directory layout :(

        tileServer.stubFor(
                WireMock.get("/getcapabilities.xml")
                .willReturn(
                        WireMock.aResponse()
                        .withBody(Files.readAllBytes(
                                Paths.get(TestUtils.getTestDataRoot() + "wmts/getCapabilities-lots-of-layers.xml"))
                                )
                        )
                );

        tileServer.stubFor(
                WireMock.get("//maps")
                .willReturn(
                        WireMock.aResponse().withBody(
                "<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<imagery xmlns=\"http://josm.openstreetmap.de/maps-1.0\">\n" +
                "<entry>\n" +
                "<name>Landsat</name>\n" +
                "<id>landsat</id>\n" +
                "<type>wmts</type>\n" +
                "<url><![CDATA[" + tileServer.url("/getcapabilities.xml") + "]]></url>\n" +
                "<default-layers>" +
                "<layer name=\"GEOGRAPHICALGRIDSYSTEMS.MAPS\" />" +
                "</default-layers>" +
                "</entry>\n" +
                "</imagery>"
                )));

        Config.getPref().putList("imagery.layers.sites", Arrays.asList(tileServer.url("//maps")));
        ImageryLayerInfo.instance.loadDefaults(true, null, false);

        assertEquals(1, ImageryLayerInfo.instance.getDefaultLayers().size());
        ImageryInfo wmtsImageryInfo = ImageryLayerInfo.instance.getDefaultLayers().get(0);
        assertEquals(1, wmtsImageryInfo.getDefaultLayers().size());
        assertEquals("GEOGRAPHICALGRIDSYSTEMS.MAPS", wmtsImageryInfo.getDefaultLayers().get(0).getLayerName());
        WMTSTileSource tileSource = new WMTSTileSource(wmtsImageryInfo);
        tileSource.initProjection(Projections.getProjectionByCode("EPSG:3857"));
        assertEquals("http://wxs.ign.fr/61fs25ymczag0c67naqvvmap/geoportail/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&"
                + "LAYER=GEOGRAPHICALGRIDSYSTEMS.MAPS"
                + "&STYLE=normal&FORMAT=image/jpeg&tileMatrixSet=PM&tileMatrix=1&tileRow=1&tileCol=1", tileSource.getTileUrl(1, 1, 1));

    }

    private void verifyTile(LatLon expected, WMTSTileSource source, int x, int y, int z) {
        LatLon ll = CoordinateConversion.coorToLL(source.tileXYToLatLon(x, y, z));
        assertEquals(expected.lat(), ll.lat(), 1e-05, "Latitude");
        assertEquals(expected.lon(), ll.lon(), 1e-05, "Longitude");
    }

    private void verifyMercatorTile(WMTSTileSource testSource, int x, int y, int z) {
        verifyMercatorTile(testSource, x, y, z, 0);
    }

    private void verifyMercatorTile(WMTSTileSource testSource, int x, int y, int z, int zoomOffset) {
        TemplatedTMSTileSource verifier = new TemplatedTMSTileSource(testImageryTMS);
        LatLon result = CoordinateConversion.coorToLL(testSource.tileXYToLatLon(x, y, z));
        LatLon expected = CoordinateConversion.coorToLL(verifier.tileXYToLatLon(x, y, z + zoomOffset));
        assertEquals(0.0, LatLon.normalizeLon(expected.lon() - result.lon()), 1e-04, "Longitude");
        assertEquals(expected.lat(), result.lat(), 1e-04, "Latitude");
    }

    @Test
    void testGisKtnGvAt() throws IOException, WMTSGetCapabilitiesException {
        ProjectionRegistry.setProjection(Projections.getProjectionByCode("EPSG:31258"));
        final WMTSTileSource source = new WMTSTileSource(testImageryGisKtnGvAt);
        source.initProjection(ProjectionRegistry.getProjection());
        final TileXY tile = source.latLonToTileXY(46.6103, 13.8558, 11);
        assertEquals("https://gis.ktn.gv.at/arcgis/rest/services/tilecache/Ortho_2013_2015" +
                        "/MapServer/WMTS/tile/1.0.0/tilecache_Ortho_2013_2015/default/default028mm/11/6299/7373.jpg",
                source.getTileUrl(11, tile.getXIndex(), tile.getYIndex()));
    }

    @Test
    void testApiKeyValid() {
        assumeFalse(testImagery12168 == null);
        try {
            ProjectionRegistry.setProjection(Projections.getProjectionByCode("EPSG:4326"));
            FeatureAdapter.registerApiKeyAdapter(id -> TestUtils.getTestDataRoot());
            ImageryInfo testImageryWMTS = new ImageryInfo(testImagery12168);
            testImageryWMTS.setUrl(testImageryWMTS.getUrl().replace(TestUtils.getTestDataRoot(), "{apikey}"));
            assertTrue(testImageryWMTS.getUrl().contains("{apikey}"), testImageryWMTS.getUrl());
            testImageryWMTS.setId("WMTSTileSourceTest#testApiKeyValid");
            WMTSTileSource ts = assertDoesNotThrow(() -> new WMTSTileSource(testImageryWMTS, ProjectionRegistry.getProjection()));
            assertEquals("http://www.ngi.be/cartoweb/1.0.0/topo/default/3812/1/3/2.png",
                    ts.getTileUrl(1, 2, 3));
        } finally {
            FeatureAdapter.registerApiKeyAdapter(new FeatureAdapter.DefaultApiKeyAdapter());
        }
    }

    @Test
    void testApiKeyInvalid() {
        assumeFalse(testImagery12168 == null);
        try {
            ProjectionRegistry.setProjection(Projections.getProjectionByCode("EPSG:4326"));
            FeatureAdapter.registerApiKeyAdapter(id -> null);
            ImageryInfo testImageryWMTS = new ImageryInfo(testImagery12168);
            testImageryWMTS.setUrl(testImageryWMTS.getUrl().replace(TestUtils.getTestDataRoot(), "{apikey}"));
            assertTrue(testImageryWMTS.getUrl().contains("{apikey}"), testImageryWMTS.getUrl());
            testImageryWMTS.setId("WMTSTileSourceTest#testApiKeyInvalid");
            org.openstreetmap.josm.data.projection.Projection projection = Projections.getProjectionByCode("EPSG:4326");
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                            () -> new WMTSTileSource(testImageryWMTS, projection));
            assertEquals(tr("Could not retrieve API key for imagery with id={0}. Cannot add layer.\n" +
                            "API key for imagery with id=WMTSTileSourceTest#testApiKeyInvalid may not be available.",
                            testImageryWMTS.getId()),
                    exception.getMessage());
        } finally {
            FeatureAdapter.registerApiKeyAdapter(new FeatureAdapter.DefaultApiKeyAdapter());
        }
    }
}
