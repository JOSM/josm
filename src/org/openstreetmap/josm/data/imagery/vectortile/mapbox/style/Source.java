// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery.vectortile.mapbox.style;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.IntFunction;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.imagery.vectortile.mapbox.InvalidMapboxVectorTileException;

/**
 * A source from a Mapbox Vector Style
 *
 * @author Taylor Smock
 * @see <a href="https://docs.mapbox.com/mapbox-gl-js/style-spec/sources/">https://docs.mapbox.com/mapbox-gl-js/style-spec/sources/</a>
 * @since xxx
 */
public class Source {
    /**
     * A common function for zoom constraints
     */
    private static class ZoomBoundFunction implements IntFunction<Integer> {
        private final int min;
        private final int max;
        /**
         * Create a new bound for zooms
         * @param min The min zoom
         * @param max The max zoom
         */
        ZoomBoundFunction(int min, int max) {
            this.min = min;
            this.max = max;
        }

        @Override public Integer apply(int value) {
            return Math.max(min, Math.min(value, max));
        }
    }

    /**
     * WMS servers should contain a "{bbox-epsg-3857}" parameter for the bbox
     */
    private static final String WMS_BBOX = "bbox-epsg-3857";

    private static final String[] NO_URLS = new String[0];

    /**
     * Constrain the min/max zooms to be between 0 and 30, as per tilejson spec
     */
    private static final IntFunction<Integer> ZOOM_BOUND_FUNCTION = new ZoomBoundFunction(0, 30);

    /* Common items */
    /**
     * The name of the source
     */
    private final String name;
    /**
     * The type of the source
     */
    private final SourceType sourceType;

    /* Common tiled data */
    /**
     * The minimum zoom supported
     */
    private final int minZoom;
    /**
     * The maximum zoom supported
     */
    private final int maxZoom;
    /**
     * The tile urls. These usually have replaceable fields.
     */
    private final String[] tileUrls;

    /* Vector and raster data */
    /**
     * The attribution to display for the user
     */
    private final String attribution;
    /**
     * The bounds of the data. We should not request data outside of the bounds
     */
    private final Bounds bounds;
    /**
     * The property to use as a feature id. Can be parameterized
     */
    private final String promoteId;
    /**
     * The tile scheme
     */
    private final Scheme scheme;
    /**
     * {@code true} if the tiles should not be cached
     */
    private final boolean volatileCache;

    /* Raster data */
    /**
     * The tile size
     */
    private final int tileSize;

    /**
     * Create a new Source object
     *
     * @param name The name of the source object
     * @param data The data to set the source information with
     */
    public Source(final String name, final JsonObject data) {
        Objects.requireNonNull(name, "Name cannot be null");
        Objects.requireNonNull(data, "Data cannot be null");
        this.name = name;
        // "type" is required (so throw an NPE if it doesn't exist)
        final String type = data.getString("type");
        this.sourceType = SourceType.valueOf(type.replace("-", "_").toUpperCase(Locale.ROOT));
        // This can also contain SourceType.RASTER_DEM (only needs encoding)
        if (SourceType.VECTOR == this.sourceType || SourceType.RASTER == this.sourceType) {
            if (data.containsKey("url")) {
                // TODO implement https://github.com/mapbox/tilejson-spec
                throw new InvalidMapboxVectorTileException("TileJson not yet supported");
            } else {
                this.minZoom = ZOOM_BOUND_FUNCTION.apply(data.getInt("minzoom", 0));
                this.maxZoom = ZOOM_BOUND_FUNCTION.apply(data.getInt("maxzoom", 22));
                this.attribution = data.getString("attribution", null);
                if (data.containsKey("bounds") && data.get("bounds").getValueType() == JsonValue.ValueType.ARRAY) {
                    final JsonArray bJsonArray = data.getJsonArray("bounds");
                    if (bJsonArray.size() != 4) {
                        throw new IllegalArgumentException(MessageFormat.format("bounds must have four values, but has {0}", bJsonArray.size()));
                    }
                    final double[] bArray = new double[bJsonArray.size()];
                    for (int i = 0; i < bJsonArray.size(); i++) {
                        bArray[i] = bJsonArray.getJsonNumber(i).doubleValue();
                    }
                    // The order in the response is
                    // [south-west longitude, south-west latitude, north-east longitude, north-east latitude]
                    this.bounds = new Bounds(bArray[1], bArray[0], bArray[3], bArray[2]);
                } else {
                    // Don't use a static instance for bounds, as it is not a immutable class
                    this.bounds = new Bounds(-85.051129, -180, 85.051129, 180);
                }
                this.promoteId = data.getString("promoteId", null);
                this.scheme = Scheme.valueOf(data.getString("scheme", "xyz").toUpperCase(Locale.ROOT));
                if (data.containsKey("tiles") && data.get("tiles").getValueType() == JsonValue.ValueType.ARRAY) {
                    this.tileUrls = data.getJsonArray("tiles").stream().filter(JsonString.class::isInstance)
                      .map(JsonString.class::cast).map(JsonString::getString)
                      // Replace bbox-epsg-3857 with bbox (already encased with {})
                      .map(url -> url.replace(WMS_BBOX, "bbox")).toArray(String[]::new);
                } else {
                    this.tileUrls = NO_URLS;
                }
                this.volatileCache = data.getBoolean("volatile", false);
                this.tileSize = data.getInt("tileSize", 512);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Get the bounds for this source
     * @return The bounds where this source can be used
     */
    public Bounds getBounds() {
        return this.bounds;
    }

    /**
     * Get the source name
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the URLs that can be used to get vector data
     *
     * @return The urls
     */
    public List<String> getUrls() {
        return Collections.unmodifiableList(Arrays.asList(this.tileUrls));
    }

    /**
     * Get the minimum zoom
     *
     * @return The min zoom (default {@code 0})
     */
    public int getMinZoom() {
        return this.minZoom;
    }

    /**
     * Get the max zoom
     *
     * @return The max zoom (default {@code 22})
     */
    public int getMaxZoom() {
        return this.maxZoom;
    }

    /**
     * Get the attribution for this source
     *
     * @return The attribution text. May be {@code null}.
     */
    public String getAttributionText() {
        return this.attribution;
    }

    @Override
    public String toString() {
        Collection<String> parts = new ArrayList<>(1 + this.getUrls().size());
        parts.add(this.getName());
        parts.addAll(this.getUrls());
        return String.join(" ", parts);
    }

    @Override
    public boolean equals(Object other) {
        if (other != null && this.getClass() == other.getClass()) {
            Source o = (Source) other;
            return Objects.equals(this.name, o.name)
              && this.sourceType == o.sourceType
              && this.minZoom == o.minZoom
              && this.maxZoom == o.maxZoom
              && Objects.equals(this.attribution, o.attribution)
              && Objects.equals(this.promoteId, o.promoteId)
              && this.scheme == o.scheme
              && this.volatileCache == o.volatileCache
              && this.tileSize == o.tileSize
              && Objects.equals(this.bounds, o.bounds)
              && Objects.deepEquals(this.tileUrls, o.tileUrls);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.sourceType, this.minZoom, this.maxZoom, this.attribution, this.promoteId,
          this.scheme, this.volatileCache, this.tileSize, this.bounds, Arrays.hashCode(this.tileUrls));
    }
}
