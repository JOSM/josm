// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.imagery;

import java.awt.HeadlessException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.HttpClient.Response;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class represents the capabilities of a WMS imagery server.
 */
public class WMSImagery {

    private static final class ChildIterator implements Iterator<Element> {
        private Element child;

        ChildIterator(Element parent) {
            child = advanceToElement(parent.getFirstChild());
        }

        private static Element advanceToElement(Node firstChild) {
            Node node = firstChild;
            while (node != null && !(node instanceof Element)) {
                node = node.getNextSibling();
            }
            return (Element) node;
        }

        @Override
        public boolean hasNext() {
            return child != null;
        }

        @Override
        public Element next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No next sibling.");
            }
            Element next = child;
            child = advanceToElement(child.getNextSibling());
            return next;
        }
    }

    /**
     * An exception that is thrown if there was an error while getting the capabilities of the WMS server.
     */
    public static class WMSGetCapabilitiesException extends Exception {
        private final String incomingData;

        /**
         * Constructs a new {@code WMSGetCapabilitiesException}
         * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
         * @param incomingData the answer from WMS server
         */
        public WMSGetCapabilitiesException(Throwable cause, String incomingData) {
            super(cause);
            this.incomingData = incomingData;
        }

        /**
         * Constructs a new {@code WMSGetCapabilitiesException}
         * @param message   the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method
         * @param incomingData the answer from the server
         * @since 10520
         */
        public WMSGetCapabilitiesException(String message, String incomingData) {
            super(message);
            this.incomingData = incomingData;
        }

        /**
         * The data that caused this exception.
         * @return The server response to the capabilities request.
         */
        public String getIncomingData() {
            return incomingData;
        }
    }

    private List<LayerDetails> layers;
    private URL serviceUrl;
    private List<String> formats;

    /**
     * Returns the list of layers.
     * @return the list of layers
     */
    public List<LayerDetails> getLayers() {
        return Collections.unmodifiableList(layers);
    }

    /**
     * Returns the service URL.
     * @return the service URL
     */
    public URL getServiceUrl() {
        return serviceUrl;
    }

    /**
     * Returns the list of supported formats.
     * @return the list of supported formats
     */
    public List<String> getFormats() {
        return Collections.unmodifiableList(formats);
    }

    /**
     * Gets the preffered format for this imagery layer.
     * @return The preffered format as mime type.
     */
    public String getPreferredFormats() {
        if (formats.contains("image/jpeg")) {
            return "image/jpeg";
        } else if (formats.contains("image/png")) {
            return "image/png";
        } else if (formats.isEmpty()) {
            return null;
        } else {
            return formats.get(0);
        }
    }

    String buildRootUrl() {
        if (serviceUrl == null) {
            return null;
        }
        StringBuilder a = new StringBuilder(serviceUrl.getProtocol());
        a.append("://").append(serviceUrl.getHost());
        if (serviceUrl.getPort() != -1) {
            a.append(':').append(serviceUrl.getPort());
        }
        a.append(serviceUrl.getPath()).append('?');
        if (serviceUrl.getQuery() != null) {
            a.append(serviceUrl.getQuery());
            if (!serviceUrl.getQuery().isEmpty() && !serviceUrl.getQuery().endsWith("&")) {
                a.append('&');
            }
        }
        return a.toString();
    }

    /**
     * Returns the URL for the "GetMap" WMS request in JPEG format.
     * @param selectedLayers the list of selected layers, matching the "LAYERS" WMS request argument
     * @return the URL for the "GetMap" WMS request
     */
    public String buildGetMapUrl(Collection<LayerDetails> selectedLayers) {
        return buildGetMapUrl(selectedLayers, "image/jpeg");
    }

    /**
     * Returns the URL for the "GetMap" WMS request.
     * @param selectedLayers the list of selected layers, matching the "LAYERS" WMS request argument
     * @param format the requested image format, matching the "FORMAT" WMS request argument
     * @return the URL for the "GetMap" WMS request
     */
    public String buildGetMapUrl(Collection<LayerDetails> selectedLayers, String format) {
        return buildRootUrl() + "FORMAT=" + format + (imageFormatHasTransparency(format) ? "&TRANSPARENT=TRUE" : "")
                + "&VERSION=1.1.1&SERVICE=WMS&REQUEST=GetMap&LAYERS="
                + selectedLayers.stream().map(x -> x.ident).collect(Collectors.joining(","))
                + "&STYLES=&SRS={proj}&WIDTH={width}&HEIGHT={height}&BBOX={bbox}";
    }

    /**
     * Attempts WMS "GetCapabilities" request and initializes internal variables if successful.
     * @param serviceUrlStr WMS service URL
     * @throws IOException if any I/O errors occurs
     * @throws WMSGetCapabilitiesException if the WMS server replies a ServiceException
     */
    public void attemptGetCapabilities(String serviceUrlStr) throws IOException, WMSGetCapabilitiesException {
        URL getCapabilitiesUrl = null;
        try {
            if (!Pattern.compile(".*GetCapabilities.*", Pattern.CASE_INSENSITIVE).matcher(serviceUrlStr).matches()) {
                // If the url doesn't already have GetCapabilities, add it in
                getCapabilitiesUrl = new URL(serviceUrlStr);
                final String getCapabilitiesQuery = "VERSION=1.1.1&SERVICE=WMS&REQUEST=GetCapabilities";
                if (getCapabilitiesUrl.getQuery() == null) {
                    getCapabilitiesUrl = new URL(serviceUrlStr + '?' + getCapabilitiesQuery);
                } else if (!getCapabilitiesUrl.getQuery().isEmpty() && !getCapabilitiesUrl.getQuery().endsWith("&")) {
                    getCapabilitiesUrl = new URL(serviceUrlStr + '&' + getCapabilitiesQuery);
                } else {
                    getCapabilitiesUrl = new URL(serviceUrlStr + getCapabilitiesQuery);
                }
            } else {
                // Otherwise assume it's a good URL and let the subsequent error
                // handling systems deal with problems
                getCapabilitiesUrl = new URL(serviceUrlStr);
            }
            // Make sure we don't keep GetCapabilities request in service URL
            serviceUrl = new URL(serviceUrlStr.replace("REQUEST=GetCapabilities", "").replace("&&", "&"));
        } catch (HeadlessException e) {
            Logging.warn(e);
            return;
        }

        final Response response = HttpClient.create(getCapabilitiesUrl).connect();

        if (response.getResponseCode() >= 400) {
            throw new WMSGetCapabilitiesException(response.getResponseMessage(), response.fetchContent());
        }

        parseCapabilities(serviceUrlStr, response.getContent());
    }

    void parseCapabilities(String serviceUrlStr, InputStream contentStream) throws IOException, WMSGetCapabilitiesException {
        String incomingData = null;
        try {
            DocumentBuilder builder = Utils.newSafeDOMBuilder();
            builder.setEntityResolver((publicId, systemId) -> {
                Logging.info("Ignoring DTD " + publicId + ", " + systemId);
                return new InputSource(new StringReader(""));
            });
            Document document = builder.parse(contentStream);
            Element root = document.getDocumentElement();

            try {
                StringWriter writer = new StringWriter();
                TransformerFactory.newInstance().newTransformer().transform(new DOMSource(document), new StreamResult(writer));
                incomingData = writer.getBuffer().toString();
                Logging.debug("Server response to Capabilities request:");
                Logging.debug(incomingData);
            } catch (TransformerFactoryConfigurationError | TransformerException e) {
                Logging.warn(e);
            }

            // Check if the request resulted in ServiceException
            if ("ServiceException".equals(root.getTagName())) {
                throw new WMSGetCapabilitiesException(root.getTextContent(), incomingData);
            }

            // Some WMS service URLs specify a different base URL for their GetMap service
            Element child = getChild(root, "Capability");
            child = getChild(child, "Request");
            child = getChild(child, "GetMap");

            formats = getChildrenStream(child, "Format")
                    .map(Node::getTextContent)
                    .filter(WMSImagery::isImageFormatSupportedWarn)
                    .collect(Collectors.toList());

            child = getChild(child, "DCPType");
            child = getChild(child, "HTTP");
            child = getChild(child, "Get");
            child = getChild(child, "OnlineResource");
            if (child != null) {
                String baseURL = child.getAttribute("xlink:href");
                if (!baseURL.equals(serviceUrlStr)) {
                    URL newURL = new URL(baseURL);
                    if (newURL.getAuthority() != null) {
                        Logging.info("GetCapabilities specifies a different service URL: " + baseURL);
                        serviceUrl = newURL;
                    }
                }
            }

            Element capabilityElem = getChild(root, "Capability");
            List<Element> children = getChildren(capabilityElem, "Layer");
            layers = parseLayers(children, new HashSet<String>());
        } catch (MalformedURLException | ParserConfigurationException | SAXException e) {
            throw new WMSGetCapabilitiesException(e, incomingData);
        }
    }

    private static boolean isImageFormatSupportedWarn(String format) {
        boolean isFormatSupported = isImageFormatSupported(format);
        if (!isFormatSupported) {
            Logging.info("Skipping unsupported image format {0}", format);
        }
        return isFormatSupported;
    }

    static boolean isImageFormatSupported(final String format) {
        return ImageIO.getImageReadersByMIMEType(format).hasNext()
                // handles image/tiff image/tiff8 image/geotiff image/geotiff8
                || isImageFormatSupported(format, "tiff", "geotiff")
                || isImageFormatSupported(format, "png")
                || isImageFormatSupported(format, "svg")
                || isImageFormatSupported(format, "bmp");
    }

    static boolean isImageFormatSupported(String format, String... mimeFormats) {
        for (String mime : mimeFormats) {
            if (format.startsWith("image/" + mime)) {
                return ImageIO.getImageReadersBySuffix(mimeFormats[0]).hasNext();
            }
        }
        return false;
    }

    static boolean imageFormatHasTransparency(final String format) {
        return format != null && (format.startsWith("image/png") || format.startsWith("image/gif")
                || format.startsWith("image/svg") || format.startsWith("image/tiff"));
    }

    /**
     * Returns a new {@code ImageryInfo} describing the given service name and selected WMS layers.
     * @param name service name
     * @param selectedLayers selected WMS layers
     * @return a new {@code ImageryInfo} describing the given service name and selected WMS layers
     */
    public ImageryInfo toImageryInfo(String name, Collection<LayerDetails> selectedLayers) {
        ImageryInfo i = new ImageryInfo(name, buildGetMapUrl(selectedLayers));
        if (selectedLayers != null) {
            Set<String> proj = new HashSet<>();
            for (WMSImagery.LayerDetails l : selectedLayers) {
                proj.addAll(l.getProjections());
            }
            i.setServerProjections(proj);
        }
        return i;
    }

    private List<LayerDetails> parseLayers(List<Element> children, Set<String> parentCrs) {
        List<LayerDetails> details = new ArrayList<>(children.size());
        for (Element element : children) {
            details.add(parseLayer(element, parentCrs));
        }
        return details;
    }

    private LayerDetails parseLayer(Element element, Set<String> parentCrs) {
        String name = getChildContent(element, "Title", null, null);
        String ident = getChildContent(element, "Name", null, null);
        String abstr = getChildContent(element, "Abstract", null, null);

        // The set of supported CRS/SRS for this layer
        Set<String> crsList = new HashSet<>();
        // ...including this layer's already-parsed parent projections
        crsList.addAll(parentCrs);

        // Parse the CRS/SRS pulled out of this layer's XML element
        // I think CRS and SRS are the same at this point
        getChildrenStream(element)
            .filter(child -> "CRS".equals(child.getNodeName()) || "SRS".equals(child.getNodeName()))
            .map(WMSImagery::getContent)
            .filter(crs -> !crs.isEmpty())
            .map(crs -> crs.trim().toUpperCase(Locale.ENGLISH))
            .forEach(crsList::add);

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
                double left = getDecimalDegree(bboxElem, "minx");
                double top = getDecimalDegree(bboxElem, "maxy");
                double right = getDecimalDegree(bboxElem, "maxx");
                double bot = getDecimalDegree(bboxElem, "miny");
                bounds = new Bounds(bot, left, top, right);
            }
        }

        List<Element> layerChildren = getChildren(element, "Layer");
        List<LayerDetails> childLayers = parseLayers(layerChildren, crsList);

        return new LayerDetails(name, ident, abstr, crsList, josmSupportsThisLayer, bounds, childLayers);
    }

    private static double getDecimalDegree(Element elem, String attr) {
        // Some real-world WMS servers use a comma instead of a dot as decimal separator (seen in Polish WMS server)
        return Double.parseDouble(elem.getAttribute(attr).replace(',', '.'));
    }

    private static boolean isProjSupported(String crs) {
        return Projections.getProjectionByCode(crs) != null;
    }

    private static String getChildContent(Element parent, String name, String missing, String empty) {
        Element child = getChild(parent, name);
        if (child == null)
            return missing;
        else {
            String content = getContent(child);
            return (!content.isEmpty()) ? content : empty;
        }
    }

    private static String getContent(Element element) {
        NodeList nl = element.getChildNodes();
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            switch (node.getNodeType()) {
                case Node.ELEMENT_NODE:
                    content.append(getContent((Element) node));
                    break;
                case Node.CDATA_SECTION_NODE:
                case Node.TEXT_NODE:
                    content.append(node.getNodeValue());
                    break;
                default: // Do nothing
            }
        }
        return content.toString().trim();
    }

    private static Stream<Element> getChildrenStream(Element parent) {
        if (parent == null) {
            // ignore missing elements
            return Stream.empty();
        } else {
            Iterable<Element> it = () -> new ChildIterator(parent);
            return StreamSupport.stream(it.spliterator(), false);
        }
    }

    private static Stream<Element> getChildrenStream(Element parent, String name) {
        return getChildrenStream(parent).filter(child -> name.equals(child.getNodeName()));
    }

    private static List<Element> getChildren(Element parent, String name) {
        return getChildrenStream(parent, name).collect(Collectors.toList());
    }

    private static Element getChild(Element parent, String name) {
        return getChildrenStream(parent, name).findFirst().orElse(null);
    }

    /**
     * The details of a layer of this WMS server.
     */
    public static class LayerDetails {

        /**
         * The layer name (WMS {@code Title})
         */
        public final String name;
        /**
         * The layer ident (WMS {@code Name})
         */
        public final String ident;
        /**
         * The layer abstract (WMS {@code Abstract})
         * @since 13199
         */
        public final String abstr;
        /**
         * The child layers of this layer
         */
        public final List<LayerDetails> children;
        /**
         * The bounds this layer can be used for
         */
        public final Bounds bounds;
        /**
         * the CRS/SRS pulled out of this layer's XML element
         */
        public final Set<String> crsList;
        /**
         * {@code true} if any of the specified projections are supported by JOSM
         */
        public final boolean supported;

        /**
         * Constructs a new {@code LayerDetails}.
         * @param name The layer name (WMS {@code Title})
         * @param ident The layer ident (WMS {@code Name})
         * @param abstr The layer abstract (WMS {@code Abstract})
         * @param crsList The CRS/SRS pulled out of this layer's XML element
         * @param supportedLayer {@code true} if any of the specified projections are supported by JOSM
         * @param bounds The bounds this layer can be used for
         * @param childLayers The child layers of this layer
         * @since 13199
         */
        public LayerDetails(String name, String ident, String abstr, Set<String> crsList, boolean supportedLayer, Bounds bounds,
                List<LayerDetails> childLayers) {
            this.name = name;
            this.ident = ident;
            this.abstr = abstr;
            this.supported = supportedLayer;
            this.children = childLayers;
            this.bounds = bounds;
            this.crsList = crsList;
        }

        /**
         * Determines if any of the specified projections are supported by JOSM.
         * @return {@code true} if any of the specified projections are supported by JOSM
         */
        public boolean isSupported() {
            return this.supported;
        }

        /**
         * Returns the CRS/SRS pulled out of this layer's XML element.
         * @return the CRS/SRS pulled out of this layer's XML element
         */
        public Set<String> getProjections() {
            return crsList;
        }

        @Override
        public String toString() {
            String baseName = (name == null || name.isEmpty()) ? ident : name;
            return abstr == null || abstr.equalsIgnoreCase(baseName) ? baseName : baseName + " (" + abstr + ')';
        }
    }
}
