// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Image;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.stream.JsonCollectors;
import javax.swing.ImageIcon;

import org.openstreetmap.gui.jmapviewer.interfaces.Attributed;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.tilesources.AbstractTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource.Mapnik;
import org.openstreetmap.gui.jmapviewer.tilesources.TileSourceInfo;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.StructUtils;
import org.openstreetmap.josm.data.StructUtils.StructEntry;
import org.openstreetmap.josm.io.Capabilities;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.IPreferences;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.StreamUtils;
import org.openstreetmap.josm.tools.Utils;

/**
 * Class that stores info about an image background layer.
 *
 * @author Frederik Ramm
 */
public class ImageryInfo extends TileSourceInfo implements Comparable<ImageryInfo>, Attributed {

    /**
     * Type of imagery entry.
     */
    public enum ImageryType {
        /** A WMS (Web Map Service) entry. **/
        WMS("wms"),
        /** A TMS (Tile Map Service) entry. **/
        TMS("tms"),
        /** TMS entry for Microsoft Bing. */
        BING("bing"),
        /** TMS entry for Russian company <a href="https://wiki.openstreetmap.org/wiki/WikiProject_Russia/kosmosnimki">ScanEx</a>. **/
        SCANEX("scanex"),
        /** A WMS endpoint entry only stores the WMS server info, without layer, which are chosen later by the user. **/
        WMS_ENDPOINT("wms_endpoint"),
        /** WMTS stores GetCapabilities URL. Does not store any information about the layer **/
        WMTS("wmts");

        private final String typeString;

        ImageryType(String typeString) {
            this.typeString = typeString;
        }

        /**
         * Returns the unique string identifying this type.
         * @return the unique string identifying this type
         * @since 6690
         */
        public final String getTypeString() {
            return typeString;
        }

        /**
         * Returns the imagery type from the given type string.
         * @param s The type string
         * @return the imagery type matching the given type string
         */
        public static ImageryType fromString(String s) {
            return Arrays.stream(ImageryType.values())
                    .filter(type -> type.getTypeString().equals(s))
                    .findFirst().orElse(null);
        }
    }

    /**
     * Category of imagery entry.
     * @since 13792
     */
    public enum ImageryCategory {
        /** A aerial or satellite photo. **/
        PHOTO(/* ICON(data/imagery/) */ "photo", tr("Aerial or satellite photo")),
        /** A map of digital terrain model, digital surface model or contour lines. **/
        ELEVATION(/* ICON(data/imagery/) */ "elevation", tr("Elevation map")),
        /** A map. **/
        MAP(/* ICON(data/imagery/) */ "map", tr("Map")),
        /** A historic or otherwise outdated map. */
        HISTORICMAP(/* ICON(data/imagery/) */ "historicmap", tr("Historic or otherwise outdated map")),
        /** A map based on OSM data. **/
        OSMBASEDMAP(/* ICON(data/imagery/) */ "osmbasedmap", tr("Map based on OSM data")),
        /** A historic or otherwise outdated aerial or satellite photo. **/
        HISTORICPHOTO(/* ICON(data/imagery/) */ "historicphoto", tr("Historic or otherwise outdated aerial or satellite photo")),
        /** A map for quality assurance **/
        QUALITY_ASSURANCE(/* ICON(data/imagery/) */ "qa", tr("Map for quality assurance")),
        /** Any other type of imagery **/
        OTHER(/* ICON(data/imagery/) */ "other", tr("Imagery not matching any other category"));

        private final String category;
        private final String description;
        private static final Map<ImageSizes, Map<ImageryCategory, ImageIcon>> iconCache =
                Collections.synchronizedMap(new EnumMap<>(ImageSizes.class));

        ImageryCategory(String category, String description) {
            this.category = category;
            this.description = description;
        }

        /**
         * Returns the unique string identifying this category.
         * @return the unique string identifying this category
         */
        public final String getCategoryString() {
            return category;
        }

        /**
         * Returns the description of this category.
         * @return the description of this category
         */
        public final String getDescription() {
            return description;
        }

        /**
         * Returns the category icon at the given size.
         * @param size icon wanted size
         * @return the category icon at the given size
         * @since 15049
         */
        public final ImageIcon getIcon(ImageSizes size) {
            return iconCache
                    .computeIfAbsent(size, x -> Collections.synchronizedMap(new EnumMap<>(ImageryCategory.class)))
                    .computeIfAbsent(this, x -> ImageProvider.get("data/imagery", x.category, size));
        }

        /**
         * Returns the imagery category from the given category string.
         * @param s The category string
         * @return the imagery category matching the given category string
         */
        public static ImageryCategory fromString(String s) {
            return Arrays.stream(ImageryCategory.values())
                    .filter(category -> category.getCategoryString().equals(s))
                    .findFirst().orElse(null);
        }
    }

    /**
     * Multi-polygon bounds for imagery backgrounds.
     * Used to display imagery coverage in preferences and to determine relevant imagery entries based on edit location.
     */
    public static class ImageryBounds extends Bounds {

        /**
         * Constructs a new {@code ImageryBounds} from string.
         * @param asString The string containing the list of shapes defining this bounds
         * @param separator The shape separator in the given string, usually a comma
         */
        public ImageryBounds(String asString, String separator) {
            super(asString, separator);
        }

        private List<Shape> shapes = new ArrayList<>();

        /**
         * Adds a new shape to this bounds.
         * @param shape The shape to add
         */
        public final void addShape(Shape shape) {
            this.shapes.add(shape);
        }

        /**
         * Sets the list of shapes defining this bounds.
         * @param shapes The list of shapes defining this bounds.
         */
        public final void setShapes(List<Shape> shapes) {
            this.shapes = shapes;
        }

        /**
         * Returns the list of shapes defining this bounds.
         * @return The list of shapes defining this bounds
         */
        public final List<Shape> getShapes() {
            return shapes;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), shapes);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            ImageryBounds that = (ImageryBounds) o;
            return Objects.equals(shapes, that.shapes);
        }
    }

    /** original name of the imagery entry in case of translation call, for multiple languages English when possible */
    private String origName;
    /** (original) language of the translated name entry */
    private String langName;
    /** whether this is a entry activated by default or not */
    private boolean defaultEntry;
    /** Whether this service requires a explicit EULA acceptance before it can be activated */
    private String eulaAcceptanceRequired;
    /** type of the imagery servics - WMS, TMS, ... */
    private ImageryType imageryType = ImageryType.WMS;
    private double pixelPerDegree;
    /** maximum zoom level for TMS imagery */
    private int defaultMaxZoom;
    /** minimum zoom level for TMS imagery */
    private int defaultMinZoom;
    /** display bounds of imagery, displayed in prefs and used for automatic imagery selection */
    private ImageryBounds bounds;
    /** projections supported by WMS servers */
    private List<String> serverProjections = Collections.emptyList();
    /** description of the imagery entry, should contain notes what type of data it is */
    private String description;
    /** language of the description entry */
    private String langDescription;
    /** Text of a text attribution displayed when using the imagery */
    private String attributionText;
    /** Link to the privacy policy of the operator */
    private String privacyPolicyURL;
    /** Link to a reference stating the permission for OSM usage */
    private String permissionReferenceURL;
    /** Link behind the text attribution displayed when using the imagery */
    private String attributionLinkURL;
    /** Image of a graphical attribution displayed when using the imagery */
    private String attributionImage;
    /** Link behind the graphical attribution displayed when using the imagery */
    private String attributionImageURL;
    /** Text with usage terms displayed when using the imagery */
    private String termsOfUseText;
    /** Link behind the text with usage terms displayed when using the imagery */
    private String termsOfUseURL;
    /** country code of the imagery (for country specific imagery) */
    private String countryCode = "";
    /**
      * creation date of the imagery (in the form YYYY-MM-DD;YYYY-MM-DD, where
      * DD and MM as well as a second date are optional).
      *
      * Also used as time filter for WMS time={time} parameter (such as Sentinel-2)
      * @since 11570
      */
    private String date;
    /**
      * marked as best in other editors
      * @since 11575
      */
    private boolean bestMarked;
    /**
      * marked as overlay
      * @since 13536
      */
    private boolean overlay;
    /**
      * list of old IDs, only for loading, not handled anywhere else
      * @since 13536
      */
    private Collection<String> oldIds;
    /** mirrors of different type for this entry */
    private List<ImageryInfo> mirrors;
    /** icon used in menu */
    private String icon;
    /** is the geo reference correct - don't offer offset handling */
    private boolean isGeoreferenceValid;
    /** which layers should be activated by default on layer addition. **/
    private List<DefaultLayer> defaultLayers = Collections.emptyList();
    /** HTTP headers **/
    private Map<String, String> customHttpHeaders = Collections.emptyMap();
    /** Should this map be transparent **/
    private boolean transparent = true;
    private int minimumTileExpire = (int) TimeUnit.MILLISECONDS.toSeconds(TMSCachedTileLoaderJob.MINIMUM_EXPIRES.get());
    /** category of the imagery */
    private ImageryCategory category;
    /** category of the imagery (input string, not saved, copied or used otherwise except for error checks) */
    private String categoryOriginalString;
    /** when adding a field, also adapt the:
     * {@link #ImageryPreferenceEntry ImageryPreferenceEntry object}
     * {@link #ImageryPreferenceEntry#ImageryPreferenceEntry(ImageryInfo) ImageryPreferenceEntry constructor}
     * {@link #ImageryInfo(ImageryPreferenceEntry) ImageryInfo constructor}
     * {@link #ImageryInfo(ImageryInfo) ImageryInfo constructor}
     * {@link #equalsPref(ImageryPreferenceEntry) equalsPref method}
     **/

    /**
     * Auxiliary class to save an {@link ImageryInfo} object in the preferences.
     */
    public static class ImageryPreferenceEntry {
        @StructEntry String name;
        @StructEntry String d;
        @StructEntry String id;
        @StructEntry String type;
        @StructEntry String url;
        @StructEntry double pixel_per_eastnorth;
        @StructEntry String eula;
        @StructEntry String attribution_text;
        @StructEntry String attribution_url;
        @StructEntry String permission_reference_url;
        @StructEntry String logo_image;
        @StructEntry String logo_url;
        @StructEntry String terms_of_use_text;
        @StructEntry String terms_of_use_url;
        @StructEntry String country_code = "";
        @StructEntry String date;
        @StructEntry int max_zoom;
        @StructEntry int min_zoom;
        @StructEntry String cookies;
        @StructEntry String bounds;
        @StructEntry String shapes;
        @StructEntry String projections;
        @StructEntry String icon;
        @StructEntry String description;
        @StructEntry MultiMap<String, String> noTileHeaders;
        @StructEntry MultiMap<String, String> noTileChecksums;
        @StructEntry int tileSize = -1;
        @StructEntry Map<String, String> metadataHeaders;
        @StructEntry boolean valid_georeference;
        @StructEntry boolean bestMarked;
        @StructEntry boolean modTileFeatures;
        @StructEntry boolean overlay;
        @StructEntry String default_layers;
        @StructEntry Map<String, String> customHttpHeaders;
        @StructEntry boolean transparent;
        @StructEntry int minimumTileExpire;
        @StructEntry String category;

        /**
         * Constructs a new empty WMS {@code ImageryPreferenceEntry}.
         */
        public ImageryPreferenceEntry() {
            // Do nothing
        }

        /**
         * Constructs a new {@code ImageryPreferenceEntry} from a given {@code ImageryInfo}.
         * @param i The corresponding imagery info
         */
        public ImageryPreferenceEntry(ImageryInfo i) {
            name = i.name;
            id = i.id;
            type = i.imageryType.getTypeString();
            url = i.url;
            pixel_per_eastnorth = i.pixelPerDegree;
            eula = i.eulaAcceptanceRequired;
            attribution_text = i.attributionText;
            attribution_url = i.attributionLinkURL;
            permission_reference_url = i.permissionReferenceURL;
            date = i.date;
            bestMarked = i.bestMarked;
            overlay = i.overlay;
            logo_image = i.attributionImage;
            logo_url = i.attributionImageURL;
            terms_of_use_text = i.termsOfUseText;
            terms_of_use_url = i.termsOfUseURL;
            country_code = i.countryCode;
            max_zoom = i.defaultMaxZoom;
            min_zoom = i.defaultMinZoom;
            cookies = i.cookies;
            icon = intern(i.icon);
            description = i.description;
            category = i.category != null ? i.category.getCategoryString() : null;
            if (i.bounds != null) {
                bounds = i.bounds.encodeAsString(",");
                String shapesString = Shape.encodeAsString(i.bounds.getShapes());
                if (!shapesString.isEmpty()) {
                    shapes = shapesString;
                }
            }
            if (!i.serverProjections.isEmpty()) {
                projections = String.join(",", i.serverProjections);
            }
            if (i.noTileHeaders != null && !i.noTileHeaders.isEmpty()) {
                noTileHeaders = new MultiMap<>(i.noTileHeaders);
            }

            if (i.noTileChecksums != null && !i.noTileChecksums.isEmpty()) {
                noTileChecksums = new MultiMap<>(i.noTileChecksums);
            }

            if (i.metadataHeaders != null && !i.metadataHeaders.isEmpty()) {
                metadataHeaders = i.metadataHeaders;
            }

            tileSize = i.getTileSize();

            valid_georeference = i.isGeoreferenceValid();
            modTileFeatures = i.isModTileFeatures();
            if (!i.defaultLayers.isEmpty()) {
                default_layers = i.defaultLayers.stream().map(DefaultLayer::toJson).collect(JsonCollectors.toJsonArray()).toString();
            }
            customHttpHeaders = i.customHttpHeaders;
            transparent = i.isTransparent();
            minimumTileExpire = i.minimumTileExpire;
        }

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder("ImageryPreferenceEntry [name=").append(name);
            if (id != null) {
                s.append(" id=").append(id);
            }
            s.append(']');
            return s.toString();
        }
    }

    /**
     * Constructs a new WMS {@code ImageryInfo}.
     */
    public ImageryInfo() {
        super();
    }

    /**
     * Constructs a new WMS {@code ImageryInfo} with a given name.
     * @param name The entry name
     */
    public ImageryInfo(String name) {
        super(name);
    }

    /**
     * Constructs a new WMS {@code ImageryInfo} with given name and extended URL.
     * @param name The entry name
     * @param url The entry extended URL
     */
    public ImageryInfo(String name, String url) {
        this(name);
        setExtendedUrl(url);
    }

    /**
     * Constructs a new WMS {@code ImageryInfo} with given name, extended and EULA URLs.
     * @param name The entry name
     * @param url The entry URL
     * @param eulaAcceptanceRequired The EULA URL
     */
    public ImageryInfo(String name, String url, String eulaAcceptanceRequired) {
        this(name);
        setExtendedUrl(url);
        this.eulaAcceptanceRequired = eulaAcceptanceRequired;
    }

    /**
     * Constructs a new {@code ImageryInfo} with given name, url, extended and EULA URLs.
     * @param name The entry name
     * @param url The entry URL
     * @param type The entry imagery type. If null, WMS will be used as default
     * @param eulaAcceptanceRequired The EULA URL
     * @param cookies The data part of HTTP cookies header in case the service requires cookies to work
     * @throws IllegalArgumentException if type refers to an unknown imagery type
     */
    public ImageryInfo(String name, String url, String type, String eulaAcceptanceRequired, String cookies) {
        this(name);
        setExtendedUrl(url);
        ImageryType t = ImageryType.fromString(type);
        this.cookies = cookies;
        this.eulaAcceptanceRequired = eulaAcceptanceRequired;
        if (t != null) {
            this.imageryType = t;
        } else if (type != null && !type.isEmpty()) {
            throw new IllegalArgumentException("unknown type: "+type);
        }
    }

    /**
     * Constructs a new {@code ImageryInfo} with given name, url, id, extended and EULA URLs.
     * @param name The entry name
     * @param url The entry URL
     * @param type The entry imagery type. If null, WMS will be used as default
     * @param eulaAcceptanceRequired The EULA URL
     * @param cookies The data part of HTTP cookies header in case the service requires cookies to work
     * @param id tile id
     * @throws IllegalArgumentException if type refers to an unknown imagery type
     */
    public ImageryInfo(String name, String url, String type, String eulaAcceptanceRequired, String cookies, String id) {
        this(name, url, type, eulaAcceptanceRequired, cookies);
        setId(id);
    }

    /**
     * Constructs a new {@code ImageryInfo} from an imagery preference entry.
     * @param e The imagery preference entry
     */
    public ImageryInfo(ImageryPreferenceEntry e) {
        super(e.name, e.url, e.id);
        CheckParameterUtil.ensureParameterNotNull(e.name, "name");
        CheckParameterUtil.ensureParameterNotNull(e.url, "url");
        description = e.description;
        cookies = e.cookies;
        eulaAcceptanceRequired = e.eula;
        imageryType = ImageryType.fromString(e.type);
        if (imageryType == null) throw new IllegalArgumentException("unknown type");
        pixelPerDegree = e.pixel_per_eastnorth;
        defaultMaxZoom = e.max_zoom;
        defaultMinZoom = e.min_zoom;
        if (e.bounds != null) {
            bounds = new ImageryBounds(e.bounds, ",");
            if (e.shapes != null) {
                try {
                    for (String s : e.shapes.split(";")) {
                        bounds.addShape(new Shape(s, ","));
                    }
                } catch (IllegalArgumentException ex) {
                    Logging.warn(ex);
                }
            }
        }
        if (e.projections != null && !e.projections.isEmpty()) {
            // split generates null element on empty string which gives one element Array[null]
            setServerProjections(Arrays.asList(e.projections.split(",")));
        }
        attributionText = intern(e.attribution_text);
        attributionLinkURL = e.attribution_url;
        permissionReferenceURL = e.permission_reference_url;
        attributionImage = e.logo_image;
        attributionImageURL = e.logo_url;
        date = e.date;
        bestMarked = e.bestMarked;
        overlay = e.overlay;
        termsOfUseText = e.terms_of_use_text;
        termsOfUseURL = e.terms_of_use_url;
        countryCode = intern(e.country_code);
        icon = intern(e.icon);
        if (e.noTileHeaders != null) {
            noTileHeaders = e.noTileHeaders.toMap();
        }
        if (e.noTileChecksums != null) {
            noTileChecksums = e.noTileChecksums.toMap();
        }
        setTileSize(e.tileSize);
        metadataHeaders = e.metadataHeaders;
        isGeoreferenceValid = e.valid_georeference;
        modTileFeatures = e.modTileFeatures;
        if (e.default_layers != null) {
            try (JsonReader jsonReader = Json.createReader(new StringReader(e.default_layers))) {
                defaultLayers = jsonReader.
                        readArray().
                        stream().
                        map(x -> DefaultLayer.fromJson((JsonObject) x, imageryType)).
                        collect(Collectors.toList());
            }
        }
        setCustomHttpHeaders(e.customHttpHeaders);
        transparent = e.transparent;
        minimumTileExpire = e.minimumTileExpire;
        category = ImageryCategory.fromString(e.category);
    }

    /**
     * Constructs a new {@code ImageryInfo} from an existing one.
     * @param i The other imagery info
     */
    public ImageryInfo(ImageryInfo i) {
        super(i.name, i.url, i.id);
        this.noTileHeaders = i.noTileHeaders;
        this.noTileChecksums = i.noTileChecksums;
        this.minZoom = i.minZoom;
        this.maxZoom = i.maxZoom;
        this.cookies = i.cookies;
        this.tileSize = i.tileSize;
        this.metadataHeaders = i.metadataHeaders;
        this.modTileFeatures = i.modTileFeatures;

        this.origName = i.origName;
        this.langName = i.langName;
        this.defaultEntry = i.defaultEntry;
        this.eulaAcceptanceRequired = null;
        this.imageryType = i.imageryType;
        this.pixelPerDegree = i.pixelPerDegree;
        this.defaultMaxZoom = i.defaultMaxZoom;
        this.defaultMinZoom = i.defaultMinZoom;
        this.bounds = i.bounds;
        this.serverProjections = i.serverProjections;
        this.description = i.description;
        this.langDescription = i.langDescription;
        this.attributionText = i.attributionText;
        this.privacyPolicyURL = i.privacyPolicyURL;
        this.permissionReferenceURL = i.permissionReferenceURL;
        this.attributionLinkURL = i.attributionLinkURL;
        this.attributionImage = i.attributionImage;
        this.attributionImageURL = i.attributionImageURL;
        this.termsOfUseText = i.termsOfUseText;
        this.termsOfUseURL = i.termsOfUseURL;
        this.countryCode = i.countryCode;
        this.date = i.date;
        this.bestMarked = i.bestMarked;
        this.overlay = i.overlay;
        // do not copy field {@code mirrors}
        this.icon = intern(i.icon);
        this.isGeoreferenceValid = i.isGeoreferenceValid;
        setDefaultLayers(i.defaultLayers);
        setCustomHttpHeaders(i.customHttpHeaders);
        this.transparent = i.transparent;
        this.minimumTileExpire = i.minimumTileExpire;
        this.categoryOriginalString = intern(i.categoryOriginalString);
        this.category = i.category;
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, imageryType);
    }

    /**
     * Check if this object equals another ImageryInfo with respect to the properties
     * that get written to the preference file.
     *
     * The field {@link #pixelPerDegree} is ignored.
     *
     * @param other the ImageryInfo object to compare to
     * @return true if they are equal
     */
    public boolean equalsPref(ImageryInfo other) {
        if (other == null) {
            return false;
        }

        // CHECKSTYLE.OFF: BooleanExpressionComplexity
        return
                Objects.equals(this.name, other.name) &&
                Objects.equals(this.id, other.id) &&
                Objects.equals(this.url, other.url) &&
                Objects.equals(this.modTileFeatures, other.modTileFeatures) &&
                Objects.equals(this.bestMarked, other.bestMarked) &&
                Objects.equals(this.overlay, other.overlay) &&
                Objects.equals(this.isGeoreferenceValid, other.isGeoreferenceValid) &&
                Objects.equals(this.cookies, other.cookies) &&
                Objects.equals(this.eulaAcceptanceRequired, other.eulaAcceptanceRequired) &&
                Objects.equals(this.imageryType, other.imageryType) &&
                Objects.equals(this.defaultMaxZoom, other.defaultMaxZoom) &&
                Objects.equals(this.defaultMinZoom, other.defaultMinZoom) &&
                Objects.equals(this.bounds, other.bounds) &&
                Objects.equals(this.serverProjections, other.serverProjections) &&
                Objects.equals(this.attributionText, other.attributionText) &&
                Objects.equals(this.attributionLinkURL, other.attributionLinkURL) &&
                Objects.equals(this.permissionReferenceURL, other.permissionReferenceURL) &&
                Objects.equals(this.attributionImageURL, other.attributionImageURL) &&
                Objects.equals(this.attributionImage, other.attributionImage) &&
                Objects.equals(this.termsOfUseText, other.termsOfUseText) &&
                Objects.equals(this.termsOfUseURL, other.termsOfUseURL) &&
                Objects.equals(this.countryCode, other.countryCode) &&
                Objects.equals(this.date, other.date) &&
                Objects.equals(this.icon, other.icon) &&
                Objects.equals(this.description, other.description) &&
                Objects.equals(this.noTileHeaders, other.noTileHeaders) &&
                Objects.equals(this.noTileChecksums, other.noTileChecksums) &&
                Objects.equals(this.metadataHeaders, other.metadataHeaders) &&
                Objects.equals(this.defaultLayers, other.defaultLayers) &&
                Objects.equals(this.customHttpHeaders, other.customHttpHeaders) &&
                Objects.equals(this.transparent, other.transparent) &&
                Objects.equals(this.minimumTileExpire, other.minimumTileExpire) &&
                Objects.equals(this.category, other.category);
        // CHECKSTYLE.ON: BooleanExpressionComplexity
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageryInfo that = (ImageryInfo) o;
        return imageryType == that.imageryType && Objects.equals(url, that.url);
    }

    private static final Map<String, String> localizedCountriesCache = new HashMap<>();
    static {
        localizedCountriesCache.put("", tr("Worldwide"));
    }

    /**
     * Returns a localized name for the given country code, or "Worldwide" if empty.
     * This function falls back on the English name, and uses the ISO code as a last-resortvalue.
     *
     * @param countryCode An ISO 3166 alpha-2 country code or a UN M.49 numeric-3 area code
     * @return The name of the country appropriate to the current locale.
     * @see Locale#getDisplayCountry
     * @since 15158
     */
    public static String getLocalizedCountry(String countryCode) {
        return localizedCountriesCache.computeIfAbsent(countryCode, code -> new Locale("en", code).getDisplayCountry());
    }

    @Override
    public String toString() {
        // Used in imagery preferences filtering, so must be efficient
        return new StringBuilder(name)
                .append('[').append(countryCode)
                // appending the localized country in toString() allows us to filter imagery preferences table with it!
                .append("] ('").append(getLocalizedCountry(countryCode)).append(')')
                .append(" - ").append(url)
                .append(" - ").append(imageryType)
                .toString();
    }

    @Override
    public int compareTo(ImageryInfo in) {
        int i = countryCode.compareTo(in.countryCode);
        if (i == 0) {
            i = name.toLowerCase(Locale.ENGLISH).compareTo(in.name.toLowerCase(Locale.ENGLISH));
        }
        if (i == 0) {
            i = url.compareTo(in.url);
        }
        if (i == 0) {
            i = Double.compare(pixelPerDegree, in.pixelPerDegree);
        }
        return i;
    }

    /**
     * Determines if URL is equal to given imagery info.
     * @param in imagery info
     * @return {@code true} if URL is equal to given imagery info
     */
    public boolean equalsBaseValues(ImageryInfo in) {
        return url.equals(in.url);
    }

    /**
     * Sets the pixel per degree value.
     * @param ppd The ppd value
     * @see #getPixelPerDegree()
     */
    public void setPixelPerDegree(double ppd) {
        this.pixelPerDegree = ppd;
    }

    /**
     * Sets the maximum zoom level.
     * @param defaultMaxZoom The maximum zoom level
     */
    public void setDefaultMaxZoom(int defaultMaxZoom) {
        this.defaultMaxZoom = defaultMaxZoom;
    }

    /**
     * Sets the minimum zoom level.
     * @param defaultMinZoom The minimum zoom level
     */
    public void setDefaultMinZoom(int defaultMinZoom) {
        this.defaultMinZoom = defaultMinZoom;
    }

    /**
     * Sets the imagery polygonial bounds.
     * @param b The imagery bounds (non-rectangular)
     */
    public void setBounds(ImageryBounds b) {
        this.bounds = b;
    }

    /**
     * Returns the imagery polygonial bounds.
     * @return The imagery bounds (non-rectangular)
     */
    public ImageryBounds getBounds() {
        return bounds;
    }

    @Override
    public boolean requiresAttribution() {
        return attributionText != null || attributionLinkURL != null || attributionImage != null
                || termsOfUseText != null || termsOfUseURL != null;
    }

    @Override
    public String getAttributionText(int zoom, ICoordinate topLeft, ICoordinate botRight) {
        return attributionText;
    }

    @Override
    public String getAttributionLinkURL() {
        return attributionLinkURL;
    }

    /**
     * Return the permission reference URL.
     * @return The url
     * @see #setPermissionReferenceURL
     * @since 11975
     */
    public String getPermissionReferenceURL() {
        return permissionReferenceURL;
    }

    /**
     * Return the privacy policy URL.
     * @return The url
     * @see #setPrivacyPolicyURL
     * @since 16127
     */
    public String getPrivacyPolicyURL() {
        return privacyPolicyURL;
    }

    @Override
    public Image getAttributionImage() {
        ImageIcon i = ImageProvider.getIfAvailable(attributionImage);
        if (i != null) {
            return i.getImage();
        }
        return null;
    }

    /**
     * Return the raw attribution logo information (an URL to the image).
     * @return The url text
     * @since 12257
     */
    public String getAttributionImageRaw() {
        return attributionImage;
    }

    @Override
    public String getAttributionImageURL() {
        return attributionImageURL;
    }

    @Override
    public String getTermsOfUseText() {
        return termsOfUseText;
    }

    @Override
    public String getTermsOfUseURL() {
        return termsOfUseURL;
    }

    /**
     * Set the attribution text
     * @param text The text
     * @see #getAttributionText(int, ICoordinate, ICoordinate)
     */
    public void setAttributionText(String text) {
        attributionText = intern(text);
    }

    /**
     * Set the attribution image
     * @param url The url of the image.
     * @see #getAttributionImageURL()
     */
    public void setAttributionImageURL(String url) {
        attributionImageURL = url;
    }

    /**
     * Set the image for the attribution
     * @param res The image resource
     * @see #getAttributionImage()
     */
    public void setAttributionImage(String res) {
        attributionImage = res;
    }

    /**
     * Sets the URL the attribution should link to.
     * @param url The url.
     * @see #getAttributionLinkURL()
     */
    public void setAttributionLinkURL(String url) {
        attributionLinkURL = url;
    }

    /**
     * Sets the permission reference URL.
     * @param url The url.
     * @see #getPermissionReferenceURL()
     * @since 11975
     */
    public void setPermissionReferenceURL(String url) {
        permissionReferenceURL = url;
    }

    /**
     * Sets the privacy policy URL.
     * @param url The url.
     * @see #getPrivacyPolicyURL()
     * @since 16127
     */
    public void setPrivacyPolicyURL(String url) {
        privacyPolicyURL = url;
    }

    /**
     * Sets the text to display to the user as terms of use.
     * @param text The text
     * @see #getTermsOfUseText()
     */
    public void setTermsOfUseText(String text) {
        termsOfUseText = text;
    }

    /**
     * Sets a url that links to the terms of use text.
     * @param text The url.
     * @see #getTermsOfUseURL()
     */
    public void setTermsOfUseURL(String text) {
        termsOfUseURL = text;
    }

    /**
     * Sets the extended URL of this entry.
     * @param url Entry extended URL containing in addition of service URL, its type and min/max zoom info
     */
    public void setExtendedUrl(String url) {
        CheckParameterUtil.ensureParameterNotNull(url);

        // Default imagery type is WMS
        this.url = url;
        this.imageryType = ImageryType.WMS;

        defaultMaxZoom = 0;
        defaultMinZoom = 0;
        for (ImageryType type : ImageryType.values()) {
            Matcher m = Pattern.compile(type.getTypeString()+"(?:\\[(?:(\\d+)[,-])?(\\d+)\\])?:(.*)").matcher(url);
            if (m.matches()) {
                this.url = m.group(3);
                this.imageryType = type;
                if (m.group(2) != null) {
                    defaultMaxZoom = Integer.parseInt(m.group(2));
                }
                if (m.group(1) != null) {
                    defaultMinZoom = Integer.parseInt(m.group(1));
                }
                break;
            }
        }

        if (serverProjections.isEmpty()) {
            Matcher m = Pattern.compile(".*\\{PROJ\\(([^)}]+)\\)\\}.*").matcher(url.toUpperCase(Locale.ENGLISH));
            if (m.matches()) {
                setServerProjections(Arrays.asList(m.group(1).split(",")));
            }
        }
    }

    /**
     * Returns the entry name.
     * @return The entry name
     * @since 6968
     */
    public String getOriginalName() {
        return this.origName != null ? this.origName : this.name;
    }

    /**
     * Sets the entry name and handle translation.
     * @param language The used language
     * @param name The entry name
     * @since 8091
     */
    public void setName(String language, String name) {
        boolean isdefault = LanguageInfo.getJOSMLocaleCode(null).equals(language);
        if (LanguageInfo.isBetterLanguage(langName, language)) {
            this.name = isdefault ? tr(name) : name;
            this.langName = language;
        }
        if (origName == null || isdefault) {
            this.origName = name;
        }
    }

    /**
     * Store the id of this info to the preferences and clear it afterwards.
     */
    public void clearId() {
        if (this.id != null) {
            Collection<String> newAddedIds = new TreeSet<>(Config.getPref().getList("imagery.layers.addedIds"));
            newAddedIds.add(this.id);
            Config.getPref().putList("imagery.layers.addedIds", new ArrayList<>(newAddedIds));
        }
        setId(null);
    }

    /**
     * Determines if this entry is enabled by default.
     * @return {@code true} if this entry is enabled by default, {@code false} otherwise
     */
    public boolean isDefaultEntry() {
        return defaultEntry;
    }

    /**
     * Sets the default state of this entry.
     * @param defaultEntry {@code true} if this entry has to be enabled by default, {@code false} otherwise
     */
    public void setDefaultEntry(boolean defaultEntry) {
        this.defaultEntry = defaultEntry;
    }

    /**
     * Gets the pixel per degree value
     * @return The ppd value.
     */
    public double getPixelPerDegree() {
        return this.pixelPerDegree;
    }

    /**
     * Returns the maximum zoom level.
     * @return The maximum zoom level
     */
    @Override
    public int getMaxZoom() {
        return this.defaultMaxZoom;
    }

    /**
     * Returns the minimum zoom level.
     * @return The minimum zoom level
     */
    @Override
    public int getMinZoom() {
        return this.defaultMinZoom;
    }

    /**
     * Returns the description text when existing.
     * @return The description
     * @since 8065
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Sets the description text when existing.
     * @param language The used language
     * @param description the imagery description text
     * @since 8091
     */
    public void setDescription(String language, String description) {
        boolean isdefault = LanguageInfo.getJOSMLocaleCode(null).equals(language);
        if (LanguageInfo.isBetterLanguage(langDescription, language)) {
            this.description = isdefault ? tr(description) : description;
            this.langDescription = intern(language);
        }
    }

    /**
     * Return the sorted list of activated Imagery IDs.
     * @return sorted list of activated Imagery IDs
     * @since 13536
     */
    public static Collection<String> getActiveIds() {
        IPreferences pref = Config.getPref();
        if (pref == null) {
            return Collections.emptyList();
        }
        List<ImageryPreferenceEntry> entries = StructUtils.getListOfStructs(pref, "imagery.entries", null, ImageryPreferenceEntry.class);
        if (entries == null) {
            return Collections.emptyList();
        }
        return entries.stream()
                .filter(prefEntry -> prefEntry.id != null && !prefEntry.id.isEmpty())
                .map(prefEntry -> prefEntry.id)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Returns a tool tip text for display.
     * @return The text
     * @since 8065
     */
    public String getToolTipText() {
        StringBuilder res = new StringBuilder(getName());
        boolean html = false;
        String dateStr = getDate();
        if (dateStr != null && !dateStr.isEmpty()) {
            res.append("<br>").append(tr("Date of imagery: {0}", dateStr));
            html = true;
        }
        if (category != null && category.getDescription() != null) {
            res.append("<br>").append(tr("Imagery category: {0}", category.getDescription()));
            html = true;
        }
        if (bestMarked) {
            res.append("<br>").append(tr("This imagery is marked as best in this region in other editors."));
            html = true;
        }
        if (overlay) {
            res.append("<br>").append(tr("This imagery is an overlay."));
            html = true;
        }
        String desc = getDescription();
        if (desc != null && !desc.isEmpty()) {
            res.append("<br>").append(Utils.escapeReservedCharactersHTML(desc));
            html = true;
        }
        if (html) {
            res.insert(0, "<html>").append("</html>");
        }
        return res.toString();
    }

    /**
     * Returns the EULA acceptance URL, if any.
     * @return The URL to an EULA text that has to be accepted before use, or {@code null}
     */
    public String getEulaAcceptanceRequired() {
        return eulaAcceptanceRequired;
    }

    /**
     * Sets the EULA acceptance URL.
     * @param eulaAcceptanceRequired The URL to an EULA text that has to be accepted before use
     */
    public void setEulaAcceptanceRequired(String eulaAcceptanceRequired) {
        this.eulaAcceptanceRequired = eulaAcceptanceRequired;
    }

    /**
     * Returns the ISO 3166-1-alpha-2 country code.
     * @return The country code (2 letters)
     */
    public String getCountryCode() {
        return countryCode;
    }

    /**
     * Sets the ISO 3166-1-alpha-2 country code.
     * @param countryCode The country code (2 letters)
     */
    public void setCountryCode(String countryCode) {
        this.countryCode = intern(countryCode);
    }

    /**
     * Returns the date information.
     * @return The date (in the form YYYY-MM-DD;YYYY-MM-DD, where
     * DD and MM as well as a second date are optional)
     * @since 11570
     */
    public String getDate() {
        return date;
    }

    /**
     * Sets the date information.
     * @param date The date information
     * @since 11570
     */
    public void setDate(String date) {
        this.date = date;
    }

    /**
     * Returns the entry icon.
     * @return The entry icon
     */
    public String getIcon() {
        return icon;
    }

    /**
     * Sets the entry icon.
     * @param icon The entry icon
     */
    public void setIcon(String icon) {
        this.icon = intern(icon);
    }

    /**
     * Get the projections supported by the server. Only relevant for
     * WMS-type ImageryInfo at the moment.
     * @return null, if no projections have been specified; the list
     * of supported projections otherwise.
     */
    public List<String> getServerProjections() {
        return Collections.unmodifiableList(serverProjections);
    }

    /**
     * Sets the list of collections the server supports
     * @param serverProjections The list of supported projections
     */
    public void setServerProjections(Collection<String> serverProjections) {
        CheckParameterUtil.ensureParameterNotNull(serverProjections, "serverProjections");
        this.serverProjections = serverProjections.stream()
                .map(String::intern)
                .collect(StreamUtils.toUnmodifiableList());
    }

    /**
     * Returns the extended URL, containing in addition of service URL, its type and min/max zoom info.
     * @return The extended URL
     */
    public String getExtendedUrl() {
        return imageryType.getTypeString() + (defaultMaxZoom != 0
            ? ('['+(defaultMinZoom != 0 ? (Integer.toString(defaultMinZoom) + ',') : "")+defaultMaxZoom+']') : "") + ':' + url;
    }

    /**
     * Gets a unique toolbar key to store this layer as toolbar item
     * @return The kay.
     */
    public String getToolbarName() {
        String res = name;
        if (pixelPerDegree != 0) {
            res += "#PPD="+pixelPerDegree;
        }
        return res;
    }

    /**
     * Gets the name that should be displayed in the menu to add this imagery layer.
     * @return The text.
     */
    public String getMenuName() {
        String res = name;
        if (pixelPerDegree != 0) {
            res += " ("+pixelPerDegree+')';
        }
        return res;
    }

    /**
     * Determines if this entry requires attribution.
     * @return {@code true} if some attribution text has to be displayed, {@code false} otherwise
     */
    public boolean hasAttribution() {
        return attributionText != null;
    }

    /**
     * Copies attribution from another {@code ImageryInfo}.
     * @param i The other imagery info to get attribution from
     */
    public void copyAttribution(ImageryInfo i) {
        this.attributionImage = i.attributionImage;
        this.attributionImageURL = i.attributionImageURL;
        this.attributionText = i.attributionText;
        this.attributionLinkURL = i.attributionLinkURL;
        this.termsOfUseText = i.termsOfUseText;
        this.termsOfUseURL = i.termsOfUseURL;
    }

    /**
     * Applies the attribution from this object to a tile source.
     * @param s The tile source
     */
    public void setAttribution(AbstractTileSource s) {
        if (attributionText != null) {
            if ("osm".equals(attributionText)) {
                s.setAttributionText(new Mapnik().getAttributionText(0, null, null));
            } else {
                s.setAttributionText(attributionText);
            }
        }
        if (attributionLinkURL != null) {
            if ("osm".equals(attributionLinkURL)) {
                s.setAttributionLinkURL(new Mapnik().getAttributionLinkURL());
            } else {
                s.setAttributionLinkURL(attributionLinkURL);
            }
        }
        if (attributionImage != null) {
            ImageIcon i = ImageProvider.getIfAvailable(null, attributionImage);
            if (i != null) {
                s.setAttributionImage(i.getImage());
            }
        }
        if (attributionImageURL != null) {
            s.setAttributionImageURL(attributionImageURL);
        }
        if (termsOfUseText != null) {
            s.setTermsOfUseText(termsOfUseText);
        }
        if (termsOfUseURL != null) {
            if ("osm".equals(termsOfUseURL)) {
                s.setTermsOfUseURL(new Mapnik().getTermsOfUseURL());
            } else {
                s.setTermsOfUseURL(termsOfUseURL);
            }
        }
    }

    /**
     * Returns the imagery type.
     * @return The imagery type
     */
    public ImageryType getImageryType() {
        return imageryType;
    }

    /**
     * Sets the imagery type.
     * @param imageryType The imagery type
     */
    public void setImageryType(ImageryType imageryType) {
        this.imageryType = imageryType;
    }

    /**
     * Returns the imagery category.
     * @return The imagery category
     * @since 13792
     */
    public ImageryCategory getImageryCategory() {
        return category;
    }

    /**
     * Sets the imagery category.
     * @param category The imagery category
     * @since 13792
     */
    public void setImageryCategory(ImageryCategory category) {
        this.category = category;
    }

    /**
     * Returns the imagery category original string (don't use except for error checks).
     * @return The imagery category original string
     * @since 13792
     */
    public String getImageryCategoryOriginalString() {
        return categoryOriginalString;
    }

    /**
     * Sets the imagery category original string (don't use except for error checks).
     * @param categoryOriginalString The imagery category original string
     * @since 13792
     */
    public void setImageryCategoryOriginalString(String categoryOriginalString) {
        this.categoryOriginalString = intern(categoryOriginalString);
    }

    /**
     * Returns true if this layer's URL is matched by one of the regular
     * expressions kept by the current OsmApi instance.
     * @return {@code true} is this entry is blacklisted, {@code false} otherwise
     */
    public boolean isBlacklisted() {
        Capabilities capabilities = OsmApi.getOsmApi().getCapabilities();
        return capabilities != null && capabilities.isOnImageryBlacklist(this.url);
    }

    /**
     * Sets the map of &lt;header name, header value&gt; that if any of this header
     * will be returned, then this tile will be treated as "no tile at this zoom level"
     *
     * @param noTileHeaders Map of &lt;header name, header value&gt; which will be treated as "no tile at this zoom level"
     * @since 9613
     */
    public void setNoTileHeaders(MultiMap<String, String> noTileHeaders) {
       if (noTileHeaders == null || noTileHeaders.isEmpty()) {
           this.noTileHeaders = null;
       } else {
            this.noTileHeaders = noTileHeaders.toMap();
       }
    }

    @Override
    public Map<String, Set<String>> getNoTileHeaders() {
        return noTileHeaders;
    }

    /**
     * Sets the map of &lt;checksum type, checksum value&gt; that if any tile with that checksum
     * will be returned, then this tile will be treated as "no tile at this zoom level"
     *
     * @param noTileChecksums Map of &lt;checksum type, checksum value&gt; which will be treated as "no tile at this zoom level"
     * @since 9613
     */
    public void setNoTileChecksums(MultiMap<String, String> noTileChecksums) {
        if (noTileChecksums == null || noTileChecksums.isEmpty()) {
            this.noTileChecksums = null;
        } else {
            this.noTileChecksums = noTileChecksums.toMap();
        }
    }

    @Override
    public Map<String, Set<String>> getNoTileChecksums() {
        return noTileChecksums;
    }

    /**
     * Returns the map of &lt;header name, metadata key&gt; indicating, which HTTP headers should
     * be moved to metadata
     *
     * @param metadataHeaders map of &lt;header name, metadata key&gt; indicating, which HTTP headers should be moved to metadata
     * @since 8418
     */
    public void setMetadataHeaders(Map<String, String> metadataHeaders) {
        if (metadataHeaders == null || metadataHeaders.isEmpty()) {
            this.metadataHeaders = null;
        } else {
            this.metadataHeaders = metadataHeaders;
        }
    }

    /**
     * Gets the flag if the georeference is valid.
     * @return <code>true</code> if it is valid.
     */
    public boolean isGeoreferenceValid() {
        return isGeoreferenceValid;
    }

    /**
     * Sets an indicator that the georeference is valid
     * @param isGeoreferenceValid <code>true</code> if it is marked as valid.
     */
    public void setGeoreferenceValid(boolean isGeoreferenceValid) {
        this.isGeoreferenceValid = isGeoreferenceValid;
    }

    /**
     * Returns the status of "best" marked status in other editors.
     * @return <code>true</code> if it is marked as best.
     * @since 11575
     */
    public boolean isBestMarked() {
        return bestMarked;
    }

    /**
     * Returns the overlay indication.
     * @return <code>true</code> if it is an overlay.
     * @since 13536
     */
    public boolean isOverlay() {
        return overlay;
    }

    /**
     * Sets an indicator that in other editors it is marked as best imagery
     * @param bestMarked <code>true</code> if it is marked as best in other editors.
     * @since 11575
     */
    public void setBestMarked(boolean bestMarked) {
        this.bestMarked = bestMarked;
    }

    /**
     * Sets overlay indication
     * @param overlay <code>true</code> if it is an overlay.
     * @since 13536
     */
    public void setOverlay(boolean overlay) {
        this.overlay = overlay;
    }

    /**
     * Adds an old Id.
     *
     * @param id the Id to be added
     * @since 13536
     */
    public void addOldId(String id) {
       if (oldIds == null) {
           oldIds = new ArrayList<>();
       }
       oldIds.add(id);
    }

    /**
     * Get old Ids.
     *
     * @return collection of ids
     * @since 13536
     */
    public Collection<String> getOldIds() {
        return oldIds;
    }

    /**
     * Adds a mirror entry. Mirror entries are completed with the data from the master entry
     * and only describe another method to access identical data.
     *
     * @param entry the mirror to be added
     * @since 9658
     */
    public void addMirror(ImageryInfo entry) {
       if (mirrors == null) {
           mirrors = new ArrayList<>();
       }
       mirrors.add(entry);
    }

    /**
     * Returns the mirror entries. Entries are completed with master entry data.
     *
     * @return the list of mirrors
     * @since 9658
     */
    public List<ImageryInfo> getMirrors() {
       List<ImageryInfo> l = new ArrayList<>();
       if (mirrors != null) {
           int num = 1;
           for (ImageryInfo i : mirrors) {
               ImageryInfo n = new ImageryInfo(this);
               if (i.defaultMaxZoom != 0) {
                   n.defaultMaxZoom = i.defaultMaxZoom;
               }
               if (i.defaultMinZoom != 0) {
                   n.defaultMinZoom = i.defaultMinZoom;
               }
               n.setServerProjections(i.getServerProjections());
               n.url = i.url;
               n.imageryType = i.imageryType;
               if (i.getTileSize() != 0) {
                   n.setTileSize(i.getTileSize());
               }
               if (i.getPrivacyPolicyURL() != null) {
                   n.setPrivacyPolicyURL(i.getPrivacyPolicyURL());
               }
               if (n.id != null) {
                   n.id = n.id + "_mirror"+num;
               }
               if (num > 1) {
                   n.name = tr("{0} mirror server {1}", n.name, num);
                   if (n.origName != null) {
                       n.origName += " mirror server " + num;
                   }
               } else {
                   n.name = tr("{0} mirror server", n.name);
                   if (n.origName != null) {
                       n.origName += " mirror server";
                   }
               }
               l.add(n);
               ++num;
           }
       }
       return l;
    }

    /**
     * Returns default layers that should be shown for this Imagery (if at all supported by imagery provider)
     * If no layer is set to default and there is more than one imagery available, then user will be asked to choose the layer
     * to work on
     * @return Collection of the layer names
     */
    public List<DefaultLayer> getDefaultLayers() {
        return defaultLayers;
    }

    /**
     * Sets the default layers that user will work with
     * @param layers set the list of default layers
     */
    public void setDefaultLayers(List<DefaultLayer> layers) {
        this.defaultLayers = Utils.toUnmodifiableList(layers);
    }

    /**
     * Returns custom HTTP headers that should be sent with request towards imagery provider
     * @return headers
     */
    public Map<String, String> getCustomHttpHeaders() {
        return customHttpHeaders;
    }

    /**
     * Sets custom HTTP headers that should be sent with request towards imagery provider
     * @param customHttpHeaders http headers
     */
    public void setCustomHttpHeaders(Map<String, String> customHttpHeaders) {
        this.customHttpHeaders = Utils.toUnmodifiableMap(customHttpHeaders);
    }

    /**
     * Determines if this imagery should be transparent.
     * @return should this imagery be transparent
     */
    public boolean isTransparent() {
        return transparent;
    }

    /**
     * Sets whether imagery should be transparent.
     * @param transparent set to true if imagery should be transparent
     */
    public void setTransparent(boolean transparent) {
        this.transparent = transparent;
    }

    /**
     * Returns minimum tile expiration in seconds.
     * @return minimum tile expiration in seconds
     */
    public int getMinimumTileExpire() {
        return minimumTileExpire;
    }

    /**
     * Sets minimum tile expiration in seconds.
     * @param minimumTileExpire minimum tile expiration in seconds
     */
    public void setMinimumTileExpire(int minimumTileExpire) {
        this.minimumTileExpire = minimumTileExpire;
    }

    /**
     * Get a string representation of this imagery info suitable for the {@code source} changeset tag.
     * @return English name, if known
     * @since 13890
     */
    public String getSourceName() {
        if (ImageryType.BING == getImageryType()) {
            return "Bing";
        } else {
            if (id != null) {
                // Retrieve english name, unfortunately not saved in preferences
                Optional<ImageryInfo> infoEn = ImageryLayerInfo.allDefaultLayers.stream().filter(x -> id.equals(x.getId())).findAny();
                if (infoEn.isPresent()) {
                    return infoEn.get().getOriginalName();
                }
            }
            return getOriginalName();
        }
    }

    private static String intern(String string) {
        return string == null ? null : string.intern();
    }
}
