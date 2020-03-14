// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.commons.jcs.access.CacheAccess;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;
import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.FeatureAdapter;
import org.openstreetmap.gui.jmapviewer.TileXY;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.tilesources.AbstractTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.BingAerialTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.ScanexTileSource;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.AddImageryLayerAction;
import org.openstreetmap.josm.actions.AddImageryLayerAction.LayerSelection;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.imagery.CoordinateConversion;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryBounds;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.data.imagery.ImageryLayerInfo;
import org.openstreetmap.josm.data.imagery.JosmTemplatedTMSTileSource;
import org.openstreetmap.josm.data.imagery.LayerDetails;
import org.openstreetmap.josm.data.imagery.Shape;
import org.openstreetmap.josm.data.imagery.TMSCachedTileLoaderJob;
import org.openstreetmap.josm.data.imagery.TemplatedWMSTileSource;
import org.openstreetmap.josm.data.imagery.TileJobOptions;
import org.openstreetmap.josm.data.imagery.WMTSTileSource;
import org.openstreetmap.josm.data.imagery.WMTSTileSource.WMTSGetCapabilitiesException;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.io.imagery.ApiKeyProvider;
import org.openstreetmap.josm.io.imagery.WMSImagery.WMSGetCapabilitiesException;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.ParallelParameterized;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.HttpClient.Response;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Integration tests of {@link ImageryPreference} class.
 */
@RunWith(ParallelParameterized.class)
public class ImageryPreferenceTestIT {

    private static final String ERROR_SEP = " -> ";
    private static final LatLon GREENWICH = new LatLon(51.47810, -0.00170);
    private static final int DEFAULT_ZOOM = 12;

    /**
     * Setup rule
     */
    @ClassRule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public static JOSMTestRules test = new JOSMTestRules().https().i18n().preferences().projection().projectionNadGrids()
                                                   .timeout((int) TimeUnit.MINUTES.toMillis(40)).parameters();

    /** Entry to test */
    private final ImageryInfo info;
    private final Map<String, Map<ImageryInfo, List<String>>> errors = Collections.synchronizedMap(new TreeMap<>());
    private final Map<String, Map<ImageryInfo, List<String>>> ignoredErrors = Collections.synchronizedMap(new TreeMap<>());
    private static final Map<String, byte[]> workingURLs = Collections.synchronizedMap(new TreeMap<>());

    private static TMSCachedTileLoaderJob helper;
    private static final List<String> errorsToIgnore = new ArrayList<>();
    private static final List<String> notIgnoredErrors = new ArrayList<>();

    /**
     * Setup test
     * @throws IOException in case of I/O error
     */
    @BeforeClass
    public static void beforeClass() throws IOException {
        FeatureAdapter.registerApiKeyAdapter(ApiKeyProvider::retrieveApiKey);
        helper = new TMSCachedTileLoaderJob(null, null, new CacheAccess<>(null), new TileJobOptions(0, 0, null, 0), null);
        errorsToIgnore.addAll(TestUtils.getIgnoredErrorMessages(ImageryPreferenceTestIT.class));
        notIgnoredErrors.addAll(errorsToIgnore);
    }

    /**
     * Cleanup test
     */
    @AfterClass
    public static void afterClass() {
        for (String e : notIgnoredErrors) {
            Logging.warn("Ignore line unused: " + e);
        }
    }

    /**
     * Returns list of imagery entries to test.
     * @return list of imagery entries to test
     */
    @Parameters(name = "{0}")
    public static List<Object[]> data() {
        ImageryLayerInfo.instance.load(false);
        return ImageryLayerInfo.instance.getDefaultLayers()
                .stream()
                .map(x -> new Object[] {x.getId(), x})
                .collect(Collectors.toList());
    }

    /**
     * Constructs a new {@code ImageryPreferenceTestIT} instance.
     * @param id entry ID, used only to name tests
     * @param info entry to test
     */
    public ImageryPreferenceTestIT(String id, ImageryInfo info) {
        this.info = Objects.requireNonNull(info);
    }

    private boolean addError(ImageryInfo info, String error) {
        String errorMsg = error.replace('\n', ' ');
        if (notIgnoredErrors.contains(errorMsg))
            notIgnoredErrors.remove(errorMsg);
        return addError(isIgnoredError(errorMsg) ? ignoredErrors : errors, info, errorMsg);
    }

    private static boolean isIgnoredError(String errorMsg) {
        int idx = errorMsg.lastIndexOf(ERROR_SEP);
        return isIgnoredSubstring(errorMsg) || (idx > -1 && isIgnoredSubstring(errorMsg.substring(idx + ERROR_SEP.length())));
    }

    private static boolean isIgnoredSubstring(String substring) {
        return errorsToIgnore.parallelStream().anyMatch(x -> substring.contains(x));
    }

    private static boolean addError(Map<String, Map<ImageryInfo, List<String>>> map, ImageryInfo info, String errorMsg) {
        return map.computeIfAbsent(info.getCountryCode(), x -> Collections.synchronizedMap(new TreeMap<>()))
                  .computeIfAbsent(info, x -> Collections.synchronizedList(new ArrayList<>()))
                  .add(errorMsg);
    }

    private Optional<byte[]> checkUrl(ImageryInfo info, String url) {
        if (url != null && !url.isEmpty()) {
            if (workingURLs.containsKey(url)) {
                return Optional.of(workingURLs.get(url));
            }
            try {
                Response response = HttpClient.create(new URL(url))
                        .setHeaders(info.getCustomHttpHeaders())
                        .setConnectTimeout((int) TimeUnit.MINUTES.toMillis(1))
                        .setReadTimeout((int) TimeUnit.MINUTES.toMillis(5))
                        .connect();
                if (response.getResponseCode() >= 400) {
                    addError(info, url + " -> HTTP " + response.getResponseCode());
                } else if (response.getResponseCode() >= 300) {
                    Logging.warn(url + " -> HTTP " + response.getResponseCode());
                }
                try {
                    byte[] data = Utils.readBytesFromStream(response.getContent());
                    if (response.getResponseCode() < 300) {
                        workingURLs.put(url, data);
                    }
                    return Optional.of(data);
                } catch (IOException e) {
                    if (response.getResponseCode() < 300) {
                        addError(info, url + ERROR_SEP + e);
                    }
                } finally {
                    response.disconnect();
                }
            } catch (IOException e) {
                addError(info, url + ERROR_SEP + e);
            }
        }
        return Optional.empty();
    }

    private void checkLinkUrl(ImageryInfo info, String url) {
        checkUrl(info, url).filter(x -> x.length == 0).ifPresent(x -> addError(info, url + " -> returned empty contents"));
    }

    private String checkTileUrl(ImageryInfo info, AbstractTileSource tileSource, ICoordinate center, int zoom)
            throws IOException {
        TileXY xy = tileSource.latLonToTileXY(center, zoom);
        for (int i = 0; i < 3; i++) {
            try {
                String url = tileSource.getTileUrl(zoom, xy.getXIndex(), xy.getYIndex());
                Optional<byte[]> optional = checkUrl(info, url);
                String error = "";
                if (optional.isPresent()) {
                    byte[] data = optional.get();
                    try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
                        if (ImageIO.read(bais) == null) {
                            error = addImageError(info, url, data, zoom, "did not return an image");
                        }
                    } catch (IOException e) {
                        error = addImageError(info, url, data, zoom, e.toString());
                        Logging.trace(e);
                    }
                }
                return error;
            } catch (IOException e) {
                // Try up to three times max to allow Bing source to initialize itself
                // and avoid random network errors
                Logging.trace(e);
                if (i == 2) {
                    throw e;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    Logging.warn(ex);
                }
            }
        }
        return "";
    }

    private static String zoomMarker(int zoom) {
        return " -> zoom " + zoom + ERROR_SEP;
    }

    private String addImageError(ImageryInfo info, String url, byte[] data, int zoom, String defaultMessage) {
        // Check if we have received an error message
        String error = helper.detectErrorMessage(new String(data, StandardCharsets.UTF_8));
        String errorMsg = url + zoomMarker(zoom) + (error != null ? error.split("\\n")[0] : defaultMessage);
        addError(info, errorMsg);
        return errorMsg;
    }

    private static LatLon getPointInShape(Shape shape) {
        final Coordinate p1 = shape.getPoints().get(0);
        final Bounds bounds = new Bounds(p1.getLat(), p1.getLon(), p1.getLat(), p1.getLon());
        shape.getPoints().forEach(p -> bounds.extend(p.getLat(), p.getLon()));

        final double w = bounds.getWidth();
        final double h = bounds.getHeight();

        final double x2 = bounds.getMinLon() + (w / 2.0);
        final double y2 = bounds.getMinLat() + (h / 2.0);

        final LatLon center = new LatLon(y2, x2);

        // check to see if center is inside shape
        if (shape.contains(center)) {
            return center;
        }

        // if center position (C) is not inside shape, try naively some other positions as follows:
        final double x1 = bounds.getMinLon() + (.25 * w);
        final double x3 = bounds.getMinLon() + (.75 * w);
        final double y1 = bounds.getMinLat() + (.25 * h);
        final double y3 = bounds.getMinLat() + (.75 * h);
        // +-----------+
        // |  5  1  6  |
        // |  4  C  2  |
        // |  8  3  7  |
        // +-----------+
        for (LatLon candidate : new LatLon[] {
                new LatLon(y1, x2),
                new LatLon(y2, x3),
                new LatLon(y3, x2),
                new LatLon(y2, x1),
                new LatLon(y1, x1),
                new LatLon(y1, x3),
                new LatLon(y3, x3),
                new LatLon(y3, x1)
        }) {
            if (shape.contains(candidate)) {
                return candidate;
            }
        }
        return center;
    }

    private static LatLon getCenter(ImageryBounds bounds) {
        List<Shape> shapes = bounds.getShapes();
        return shapes != null && !shapes.isEmpty() ? getPointInShape(shapes.get(0)) : bounds.getCenter();
    }

    private void checkEntry(ImageryInfo info) {
        Logging.info("Checking "+ info);

        if (info.getAttributionImageRaw() != null && info.getAttributionImage() == null) {
            addError(info, "Can't fetch attribution image: " + info.getAttributionImageRaw());
        }

        checkLinkUrl(info, info.getAttributionImageURL());
        checkLinkUrl(info, info.getAttributionLinkURL());
        String eula = info.getEulaAcceptanceRequired();
        if (eula != null) {
            checkLinkUrl(info, eula.replaceAll("\\{lang\\}", ""));
        }
        checkLinkUrl(info, info.getPrivacyPolicyURL());
        checkLinkUrl(info, info.getPermissionReferenceURL());
        checkLinkUrl(info, info.getTermsOfUseURL());
        if (info.getUrl().contains("{time}")) {
            info.setDate("2020-01-01T00:00:00Z/2020-01-02T00:00:00Z");
        }

        try {
            ImageryBounds bounds = info.getBounds();
            // Some imagery sources do not define tiles at (0,0). So pickup Greenwich Royal Observatory for global sources
            ICoordinate center = CoordinateConversion.llToCoor(bounds != null ? getCenter(bounds) : GREENWICH);
            AbstractTileSource tileSource = getTileSource(info);
            // test min zoom and try to detect the correct value in case of error
            int maxZoom = info.getMaxZoom() > 0 ? Math.min(DEFAULT_ZOOM, info.getMaxZoom()) : DEFAULT_ZOOM;
            for (int zoom = info.getMinZoom(); zoom < maxZoom; zoom++) {
                if (!isZoomError(checkTileUrl(info, tileSource, center, zoom))) {
                    break;
                }
            }
            // checking max zoom for real is complex, see https://josm.openstreetmap.de/ticket/16073#comment:27
            if (info.getMaxZoom() > 0 && info.getImageryType() != ImageryType.SCANEX) {
                checkTileUrl(info, tileSource, center, Utils.clamp(DEFAULT_ZOOM, info.getMinZoom() + 1, info.getMaxZoom()));
            }
        } catch (IOException | RuntimeException | WMSGetCapabilitiesException | WMTSGetCapabilitiesException e) {
            addError(info, info.getUrl() + ERROR_SEP + e.toString());
        }

        for (ImageryInfo mirror : info.getMirrors()) {
            checkEntry(mirror);
        }
    }

    private static boolean isZoomError(String error) {
        String[] parts = error.split(ERROR_SEP);
        String lastPart = parts.length > 0 ? parts[parts.length - 1].toLowerCase(Locale.ENGLISH) : "";
        return lastPart.contains("bbox")
            || lastPart.contains("bounding box");
    }

    private static Projection getProjection(ImageryInfo info) {
        for (String code : info.getServerProjections()) {
            Projection proj = Projections.getProjectionByCode(code);
            if (proj != null) {
                return proj;
            }
        }
        return ProjectionRegistry.getProjection();
    }

    @SuppressWarnings("fallthrough")
    private static AbstractTileSource getTileSource(ImageryInfo info)
            throws IOException, WMTSGetCapabilitiesException, WMSGetCapabilitiesException {
        switch (info.getImageryType()) {
            case BING:
                return new BingAerialTileSource(info);
            case SCANEX:
                return new ScanexTileSource(info);
            case TMS:
                return new JosmTemplatedTMSTileSource(info);
            case WMS_ENDPOINT:
                info = convertWmsEndpointToWms(info); // fall-through
            case WMS:
                return new TemplatedWMSTileSource(info, getProjection(info));
            case WMTS:
                return new WMTSTileSource(info, getProjection(info));
            default:
                throw new UnsupportedOperationException(info.toString());
        }
    }

    private static ImageryInfo convertWmsEndpointToWms(ImageryInfo info) throws IOException, WMSGetCapabilitiesException {
        return Optional.ofNullable(AddImageryLayerAction.getWMSLayerInfo(
                info, wms -> new LayerSelection(firstLeafLayer(wms.getLayers()), wms.getPreferredFormat(), true)))
                .orElseThrow(() -> new IllegalStateException("Unable to convert WMS_ENDPOINT to WMS"));
    }

    private static List<LayerDetails> firstLeafLayer(List<LayerDetails> layers) {
        for (LayerDetails layer : layers) {
            boolean hasNoChildren = layer.getChildren().isEmpty();
            if (hasNoChildren && layer.getName() != null) {
                return Collections.singletonList(layer);
            } else if (!hasNoChildren) {
                return firstLeafLayer(layer.getChildren());
            }
        }
        throw new IllegalArgumentException("Unable to find a valid WMS layer");
    }

    private static String format(Map<String, Map<ImageryInfo, List<String>>> map) {
        return map.toString().replaceAll("\\}, ", "\n\\}, ").replaceAll(", ImageryInfo\\{", "\n      ,ImageryInfo\\{");
    }

    /**
     * Test that available imagery entry is valid.
     */
    @Test
    public void testImageryEntryValidity() {
        checkEntry(info);
        assertTrue(format(errors), errors.isEmpty());
        assertFalse(workingURLs.isEmpty());
        assumeTrue(format(ignoredErrors), ignoredErrors.isEmpty());
    }
}
