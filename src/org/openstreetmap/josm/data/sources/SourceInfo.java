// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.sources;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Image;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.swing.ImageIcon;

import org.openstreetmap.gui.jmapviewer.interfaces.Attributed;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.tilesources.AbstractTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource.Mapnik;
import org.openstreetmap.gui.jmapviewer.tilesources.TileSourceInfo;
import org.openstreetmap.josm.data.StructUtils;
import org.openstreetmap.josm.data.imagery.DefaultLayer;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryPreferenceEntry;
import org.openstreetmap.josm.io.Capabilities;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.IPreferences;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.Utils;

/**
 * This class is an abstraction for source information to be used in a panel like ImageryProvidersPanel.
 *
 * @author Taylor Smock
 * @param <T> The SourceCategory The categories enum for the source
 * @param <U> The SourceType The type enum of the source
 * @param <V> The SourceBounds The bound type for the entry
 * @param <W> The storage for the entry
 *
 * @since 16545
 */
public class SourceInfo<T extends ISourceCategory<?>, U extends ISourceType<?>, V extends SourceBounds, W extends SourcePreferenceEntry<?>>
    extends TileSourceInfo implements Comparable<SourceInfo<T, U, V, W>>, Attributed {
    /** original name of the source entry in case of translation call, for multiple languages English when possible */
    protected String origName;
    /** (original) language of the translated name entry */
    protected String langName;
    /** whether this is a entry activated by default or not */
    protected boolean defaultEntry;
    /** Whether this service requires a explicit EULA acceptance before it can be activated */
    protected String eulaAcceptanceRequired;
    /** type of the services - WMS, TMS, ... */
    protected U sourceType;
    /** display bounds of imagery, displayed in prefs and used for automatic imagery selection */
    protected V bounds;
    /** description of the imagery entry, should contain notes what type of data it is */
    protected String description;
    /** language of the description entry */
    protected String langDescription;
    /** Text of a text attribution displayed when using the imagery */
    protected String attributionText;
    /** Link to the privacy policy of the operator */
    protected String privacyPolicyURL;
    /** Link to a reference stating the permission for OSM usage */
    protected String permissionReferenceURL;
    /** Link behind the text attribution displayed when using the imagery */
    protected String attributionLinkURL;
    /** Image of a graphical attribution displayed when using the imagery */
    protected String attributionImage;
    /** Link behind the graphical attribution displayed when using the imagery */
    protected String attributionImageURL;
    /** Text with usage terms displayed when using the imagery */
    protected String termsOfUseText;
    /** Link behind the text with usage terms displayed when using the imagery */
    protected String termsOfUseURL;
    /** country code of the imagery (for country specific imagery) */
    protected String countryCode = "";
    /**
      * creation date of the source (in the form YYYY-MM-DD;YYYY-MM-DD, where
      * DD and MM as well as a second date are optional).
      *
      * Also used as time filter for WMS time={time} parameter (such as Sentinel-2)
      * @since 11570
      */
    protected String date;
    /**
      * list of old IDs, only for loading, not handled anywhere else
      * @since 13536
      */
    protected Collection<String> oldIds;
    /** icon used in menu */
    protected String icon;
    /** which layers should be activated by default on layer addition. **/
    protected List<DefaultLayer> defaultLayers = Collections.emptyList();
    /** HTTP headers **/
    protected Map<String, String> customHttpHeaders = Collections.emptyMap();
    /** category of the imagery */
    protected T category;
    /** category of the imagery (input string, not saved, copied or used otherwise except for error checks) */
    protected String categoryOriginalString;
    /** when adding a field, also adapt the:
     * {@link #ImageryPreferenceEntry ImageryPreferenceEntry object}
     * {@link #ImageryPreferenceEntry#ImageryPreferenceEntry(ImageryInfo) ImageryPreferenceEntry constructor}
     * {@link #ImageryInfo(ImageryPreferenceEntry) ImageryInfo constructor}
     * {@link #ImageryInfo(ImageryInfo) ImageryInfo constructor}
     * {@link #equalsPref(ImageryPreferenceEntry) equalsPref method}
     **/

    /**
     * Creates empty SourceInfo class
     */
    public SourceInfo() {
        super();
    }

    /**
     * Create a SourceInfo class
     *
     * @param name name
     */
    public SourceInfo(String name) {
        super(name);
    }

    /**
     * Create a SourceInfo class
     *
     * @param name name
     * @param url base URL
     * @param id unique id
     */
    public SourceInfo(String name, String url, String id) {
        super(name, url, id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, sourceType);
    }

    /**
     * Check if this object equals another SourceInfo with respect to the properties
     * that get written to the preference file.
     *
     * This should be overridden and called in subclasses.
     *
     * @param other the SourceInfo object to compare to
     * @return true if they are equal
     */
    public boolean equalsPref(SourceInfo<T, U, V, W> other) {
        if (other == null) {
            return false;
        }

        // CHECKSTYLE.OFF: BooleanExpressionComplexity
        return
                Objects.equals(this.name, other.name) &&
                Objects.equals(this.id, other.id) &&
                Objects.equals(this.url, other.url) &&
                Objects.equals(this.modTileFeatures, other.modTileFeatures) &&
                Objects.equals(this.cookies, other.cookies) &&
                Objects.equals(this.eulaAcceptanceRequired, other.eulaAcceptanceRequired) &&
                Objects.equals(this.sourceType, other.sourceType) &&
                Objects.equals(this.bounds, other.bounds) &&
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
                Objects.equals(this.category, other.category);
        // CHECKSTYLE.ON: BooleanExpressionComplexity
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SourceInfo<?, ?, ?, ?> that = (SourceInfo<?, ?, ?, ?>) o;
        return sourceType == that.sourceType && Objects.equals(url, that.url);
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
                .append(" - ").append(sourceType)
                .toString();
    }

    @Override
    public int compareTo(SourceInfo<T, U, V, W> in) {
        int i = countryCode.compareTo(in.countryCode);
        if (i == 0) {
            i = name.toLowerCase(Locale.ENGLISH).compareTo(in.name.toLowerCase(Locale.ENGLISH));
        }
        if (i == 0) {
            i = url.compareTo(in.url);
        }
        return i;
    }

    /**
     * Determines if URL is equal to given source info.
     * @param in source info
     * @return {@code true} if URL is equal to given source info
     */
    public boolean equalsBaseValues(SourceInfo<T, U, V, W> in) {
        return url.equals(in.url);
    }

    /**
     * Sets the source polygonial bounds.
     * @param b The source bounds (non-rectangular)
     */
    public void setBounds(V b) {
        this.bounds = b;
    }

    /**
     * Returns the source polygonial bounds.
     * @return The source bounds (non-rectangular)
     */
    public V getBounds() {
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
        attributionText = Utils.intern(text);
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
            this.langDescription = Utils.intern(language);
        }
    }

    /**
     * Return the sorted list of activated source IDs.
     * @param <W> The type of active id to get
     * @param clazz The class of the type of id
     * @return sorted list of activated source IDs
     * @since 13536, xxx (extracted)
     */
    public static <W extends SourceInfo<?, ?, ?, ?>> Collection<String> getActiveIds(Class<W> clazz) {
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
        this.countryCode = Utils.intern(countryCode);
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
        this.icon = Utils.intern(icon);
    }

    /**
     * Determines if this entry requires attribution.
     * @return {@code true} if some attribution text has to be displayed, {@code false} otherwise
     */
    public boolean hasAttribution() {
        return attributionText != null;
    }

    /**
     * Copies attribution from another {@code SourceInfo}.
     * @param i The other source info to get attribution from
     */
    public void copyAttribution(SourceInfo<T, U, V, W> i) {
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
     * Returns the source type.
     * @return The source type
     */
    public U getSourceType() {
        return sourceType;
    }

    /**
     * Sets the source type.
     * @param imageryType The source type
     */
    public void setSourceType(U imageryType) {
        this.sourceType = imageryType;
    }

    /**
     * Returns the source category.
     * @return The source category
     */
    public T getSourceCategory() {
        return category;
    }

    /**
     * Sets the source category.
     * @param category The source category
     */
    public void setSourceCategory(T category) {
        this.category = category;
    }

    /**
     * Returns the source category original string (don't use except for error checks).
     * @return The source category original string
     */
    public String getSourceCategoryOriginalString() {
        return categoryOriginalString;
    }

    /**
     * Sets the source category original string (don't use except for error checks).
     * @param categoryOriginalString The source category original string
     */
    public void setSourceCategoryOriginalString(String categoryOriginalString) {
        this.categoryOriginalString = Utils.intern(categoryOriginalString);
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
}
