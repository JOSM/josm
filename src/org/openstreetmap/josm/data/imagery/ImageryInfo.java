// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource.Mapnik;
import org.openstreetmap.gui.jmapviewer.tilesources.TMSTileSource;

/**
 * Class that stores info about an image background layer.
 *
 * @author Frederik Ramm <frederik@remote.org>
 */
public class ImageryInfo implements Comparable<ImageryInfo> {

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
    private int maxZoom = 0;
    private int defaultMaxZoom = 0;
    private int defaultMinZoom = 0;
    private ImageryBounds bounds = null;
    private List<String> serverProjections;
    private String attributionText;
    private String attributionImage;
    private String attributionLinkURL;
    private String termsOfUseURL;
    private String countryCode = "";

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

    public ArrayList<String> getInfoArray() {
        ArrayList<String> res = new ArrayList<String>();
        res.add(name);
        res.add((url != null && !url.isEmpty()) ? getExtendedUrl() : null);
        res.add(cookies);
        if(imageryType == ImageryType.WMS || imageryType == ImageryType.HTML) {
            res.add(pixelPerDegree != 0.0 ? String.valueOf(pixelPerDegree) : null);
        } else {
            res.add(maxZoom != 0 ? String.valueOf(maxZoom) : null);
        }
        res.add(bounds != null ? bounds.encodeAsString(",") : null);
        res.add(attributionText);
        res.add(attributionLinkURL);
        res.add(attributionImage);
        res.add(termsOfUseURL);
        // Shapes
        String shapesString = "";
        if (bounds != null) {
            for (Shape s : bounds.getShapes()) {
                if (!shapesString.isEmpty()) {
                    shapesString += ";";
                }
                shapesString += s.encodeAsString(",");
            }
        }
        res.add(shapesString.isEmpty() ? null : shapesString);
        return res;
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
            } else {
                this.maxZoom=Integer.valueOf(array.get(3));
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
    }

    public ImageryInfo(ImageryInfo i) {
        this.name=i.name;
        this.url=i.url;
        this.cookies=i.cookies;
        this.imageryType=i.imageryType;
        this.defaultMinZoom=i.defaultMinZoom;
        this.maxZoom=i.maxZoom;
        this.defaultMaxZoom=i.defaultMaxZoom;
        this.pixelPerDegree=i.pixelPerDegree;
        this.eulaAcceptanceRequired = null;
        this.bounds = i.bounds;
        this.attributionImage = i.attributionImage;
        this.attributionLinkURL = i.attributionLinkURL;
        this.attributionText = i.attributionText;
        this.termsOfUseURL = i.termsOfUseURL;
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
    
    public void setMaxZoom(int maxZoom) {
        this.maxZoom = maxZoom;
    }

    public void setBounds(ImageryBounds b) {
        this.bounds = b;
    }

    public ImageryBounds getBounds() {
        return bounds;
    }

    public void setAttributionText(String text) {
        attributionText = text;
    }

    public void setAttributionImage(String text) {
        attributionImage = text;
    }

    public void setAttributionLinkURL(String text) {
        attributionLinkURL = text;
    }

    public void setTermsOfUseURL(String text) {
        termsOfUseURL = text;
    }

    public void setExtendedUrl(String url) {
        CheckParameterUtil.ensureParameterNotNull(url);
        
        defaultMaxZoom = 0;
        defaultMinZoom = 0;
        for (ImageryType type : ImageryType.values()) {
            Matcher m = Pattern.compile(type.getUrlString()+"(?:\\[(?:(\\d+),)?(\\d+)\\])?:(.*)").matcher(url);
            if(m.matches()) {
                this.url = m.group(3);
                this.imageryType = type;
                if(m.group(2) != null) {
                    defaultMaxZoom = Integer.valueOf(m.group(2));
                    maxZoom = defaultMaxZoom;
                }
                if(m.group(1) != null) {
                    defaultMinZoom = Integer.valueOf(m.group(1));
                }
                return;
            }
        }

        // Default imagery type is WMS
        this.url = url;
        this.imageryType = ImageryType.WMS;
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
        return this.maxZoom;
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

    /**
     * Get the projections supported by the server. Only relevant for
     * WMS-type ImageryInfo at the moment.
     * @return null, if no projections have been specified; the list
     * of supported projections otherwise.
     */
    public List<String> getServerProjections() {
        if (serverProjections == null) return null;
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
        } else if(maxZoom != 0 && maxZoom != defaultMaxZoom) {
            res += " (z"+maxZoom+")";
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
        this.attributionLinkURL = i.attributionLinkURL;
        this.attributionText = i.attributionText;
        this.termsOfUseURL = i.termsOfUseURL;
    }

    public void setAttribution(TMSTileSource s)
    {
        if(attributionLinkURL != null) {
            if(attributionLinkURL.equals("osm"))
                s.setAttributionLinkURL(new Mapnik().getAttributionLinkURL());
            else
                s.setAttributionLinkURL(attributionLinkURL);
        }
        if(attributionText != null) {
            if(attributionText.equals("osm"))
                s.setAttributionText(new Mapnik().getAttributionText(0, null, null));
            else
                s.setAttributionText(attributionText);
        }
        if(attributionImage != null) {
            ImageIcon i = ImageProvider.getIfAvailable(null, attributionImage);
            if(i != null)
                s.setAttributionImage(i.getImage());
        }
        if(termsOfUseURL != null) {
            if(termsOfUseURL.equals("osm"))
                s.setTermsOfUseURL(new Mapnik().getTermsOfUseURL());
            else
                s.setTermsOfUseURL(termsOfUseURL);
        }
    }

    public ImageryType getImageryType() {
        return imageryType;
    }

    public void setImageryType(ImageryType imageryType) {
        this.imageryType = imageryType;
    }

    public static boolean isUrlWithPatterns(String url) {
        return url != null && url.contains("{") && url.contains("}");
    }

    /**
     * Returns true if this layer's URL is matched by one of the regular
     * expressions kept by the current OsmApi instance.
     */
    public boolean isBlacklisted() {
        return OsmApi.getOsmApi().getCapabilities().isOnImageryBlacklist(this.url);
    }
}
