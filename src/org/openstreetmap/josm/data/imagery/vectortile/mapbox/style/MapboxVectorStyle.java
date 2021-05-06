// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery.vectortile.mapbox.style;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.json.JsonValue;

import org.openstreetmap.josm.data.imagery.vectortile.mapbox.InvalidMapboxVectorTileException;
import org.openstreetmap.josm.data.preferences.JosmBaseDirectories;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;

/**
 * Create a mapping for a Mapbox Vector Style
 *
 * @author Taylor Smock
 * @see <a href="https://docs.mapbox.com/mapbox-gl-js/style-spec/">https://docs.mapbox.com/mapbox-gl-js/style-spec/</a>
 * @since xxx
 */
public class MapboxVectorStyle {

    private static final ConcurrentHashMap<String, MapboxVectorStyle> STYLE_MAPPING = new ConcurrentHashMap<>();

    /**
     * Get a MapboxVector style for a URL
     * @param url The url to get
     * @return The Mapbox Vector Style. May be {@code null} if there was an error.
     */
    public static MapboxVectorStyle getMapboxVectorStyle(String url) {
        return STYLE_MAPPING.computeIfAbsent(url, key -> {
            try (CachedFile style = new CachedFile(url);
                    BufferedReader reader = style.getContentReader();
                    JsonReader jsonReader = Json.createReader(reader)) {
                JsonStructure structure = jsonReader.read();
                return new MapboxVectorStyle(structure.asJsonObject());
            } catch (IOException e) {
                Logging.error(e);
            }
            // Documentation indicates that this will <i>not</i> be entered into the map, which means that this will be
            // retried if something goes wrong.
            return null;
        });
    }

    /** The version for the style specification */
    private final int version;
    /** The optional name for the vector style */
    private final String name;
    /** The optional URL for sprites. This mush be absolute (so it must contain the scheme, authority, and path). */
    private final String spriteUrl;
    /** The optional URL for glyphs. This may have replaceable values in it. */
    private final String glyphUrl;
    /** The required collection of sources with a list of layers that are applicable for that source*/
    private final Map<Source, ElemStyles> sources;

    /**
     * Create a new MapboxVector style. You should prefer {@link #getMapboxVectorStyle(String)}
     * for deduplication purposes.
     *
     * @param jsonObject The object to create the style from
     * @see #getMapboxVectorStyle(String)
     */
    public MapboxVectorStyle(JsonObject jsonObject) {
        // There should be a version specifier. We currently only support version 8.
        // This can throw an NPE when there is no version number.
        this.version = jsonObject.getInt("version");
        if (this.version == 8) {
            this.name = jsonObject.getString("name", null);
            String id = jsonObject.getString("id", this.name);
            this.spriteUrl = jsonObject.getString("sprite", null);
            this.glyphUrl = jsonObject.getString("glyphs", null);
            final List<Source> sourceList;
            if (jsonObject.containsKey("sources") && jsonObject.get("sources").getValueType() == JsonValue.ValueType.OBJECT) {
                final JsonObject sourceObj = jsonObject.getJsonObject("sources");
                sourceList = sourceObj.entrySet().stream().filter(entry -> entry.getValue().getValueType() == JsonValue.ValueType.OBJECT)
                  .map(entry -> {
                      try {
                          return new Source(entry.getKey(), entry.getValue().asJsonObject());
                      } catch (InvalidMapboxVectorTileException e) {
                          Logging.error(e);
                          // Reraise if not a known exception
                          if (!"TileJson not yet supported".equals(e.getMessage())) {
                              throw e;
                          }
                      }
                      return null;
                  }).filter(Objects::nonNull).collect(Collectors.toList());
            } else {
                sourceList = Collections.emptyList();
            }
            final List<Layers> layers;
            if (jsonObject.containsKey("layers") && jsonObject.get("layers").getValueType() == JsonValue.ValueType.ARRAY) {
                JsonArray lArray = jsonObject.getJsonArray("layers");
                layers = lArray.stream().filter(JsonObject.class::isInstance).map(JsonObject.class::cast).map(obj -> new Layers(id, obj))
                  .collect(Collectors.toList());
            } else {
                layers = Collections.emptyList();
            }
            final Map<Optional<Source>, List<Layers>> sourceLayer = layers.stream().collect(
              Collectors.groupingBy(layer -> sourceList.stream().filter(source -> source.getName().equals(layer.getSource()))
                .findFirst(), LinkedHashMap::new, Collectors.toList()));
            // Abuse HashMap null (null == default)
            this.sources = new LinkedHashMap<>();
            for (Map.Entry<Optional<Source>, List<Layers>> entry : sourceLayer.entrySet()) {
                final Source source = entry.getKey().orElse(null);
                final String data = entry.getValue().stream().map(Layers::toString).collect(Collectors.joining());
                final String metaData = "meta{title:" + (source == null ? "Generated Style" :
                  source.getName()) + ";version:\"autogenerated\";description:\"auto generated style\";}";

                // This is the default canvas
                final String canvas = "canvas{default-points:false;default-lines:false;}";
                final MapCSSStyleSource style = new MapCSSStyleSource(metaData + canvas + data);
                // Save to directory
                MainApplication.worker.execute(() -> this.save((source == null ? data.hashCode() : source.getName()) + ".mapcss", style));
                this.sources.put(source, new ElemStyles(Collections.singleton(style)));
            }
            if (this.spriteUrl != null && !this.spriteUrl.trim().isEmpty()) {
                MainApplication.worker.execute(this::fetchSprites);
            }
        } else {
            throw new IllegalArgumentException(tr("Vector Tile Style Version not understood: version {0} (json: {1})",
              this.version, jsonObject));
        }
    }

    /**
     * Fetch sprites. Please note that this is (literally) only png. Unfortunately.
     * @see <a href="https://docs.mapbox.com/mapbox-gl-js/style-spec/sprite/">https://docs.mapbox.com/mapbox-gl-js/style-spec/sprite/</a>
     */
    private void fetchSprites() {
        // HiDPI images first -- if this succeeds, don't bother with the lower resolution (JOSM has no method to switch)
        try (CachedFile spriteJson = new CachedFile(this.spriteUrl + "@2x.json");
          CachedFile spritePng = new CachedFile(this.spriteUrl + "@2x.png")) {
            if (parseSprites(spriteJson, spritePng)) {
                return;
            }
        }
        try (CachedFile spriteJson = new CachedFile(this.spriteUrl + ".json");
        CachedFile spritePng = new CachedFile(this.spriteUrl + ".png")) {
            parseSprites(spriteJson, spritePng);
        }
    }

    private boolean parseSprites(CachedFile spriteJson, CachedFile spritePng) {
        /* JSON looks like this:
         * { "image-name": {"width": width, "height": height, "x": x, "y": y, "pixelRatio": 1 }}
         * width/height are the dimensions of the image
         * x -- distance right from top left
         * y -- distance down from top left
         * pixelRatio -- this <i>appears</i> to be from the "@2x" (default 1)
         * content -- [left, top corner, right, bottom corner]
         * stretchX -- [[from, to], [from, to], ...]
         * stretchY -- [[from, to], [from, to], ...]
         */
        final JsonObject spriteObject;
        final BufferedImage spritePngImage;
        try (BufferedReader spriteJsonBufferedReader = spriteJson.getContentReader();
          JsonReader spriteJsonReader = Json.createReader(spriteJsonBufferedReader);
          InputStream spritePngBufferedReader = spritePng.getInputStream()
        ) {
            spriteObject = spriteJsonReader.read().asJsonObject();
            spritePngImage = ImageIO.read(spritePngBufferedReader);
        } catch (IOException e) {
            Logging.error(e);
            return false;
        }
        for (Map.Entry<String, JsonValue> entry : spriteObject.entrySet()) {
            final JsonObject info = entry.getValue().asJsonObject();
            int width = info.getInt("width");
            int height = info.getInt("height");
            int x = info.getInt("x");
            int y = info.getInt("y");
            save(entry.getKey() + ".png", spritePngImage.getSubimage(x, y, width, height));
        }
        return true;
    }

    private void save(String name, Object object) {
        final File cache;
        if (object instanceof Image) {
            // Images have a specific location where they are looked for
            cache = new File(Config.getDirs().getUserDataDirectory(true), "images");
        } else {
            cache = JosmBaseDirectories.getInstance().getCacheDirectory(true);
        }
        final File location = new File(cache, this.name != null ? this.name : Integer.toString(this.hashCode()));
        if ((!location.exists() && !location.mkdirs()) || (location.exists() && !location.isDirectory())) {
            // Don't try to save if the file exists and is not a directory or we couldn't create it
            return;
        }
        final File toSave = new File(location, name);
        try (OutputStream fileOutputStream = Files.newOutputStream(toSave.toPath())) {
            if (object instanceof String) {
                fileOutputStream.write(((String) object).getBytes(StandardCharsets.UTF_8));
            } else if (object instanceof MapCSSStyleSource) {
                MapCSSStyleSource source = (MapCSSStyleSource) object;
                try (InputStream inputStream = source.getSourceInputStream()) {
                    int byteVal = inputStream.read();
                    do {
                        fileOutputStream.write(byteVal);
                        byteVal = inputStream.read();
                    } while (byteVal > -1);
                    source.url = "file:/" + toSave.getAbsolutePath().replace('\\', '/');
                    if (source.isLoaded()) {
                        source.loadStyleSource();
                    }
                }
            } else if (object instanceof BufferedImage) {
                // This directory is checked first when getting images
                ImageIO.write((BufferedImage) object, "png", toSave);
            }
        } catch (IOException e) {
            Logging.info(e);
        }
    }

    /**
     * Get the generated layer->style mapping
     * @return The mapping (use to enable/disable a paint style)
     */
    public Map<Source, ElemStyles> getSources() {
        return this.sources;
    }

    /**
     * Get the sprite url for the style
     * @return The base sprite url
     */
    public String getSpriteUrl() {
        return this.spriteUrl;
    }

    @Override
    public boolean equals(Object other) {
        if (other != null && other.getClass() == this.getClass()) {
            MapboxVectorStyle o = (MapboxVectorStyle) other;
            return this.version == o.version
              && Objects.equals(this.name, o.name)
              && Objects.equals(this.glyphUrl, o.glyphUrl)
              && Objects.equals(this.spriteUrl, o.spriteUrl)
              && Objects.equals(this.sources, o.sources);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.version, this.glyphUrl, this.spriteUrl, this.sources);
    }
}
