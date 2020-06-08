// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.swing.ImageIcon;

import org.openstreetmap.josm.data.StructUtils.StructEntry;
import org.openstreetmap.josm.data.sources.ISourceCategory;
import org.openstreetmap.josm.data.sources.ISourceType;
import org.openstreetmap.josm.data.sources.SourceBounds;
import org.openstreetmap.josm.data.sources.SourceInfo;
import org.openstreetmap.josm.data.sources.SourcePreferenceEntry;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.StreamUtils;
import org.openstreetmap.josm.tools.Utils;

/**
 * Class that stores info about an image background layer.
 *
 * @author Frederik Ramm
 */
public class ImageryInfo extends
        SourceInfo<ImageryInfo.ImageryCategory, ImageryInfo.ImageryType, ImageryInfo.ImageryBounds, ImageryInfo.ImageryPreferenceEntry> {

    /**
     * Type of imagery entry.
     */
    public enum ImageryType implements ISourceType<ImageryType> {
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
        @Override
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

        @Override
        public ImageryType getFromString(String s) {
            return fromString(s);
        }

        @Override
        public ImageryType getDefault() {
            return WMS;
        }
    }

    /**
     * Category of imagery entry.
     * @since 13792
     */
    public enum ImageryCategory implements ISourceCategory<ImageryCategory> {
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
        @Override
        public final String getCategoryString() {
            return category;
        }

        /**
         * Returns the description of this category.
         * @return the description of this category
         */
        @Override
        public final String getDescription() {
            return description;
        }

        /**
         * Returns the category icon at the given size.
         * @param size icon wanted size
         * @return the category icon at the given size
         * @since 15049
         */
        @Override
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

        @Override
        public ImageryCategory getDefault() {
            return OTHER;
        }

        @Override
        public ImageryCategory getFromString(String s) {
            return fromString(s);
        }
    }

    /**
     * Multi-polygon bounds for imagery backgrounds.
     * Used to display imagery coverage in preferences and to determine relevant imagery entries based on edit location.
     */
    public static class ImageryBounds extends SourceBounds {

        /**
         * Constructs a new {@code ImageryBounds} from string.
         * @param asString The string containing the list of shapes defining this bounds
         * @param separator The shape separator in the given string, usually a comma
         */
        public ImageryBounds(String asString, String separator) {
            super(asString, separator);
        }
    }

    private double pixelPerDegree;
    /** maximum zoom level for TMS imagery */
    private int defaultMaxZoom;
    /** minimum zoom level for TMS imagery */
    private int defaultMinZoom;
    /** projections supported by WMS servers */
    private List<String> serverProjections = Collections.emptyList();
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

    /** mirrors of different type for this entry */
    protected List<ImageryInfo> mirrors;
    /**
     * Auxiliary class to save an {@link ImageryInfo} object in the preferences.
     */
    /** is the geo reference correct - don't offer offset handling */
    private boolean isGeoreferenceValid;
    /** Should this map be transparent **/
    private boolean transparent = true;
    private int minimumTileExpire = (int) TimeUnit.MILLISECONDS.toSeconds(TMSCachedTileLoaderJob.MINIMUM_EXPIRES.get());

    /**
     * The ImageryPreferenceEntry class for storing data in JOSM preferences.
     *
     * @author Frederik Ramm, modified by Taylor Smock
     */
    public static class ImageryPreferenceEntry extends SourcePreferenceEntry<ImageryInfo> {
        @StructEntry String d;
        @StructEntry double pixel_per_eastnorth;
        @StructEntry int max_zoom;
        @StructEntry int min_zoom;
        @StructEntry String projections;
        @StructEntry MultiMap<String, String> noTileHeaders;
        @StructEntry MultiMap<String, String> noTileChecksums;
        @StructEntry int tileSize = -1;
        @StructEntry Map<String, String> metadataHeaders;
        @StructEntry boolean valid_georeference;
        @StructEntry boolean bestMarked;
        @StructEntry boolean modTileFeatures;
        @StructEntry boolean overlay;
        @StructEntry boolean transparent;
        @StructEntry int minimumTileExpire;

        /**
         * Constructs a new empty WMS {@code ImageryPreferenceEntry}.
         */
        public ImageryPreferenceEntry() {
            super();
        }

        /**
         * Constructs a new {@code ImageryPreferenceEntry} from a given {@code ImageryInfo}.
         * @param i The corresponding imagery info
         */
        public ImageryPreferenceEntry(ImageryInfo i) {
            super(i);
            pixel_per_eastnorth = i.pixelPerDegree;
            bestMarked = i.bestMarked;
            overlay = i.overlay;
            max_zoom = i.defaultMaxZoom;
            min_zoom = i.defaultMinZoom;
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
            this.sourceType = t;
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
        sourceType = ImageryType.fromString(e.type);
        if (sourceType == null) throw new IllegalArgumentException("unknown type");
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
        attributionText = Utils.intern(e.attribution_text);
        attributionLinkURL = e.attribution_url;
        permissionReferenceURL = e.permission_reference_url;
        attributionImage = e.logo_image;
        attributionImageURL = e.logo_url;
        date = e.date;
        bestMarked = e.bestMarked;
        overlay = e.overlay;
        termsOfUseText = e.terms_of_use_text;
        termsOfUseURL = e.terms_of_use_url;
        countryCode = Utils.intern(e.country_code);
        icon = Utils.intern(e.icon);
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
                        map(x -> DefaultLayer.fromJson((JsonObject) x, sourceType)).
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
        this.sourceType = i.sourceType;
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
        this.icon = Utils.intern(i.icon);
        this.isGeoreferenceValid = i.isGeoreferenceValid;
        setDefaultLayers(i.defaultLayers);
        setCustomHttpHeaders(i.customHttpHeaders);
        this.transparent = i.transparent;
        this.minimumTileExpire = i.minimumTileExpire;
        this.categoryOriginalString = Utils.intern(i.categoryOriginalString);
        this.category = i.category;
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
                n.sourceType = i.sourceType;
                if (i.getTileSize() != 0) {
                    n.setTileSize(i.getTileSize());
                }
                if (i.getPrivacyPolicyURL() != null) {
                    n.setPrivacyPolicyURL(i.getPrivacyPolicyURL());
                }
                if (n.id != null) {
                    n.id = n.id + "_mirror" + num;
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
     * Check if this object equals another ImageryInfo with respect to the properties
     * that get written to the preference file.
     *
     * The field {@link #pixelPerDegree} is ignored.
     *
     * @param other the ImageryInfo object to compare to
     * @return true if they are equal
     */
    @Override
    public boolean equalsPref(SourceInfo<ImageryInfo.ImageryCategory, ImageryInfo.ImageryType,
            ImageryInfo.ImageryBounds, ImageryInfo.ImageryPreferenceEntry> other) {
        if (!(other instanceof ImageryInfo)) {
            return false;
        }
        ImageryInfo realOther = (ImageryInfo) other;

        // CHECKSTYLE.OFF: BooleanExpressionComplexity
        return super.equalsPref(realOther) &&
                Objects.equals(this.bestMarked, realOther.bestMarked) &&
                Objects.equals(this.overlay, realOther.overlay) &&
                Objects.equals(this.isGeoreferenceValid, realOther.isGeoreferenceValid) &&
                Objects.equals(this.defaultMaxZoom, realOther.defaultMaxZoom) &&
                Objects.equals(this.defaultMinZoom, realOther.defaultMinZoom) &&
                Objects.equals(this.serverProjections, realOther.serverProjections) &&
                Objects.equals(this.transparent, realOther.transparent) &&
                Objects.equals(this.minimumTileExpire, realOther.minimumTileExpire);
        // CHECKSTYLE.ON: BooleanExpressionComplexity
    }

    @Override
    public int compareTo(SourceInfo<ImageryInfo.ImageryCategory, ImageryInfo.ImageryType,
            ImageryInfo.ImageryBounds, ImageryInfo.ImageryPreferenceEntry> other) {
        int i = super.compareTo(other);
        if (other instanceof ImageryInfo) {
            ImageryInfo in = (ImageryInfo) other;
            if (i == 0) {
                i = Double.compare(pixelPerDegree, in.pixelPerDegree);
            }
        }
        return i;
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
     * Sets the extended URL of this entry.
     * @param url Entry extended URL containing in addition of service URL, its type and min/max zoom info
     */
    public void setExtendedUrl(String url) {
        CheckParameterUtil.ensureParameterNotNull(url);

        // Default imagery type is WMS
        this.url = url;
        this.sourceType = ImageryType.WMS;

        defaultMaxZoom = 0;
        defaultMinZoom = 0;
        for (ImageryType type : ImageryType.values()) {
            Matcher m = Pattern.compile(type.getTypeString()+"(?:\\[(?:(\\d+)[,-])?(\\d+)\\])?:(.*)").matcher(url);
            if (m.matches()) {
                this.url = m.group(3);
                this.sourceType = type;
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
     * Returns a tool tip text for display.
     * @return The text
     * @since 8065
     */
    @Override
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
        return sourceType.getTypeString() + (defaultMaxZoom != 0
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
     * Returns the imagery type.
     * @return The imagery type
     * @see SourceInfo#getSourceType
     */
    public ImageryType getImageryType() {
        return super.getSourceType() != null ? super.getSourceType() : ImageryType.WMS.getDefault();
    }

    /**
     * Sets the imagery type.
     * @param imageryType The imagery type
     * @see SourceInfo#setSourceType
     */
    public void setImageryType(ImageryType imageryType) {
        super.setSourceType(imageryType);
    }

    /**
     * Returns the imagery category.
     * @return The imagery category
     * @see SourceInfo#getSourceCategory
     * @since 13792
     */
    public ImageryCategory getImageryCategory() {
        return super.getSourceCategory();
    }

    /**
     * Sets the imagery category.
     * @param category The imagery category
     * @see SourceInfo#setSourceCategory
     * @since 13792
     */
    public void setImageryCategory(ImageryCategory category) {
        super.setSourceCategory(category);
    }

    /**
     * Returns the imagery category original string (don't use except for error checks).
     * @return The imagery category original string
     * @see SourceInfo#getSourceCategoryOriginalString
     * @since 13792
     */
    public String getImageryCategoryOriginalString() {
        return super.getSourceCategoryOriginalString();
    }

    /**
     * Sets the imagery category original string (don't use except for error checks).
     * @param categoryOriginalString The imagery category original string
     * @see SourceInfo#setSourceCategoryOriginalString
     * @since 13792
     */
    public void setImageryCategoryOriginalString(String categoryOriginalString) {
        super.setSourceCategoryOriginalString(categoryOriginalString);
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

    /**
     * Return the sorted list of activated source IDs.
     * @return sorted list of activated source IDs
     * @since 13536
     */
    public static Collection<String> getActiveIds() {
        return getActiveIds(ImageryInfo.class);
    }
}
