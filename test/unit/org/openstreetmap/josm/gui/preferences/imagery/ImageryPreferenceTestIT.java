// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.apache.commons.jcs.access.CacheAccess;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.TileXY;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.tilesources.AbstractTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.BingAerialTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.ScanexTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.TemplatedTMSTileSource;
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
import org.openstreetmap.josm.io.imagery.WMSImagery.WMSGetCapabilitiesException;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.HttpClient.Response;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Integration tests of {@link ImageryPreference} class.
 */
public class ImageryPreferenceTestIT {

    private static final LatLon GREENWICH = new LatLon(51.47810, -0.00170);
    private static final int DEFAULT_ZOOM = 12;

    /**
     * Setup rule
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().https().preferences().projection().projectionNadGrids().timeout(10000*120);

    private final Map<String, Map<ImageryInfo, List<String>>> errors = Collections.synchronizedMap(new TreeMap<>());
    private final Map<String, byte[]> workingURLs = Collections.synchronizedMap(new TreeMap<>());

    private TMSCachedTileLoaderJob helper;
    private List<String> ignoredErrors;

    /**
     * Setup test
     * @throws IOException in case of I/O error
     */
    @Before
    public void before() throws IOException {
        helper = new TMSCachedTileLoaderJob(null, null, new CacheAccess<>(null), new TileJobOptions(0, 0, null, 0), null);
        ignoredErrors = TestUtils.getIgnoredErrorMessages(ImageryPreferenceTestIT.class);
    }

    private boolean addError(ImageryInfo info, String error) {
        return !ignoredErrors.contains(error) &&
               errors.computeIfAbsent(info.getCountryCode(), x -> Collections.synchronizedMap(new TreeMap<>()))
                     .computeIfAbsent(info, x -> Collections.synchronizedList(new ArrayList<>()))
                     .add(error);
    }

    private Optional<byte[]> checkUrl(ImageryInfo info, String url) {
        if (url != null && !url.isEmpty()) {
            if (workingURLs.containsKey(url)) {
                return Optional.of(workingURLs.get(url));
            }
            try {
                Response response = HttpClient.create(new URL(url))
                        .setHeaders(info.getCustomHttpHeaders())
                        .setConnectTimeout((int) TimeUnit.SECONDS.toMillis(30))
                        .setReadTimeout((int) TimeUnit.SECONDS.toMillis(60))
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
                        addError(info, url + " -> " + e);
                    }
                } finally {
                    response.disconnect();
                }
            } catch (IOException e) {
                addError(info, url + " -> " + e);
            }
        }
        return Optional.empty();
    }

    private void checkLinkUrl(ImageryInfo info, String url) {
        checkUrl(info, url).filter(x -> x.length == 0).ifPresent(x -> addError(info, url + " -> returned empty contents"));
    }

    private boolean checkTileUrl(ImageryInfo info, AbstractTileSource tileSource, ICoordinate center, int zoom)
            throws IOException {
        TileXY xy = tileSource.latLonToTileXY(center, zoom);
        for (int i = 0; i < 3; i++) {
            try {
                String url = tileSource.getTileUrl(zoom, xy.getXIndex(), xy.getYIndex());
                checkUrl(info, url).ifPresent(data -> {
                    try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
                        if (ImageIO.read(bais) == null) {
                            addImageError(info, url, data, zoom, "did not return an image");
                        }
                    } catch (IOException e) {
                        addImageError(info, url, data, zoom, e.toString());
                        Logging.trace(e);
                    }
                });
                // Determines if this is a success (no error message with current zoom marker)
                return errors.getOrDefault(info.getCountryCode(), Collections.emptyMap())
                              .getOrDefault(info, Collections.emptyList())
                              .stream().noneMatch(e -> e.contains(zoomMarker(zoom)));
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
        return false;
    }

    private static String zoomMarker(int zoom) {
        return " -> zoom " + zoom + " -> ";
    }

    private void addImageError(ImageryInfo info, String url, byte[] data, int zoom, String defaultMessage) {
        // Check if we have received an error message
        String error = helper.detectErrorMessage(new String(data, StandardCharsets.UTF_8));
        addError(info, url + zoomMarker(zoom) + (error != null ? error.split("\\n")[0] : defaultMessage));
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
        checkLinkUrl(info, info.getPermissionReferenceURL());
        checkLinkUrl(info, info.getTermsOfUseURL());

        try {
            ImageryBounds bounds = info.getBounds();
            // Some imagery sources do not define tiles at (0,0). So pickup Greenwich Royal Observatory for global sources
            ICoordinate center = CoordinateConversion.llToCoor(bounds != null ? getCenter(bounds) : GREENWICH);
            AbstractTileSource tileSource = getTileSource(info);
            // test min zoom and try to detect the correct value in case of error
            int maxZoom = info.getMaxZoom() > 0 ? Math.min(DEFAULT_ZOOM, info.getMaxZoom()) : DEFAULT_ZOOM;
            for (int zoom = info.getMinZoom(); zoom < maxZoom; zoom++) {
                if (checkTileUrl(info, tileSource, center, zoom)) {
                    break;
                }
            }
            // checking max zoom for real is complex, see https://josm.openstreetmap.de/ticket/16073#comment:27
            if (info.getMaxZoom() > 0 && info.getImageryType() != ImageryType.SCANEX) {
                checkTileUrl(info, tileSource, center, Utils.clamp(DEFAULT_ZOOM, info.getMinZoom() + 1, info.getMaxZoom()));
            }
        } catch (IOException | RuntimeException | WMSGetCapabilitiesException | WMTSGetCapabilitiesException e) {
            addError(info, info.getUrl() + " -> " + e.toString());
        }

        for (ImageryInfo mirror : info.getMirrors()) {
            checkEntry(mirror);
        }
    }

    private static Projection getProjection(ImageryInfo info) {
        if (!info.getServerProjections().isEmpty()) {
            Projection proj = Projections.getProjectionByCode(info.getServerProjections().get(0));
            if (proj != null) {
                return proj;
            }
        }
        return ProjectionRegistry.getProjection();
    }

    private static AbstractTileSource getTileSource(ImageryInfo info)
            throws IOException, WMTSGetCapabilitiesException, WMSGetCapabilitiesException {
        switch (info.getImageryType()) {
            case BING:
                return new BingAerialTileSource(info);
            case SCANEX:
                return new ScanexTileSource(info);
            case TMS:
                return new TemplatedTMSTileSource(info);
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

    /**
     * Test that available imagery entries are valid.
     * @throws Exception in case of error
     */
    @Test
    public void testValidityOfAvailableImageryEntries() throws Exception {
        ImageryLayerInfo.instance.load(false);
        ImageryLayerInfo.instance.getDefaultLayers().parallelStream().forEach(this::checkEntry);
        assertTrue(errors.toString().replaceAll("\\}, ", "\n\\}, ").replaceAll(", ImageryInfo\\{", "\n      ,ImageryInfo\\{"),
                errors.isEmpty());
        assertFalse(workingURLs.isEmpty());
    }
}
