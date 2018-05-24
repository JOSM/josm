// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.imagery;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.imagery.DefaultLayer;
import org.openstreetmap.josm.data.imagery.GetCapabilitiesParseHelper;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.LayerDetails;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * This class represents the capabilities of a WMS imagery server.
 */
public class WMSImagery {

    private static final String CAPABILITIES_QUERY_STRING = "SERVICE=WMS&REQUEST=GetCapabilities";

    /**
     * WMS namespace address
     */
    public static final String WMS_NS_URL = "http://www.opengis.net/wms";

    // CHECKSTYLE.OFF: SingleSpaceSeparator
    // WMS 1.0 - 1.3.0
    private static final QName CAPABILITITES_ROOT_130 = new QName("WMS_Capabilities", WMS_NS_URL);
    private static final QName QN_ABSTRACT            = new QName(WMS_NS_URL, "Abstract");
    private static final QName QN_CAPABILITY          = new QName(WMS_NS_URL, "Capability");
    private static final QName QN_CRS                 = new QName(WMS_NS_URL, "CRS");
    private static final QName QN_DCPTYPE             = new QName(WMS_NS_URL, "DCPType");
    private static final QName QN_FORMAT              = new QName(WMS_NS_URL, "Format");
    private static final QName QN_GET                 = new QName(WMS_NS_URL, "Get");
    private static final QName QN_GETMAP              = new QName(WMS_NS_URL, "GetMap");
    private static final QName QN_HTTP                = new QName(WMS_NS_URL, "HTTP");
    private static final QName QN_LAYER               = new QName(WMS_NS_URL, "Layer");
    private static final QName QN_NAME                = new QName(WMS_NS_URL, "Name");
    private static final QName QN_REQUEST             = new QName(WMS_NS_URL, "Request");
    private static final QName QN_SERVICE             = new QName(WMS_NS_URL, "Service");
    private static final QName QN_STYLE               = new QName(WMS_NS_URL, "Style");
    private static final QName QN_TITLE               = new QName(WMS_NS_URL, "Title");
    private static final QName QN_BOUNDINGBOX         = new QName(WMS_NS_URL, "BoundingBox");
    private static final QName QN_EX_GEOGRAPHIC_BBOX  = new QName(WMS_NS_URL, "EX_GeographicBoundingBox");
    private static final QName QN_WESTBOUNDLONGITUDE  = new QName(WMS_NS_URL, "westBoundLongitude");
    private static final QName QN_EASTBOUNDLONGITUDE  = new QName(WMS_NS_URL, "eastBoundLongitude");
    private static final QName QN_SOUTHBOUNDLATITUDE  = new QName(WMS_NS_URL, "southBoundLatitude");
    private static final QName QN_NORTHBOUNDLATITUDE  = new QName(WMS_NS_URL, "northBoundLatitude");
    private static final QName QN_ONLINE_RESOURCE     = new QName(WMS_NS_URL, "OnlineResource");

    // WMS 1.1 - 1.1.1
    private static final QName CAPABILITIES_ROOT_111 = new QName("WMT_MS_Capabilities");
    private static final QName QN_SRS                = new QName("SRS");
    private static final QName QN_LATLONBOUNDINGBOX  = new QName("LatLonBoundingBox");

    // CHECKSTYLE.ON: SingleSpaceSeparator

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

    private final Map<String, String> headers = new ConcurrentHashMap<>();
    private String version = "1.1.1"; // default version
    private String getMapUrl;
    private URL capabilitiesUrl;
    private final List<String> formats = new ArrayList<>();
    private List<LayerDetails> layers = new ArrayList<>();

    private String title;

    /**
     * Make getCapabilities request towards given URL
     * @param url service url
     * @throws IOException when connection error when fetching get capabilities document
     * @throws WMSGetCapabilitiesException when there are errors when parsing get capabilities document
     */
    public WMSImagery(String url) throws IOException, WMSGetCapabilitiesException {
        this(url, null);
    }

    /**
     * Make getCapabilities request towards given URL using headers
     * @param url service url
     * @param headers HTTP headers to be sent with request
     * @throws IOException when connection error when fetching get capabilities document
     * @throws WMSGetCapabilitiesException when there are errors when parsing get capabilities document
     */
    public WMSImagery(String url, Map<String, String> headers) throws IOException, WMSGetCapabilitiesException {
        if (headers != null) {
            this.headers.putAll(headers);
        }

        IOException savedExc = null;
        String workingAddress = null;
        url_search:
        for (String z: new String[]{
                normalizeUrl(url),
                url,
                url + CAPABILITIES_QUERY_STRING,
        }) {
            for (String ver: new String[]{"", "&VERSION=1.3.0", "&VERSION=1.1.1"}) {
                try {
                    attemptGetCapabilities(z + ver);
                    workingAddress = z;
                    calculateChildren();
                    // clear saved exception - we've got something working
                    savedExc = null;
                    break url_search;
                } catch (IOException e) {
                    savedExc = e;
                    Logging.warn(e);
                }
            }
        }

        if (workingAddress != null) {
            try {
                capabilitiesUrl = new URL(workingAddress);
            } catch (MalformedURLException e) {
                if (savedExc != null) {
                    savedExc = e;
                }
                try {
                    capabilitiesUrl = new File(workingAddress).toURI().toURL();
                } catch (MalformedURLException e1) { // NOPMD
                    // do nothing, raise original exception
                    Logging.trace(e1);
                }
            }
        }

        if (savedExc != null) {
            throw savedExc;
        }
    }

    private void calculateChildren() {
        Map<LayerDetails, List<LayerDetails>> layerChildren = layers.stream()
                .filter(x -> x.getParent() != null) // exclude top-level elements
                .collect(Collectors.groupingBy(LayerDetails::getParent));
        for (LayerDetails ld: layers) {
            if (layerChildren.containsKey(ld)) {
                ld.setChildren(layerChildren.get(ld));
            }
        }
        // leave only top-most elements in the list
        layers = layers.stream().filter(x -> x.getParent() == null).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Returns the list of top-level layers.
     * @return the list of top-level layers
     */
    public List<LayerDetails> getLayers() {
        return Collections.unmodifiableList(layers);
    }

    /**
     * Returns the list of supported formats.
     * @return the list of supported formats
     */
    public Collection<String> getFormats() {
        return Collections.unmodifiableList(formats);
    }

    /**
     * Gets the preferred format for this imagery layer.
     * @return The preferred format as mime type.
     */
    public String getPreferredFormat() {
        if (formats.contains("image/png")) {
            return "image/png";
        } else if (formats.contains("image/jpeg")) {
            return "image/jpeg";
        } else if (formats.isEmpty()) {
            return null;
        } else {
            return formats.get(0);
        }
    }

    /**
     * @return root URL of services in this GetCapabilities
     */
    public String buildRootUrl() {
        if (getMapUrl == null && capabilitiesUrl == null) {
            return null;
        }
        if (getMapUrl != null) {
            return getMapUrl;
        }

        URL serviceUrl = capabilitiesUrl;
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
     * Returns URL for accessing GetMap service. String will contain following parameters:
     * * {proj} - that needs to be replaced with projection (one of {@link #getServerProjections(List)})
     * * {width} - that needs to be replaced with width of the tile
     * * {height} - that needs to be replaces with height of the tile
     * * {bbox} - that needs to be replaced with area that should be fetched (in {proj} coordinates)
     *
     * Format of the response will be calculated using {@link #getPreferredFormat()}
     *
     * @param selectedLayers list of DefaultLayer selection of layers to be shown
     * @param transparent whether returned images should contain transparent pixels (if supported by format)
     * @return URL template for GetMap service containing
     */
    public String buildGetMapUrl(List<DefaultLayer> selectedLayers, boolean transparent) {
        return buildGetMapUrl(
                getLayers(selectedLayers),
                selectedLayers.stream().map(DefaultLayer::getStyle).collect(Collectors.toList()),
                transparent);
    }

    /**
     * @param selectedLayers selected layers as subset of the tree returned by {@link #getLayers()}
     * @param selectedStyles selected styles for all selectedLayers
     * @param transparent whether returned images should contain transparent pixels (if supported by format)
     * @return URL template for GetMap service
     * @see #buildGetMapUrl(List, boolean)
     */
    public String buildGetMapUrl(List<LayerDetails> selectedLayers, List<String> selectedStyles, boolean transparent) {
        return buildGetMapUrl(
                selectedLayers.stream().map(LayerDetails::getName).collect(Collectors.toList()),
                selectedStyles,
                getPreferredFormat(),
                transparent);
    }

    /**
     * @param selectedLayers selected layers as list of strings
     * @param selectedStyles selected styles of layers as list of strings
     * @param format format of the response - one of {@link #getFormats()}
     * @param transparent whether returned images should contain transparent pixels (if supported by format)
     * @return URL template for GetMap service
     * @see #buildGetMapUrl(List, boolean)
     */
    public String buildGetMapUrl(List<String> selectedLayers,
            Collection<String> selectedStyles,
            String format,
            boolean transparent) {

        Utils.ensure(selectedStyles == null || selectedLayers.size() == selectedStyles.size(),
                tr("Styles size {0} does not match layers size {1}"),
                selectedStyles == null ? 0 : selectedStyles.size(),
                        selectedLayers.size());

        return buildRootUrl() + "FORMAT=" + format + ((imageFormatHasTransparency(format) && transparent) ? "&TRANSPARENT=TRUE" : "")
                + "&VERSION=" + this.version + "&SERVICE=WMS&REQUEST=GetMap&LAYERS="
                + selectedLayers.stream().collect(Collectors.joining(","))
                + "&STYLES="
                + (selectedStyles != null ? Utils.join(",", selectedStyles) : "")
                + "&"
                + (belowWMS130() ? "SRS" : "CRS")
                + "={proj}&WIDTH={width}&HEIGHT={height}&BBOX={bbox}";
    }

    private boolean tagEquals(QName a, QName b) {
        boolean ret = a.equals(b);
        if (ret) {
            return ret;
        }

        if (belowWMS130()) {
            return a.getLocalPart().equals(b.getLocalPart());
        }

        return false;
    }

    private void attemptGetCapabilities(String url) throws IOException, WMSGetCapabilitiesException {
        Logging.debug("Trying WMS getcapabilities with url {0}", url);
        try (CachedFile cf = new CachedFile(url); InputStream in = cf.setHttpHeaders(headers).
                setMaxAge(7 * CachedFile.DAYS).
                setCachingStrategy(CachedFile.CachingStrategy.IfModifiedSince).
                getInputStream()) {

            try {
                XMLStreamReader reader = GetCapabilitiesParseHelper.getReader(in);
                for (int event = reader.getEventType(); reader.hasNext(); event = reader.next()) {
                    if (event == XMLStreamReader.START_ELEMENT) {
                        if (tagEquals(CAPABILITIES_ROOT_111, reader.getName())) {
                            // version 1.1.1
                            this.version = reader.getAttributeValue(null, "version");
                            if (this.version == null) {
                                this.version = "1.1.1";
                            }
                        }
                        if (tagEquals(CAPABILITITES_ROOT_130, reader.getName())) {
                            this.version = reader.getAttributeValue(WMS_NS_URL, "version");
                        }
                        if (tagEquals(QN_SERVICE, reader.getName())) {
                            parseService(reader);
                        }

                        if (tagEquals(QN_CAPABILITY, reader.getName())) {
                            parseCapability(reader);
                        }
                    }
                }
            } catch (XMLStreamException e) {
                String content = new String(cf.getByteContent(), UTF_8);
                cf.clear(); // if there is a problem with parsing of the file, remove it from the cache
                throw new WMSGetCapabilitiesException(e, content);
            }
        }
    }

    private void parseService(XMLStreamReader reader) throws XMLStreamException {
        if (GetCapabilitiesParseHelper.moveReaderToTag(reader, this::tagEquals, QN_TITLE)) {
            this.title = reader.getElementText();
            // CHECKSTYLE.OFF: EmptyBlock
            for (int event = reader.getEventType();
                    reader.hasNext() && !(event == XMLStreamReader.END_ELEMENT && tagEquals(QN_SERVICE, reader.getName()));
                    event = reader.next()) {
                // empty loop, just move reader to the end of Service tag, if moveReaderToTag return false, it's already done
            }
            // CHECKSTYLE.ON: EmptyBlock
        }
    }

    private void parseCapability(XMLStreamReader reader) throws XMLStreamException {
        for (int event = reader.getEventType();
                reader.hasNext() && !(event == XMLStreamReader.END_ELEMENT && tagEquals(QN_CAPABILITY, reader.getName()));
                event = reader.next()) {

            if (event == XMLStreamReader.START_ELEMENT) {
                if (tagEquals(QN_REQUEST, reader.getName())) {
                    parseRequest(reader);
                }
                if (tagEquals(QN_LAYER, reader.getName())) {
                    parseLayer(reader, null);
                }
            }
        }
    }

    private void parseRequest(XMLStreamReader reader) throws XMLStreamException {
        String mode = "";
        String getMapUrl = "";
        if (GetCapabilitiesParseHelper.moveReaderToTag(reader, this::tagEquals, QN_GETMAP)) {
            for (int event = reader.getEventType();
                    reader.hasNext() && !(event == XMLStreamReader.END_ELEMENT && tagEquals(QN_GETMAP, reader.getName()));
                    event = reader.next()) {

                if (event == XMLStreamReader.START_ELEMENT) {
                    if (tagEquals(QN_FORMAT, reader.getName())) {
                        String value = reader.getElementText();
                        if (isImageFormatSupportedWarn(value) && !this.formats.contains(value)) {
                            this.formats.add(value);
                        }
                    }
                    if (tagEquals(QN_DCPTYPE, reader.getName()) && GetCapabilitiesParseHelper.moveReaderToTag(reader,
                            this::tagEquals, QN_HTTP, QN_GET)) {
                        mode = reader.getName().getLocalPart();
                        if (GetCapabilitiesParseHelper.moveReaderToTag(reader, this::tagEquals, QN_ONLINE_RESOURCE)) {
                            getMapUrl = reader.getAttributeValue(GetCapabilitiesParseHelper.XLINK_NS_URL, "href");
                        }
                        // TODO should we handle also POST?
                        if ("GET".equalsIgnoreCase(mode) && getMapUrl != null && !"".equals(getMapUrl)) {
                            this.getMapUrl = getMapUrl;
                        }
                    }
                }
            }
        }
    }

    private void parseLayer(XMLStreamReader reader, LayerDetails parentLayer) throws XMLStreamException {
        LayerDetails ret = new LayerDetails(parentLayer);
        for (int event = reader.next(); // start with advancing reader by one element to get the contents of the layer
                reader.hasNext() && !(event == XMLStreamReader.END_ELEMENT && tagEquals(QN_LAYER, reader.getName()));
                event = reader.next()) {

            if (event == XMLStreamReader.START_ELEMENT) {
                if (tagEquals(QN_NAME, reader.getName())) {
                    ret.setName(reader.getElementText());
                }
                if (tagEquals(QN_ABSTRACT, reader.getName())) {
                    ret.setAbstract(GetCapabilitiesParseHelper.getElementTextWithSubtags(reader));
                }
                if (tagEquals(QN_TITLE, reader.getName())) {
                    ret.setTitle(reader.getElementText());
                }
                if (tagEquals(QN_CRS, reader.getName())) {
                    ret.addCrs(reader.getElementText());
                }
                if (tagEquals(QN_SRS, reader.getName()) && belowWMS130()) {
                    ret.addCrs(reader.getElementText());
                }
                if (tagEquals(QN_STYLE, reader.getName())) {
                    parseAndAddStyle(reader, ret);
                }
                if (tagEquals(QN_LAYER, reader.getName())) {
                    parseLayer(reader, ret);
                }
                if (tagEquals(QN_EX_GEOGRAPHIC_BBOX, reader.getName()) && ret.getBounds() == null) {
                    ret.setBounds(parseExGeographic(reader));
                }
                if (tagEquals(QN_BOUNDINGBOX, reader.getName())) {
                    Projection conv;
                    if (belowWMS130()) {
                        conv = Projections.getProjectionByCode(reader.getAttributeValue(WMS_NS_URL, "SRS"));
                    } else {
                        conv = Projections.getProjectionByCode(reader.getAttributeValue(WMS_NS_URL, "CRS"));
                    }
                    if (ret.getBounds() == null && conv != null) {
                        ret.setBounds(parseBoundingBox(reader, conv));
                    }
                }
                if (tagEquals(QN_LATLONBOUNDINGBOX, reader.getName()) && belowWMS130() && ret.getBounds() == null) {
                    ret.setBounds(parseBoundingBox(reader, null));
                }
            }
        }
        this.layers.add(ret);
    }

    /**
     * @return if this service operates at protocol level below 1.3.0
     */
    public boolean belowWMS130() {
        return "1.1.1".equals(version) || "1.1".equals(version) || "1.0".equals(version);
    }

    private void parseAndAddStyle(XMLStreamReader reader, LayerDetails ld) throws XMLStreamException {
        String name = null;
        String title = null;
        for (int event = reader.getEventType();
                reader.hasNext() && !(event == XMLStreamReader.END_ELEMENT && tagEquals(QN_STYLE, reader.getName()));
                event = reader.next()) {
            if (event == XMLStreamReader.START_ELEMENT) {
                if (tagEquals(QN_NAME, reader.getName())) {
                    name = reader.getElementText();
                }
                if (tagEquals(QN_TITLE, reader.getName())) {
                    title = reader.getElementText();
                }
            }
        }
        if (name == null) {
            name = "";
        }
        ld.addStyle(name, title);
    }

    private Bounds parseExGeographic(XMLStreamReader reader) throws XMLStreamException {
        String minx = null, maxx = null, maxy = null, miny = null;

        for (int event = reader.getEventType();
                reader.hasNext() && !(event == XMLStreamReader.END_ELEMENT && tagEquals(QN_EX_GEOGRAPHIC_BBOX, reader.getName()));
                event = reader.next()) {
            if (event == XMLStreamReader.START_ELEMENT) {
                if (tagEquals(QN_WESTBOUNDLONGITUDE, reader.getName())) {
                    minx = reader.getElementText();
                }

                if (tagEquals(QN_EASTBOUNDLONGITUDE, reader.getName())) {
                    maxx = reader.getElementText();
                }

                if (tagEquals(QN_SOUTHBOUNDLATITUDE, reader.getName())) {
                    miny = reader.getElementText();
                }

                if (tagEquals(QN_NORTHBOUNDLATITUDE, reader.getName())) {
                    maxy = reader.getElementText();
                }
            }
        }
        return parseBBox(null, miny, minx, maxy, maxx);
    }

    private Bounds parseBoundingBox(XMLStreamReader reader, Projection conv) {
        Function<String, String> attrGetter = tag -> belowWMS130() ?
                reader.getAttributeValue(null, tag)
                : reader.getAttributeValue(WMS_NS_URL, tag);

                return parseBBox(
                        conv,
                        attrGetter.apply("miny"),
                        attrGetter.apply("minx"),
                        attrGetter.apply("maxy"),
                        attrGetter.apply("maxx")
                        );
    }

    private static Bounds parseBBox(Projection conv, String miny, String minx, String maxy, String maxx) {
        if (miny == null || minx == null || maxy == null || maxx == null) {
            return null;
        }
        if (conv != null) {
            return new Bounds(
                    conv.eastNorth2latlon(new EastNorth(getDecimalDegree(minx), getDecimalDegree(miny))),
                    conv.eastNorth2latlon(new EastNorth(getDecimalDegree(maxx), getDecimalDegree(maxy)))
                    );
        }
        return new Bounds(
                getDecimalDegree(miny),
                getDecimalDegree(minx),
                getDecimalDegree(maxy),
                getDecimalDegree(maxx)
                );
    }

    private static double getDecimalDegree(String value) {
        // Some real-world WMS servers use a comma instead of a dot as decimal separator (seen in Polish WMS server)
        return Double.parseDouble(value.replace(',', '.'));
    }

    private static String normalizeUrl(String serviceUrlStr) throws MalformedURLException {
        URL getCapabilitiesUrl = null;
        String ret = null;

        if (!Pattern.compile(".*GetCapabilities.*", Pattern.CASE_INSENSITIVE).matcher(serviceUrlStr).matches()) {
            // If the url doesn't already have GetCapabilities, add it in
            getCapabilitiesUrl = new URL(serviceUrlStr);
            ret = serviceUrlStr;
            if (getCapabilitiesUrl.getQuery() == null) {
                ret = serviceUrlStr + '?' + CAPABILITIES_QUERY_STRING;
            } else if (!getCapabilitiesUrl.getQuery().isEmpty() && !getCapabilitiesUrl.getQuery().endsWith("&")) {
                ret = serviceUrlStr + '&' + CAPABILITIES_QUERY_STRING;
            } else {
                ret = serviceUrlStr + CAPABILITIES_QUERY_STRING;
            }
        } else {
            // Otherwise assume it's a good URL and let the subsequent error
            // handling systems deal with problems
            ret = serviceUrlStr;
        }
        return ret;
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
     * Creates ImageryInfo object from this GetCapabilities document
     *
     * @param name name of imagery layer
     * @param selectedLayers layers which are to be used by this imagery layer
     * @param selectedStyles styles that should be used for selectedLayers
     * @param transparent if layer should be transparent
     * @return ImageryInfo object
     */
    public ImageryInfo toImageryInfo(String name, List<LayerDetails> selectedLayers, List<String> selectedStyles, boolean transparent) {
        ImageryInfo i = new ImageryInfo(name, buildGetMapUrl(selectedLayers, selectedStyles, transparent));
        if (selectedLayers != null && !selectedLayers.isEmpty()) {
            i.setServerProjections(getServerProjections(selectedLayers));
        }
        return i;
    }

    /**
     * Returns projections that server supports for provided list of layers. This will be intersection of projections
     * defined for each layer
     *
     * @param selectedLayers list of layers
     * @return projection code
     */
    public Collection<String> getServerProjections(List<LayerDetails> selectedLayers) {
        if (selectedLayers.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> proj = new HashSet<>(selectedLayers.get(0).getCrs());

        // set intersect with all layers
        for (LayerDetails ld: selectedLayers) {
            proj.retainAll(ld.getCrs());
        }
        return proj;
    }

    /**
     * @param defaultLayers default layers that should select layer object
     * @return collection of LayerDetails specified by DefaultLayers
     */
    public List<LayerDetails> getLayers(List<DefaultLayer> defaultLayers) {
        Collection<String> layerNames = defaultLayers.stream().map(DefaultLayer::getLayerName).collect(Collectors.toList());
        return layers.stream()
                .flatMap(LayerDetails::flattened)
                .filter(x -> layerNames.contains(x.getName()))
                .collect(Collectors.toList());
    }

    /**
     * @return title of this service
     */
    public String getTitle() {
        return title;
    }
}
