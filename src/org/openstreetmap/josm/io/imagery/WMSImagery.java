// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.imagery;

import java.awt.HeadlessException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.gui.preferences.projection.ProjectionChoice;
import org.openstreetmap.josm.gui.preferences.projection.ProjectionPreference;
import org.openstreetmap.josm.io.UTFInputStreamReader;
import org.openstreetmap.josm.tools.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class WMSImagery {

    public static class WMSGetCapabilitiesException extends Exception {
        private final String incomingData;

        public WMSGetCapabilitiesException(Throwable cause, String incomingData) {
            super(cause);
            this.incomingData = incomingData;
        }

        public String getIncomingData() {
            return incomingData;
        }
    }

    private List<LayerDetails> layers;
    private URL serviceUrl;
    private List<String> formats;

    public List<LayerDetails> getLayers() {
        return layers;
    }

    public URL getServiceUrl() {
        return serviceUrl;
    }

    public List<String> getFormats() {
        return formats;
    }

    String buildRootUrl() {
        if (serviceUrl == null) {
            return null;
        }
        StringBuilder a = new StringBuilder(serviceUrl.getProtocol());
        a.append("://");
        a.append(serviceUrl.getHost());
        if (serviceUrl.getPort() != -1) {
            a.append(":");
            a.append(serviceUrl.getPort());
        }
        a.append(serviceUrl.getPath());
        a.append("?");
        if (serviceUrl.getQuery() != null) {
            a.append(serviceUrl.getQuery());
            if (!serviceUrl.getQuery().isEmpty() && !serviceUrl.getQuery().endsWith("&")) {
                a.append("&");
            }
        }
        return a.toString();
    }

    public String buildGetMapUrl(Collection<LayerDetails> selectedLayers) {
        return buildGetMapUrl(selectedLayers, "image/jpeg");
    }

    public String buildGetMapUrl(Collection<LayerDetails> selectedLayers, String format) {
        return buildRootUrl()
                + "FORMAT=" + format + "&VERSION=1.1.1&SERVICE=WMS&REQUEST=GetMap&LAYERS="
                + Utils.join(",", Utils.transform(selectedLayers, new Utils.Function<LayerDetails, String>() {
            @Override
            public String apply(LayerDetails x) {
                return x.ident;
            }
        }))
                + "&STYLES=&SRS={proj}&WIDTH={width}&HEIGHT={height}&BBOX={bbox}";
    }

    public void attemptGetCapabilities(String serviceUrlStr) throws MalformedURLException, IOException, WMSGetCapabilitiesException {
        URL getCapabilitiesUrl = null;
        try {
            if (!Pattern.compile(".*GetCapabilities.*", Pattern.CASE_INSENSITIVE).matcher(serviceUrlStr).matches()) {
                // If the url doesn't already have GetCapabilities, add it in
                getCapabilitiesUrl = new URL(serviceUrlStr);
                final String getCapabilitiesQuery = "VERSION=1.1.1&SERVICE=WMS&REQUEST=GetCapabilities";
                if (getCapabilitiesUrl.getQuery() == null) {
                    getCapabilitiesUrl = new URL(serviceUrlStr + "?" + getCapabilitiesQuery);
                } else if (!getCapabilitiesUrl.getQuery().isEmpty() && !getCapabilitiesUrl.getQuery().endsWith("&")) {
                    getCapabilitiesUrl = new URL(serviceUrlStr + "&" + getCapabilitiesQuery);
                } else {
                    getCapabilitiesUrl = new URL(serviceUrlStr + getCapabilitiesQuery);
                }
            } else {
                // Otherwise assume it's a good URL and let the subsequent error
                // handling systems deal with problems
                getCapabilitiesUrl = new URL(serviceUrlStr);
            }
            serviceUrl = new URL(serviceUrlStr);
        } catch (HeadlessException e) {
            return;
        }

        Main.info("GET " + getCapabilitiesUrl.toString());
        URLConnection openConnection = Utils.openHttpConnection(getCapabilitiesUrl);
        InputStream inputStream = openConnection.getInputStream();
        BufferedReader br = new BufferedReader(UTFInputStreamReader.create(inputStream));
        String line;
        StringBuilder ba = new StringBuilder();
        try {
            while ((line = br.readLine()) != null) {
                ba.append(line);
                ba.append("\n");
            }
        } finally {
            br.close();
        }
        String incomingData = ba.toString();

        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setValidating(false);
            builderFactory.setNamespaceAware(true);
            DocumentBuilder builder = null;
            builder = builderFactory.newDocumentBuilder();
            builder.setEntityResolver(new EntityResolver() {
                @Override
                public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                    Main.info("Ignoring DTD " + publicId + ", " + systemId);
                    return new InputSource(new StringReader(""));
                }
            });
            Document document = null;
            document = builder.parse(new InputSource(new StringReader(incomingData)));

            // Some WMS service URLs specify a different base URL for their GetMap service
            Element child = getChild(document.getDocumentElement(), "Capability");
            child = getChild(child, "Request");
            child = getChild(child, "GetMap");

            formats = new ArrayList<String>(Utils.transform(getChildren(child, "Format"), new Utils.Function<Element, String>() {
                @Override
                public String apply(Element x) {
                    return x.getTextContent();
                }
            }));

            child = getChild(child, "DCPType");
            child = getChild(child, "HTTP");
            child = getChild(child, "Get");
            child = getChild(child, "OnlineResource");
            if (child != null) {
                String baseURL = child.getAttribute("xlink:href");
                if (baseURL != null && !baseURL.equals(serviceUrlStr)) {
                    Main.info("GetCapabilities specifies a different service URL: " + baseURL);
                    serviceUrl = new URL(baseURL);
                }
            }

            Element capabilityElem = getChild(document.getDocumentElement(), "Capability");
            List<Element> children = getChildren(capabilityElem, "Layer");
            layers = parseLayers(children, new HashSet<String>());
        } catch (Exception e) {
            throw new WMSGetCapabilitiesException(e, incomingData);
        }

    }

    public ImageryInfo toImageryInfo(String name, Collection<LayerDetails> selectedLayers) {
        ImageryInfo i = new ImageryInfo(name, buildGetMapUrl(selectedLayers));
        if (selectedLayers != null) {
            HashSet<String> proj = new HashSet<String>();
            for (WMSImagery.LayerDetails l : selectedLayers) {
                proj.addAll(l.getProjections());
            }
            i.setServerProjections(proj);
        }
        return i;
    }

    private List<LayerDetails> parseLayers(List<Element> children, Set<String> parentCrs) {
        List<LayerDetails> details = new ArrayList<LayerDetails>(children.size());
        for (Element element : children) {
            details.add(parseLayer(element, parentCrs));
        }
        return details;
    }

    private LayerDetails parseLayer(Element element, Set<String> parentCrs) {
        String name = getChildContent(element, "Title", null, null);
        String ident = getChildContent(element, "Name", null, null);

        // The set of supported CRS/SRS for this layer
        Set<String> crsList = new HashSet<String>();
        // ...including this layer's already-parsed parent projections
        crsList.addAll(parentCrs);

        // Parse the CRS/SRS pulled out of this layer's XML element
        // I think CRS and SRS are the same at this point
        List<Element> crsChildren = getChildren(element, "CRS");
        crsChildren.addAll(getChildren(element, "SRS"));
        for (Element child : crsChildren) {
            String crs = (String) getContent(child);
            if (!crs.isEmpty()) {
                String upperCase = crs.trim().toUpperCase();
                crsList.add(upperCase);
            }
        }

        // Check to see if any of the specified projections are supported by JOSM
        boolean josmSupportsThisLayer = false;
        for (String crs : crsList) {
            josmSupportsThisLayer |= isProjSupported(crs);
        }

        Bounds bounds = null;
        Element bboxElem = getChild(element, "EX_GeographicBoundingBox");
        if (bboxElem != null) {
            // Attempt to use EX_GeographicBoundingBox for bounding box
            double left = Double.parseDouble(getChildContent(bboxElem, "westBoundLongitude", null, null));
            double top = Double.parseDouble(getChildContent(bboxElem, "northBoundLatitude", null, null));
            double right = Double.parseDouble(getChildContent(bboxElem, "eastBoundLongitude", null, null));
            double bot = Double.parseDouble(getChildContent(bboxElem, "southBoundLatitude", null, null));
            bounds = new Bounds(bot, left, top, right);
        } else {
            // If that's not available, try LatLonBoundingBox
            bboxElem = getChild(element, "LatLonBoundingBox");
            if (bboxElem != null) {
                double left = Double.parseDouble(bboxElem.getAttribute("minx"));
                double top = Double.parseDouble(bboxElem.getAttribute("maxy"));
                double right = Double.parseDouble(bboxElem.getAttribute("maxx"));
                double bot = Double.parseDouble(bboxElem.getAttribute("miny"));
                bounds = new Bounds(bot, left, top, right);
            }
        }

        List<Element> layerChildren = getChildren(element, "Layer");
        List<LayerDetails> childLayers = parseLayers(layerChildren, crsList);

        return new LayerDetails(name, ident, crsList, josmSupportsThisLayer, bounds, childLayers);
    }

    private boolean isProjSupported(String crs) {
        for (ProjectionChoice pc : ProjectionPreference.getProjectionChoices()) {
            if (pc.getPreferencesFromCode(crs) != null) return true;
        }
        return false;
    }

    private static String getChildContent(Element parent, String name, String missing, String empty) {
        Element child = getChild(parent, name);
        if (child == null)
            return missing;
        else {
            String content = (String) getContent(child);
            return (!content.isEmpty()) ? content : empty;
        }
    }

    private static Object getContent(Element element) {
        NodeList nl = element.getChildNodes();
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            switch (node.getNodeType()) {
                case Node.ELEMENT_NODE:
                    return node;
                case Node.CDATA_SECTION_NODE:
                case Node.TEXT_NODE:
                    content.append(node.getNodeValue());
                    break;
            }
        }
        return content.toString().trim();
    }

    private static List<Element> getChildren(Element parent, String name) {
        List<Element> retVal = new ArrayList<Element>();
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element && name.equals(child.getNodeName())) {
                retVal.add((Element) child);
            }
        }
        return retVal;
    }

    private static Element getChild(Element parent, String name) {
        if (parent == null)
            return null;
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element && name.equals(child.getNodeName()))
                return (Element) child;
        }
        return null;
    }

    public static class LayerDetails {

        public final String name;
        public final String ident;
        public final List<LayerDetails> children;
        public final Bounds bounds;
        public final Set<String> crsList;
        public final boolean supported;

        public LayerDetails(String name, String ident, Set<String> crsList,
                            boolean supportedLayer, Bounds bounds,
                            List<LayerDetails> childLayers) {
            this.name = name;
            this.ident = ident;
            this.supported = supportedLayer;
            this.children = childLayers;
            this.bounds = bounds;
            this.crsList = crsList;
        }

        public boolean isSupported() {
            return this.supported;
        }

        public Set<String> getProjections() {
            return crsList;
        }

        @Override
        public String toString() {
            if (this.name == null || this.name.isEmpty())
                return this.ident;
            else
                return this.name;
        }

    }
}
