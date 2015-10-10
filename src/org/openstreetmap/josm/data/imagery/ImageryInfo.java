// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Image;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;

import org.openstreetmap.gui.jmapviewer.OsmMercator;
import org.openstreetmap.gui.jmapviewer.interfaces.Attributed;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.tilesources.AbstractTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource.Mapnik;
import org.openstreetmap.gui.jmapviewer.tilesources.TileSourceInfo;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Preferences.pref;
import org.openstreetmap.josm.io.Capabilities;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.LanguageInfo;

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
        /** An HTML proxy (previously used for Yahoo imagery) entry. **/
        HTML("html"),
        /** TMS entry for Microsoft Bing. */
        BING("bing"),
        /** TMS entry for Russian company <a href="https://wiki.openstreetmap.org/wiki/WikiProject_Russia/kosmosnimki">ScanEx</a>. **/
        SCANEX("scanex"),
        /** A WMS endpoint entry only stores the WMS server info, without layer, which are chosen later by the user. **/
        WMS_ENDPOINT("wms_endpoint"),
        /** WMTS stores GetCapabilities URL. Does not store any information about the layer **/
        WMTS("wmts");


        private final String typeString;

        ImageryType(String urlString) {
            this.typeString = urlString;
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
            for (ImageryType type : ImageryType.values()) {
                if (type.getTypeString().equals(s)) {
                    return type;
                }
            }
            return null;
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
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + ((shapes == null) ? 0 : shapes.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!super.equals(obj))
                return false;
            if (getClass() != obj.getClass())
                return false;
            ImageryBounds other = (ImageryBounds) obj;
            if (shapes == null) {
                if (other.shapes != null)
                    return false;
            } else if (!shapes.equals(other.shapes))
                return false;
            return true;
        }
    }


    /** original name of the imagery entry in case of translation call, for multiple languages English when possible */
    private String origName;
    /** (original) language of the translated name entry */
    private String langName;
    /** whether this is a entry activated by default or not */
    private boolean defaultEntry;
    /** The data part of HTTP cookies header in case the service requires cookies to work */
    private String cookies;
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
    private List<String> serverProjections;
    /** description of the imagery entry, should contain notes what type of data it is */
    private String description;
    /** language of the description entry */
    private String langDescription;
    /** Text of a text attribution displayed when using the imagery */
    private String attributionText;
    /** Link behing the text attribution displayed when using the imagery */
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
    /** icon used in menu */
    private String icon;
    // when adding a field, also adapt the ImageryInfo(ImageryInfo) constructor, equals method, and ImageryPreferenceEntry

    /**
     * Auxiliary class to save an {@link ImageryInfo} object in the preferences.
     */
    public static class ImageryPreferenceEntry {
        @pref String name;
        @pref String id;
        @pref String type;
        @pref String url;
        @pref double pixel_per_eastnorth;
        @pref String eula;
        @pref String attribution_text;
        @pref String attribution_url;
        @pref String logo_image;
        @pref String logo_url;
        @pref String terms_of_use_text;
        @pref String terms_of_use_url;
        @pref String country_code = "";
        @pref int max_zoom;
        @pref int min_zoom;
        @pref String cookies;
        @pref String bounds;
        @pref String shapes;
        @pref String projections;
        @pref String icon;
        @pref String description;
        @pref Map<String, String> noTileHeaders;
        @pref int tileSize = OsmMercator.DEFAUL_TILE_SIZE;
        @pref Map<String, String> metadataHeaders;

        /**
         * Constructs a new empty WMS {@code ImageryPreferenceEntry}.
         */
        public ImageryPreferenceEntry() {
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
            logo_image = i.attributionImage;
            logo_url = i.attributionImageURL;
            terms_of_use_text = i.termsOfUseText;
            terms_of_use_url = i.termsOfUseURL;
            country_code = i.countryCode;
            max_zoom = i.defaultMaxZoom;
            min_zoom = i.defaultMinZoom;
            cookies = i.cookies;
            icon = i.icon;
            description = i.description;
            if (i.bounds != null) {
                bounds = i.bounds.encodeAsString(",");
                StringBuilder shapesString = new StringBuilder();
                for (Shape s : i.bounds.getShapes()) {
                    if (shapesString.length() > 0) {
                        shapesString.append(';');
                    }
                    shapesString.append(s.encodeAsString(","));
                }
                if (shapesString.length() > 0) {
                    shapes = shapesString.toString();
                }
            }
            if (i.serverProjections != null && !i.serverProjections.isEmpty()) {
                StringBuilder val = new StringBuilder();
                for (String p : i.serverProjections) {
                    if (val.length() > 0) {
                        val.append(',');
                    }
                    val.append(p);
                }
                projections = val.toString();
            }
            if (i.noTileHeaders != null && !i.noTileHeaders.isEmpty()) {
                noTileHeaders = i.noTileHeaders;
            }

            if (i.metadataHeaders != null && !i.metadataHeaders.isEmpty()) {
                metadataHeaders = i.metadataHeaders;
            }

            tileSize = i.getTileSize();
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
        } else if (type != null && !type.trim().isEmpty()) {
            throw new IllegalArgumentException("unknown type: "+type);
        }
    }

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
                    Main.warn(ex);
                }
            }
        }
        if (e.projections != null) {
            serverProjections = Arrays.asList(e.projections.split(","));
        }
        attributionText = e.attribution_text;
        attributionLinkURL = e.attribution_url;
        attributionImage = e.logo_image;
        attributionImageURL = e.logo_url;
        termsOfUseText = e.terms_of_use_text;
        termsOfUseURL = e.terms_of_use_url;
        countryCode = e.country_code;
        icon = e.icon;
        if (e.noTileHeaders != null) {
            noTileHeaders = e.noTileHeaders;
        }
        setTileSize(e.tileSize);
        metadataHeaders = e.metadataHeaders;
    }

    /**
     * Constructs a new {@code ImageryInfo} from an existing one.
     * @param i The other imagery info
     */
    public ImageryInfo(ImageryInfo i) {
        super(i.name, i.url, i.id);
        this.defaultEntry = i.defaultEntry;
        this.cookies = i.cookies;
        this.eulaAcceptanceRequired = null;
        this.imageryType = i.imageryType;
        this.pixelPerDegree = i.pixelPerDegree;
        this.defaultMaxZoom = i.defaultMaxZoom;
        this.defaultMinZoom = i.defaultMinZoom;
        this.bounds = i.bounds;
        this.serverProjections = i.serverProjections;
        this.attributionText = i.attributionText;
        this.attributionLinkURL = i.attributionLinkURL;
        this.attributionImage = i.attributionImage;
        this.attributionImageURL = i.attributionImageURL;
        this.termsOfUseText = i.termsOfUseText;
        this.termsOfUseURL = i.termsOfUseURL;
        this.countryCode = i.countryCode;
        this.icon = i.icon;
        this.description = i.description;
        this.noTileHeaders = i.noTileHeaders;
        this.metadataHeaders = i.metadataHeaders;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ImageryInfo that = (ImageryInfo) o;

        if (imageryType != that.imageryType) return false;
        if (url != null ? !url.equals(that.url) : that.url != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
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

        return
                Objects.equals(this.name, other.name) &&
                Objects.equals(this.id, other.id) &&
                Objects.equals(this.url, other.url) &&
                Objects.equals(this.cookies, other.cookies) &&
                Objects.equals(this.eulaAcceptanceRequired, other.eulaAcceptanceRequired) &&
                Objects.equals(this.imageryType, other.imageryType) &&
                Objects.equals(this.defaultMaxZoom, other.defaultMaxZoom) &&
                Objects.equals(this.defaultMinZoom, other.defaultMinZoom) &&
                Objects.equals(this.bounds, other.bounds) &&
                Objects.equals(this.serverProjections, other.serverProjections) &&
                Objects.equals(this.attributionText, other.attributionText) &&
                Objects.equals(this.attributionLinkURL, other.attributionLinkURL) &&
                Objects.equals(this.attributionImageURL, other.attributionImageURL) &&
                Objects.equals(this.attributionImage, other.attributionImage) &&
                Objects.equals(this.termsOfUseText, other.termsOfUseText) &&
                Objects.equals(this.termsOfUseURL, other.termsOfUseURL) &&
                Objects.equals(this.countryCode, other.countryCode) &&
                Objects.equals(this.icon, other.icon) &&
                Objects.equals(this.description, other.description) &&
                Objects.equals(this.noTileHeaders, other.noTileHeaders) &&
                Objects.equals(this.metadataHeaders, other.metadataHeaders);
    }

    @Override
    public int hashCode() {
        int result = url != null ? url.hashCode() : 0;
        result = 31 * result + (imageryType != null ? imageryType.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ImageryInfo{" +
                "name='" + name + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", url='" + url + '\'' +
                ", imageryType=" + imageryType +
                '}';
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

    public boolean equalsBaseValues(ImageryInfo in) {
        return url.equals(in.url);
    }

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
        return attributionText != null || attributionImage != null || termsOfUseText != null || termsOfUseURL != null;
    }

    @Override
    public String getAttributionText(int zoom, ICoordinate topLeft, ICoordinate botRight) {
        return attributionText;
    }

    @Override
    public String getAttributionLinkURL() {
        return attributionLinkURL;
    }

    @Override
    public Image getAttributionImage() {
        ImageIcon i = ImageProvider.getIfAvailable(attributionImage);
        if (i != null) {
            return i.getImage();
        }
        return null;
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

    public void setAttributionText(String text) {
        attributionText = text;
    }

    public void setAttributionImageURL(String text) {
        attributionImageURL = text;
    }

    public void setAttributionImage(String text) {
        attributionImage = text;
    }

    public void setAttributionLinkURL(String text) {
        attributionLinkURL = text;
    }

    public void setTermsOfUseText(String text) {
        termsOfUseText = text;
    }

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
            Matcher m = Pattern.compile(type.getTypeString()+"(?:\\[(?:(\\d+),)?(\\d+)\\])?:(.*)").matcher(url);
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

        if (serverProjections == null || serverProjections.isEmpty()) {
            try {
                serverProjections = new ArrayList<>();
                Matcher m = Pattern.compile(".*\\{PROJ\\(([^)}]+)\\)\\}.*").matcher(url.toUpperCase(Locale.ENGLISH));
                if (m.matches()) {
                    for (String p : m.group(1).split(",")) {
                        serverProjections.add(p);
                    }
                }
            } catch (Exception e) {
                Main.warn(e);
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

    public void clearId() {
        if (this.id != null) {
            Collection<String> newAddedIds = new TreeSet<>(Main.pref.getCollection("imagery.layers.addedIds"));
            newAddedIds.add(this.id);
            Main.pref.putCollection("imagery.layers.addedIds", newAddedIds);
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
     * Return the data part of HTTP cookies header in case the service requires cookies to work
     * @return the cookie data part
     */
    @Override
    public String getCookies() {
        return this.cookies;
    }

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
            this.langDescription = language;
        }
    }

    /**
     * Returns a tool tip text for display.
     * @return The text
     * @since 8065
     */
    public String getToolTipText() {
        String desc = getDescription();
        if (desc != null && !desc.isEmpty()) {
            return "<html>" + getName() + "<br>" + desc + "</html>";
        }
        return getName();
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
        this.countryCode = countryCode;
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
        this.icon = icon;
    }

    /**
     * Get the projections supported by the server. Only relevant for
     * WMS-type ImageryInfo at the moment.
     * @return null, if no projections have been specified; the list
     * of supported projections otherwise.
     */
    public List<String> getServerProjections() {
        if (serverProjections == null)
            return Collections.emptyList();
        return Collections.unmodifiableList(serverProjections);
    }

    public void setServerProjections(Collection<String> serverProjections) {
        this.serverProjections = new ArrayList<>(serverProjections);
    }

    /**
     * Returns the extended URL, containing in addition of service URL, its type and min/max zoom info.
     * @return The extended URL
     */
    public String getExtendedUrl() {
        return imageryType.getTypeString() + (defaultMaxZoom != 0
            ? "["+(defaultMinZoom != 0 ? defaultMinZoom+',' : "")+defaultMaxZoom+"]" : "") + ':' + url;
    }

    public String getToolbarName() {
        String res = name;
        if (pixelPerDegree != 0) {
            res += "#PPD="+pixelPerDegree;
        }
        return res;
    }

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
     * @since 8344
     */
    public void setNoTileHeaders(Map<String, String> noTileHeaders) {
       this.noTileHeaders = noTileHeaders;
    }

    @Override
    public Map<String, String> getNoTileHeaders() {
        return noTileHeaders;
    }

    /**
     * Returns the map of &lt;header name, metadata key&gt; indicating, which HTTP headers should
     * be moved to metadata
     *
     * @param metadataHeaders map of &lt;header name, metadata key&gt; indicating, which HTTP headers should be moved to metadata
     * @since 8418
     */
    public void setMetadataHeaders(Map<String, String> metadataHeaders) {
        this.metadataHeaders = metadataHeaders;
    }
}
