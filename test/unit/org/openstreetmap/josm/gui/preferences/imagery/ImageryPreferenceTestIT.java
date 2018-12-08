// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.gui.jmapviewer.TileXY;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.tilesources.AbstractTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.BingAerialTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.ScanexTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.TemplatedTMSTileSource;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.imagery.CoordinateConversion;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryBounds;
import org.openstreetmap.josm.data.imagery.ImageryLayerInfo;
import org.openstreetmap.josm.data.imagery.Shape;
import org.openstreetmap.josm.data.imagery.TemplatedWMSTileSource;
import org.openstreetmap.josm.data.imagery.WMSEndpointTileSource;
import org.openstreetmap.josm.data.imagery.WMTSTileSource;
import org.openstreetmap.josm.data.imagery.WMTSTileSource.WMTSGetCapabilitiesException;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.HttpClient.Response;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Integration tests of {@link ImageryPreference} class.
 */
public class ImageryPreferenceTestIT {

    /**
     * Setup rule
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().https().projection().projectionNadGrids().timeout(10000*60);

    private final Map<String, Map<ImageryInfo, List<String>>> errors = Collections.synchronizedMap(new TreeMap<>());
    private final Set<String> workingURLs = Collections.synchronizedSet(new HashSet<>());

    private boolean addError(ImageryInfo info, String error) {
        return errors.computeIfAbsent(info.getCountryCode(), x -> Collections.synchronizedMap(new TreeMap<>()))
                     .computeIfAbsent(info, x -> Collections.synchronizedList(new ArrayList<>()))
                     .add(error);
    }

    private void checkUrl(ImageryInfo info, String url) {
        if (url != null && !workingURLs.contains(url)) {
            try {
                Response response = HttpClient.create(new URL(url)).connect();
                if (response.getResponseCode() >= 400) {
                    addError(info, url + " -> HTTP " + response.getResponseCode());
                } else if (response.getResponseCode() >= 300) {
                    Logging.warn(url + " -> HTTP " + response.getResponseCode());
                } else {
                    workingURLs.add(url);
                }
                response.disconnect();
            } catch (IOException e) {
                addError(info, url + " -> " + e);
            }
        }
    }

    private void checkTileUrl(ImageryInfo info, AbstractTileSource tileSource, ICoordinate center, int zoom)
            throws IOException {
        TileXY xy = tileSource.latLonToTileXY(center, zoom);
        for (int i = 0; i < 3; i++) {
            try {
                checkUrl(info, tileSource.getTileUrl(zoom, xy.getXIndex(), xy.getYIndex()));
                return;
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
    }

    private static LatLon getCenter(ImageryBounds bounds) {
        List<Shape> shapes = bounds.getShapes();
        Projection proj = ProjectionRegistry.getProjection();
        return shapes != null && shapes.size() > 1
                ? proj.eastNorth2latlon(
                        Geometry.getCentroidEN(shapes.get(0).getPoints().stream()
                                .map(CoordinateConversion::coorToLL)
                                .map(proj::latlon2eastNorth)
                                .collect(Collectors.toList())))
                : bounds.getCenter();
    }

    private void checkEntry(ImageryInfo info) {
        Logging.info("Checking "+ info);

        if (info.getAttributionImageRaw() != null && info.getAttributionImage() == null) {
            addError(info, "Can't fetch attribution image: " + info.getAttributionImageRaw());
        }

        checkUrl(info, info.getAttributionImageURL());
        checkUrl(info, info.getAttributionLinkURL());
        String eula = info.getEulaAcceptanceRequired();
        if (eula != null) {
            checkUrl(info, eula.replaceAll("\\{lang\\}", ""));
        }
        checkUrl(info, info.getPermissionReferenceURL());
        checkUrl(info, info.getTermsOfUseURL());

        try {
            ImageryBounds bounds = info.getBounds();
            // Some imagery sources do not define tiles at (0,0). So pickup Greenwich Royal Observatory for global sources
            ICoordinate center = CoordinateConversion.llToCoor(bounds != null ? getCenter(bounds) : new LatLon(51.47810, -0.00170));
            AbstractTileSource tileSource = getTileSource(info);
            checkTileUrl(info, tileSource, center, info.getMinZoom());
            // checking max zoom for real is complex, see https://josm.openstreetmap.de/ticket/16073#comment:27
            if (info.getMaxZoom() > 0) {
                checkTileUrl(info, tileSource, center, Utils.clamp(12, info.getMinZoom() + 1, info.getMaxZoom()));
            }
        } catch (IOException | WMTSGetCapabilitiesException | IllegalArgumentException e) {
            addError(info, e.toString());
        }

        for (ImageryInfo mirror : info.getMirrors()) {
            checkEntry(mirror);
        }
    }

    private static AbstractTileSource getTileSource(ImageryInfo info) throws IOException, WMTSGetCapabilitiesException {
        switch (info.getImageryType()) {
            case BING:
                return new BingAerialTileSource(info);
            case SCANEX:
                return new ScanexTileSource(info);
            case TMS:
                return new TemplatedTMSTileSource(info);
            case WMS:
                return new TemplatedWMSTileSource(info, ProjectionRegistry.getProjection());
            case WMS_ENDPOINT:
                return new WMSEndpointTileSource(info, ProjectionRegistry.getProjection());
            case WMTS:
                return new WMTSTileSource(info, ProjectionRegistry.getProjection());
            default:
                throw new UnsupportedOperationException(info.toString());
        }
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
    }
}
