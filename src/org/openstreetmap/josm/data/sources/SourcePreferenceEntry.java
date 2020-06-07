// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.sources;

import java.util.Map;

import javax.json.stream.JsonCollectors;

import org.openstreetmap.josm.data.StructUtils.StructEntry;
import org.openstreetmap.josm.data.imagery.DefaultLayer;
import org.openstreetmap.josm.data.imagery.Shape;
import org.openstreetmap.josm.tools.Utils;

/**
 * A generic SourcePreferenceEntry that is used for storing data in JOSM preferences.
 * This is intended to be removed, at some point. User beware.
 *
 * @author Taylor Smock
 *
 * @param <T> The type of SourceInfo
 */
public class SourcePreferenceEntry<T extends SourceInfo<?, ?, ?, ?>> {
    /** The name of the source */
    @StructEntry public String name;
    /** A *unique* id for the source */
    @StructEntry public String id;
    /** The type of the source (e.g., WMS, WMTS, etc.) */
    @StructEntry public String type;
    /** The URL for the source (base url) */
    @StructEntry public String url;
    /** The EULA for the source */
    @StructEntry public String eula;
    /** The attribution text for the source */
    @StructEntry public String attribution_text;
    /** The attribution URL for the source */
    @StructEntry public String attribution_url;
    /** The permission reference url (i.e., how do we know we have permission?) */
    @StructEntry public String permission_reference_url;
    /** The logo to be used for the source */
    @StructEntry public String logo_image;
    /** The logo url */
    @StructEntry public String logo_url;
    /** The TOU text */
    @StructEntry public String terms_of_use_text;
    /** The URL for the TOU */
    @StructEntry public String terms_of_use_url;
    /** The country code for the source (usually ISO 3166-1 alpha-2) */
    @StructEntry public String country_code = "";
    /** The date for the source */
    @StructEntry public String date;
    /** The cookies required to get the source */
    @StructEntry public String cookies;
    /** The bounds of the source */
    @StructEntry public String bounds;
    /** The shape of the source (mostly used for visual aid purposes) */
    @StructEntry public String shapes;
    /** The icon for the source (not necessarily the same as the logo) */
    @StructEntry public String icon;
    /** The description of the source */
    @StructEntry public String description;
    /** The default layers for the source, if any (mostly useful for imagery) */
    @StructEntry public String default_layers;
    /** Any custom HTTP headers */
    @StructEntry public Map<String, String> customHttpHeaders;
    /** The category string for the source */
    @StructEntry public String category;

    /**
     * Constructs a new empty {@code SourcePreferenceEntry}.
     */
    public SourcePreferenceEntry() {
        // Do nothing
    }

    /**
     * Constructs a new {@code SourcePreferenceEntry} from a given {@code SourceInfo}.
     * @param i The corresponding source info
     */
    public SourcePreferenceEntry(T i) {
        name = i.getName();
        id = i.getId();
        type = i.sourceType.getTypeString();
        url = i.getUrl();
        eula = i.eulaAcceptanceRequired;
        attribution_text = i.attributionText;
        attribution_url = i.attributionLinkURL;
        permission_reference_url = i.permissionReferenceURL;
        date = i.date;
        logo_image = i.attributionImage;
        logo_url = i.attributionImageURL;
        terms_of_use_text = i.termsOfUseText;
        terms_of_use_url = i.termsOfUseURL;
        country_code = i.countryCode;
        cookies = i.getCookies();
        icon = Utils.intern(i.icon);
        description = i.description;
        category = i.category != null ? i.category.getCategoryString() : null;
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

        if (!i.defaultLayers.isEmpty()) {
            default_layers = i.defaultLayers.stream().map(DefaultLayer::toJson).collect(JsonCollectors.toJsonArray()).toString();
        }
        customHttpHeaders = i.customHttpHeaders;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(getClass().getSimpleName()).append(" [name=").append(name);
        if (id != null) {
            s.append(" id=").append(id);
        }
        s.append(']');
        return s.toString();
    }
}
