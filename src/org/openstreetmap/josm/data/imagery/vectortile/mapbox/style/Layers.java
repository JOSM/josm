// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery.vectortile.mapbox.style;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.gui.mappaint.StyleKeys;
import org.openstreetmap.josm.tools.Utils;

import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

/**
 * Mapbox style layers
 * @author Taylor Smock
 * @see <a href="https://docs.mapbox.com/mapbox-gl-js/style-spec/layers/">https://docs.mapbox.com/mapbox-gl-js/style-spec/layers/</a>
 * @since 17862
 */
public class Layers {
    /**
     * The layer type. This affects the rendering.
     * @author Taylor Smock
     * @since 17862
     */
    enum Type {
        /** Filled polygon with an (optional) border */
        FILL,
        /** A line */
        LINE,
        /** A symbol */
        SYMBOL,
        /** A circle */
        CIRCLE,
        /** A heatmap */
        HEATMAP,
        /** A 3D polygon extrusion */
        FILL_EXTRUSION,
        /** Raster */
        RASTER,
        /** Hillshade data */
        HILLSHADE,
        /** A background color or pattern */
        BACKGROUND,
        /** The fallback layer */
        SKY
    }

    private static final char SEMI_COLON = ';';
    private static final Pattern CURLY_BRACES = Pattern.compile("(\\{(.*?)})");
    private static final String PAINT = "paint";

    /** A required unique layer name */
    private final String id;
    /** The required type */
    private final Type type;
    /** An optional expression */
    private final Expression filter;
    /** The max zoom for the layer */
    private final int maxZoom;
    /** The min zoom for the layer */
    private final int minZoom;

    /** Default paint properties for this layer */
    private final String paintProperties;

    /** A source description to be used with this layer. Required for everything <i>but</i> {@link Type#BACKGROUND} */
    private final String source;
    /** Layer to use from the vector tile source. Only allowed with {@link SourceType#VECTOR}. */
    private final String sourceLayer;
    /** The id for the style -- used for image paths */
    private final String styleId;
    /**
     * Create a layer object
     * @param layerInfo The info to use to create the layer
     */
    public Layers(final JsonObject layerInfo) {
        this (null, layerInfo);
    }

    /**
     * Create a layer object
     * @param styleId The id for the style (image paths require this)
     * @param layerInfo The info to use to create the layer
     */
    public Layers(final String styleId, final JsonObject layerInfo) {
        this.id = layerInfo.getString("id");
        this.styleId = styleId;
        this.type = Type.valueOf(layerInfo.getString("type").replace("-", "_").toUpperCase(Locale.ROOT));
        if (layerInfo.containsKey("filter")) {
            this.filter = new Expression(layerInfo.get("filter"));
        } else {
            this.filter = Expression.EMPTY_EXPRESSION;
        }
        // minZoom <= showable zooms < maxZoom. This should be fractional, but our mapcss implementations expects ints.
        this.minZoom = layerInfo.getInt("minzoom", Integer.MIN_VALUE);
        int tMaxZoom = layerInfo.getInt("maxzoom", Integer.MAX_VALUE);
        if (tMaxZoom == Integer.MAX_VALUE) {
            this.maxZoom = Integer.MAX_VALUE;
        } else {
            this.maxZoom = Math.max(this.minZoom, Math.max(0, tMaxZoom - 1));
        }
        // There is a metadata field (I don't *think* I need it?)
        // source is only optional with {@link Type#BACKGROUND}.
        if (this.type == Type.BACKGROUND) {
            this.source = layerInfo.getString("source", null);
        } else {
            this.source = layerInfo.getString("source");
        }
        if (layerInfo.containsKey(PAINT) && layerInfo.get(PAINT).getValueType() == JsonValue.ValueType.OBJECT) {
            final JsonObject paintObject = layerInfo.getJsonObject(PAINT);
            final JsonObject layoutObject = layerInfo.getOrDefault("layout", JsonValue.EMPTY_JSON_OBJECT).asJsonObject();
            // Don't throw exceptions here, since we may just point at the styling
            if ("visible".equalsIgnoreCase(layoutObject.getString("visibility", "visible"))) {
                switch (type) {
                case FILL:
                    // area
                    this.paintProperties = parsePaintFill(paintObject);
                    break;
                case LINE:
                    // way
                    this.paintProperties = parsePaintLine(layoutObject, paintObject);
                    break;
                case CIRCLE:
                    // point
                    this.paintProperties = parsePaintCircle(paintObject);
                    break;
                case SYMBOL:
                    // point
                    this.paintProperties = parsePaintSymbol(layoutObject, paintObject);
                    break;
                case BACKGROUND:
                    // canvas only
                    this.paintProperties = parsePaintBackground(paintObject);
                    break;
                default:
                    this.paintProperties = "";
                }
            } else {
                this.paintProperties = "";
            }
        } else {
            this.paintProperties = "";
        }
        this.sourceLayer = layerInfo.getString("source-layer", null);
    }

    /**
     * Get the filter for this layer
     * @return The filter
     */
    public Expression getFilter() {
        return this.filter;
    }

    /**
     * Get the unique id for this layer
     * @return The unique id
     */
    public String getId() {
        return this.id;
    }

    /**
     * Get the type of this layer
     * @return The layer type
     */
    public Type getType() {
        return this.type;
    }

    private static String parsePaintLine(final JsonObject layoutObject, final JsonObject paintObject) {
        final StringBuilder sb = new StringBuilder(36);
        // line-blur, default 0 (px)
        // line-color, default #000000, disabled by line-pattern
        final String color = paintObject.getString("line-color", "#000000");
        sb.append(StyleKeys.COLOR).append(':').append(color).append(SEMI_COLON);
        // line-opacity, default 1 (0-1)
        final JsonNumber opacity = paintObject.getJsonNumber("line-opacity");
        if (opacity != null) {
            sb.append(StyleKeys.OPACITY).append(':').append(opacity.numberValue().doubleValue()).append(SEMI_COLON);
        }
        // line-cap, default butt (butt|round|square)
        final String cap = layoutObject.getString("line-cap", "butt");
        sb.append(StyleKeys.LINECAP).append(':');
        switch (cap) {
        case "round":
        case "square":
            sb.append(cap);
            break;
        case "butt":
        default:
            sb.append("none");
        }

        sb.append(SEMI_COLON);
        // line-dasharray, array of number >= 0, units in line widths, disabled by line-pattern
        if (paintObject.containsKey("line-dasharray")) {
            final JsonArray dashArray = paintObject.getJsonArray("line-dasharray");
            sb.append(StyleKeys.DASHES).append(':');
            sb.append(dashArray.stream().filter(JsonNumber.class::isInstance).map(JsonNumber.class::cast)
              .map(JsonNumber::toString).collect(Collectors.joining(",")));
            sb.append(SEMI_COLON);
        }
        // line-gap-width
        // line-gradient
        // line-join
        // line-miter-limit
        // line-offset
        // line-pattern TODO this first, since it disables stuff
        // line-round-limit
        // line-sort-key
        // line-translate
        // line-translate-anchor
        // line-width
        final JsonNumber width = paintObject.getJsonNumber("line-width");
        sb.append(StyleKeys.WIDTH).append(':').append(width == null ? 1 : width.toString()).append(SEMI_COLON);
        return sb.toString();
    }

    private static String parsePaintCircle(final JsonObject paintObject) {
        final StringBuilder sb = new StringBuilder(150).append("symbol-shape:circle;")
          // circle-blur
          // circle-color
          .append("symbol-fill-color:").append(paintObject.getString("circle-color", "#000000")).append(SEMI_COLON);
        // circle-opacity
        final JsonNumber fillOpacity = paintObject.getJsonNumber("circle-opacity");
        sb.append("symbol-fill-opacity:").append(fillOpacity != null ? fillOpacity.numberValue().toString() : "1").append(SEMI_COLON);
        // circle-pitch-alignment // not 3D
        // circle-pitch-scale // not 3D
        // circle-radius
        final JsonNumber radius = paintObject.getJsonNumber("circle-radius");
        sb.append("symbol-size:").append(radius != null ? (2 * radius.numberValue().doubleValue()) : "10").append(SEMI_COLON)
          // circle-sort-key
          // circle-stroke-color
          .append("symbol-stroke-color:").append(paintObject.getString("circle-stroke-color", "#000000")).append(SEMI_COLON);
        // circle-stroke-opacity
        final JsonNumber strokeOpacity = paintObject.getJsonNumber("circle-stroke-opacity");
        sb.append("symbol-stroke-opacity:").append(strokeOpacity != null ? strokeOpacity.numberValue().toString() : "1").append(SEMI_COLON);
        // circle-stroke-width
        final JsonNumber strokeWidth = paintObject.getJsonNumber("circle-stroke-width");
        sb.append("symbol-stroke-width:").append(strokeWidth != null ? strokeWidth.numberValue().toString() : "0").append(SEMI_COLON);
        // circle-translate
        // circle-translate-anchor
        return sb.toString();
    }

    private String parsePaintSymbol(
      final JsonObject layoutObject,
      final JsonObject paintObject) {
        final StringBuilder sb = new StringBuilder();
        // icon-allow-overlap
        // icon-anchor
        // icon-color
        // icon-halo-blur
        // icon-halo-color
        // icon-halo-width
        // icon-ignore-placement
        // icon-image
        boolean iconImage = false;
        if (layoutObject.containsKey("icon-image")) {
            sb.append(/* NO-ICON */"icon-image:concat(");
            if (!Utils.isBlank(this.styleId)) {
                sb.append('"').append(this.styleId).append('/').append("\",");
            }
            Matcher matcher = CURLY_BRACES.matcher(layoutObject.getString("icon-image"));
            StringBuffer stringBuffer = new StringBuffer();
            int previousMatch;
            if (matcher.lookingAt()) {
                matcher.appendReplacement(stringBuffer, "tag(\"$2\"),\"");
                previousMatch = matcher.end();
            } else {
                previousMatch = 0;
                stringBuffer.append('"');
            }
            while (matcher.find()) {
                if (matcher.start() == previousMatch) {
                    matcher.appendReplacement(stringBuffer, ",tag(\"$2\")");
                } else {
                    matcher.appendReplacement(stringBuffer, "\",tag(\"$2\"),\"");
                }
                previousMatch = matcher.end();
            }
            if (matcher.hitEnd() && stringBuffer.toString().endsWith(",\"")) {
                stringBuffer.delete(stringBuffer.length() - ",\"".length(), stringBuffer.length());
            } else if (!matcher.hitEnd()) {
                stringBuffer.append('"');
            }
            StringBuffer tail = new StringBuffer();
            matcher.appendTail(tail);
            if (tail.length() > 0) {
                String current = stringBuffer.toString();
                if (!"\"".equals(current) && !current.endsWith(",\"")) {
                    stringBuffer.append(",\"");
                }
                stringBuffer.append(tail);
                stringBuffer.append('"');
            }

            sb.append(stringBuffer).append(')').append(SEMI_COLON);
            iconImage = true;
        }
        // icon-keep-upright
        // icon-offset
        if (iconImage && layoutObject.containsKey("icon-offset")) {
            // default [0, 0], right,down == positive, left,up == negative
            final List<JsonNumber> offset = layoutObject.getJsonArray("icon-offset").getValuesAs(JsonNumber.class);
            // Assume that the offset must be size 2. Probably not necessary, but docs aren't necessary clear.
            if (offset.size() == 2) {
                sb.append("icon-offset-x:").append(offset.get(0).doubleValue()).append(SEMI_COLON)
                  .append("icon-offset-y:").append(offset.get(1).doubleValue()).append(SEMI_COLON);
            }
        }
        // icon-opacity
        if (iconImage && paintObject.containsKey("icon-opacity")) {
            final double opacity = paintObject.getJsonNumber("icon-opacity").doubleValue();
            sb.append("icon-opacity:").append(opacity).append(SEMI_COLON);
        }
        // icon-optional
        // icon-padding
        // icon-pitch-alignment
        // icon-rotate
        if (iconImage && layoutObject.containsKey("icon-rotate")) {
            final double rotation = layoutObject.getJsonNumber("icon-rotate").doubleValue();
            sb.append("icon-rotation:").append(rotation).append(SEMI_COLON);
        }
        // icon-rotation-alignment
        // icon-size
        // icon-text-fit
        // icon-text-fit-padding
        // icon-translate
        // icon-translate-anchor
        // symbol-avoid-edges
        // symbol-placement
        // symbol-sort-key
        // symbol-spacing
        // symbol-z-order
        // text-allow-overlap
        // text-anchor
        // text-color
        if (paintObject.containsKey(StyleKeys.TEXT_COLOR)) {
            sb.append(StyleKeys.TEXT_COLOR).append(':').append(paintObject.getString(StyleKeys.TEXT_COLOR)).append(SEMI_COLON);
        }
        // text-field
        if (layoutObject.containsKey("text-field")) {
            sb.append(StyleKeys.TEXT).append(':')
              .append(layoutObject.getString("text-field").replace("}", "").replace("{", ""))
              .append(SEMI_COLON);
        }
        // text-font
        if (layoutObject.containsKey("text-font")) {
            List<String> fonts = layoutObject.getJsonArray("text-font").stream().filter(JsonString.class::isInstance)
              .map(JsonString.class::cast).map(JsonString::getString).collect(Collectors.toList());
            Font[] systemFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
            for (String fontString : fonts) {
                Collection<Font> fontMatches = Stream.of(systemFonts)
                  .filter(font -> Arrays.asList(font.getName(), font.getFontName(), font.getFamily(), font.getPSName()).contains(fontString))
                  .collect(Collectors.toList());
                if (!fontMatches.isEmpty()) {
                    final Font setFont = fontMatches.stream().filter(font -> font.getName().equals(fontString)).findAny()
                      .orElseGet(() -> fontMatches.stream().filter(font -> font.getFontName().equals(fontString)).findAny()
                        .orElseGet(() -> fontMatches.stream().filter(font -> font.getPSName().equals(fontString)).findAny()
                        .orElseGet(() -> fontMatches.stream().filter(font -> font.getFamily().equals(fontString)).findAny().orElse(null))));
                    if (setFont != null) {
                        sb.append(StyleKeys.FONT_FAMILY).append(':').append('"').append(setFont.getFamily()).append('"').append(SEMI_COLON);
                        sb.append(StyleKeys.FONT_WEIGHT).append(':').append(setFont.isBold() ? "bold" : "normal").append(SEMI_COLON);
                        sb.append(StyleKeys.FONT_STYLE).append(':').append(setFont.isItalic() ? "italic" : "normal").append(SEMI_COLON);
                        break;
                    }
                }
            }
        }
        // text-halo-blur
        // text-halo-color
        if (paintObject.containsKey(StyleKeys.TEXT_HALO_COLOR)) {
            sb.append(StyleKeys.TEXT_HALO_COLOR).append(':').append(paintObject.getString(StyleKeys.TEXT_HALO_COLOR)).append(SEMI_COLON);
        }
        // text-halo-width
        if (paintObject.containsKey("text-halo-width")) {
            sb.append(StyleKeys.TEXT_HALO_RADIUS).append(':').append(paintObject.getJsonNumber("text-halo-width").intValue() / 2)
                    .append(SEMI_COLON);
        }
        // text-ignore-placement
        // text-justify
        // text-keep-upright
        // text-letter-spacing
        // text-line-height
        // text-max-angle
        // text-max-width
        // text-offset
        // text-opacity
        if (paintObject.containsKey(StyleKeys.TEXT_OPACITY)) {
            sb.append(StyleKeys.TEXT_OPACITY).append(':').append(paintObject.getJsonNumber(StyleKeys.TEXT_OPACITY).doubleValue())
                    .append(SEMI_COLON);
        }
        // text-optional
        // text-padding
        // text-pitch-alignment
        // text-radial-offset
        // text-rotate
        // text-rotation-alignment
        // text-size
        final JsonNumber textSize = layoutObject.getJsonNumber("text-size");
        sb.append(StyleKeys.FONT_SIZE).append(':').append(textSize != null ? textSize.numberValue().toString() : "16").append(SEMI_COLON);
        // text-transform
        // text-translate
        // text-translate-anchor
        // text-variable-anchor
        // text-writing-mode
        return sb.toString();
    }

    private static String parsePaintBackground(final JsonObject paintObject) {
        final StringBuilder sb = new StringBuilder(20);
        // background-color
        final String bgColor = paintObject.getString("background-color", null);
        if (bgColor != null) {
            sb.append(StyleKeys.FILL_COLOR).append(':').append(bgColor).append(SEMI_COLON);
        }
        // background-opacity
        // background-pattern
        return sb.toString();
    }

    private static String parsePaintFill(final JsonObject paintObject) {
        StringBuilder sb = new StringBuilder(50)
          // fill-antialias
          // fill-color
          .append(StyleKeys.FILL_COLOR).append(':').append(paintObject.getString(StyleKeys.FILL_COLOR, "#000000")).append(SEMI_COLON);
        // fill-opacity
        final JsonNumber opacity = paintObject.getJsonNumber(StyleKeys.FILL_OPACITY);
        sb.append(StyleKeys.FILL_OPACITY).append(':').append(opacity != null ? opacity.numberValue().toString() : "1").append(SEMI_COLON)
          // fill-outline-color
          .append(StyleKeys.COLOR).append(':').append(paintObject.getString("fill-outline-color",
          paintObject.getString("fill-color", "#000000"))).append(SEMI_COLON);
        // fill-pattern
        // fill-sort-key
        // fill-translate
        // fill-translate-anchor
        return sb.toString();
    }

    /**
     * Converts this layer object to a mapcss entry string (to be parsed later)
     * @return The mapcss entry (string form)
     */
    @Override
    public String toString() {
        if (this.filter.toString().isEmpty() && this.paintProperties.isEmpty()) {
            return "";
        } else if (this.type == Type.BACKGROUND) {
            // AFAIK, paint has no zoom levels, and doesn't accept a layer
            return "canvas{" + this.paintProperties + "}";
        }

        final String zoomSelector;
        if (this.minZoom == this.maxZoom) {
            zoomSelector = "|z" + this.minZoom;
        } else if (this.minZoom > Integer.MIN_VALUE && this.maxZoom == Integer.MAX_VALUE) {
            zoomSelector = "|z" + this.minZoom + "-";
        } else if (this.minZoom == Integer.MIN_VALUE && this.maxZoom < Integer.MAX_VALUE) {
            zoomSelector = "|z-" + this.maxZoom;
        } else if (this.minZoom > Integer.MIN_VALUE) {
            zoomSelector = MessageFormat.format("|z{0}-{1}", this.minZoom, this.maxZoom);
        } else {
            zoomSelector = "";
        }
        final String commonData = zoomSelector + this.filter.toString() + "::" + this.id + "{" + this.paintProperties + "}";

        if (this.type == Type.CIRCLE || this.type == Type.SYMBOL) {
            return "node" + commonData;
        } else if (this.type == Type.FILL) {
            return "area" + commonData;
        } else if (this.type == Type.LINE) {
            return "way" + commonData;
        }
        return super.toString();
    }

    /**
     * Get the source that this applies to
     * @return The source name
     */
    public String getSource() {
        return this.source;
    }

    /**
     * Get the layer that this applies to
     * @return The layer name
     */
    public String getSourceLayer() {
        return this.sourceLayer;
    }

    @Override
    public boolean equals(Object other) {
        if (other != null && this.getClass() == other.getClass()) {
            Layers o = (Layers) other;
            return this.type == o.type
              && this.minZoom == o.minZoom
              && this.maxZoom == o.maxZoom
              && Objects.equals(this.id, o.id)
              && Objects.equals(this.styleId, o.styleId)
              && Objects.equals(this.sourceLayer, o.sourceLayer)
              && Objects.equals(this.source, o.source)
              && Objects.equals(this.filter, o.filter)
              && Objects.equals(this.paintProperties, o.paintProperties);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.type, this.minZoom, this.maxZoom, this.id, this.styleId, this.sourceLayer, this.source,
          this.filter, this.paintProperties);
    }
}
