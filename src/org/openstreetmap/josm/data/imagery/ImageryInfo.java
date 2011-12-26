// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import java.awt.Image;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Preferences.pref;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.interfaces.Attributed;
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource.Mapnik;
import org.openstreetmap.gui.jmapviewer.tilesources.AbstractTileSource;

/**
 * Class that stores info about an image background layer.
 *
 * @author Frederik Ramm <frederik@remote.org>
 */
public class ImageryInfo implements Comparable<ImageryInfo>, Attributed {

    public enum ImageryType {
        WMS("wms"),
        TMS("tms"),
        HTML("html"),
        BING("bing"),
        SCANEX("scanex");

        private String urlString;
        ImageryType(String urlString) {
            this.urlString = urlString;
        }
        public String getUrlString() {
            return urlString;
        }
    }

    public static class ImageryBounds extends Bounds {
        public ImageryBounds(String asString, String separator) {
            super(asString, separator);
        }

        private List<Shape> shapes = new ArrayList<Shape>();

        public void addShape(Shape shape) {
            this.shapes.add(shape);
        }

        public void setShapes(List<Shape> shapes) {
            this.shapes = shapes;
        }

        public List<Shape> getShapes() {
            return shapes;
        }
    }

    private String name;
    private String url = null;
    private boolean defaultEntry = false;
    private String cookies = null;
    private String eulaAcceptanceRequired= null;
    private ImageryType imageryType = ImageryType.WMS;
    private double pixelPerDegree = 0.0;
    private int defaultMaxZoom = 0;
    private int defaultMinZoom = 0;
    private ImageryBounds bounds = null;
    private List<String> serverProjections;
    private String attributionText;
    private String attributionLinkURL;
    private String attributionImage;
    private String attributionImageURL;
    private String termsOfUseText;
    private String termsOfUseURL;
    private String countryCode = "";
    private String icon;

    /** auxiliary class to save an ImageryInfo object in the preferences */
    public static class ImageryPreferenceEntry {
        @pref String name;
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

        public ImageryPreferenceEntry() {
        }

        public ImageryPreferenceEntry(ImageryInfo i) {
            name = i.name;
            type = i.imageryType.getUrlString();
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
            if (i.bounds != null) {
                bounds = i.bounds.encodeAsString(",");
                String shapesString = "";
                for (Shape s : i.bounds.getShapes()) {
                    if (!shapesString.isEmpty()) {
                        shapesString += ";";
                    }
                    shapesString += s.encodeAsString(",");
                }
                if (!shapesString.isEmpty()) {
                    shapes = shapesString;
                }
            }
            if (i.serverProjections != null && !i.serverProjections.isEmpty()) {
                String val = "";
                for (String p : i.serverProjections) {
                    if (!val.isEmpty())
                        val += ",";
                    val += p;
                }
                projections = val;
            }
        }
    }

    public ImageryInfo() {
    }

    public ImageryInfo(String name) {
        this.name=name;
    }

    public ImageryInfo(String name, String url) {
        this.name=name;
        setExtendedUrl(url);
    }

    public ImageryInfo(String name, String url, String eulaAcceptanceRequired) {
        this.name=name;
        setExtendedUrl(url);
        this.eulaAcceptanceRequired = eulaAcceptanceRequired;
    }

    public ImageryInfo(String name, String url, String eulaAcceptanceRequired, String cookies) {
        this.name=name;
        setExtendedUrl(url);
        this.cookies=cookies;
        this.eulaAcceptanceRequired = eulaAcceptanceRequired;
    }

    public ImageryInfo(String name, String url, String cookies, double pixelPerDegree) {
        this.name=name;
        setExtendedUrl(url);
        this.cookies=cookies;
        this.pixelPerDegree=pixelPerDegree;
    }

    public ImageryInfo(ImageryPreferenceEntry e) {
        name = e.name;
        url = e.url;
        cookies = e.cookies;
        eulaAcceptanceRequired = e.eula;
        for (ImageryType type : ImageryType.values()) {
            if (type.getUrlString().equals(e.type)) {
                imageryType = type;
                break;
            }
        }
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
                    Main.warn(ex.toString());
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
    }

    public ImageryInfo(Collection<String> list) {
        ArrayList<String> array = new ArrayList<String>(list);
        this.name=array.get(0);
        if(array.size() >= 2 && !array.get(1).isEmpty()) {
            setExtendedUrl(array.get(1));
        }
        if(array.size() >= 3 && !array.get(2).isEmpty()) {
            this.cookies=array.get(2);
        }
        if(array.size() >= 4 && !array.get(3).isEmpty()) {
            if (imageryType == ImageryType.WMS || imageryType == ImageryType.HTML) {
                this.pixelPerDegree=Double.valueOf(array.get(3));
            }
        }
        if(array.size() >= 5 && !array.get(4).isEmpty()) {
            try {
                bounds = new ImageryBounds(array.get(4), ",");
            } catch (IllegalArgumentException e) {
                Main.warn(e.toString());
            }
        }
        if(array.size() >= 6 && !array.get(5).isEmpty()) {
            setAttributionText(array.get(5));
        }
        if(array.size() >= 7 && !array.get(6).isEmpty()) {
            setAttributionLinkURL(array.get(6));
        }
        if(array.size() >= 8 && !array.get(7).isEmpty()) {
            setAttributionImage(array.get(7));
        }
        if(array.size() >= 9 && !array.get(8).isEmpty()) {
            setTermsOfUseURL(array.get(8));
        }
        if(bounds != null && array.size() >= 10 && !array.get(9).isEmpty()) {
            try {
                for (String s : array.get(9).split(";")) {
                    bounds.addShape(new Shape(s, ","));
                }
            } catch (IllegalArgumentException e) {
                Main.warn(e.toString());
            }
        }
        if(array.size() >= 11 && !array.get(10).isEmpty()) {
            serverProjections = Arrays.asList(array.get(10).split(","));
        }
    }

    public ImageryInfo(ImageryInfo i) {
        this.name=i.name;
        this.url=i.url;
        this.cookies=i.cookies;
        this.imageryType=i.imageryType;
        this.defaultMinZoom=i.defaultMinZoom;
        this.defaultMaxZoom=i.defaultMaxZoom;
        this.pixelPerDegree=i.pixelPerDegree;
        this.eulaAcceptanceRequired = null;
        this.bounds = i.bounds;
        this.attributionText = i.attributionText;
        this.attributionLinkURL = i.attributionLinkURL;
        this.attributionImage = i.attributionImage;
        this.attributionImageURL = i.attributionImageURL;
        this.termsOfUseText = i.termsOfUseText;
        this.termsOfUseURL = i.termsOfUseURL;
        this.serverProjections = i.serverProjections;
        this.icon = i.icon;
    }

    @Override
    public int compareTo(ImageryInfo in)
    {
        int i = countryCode.compareTo(in.countryCode);
        if (i == 0) {
            i = name.compareTo(in.name);
        }
        if (i == 0) {
            i = url.compareTo(in.url);
        }
        if (i == 0) {
            i = Double.compare(pixelPerDegree, in.pixelPerDegree);
        }
        return i;
    }

    public boolean equalsBaseValues(ImageryInfo in)
    {
        return url.equals(in.url);
    }

    public void setPixelPerDegree(double ppd) {
        this.pixelPerDegree = ppd;
    }

    public void setDefaultMaxZoom(int defaultMaxZoom) {
        this.defaultMaxZoom = defaultMaxZoom;
    }

    public void setDefaultMinZoom(int defaultMinZoom) {
        this.defaultMinZoom = defaultMinZoom;
    }

    public void setBounds(ImageryBounds b) {
        this.bounds = b;
    }

    public ImageryBounds getBounds() {
        return bounds;
    }

    @Override
    public boolean requiresAttribution() {
        return attributionText != null || attributionImage != null || termsOfUseText != null || termsOfUseURL != null;
    }

    @Override
    public String getAttributionText(int zoom, Coordinate topLeft, Coordinate botRight) {
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

    public void setExtendedUrl(String url) {
        CheckParameterUtil.ensureParameterNotNull(url);

        // Default imagery type is WMS
        this.url = url;
        this.imageryType = ImageryType.WMS;

        defaultMaxZoom = 0;
        defaultMinZoom = 0;
        for (ImageryType type : ImageryType.values()) {
            Matcher m = Pattern.compile(type.getUrlString()+"(?:\\[(?:(\\d+),)?(\\d+)\\])?:(.*)").matcher(url);
            if(m.matches()) {
                this.url = m.group(3);
                this.imageryType = type;
                if(m.group(2) != null) {
                    defaultMaxZoom = Integer.valueOf(m.group(2));
                }
                if(m.group(1) != null) {
                    defaultMinZoom = Integer.valueOf(m.group(1));
                }
                break;
            }
        }

        if(url.contains("{") && url.contains("}")) {
            if(serverProjections == null || serverProjections.isEmpty()) {
                try {
                    serverProjections = new ArrayList<String>();
                    Matcher m = Pattern.compile(".*\\{PROJ\\(([^)}]+)\\)\\}.*").matcher(url.toUpperCase());
                    if(m.matches()) {
                        for(String p : m.group(1).split(","))
                            serverProjections.add(p);
                    }
                } catch(Exception e) {
                }
            }
        // FIXME: Remove else case in March 2012 - convert old style WMS/TMS
        } else {
            url = this.url;
            if(imageryType == ImageryType.WMS) {
                if(!url.endsWith("&") && !url.endsWith("?")) {
                    url = url + (url.contains("?") ? "&" : "?");
                }
                try {
                    Matcher m = Pattern.compile(".*SRS=([a-z0-9:]+).*", Pattern.CASE_INSENSITIVE).matcher(url.toUpperCase());
                    if(m.matches()) {
                        String newProj = m.group(1);
                        if(serverProjections == null || serverProjections.isEmpty())
                            serverProjections = Collections.singletonList(newProj);
                        url = url.replaceAll("([sS][rR][sS]=)[a-zA-Z0-9:]+","SRS={proj("+newProj+")}");
                    } else
                        url += "SRS={proj}&";
                } catch(Exception e) {
                }
                url += "WIDTH={width}&height={height}&BBOX={bbox}";
            }
            else if(imageryType == ImageryType.TMS) {
                if(!url.endsWith("/"))
                    url += "/";
                url += "{zoom}/{x}/{y}.png";
            }
            if(!url.equals(this.url)) {
                Main.warn("Changed Imagery URL '"+this.url+"' to '"+url+"'");
                this.url = url;
            }
        }
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isDefaultEntry() {
        return defaultEntry;
    }

    public void setDefaultEntry(boolean defaultEntry) {
        this.defaultEntry = defaultEntry;
    }

    public String getCookies() {
        return this.cookies;
    }

    public double getPixelPerDegree() {
        return this.pixelPerDegree;
    }

    public int getMaxZoom() {
        return this.defaultMaxZoom;
    }

    public int getMinZoom() {
        return this.defaultMinZoom;
    }

    public String getEulaAcceptanceRequired() {
        return eulaAcceptanceRequired;
    }

    public void setEulaAcceptanceRequired(String eulaAcceptanceRequired) {
        this.eulaAcceptanceRequired = eulaAcceptanceRequired;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getIcon() {
        return icon;
    }

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
        this.serverProjections = new ArrayList<String>(serverProjections);
    }

    public String getExtendedUrl() {
        return imageryType.getUrlString() + (defaultMaxZoom != 0
            ? "["+(defaultMinZoom != 0 ? defaultMinZoom+",":"")+defaultMaxZoom+"]" : "") + ":" + url;
    }

    public String getToolbarName()
    {
        String res = name;
        if(pixelPerDegree != 0.0) {
            res += "#PPD="+pixelPerDegree;
        }
        return res;
    }

    public String getMenuName()
    {
        String res = name;
        if(pixelPerDegree != 0.0) {
            res += " ("+pixelPerDegree+")";
        }
        return res;
    }

    public boolean hasAttribution()
    {
        return attributionText != null;
    }

    public void copyAttribution(ImageryInfo i)
    {
        this.attributionImage = i.attributionImage;
        this.attributionImageURL = i.attributionImageURL;
        this.attributionText = i.attributionText;
        this.attributionLinkURL = i.attributionLinkURL;
        this.termsOfUseText = i.termsOfUseText;
        this.termsOfUseURL = i.termsOfUseURL;
    }

    /**
     * Applies the attribution from this object to a TMSTileSource.
     */
    public void setAttribution(AbstractTileSource s) {
        if (attributionText != null) {
            if (attributionText.equals("osm")) {
                s.setAttributionText(new Mapnik().getAttributionText(0, null, null));
            } else {
                s.setAttributionText(attributionText);
            }
        }
        if (attributionLinkURL != null) {
            if (attributionLinkURL.equals("osm")) {
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
            if (termsOfUseURL.equals("osm")) {
                s.setTermsOfUseURL(new Mapnik().getTermsOfUseURL());
            } else {
                s.setTermsOfUseURL(termsOfUseURL);
            }
        }
    }

    public ImageryType getImageryType() {
        return imageryType;
    }

    public void setImageryType(ImageryType imageryType) {
        this.imageryType = imageryType;
    }

    /**
     * Returns true if this layer's URL is matched by one of the regular
     * expressions kept by the current OsmApi instance.
     */
    public boolean isBlacklisted() {
        return OsmApi.getOsmApi().getCapabilities().isOnImageryBlacklist(this.url);
    }
}
