// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.Projected;
import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.TileRange;
import org.openstreetmap.gui.jmapviewer.TileXY;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.interfaces.IProjected;
import org.openstreetmap.gui.jmapviewer.interfaces.TemplatedTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.AbstractTMSTileSource;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.imagery.GetCapabilitiesParseHelper.TransferMode;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.NativeScaleLayer.ScaleList;
import org.openstreetmap.josm.gui.layer.imagery.WMTSLayerSelection;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Tile Source handling WMTS providers
 *
 * @author Wiktor Niesiobędzki
 * @since 8526
 */
public class WMTSTileSource extends AbstractTMSTileSource implements TemplatedTileSource {
    /**
     * WMTS namespace address
     */
    public static final String WMTS_NS_URL = "http://www.opengis.net/wmts/1.0";

    // CHECKSTYLE.OFF: SingleSpaceSeparator
    private static final QName QN_CONTENTS            = new QName(WMTSTileSource.WMTS_NS_URL, "Contents");
    private static final QName QN_DEFAULT             = new QName(WMTSTileSource.WMTS_NS_URL, "Default");
    private static final QName QN_DIMENSION           = new QName(WMTSTileSource.WMTS_NS_URL, "Dimension");
    private static final QName QN_FORMAT              = new QName(WMTSTileSource.WMTS_NS_URL, "Format");
    private static final QName QN_LAYER               = new QName(WMTSTileSource.WMTS_NS_URL, "Layer");
    private static final QName QN_MATRIX_WIDTH        = new QName(WMTSTileSource.WMTS_NS_URL, "MatrixWidth");
    private static final QName QN_MATRIX_HEIGHT       = new QName(WMTSTileSource.WMTS_NS_URL, "MatrixHeight");
    private static final QName QN_RESOURCE_URL        = new QName(WMTSTileSource.WMTS_NS_URL, "ResourceURL");
    private static final QName QN_SCALE_DENOMINATOR   = new QName(WMTSTileSource.WMTS_NS_URL, "ScaleDenominator");
    private static final QName QN_STYLE               = new QName(WMTSTileSource.WMTS_NS_URL, "Style");
    private static final QName QN_TILEMATRIX          = new QName(WMTSTileSource.WMTS_NS_URL, "TileMatrix");
    private static final QName QN_TILEMATRIXSET       = new QName(WMTSTileSource.WMTS_NS_URL, "TileMatrixSet");
    private static final QName QN_TILEMATRIX_SET_LINK = new QName(WMTSTileSource.WMTS_NS_URL, "TileMatrixSetLink");
    private static final QName QN_TILE_WIDTH          = new QName(WMTSTileSource.WMTS_NS_URL, "TileWidth");
    private static final QName QN_TILE_HEIGHT         = new QName(WMTSTileSource.WMTS_NS_URL, "TileHeight");
    private static final QName QN_TOPLEFT_CORNER      = new QName(WMTSTileSource.WMTS_NS_URL, "TopLeftCorner");
    private static final QName QN_VALUE               = new QName(WMTSTileSource.WMTS_NS_URL, "Value");
    // CHECKSTYLE.ON: SingleSpaceSeparator

    private static final String PATTERN_HEADER = "\\{header\\(([^,]+),([^}]+)\\)\\}";

    private static final String URL_GET_ENCODING_PARAMS = "SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER={layer}&STYLE={style}&"
            + "FORMAT={format}&tileMatrixSet={TileMatrixSet}&tileMatrix={TileMatrix}&tileRow={TileRow}&tileCol={TileCol}";

    private static final String[] ALL_PATTERNS = {
        PATTERN_HEADER,
    };

    private int cachedTileSize = -1;

    private static class TileMatrix {
        private String identifier;
        private double scaleDenominator;
        private EastNorth topLeftCorner;
        private int tileWidth;
        private int tileHeight;
        private int matrixWidth = -1;
        private int matrixHeight = -1;
    }

    private static class TileMatrixSetBuilder {
        // sorted by zoom level
        SortedSet<TileMatrix> tileMatrix = new TreeSet<>((o1, o2) -> -1 * Double.compare(o1.scaleDenominator, o2.scaleDenominator));
        private String crs;
        private String identifier;

        TileMatrixSet build() {
            return new TileMatrixSet(this);
        }
    }

    /**
     *
     * class representing WMTS TileMatrixSet
     * This connects projection and TileMatrix (how the map is divided in tiles)
     *
     */
    public static class TileMatrixSet {

        private final List<TileMatrix> tileMatrix;
        private final String crs;
        private final String identifier;

        TileMatrixSet(TileMatrixSet tileMatrixSet) {
            if (tileMatrixSet != null) {
                tileMatrix = new ArrayList<>(tileMatrixSet.tileMatrix);
                crs = tileMatrixSet.crs;
                identifier = tileMatrixSet.identifier;
            } else {
                tileMatrix = Collections.emptyList();
                crs = null;
                identifier = null;
            }
        }

        TileMatrixSet(TileMatrixSetBuilder builder) {
            tileMatrix = new ArrayList<>(builder.tileMatrix);
            crs = builder.crs;
            identifier = builder.identifier;
        }

        @Override
        public String toString() {
            return "TileMatrixSet [crs=" + crs + ", identifier=" + identifier + ']';
        }

        /**
         *
         * @return identifier of this TileMatrixSet
         */
        public String getIdentifier() {
            return identifier;
        }

        /**
         *
         * @return projection of this tileMatrix
         */
        public String getCrs() {
            return crs;
        }
    }

    private static class Dimension {
        private String identifier;
        private String defaultValue;
        private final List<String> values = new ArrayList<>();
    }

    /**
     * Class representing WMTS Layer information
     *
     */
    public static class Layer {
        private String format;
        private String identifier;
        private String title;
        private TileMatrixSet tileMatrixSet;
        private String baseUrl;
        private String style;
        private final Collection<String> tileMatrixSetLinks = new ArrayList<>();
        private final Collection<Dimension> dimensions = new ArrayList<>();

        Layer(Layer l) {
            Objects.requireNonNull(l);
            format = l.format;
            identifier = l.identifier;
            title = l.title;
            baseUrl = l.baseUrl;
            style = l.style;
            tileMatrixSet = new TileMatrixSet(l.tileMatrixSet);
            dimensions.addAll(l.dimensions);
        }

        Layer() {
        }

        /**
         * Get title of the layer for user display.
         *
         * This is either the content of the Title element (if available) or
         * the layer identifier (as fallback)
         * @return title of the layer for user display
         */
        public String getUserTitle() {
            return title != null ? title : identifier;
        }

        @Override
        public String toString() {
            return "Layer [identifier=" + identifier + ", title=" + title + ", tileMatrixSet="
                    + tileMatrixSet + ", baseUrl=" + baseUrl + ", style=" + style + ']';
        }

        /**
         *
         * @return identifier of this layer
         */
        public String getIdentifier() {
            return identifier;
        }

        /**
         *
         * @return style of this layer
         */
        public String getStyle() {
            return style;
        }

        /**
         *
         * @return tileMatrixSet of this layer
         */
        public TileMatrixSet getTileMatrixSet() {
            return tileMatrixSet;
        }
    }

    /**
     * Exception thrown when parser doesn't find expected information in GetCapabilities document
     *
     */
    public static class WMTSGetCapabilitiesException extends Exception {

        /**
         * Create WMTS exception
         * @param cause description of cause
         */
        public WMTSGetCapabilitiesException(String cause) {
            super(cause);
        }

        /**
         * Create WMTS exception
         * @param cause description of cause
         * @param t nested exception
         */
        public WMTSGetCapabilitiesException(String cause, Throwable t) {
            super(cause, t);
        }
    }

    private static final class SelectLayerDialog extends ExtendedDialog {
        private final WMTSLayerSelection list;

        SelectLayerDialog(Collection<Layer> layers) {
            super(MainApplication.getMainFrame(), tr("Select WMTS layer"), tr("Add layers"), tr("Cancel"));
            this.list = new WMTSLayerSelection(groupLayersByNameAndTileMatrixSet(layers));
            setContent(list);
        }

        public DefaultLayer getSelectedLayer() {
            Layer selectedLayer = list.getSelectedLayer();
            return new DefaultLayer(ImageryType.WMTS, selectedLayer.identifier, selectedLayer.style, selectedLayer.tileMatrixSet.identifier);
        }

    }

    private final Map<String, String> headers = new ConcurrentHashMap<>();
    private final Collection<Layer> layers;
    private Layer currentLayer;
    private TileMatrixSet currentTileMatrixSet;
    private double crsScale;
    private final GetCapabilitiesParseHelper.TransferMode transferMode;

    private ScaleList nativeScaleList;

    private final DefaultLayer defaultLayer;

    private Projection tileProjection;

    /**
     * Creates a tile source based on imagery info
     * @param info imagery info
     * @throws IOException if any I/O error occurs
     * @throws WMTSGetCapabilitiesException when document didn't contain any layers
     * @throws IllegalArgumentException if any other error happens for the given imagery info
     */
    public WMTSTileSource(ImageryInfo info) throws IOException, WMTSGetCapabilitiesException {
        super(info);
        CheckParameterUtil.ensureThat(info.getDefaultLayers().size() < 2, "At most 1 default layer for WMTS is supported");
        this.headers.putAll(info.getCustomHttpHeaders());
        this.baseUrl = GetCapabilitiesParseHelper.normalizeCapabilitiesUrl(handleTemplate(info.getUrl()));
        WMTSCapabilities capabilities = getCapabilities(baseUrl, headers);
        this.layers = capabilities.getLayers();
        this.baseUrl = capabilities.getBaseUrl();
        this.transferMode = capabilities.getTransferMode();
        if (info.getDefaultLayers().isEmpty()) {
            Logging.warn(tr("No default layer selected, choosing first layer."));
            if (!layers.isEmpty()) {
                Layer first = layers.iterator().next();
                this.defaultLayer = new DefaultLayer(info.getImageryType(), first.identifier, first.style, first.tileMatrixSet.identifier);
            } else {
                this.defaultLayer = null;
            }
        } else {
            this.defaultLayer = info.getDefaultLayers().get(0);
        }
        if (this.layers.isEmpty())
            throw new IllegalArgumentException(tr("No layers defined by getCapabilities document: {0}", info.getUrl()));
    }

    /**
     * Creates a tile source based on imagery info and initializes it with given projection.
     * @param info imagery info
     * @param projection projection to be used by this TileSource
     * @throws IOException if any I/O error occurs
     * @throws WMTSGetCapabilitiesException when document didn't contain any layers
     * @throws IllegalArgumentException if any other error happens for the given imagery info
     * @since 14507
     */
    public WMTSTileSource(ImageryInfo info, Projection projection) throws IOException, WMTSGetCapabilitiesException {
        this(info);
        initProjection(projection);
    }

    /**
     * Creates a dialog based on this tile source with all available layers and returns the name of selected layer
     * @return Name of selected layer
     */
    public DefaultLayer userSelectLayer() {
        Map<String, List<Layer>> layerById = layers.stream().collect(
                Collectors.groupingBy(x -> x.identifier));
        if (layerById.size() == 1) { // only one layer
            List<Layer> ls = layerById.entrySet().iterator().next().getValue()
                    .stream().filter(
                            u -> u.tileMatrixSet.crs.equals(ProjectionRegistry.getProjection().toCode()))
                    .collect(Collectors.toList());
            if (ls.size() == 1) {
                // only one tile matrix set with matching projection - no point in asking
                Layer selectedLayer = ls.get(0);
                return new DefaultLayer(ImageryType.WMTS, selectedLayer.identifier, selectedLayer.style, selectedLayer.tileMatrixSet.identifier);
            }
        }

        final SelectLayerDialog layerSelection = new SelectLayerDialog(layers);
        if (layerSelection.showDialog().getValue() == 1) {
            return layerSelection.getSelectedLayer();
        }
        return null;
    }

    private String handleTemplate(String url) {
        Pattern pattern = Pattern.compile(PATTERN_HEADER);
        StringBuffer output = new StringBuffer();
        Matcher matcher = pattern.matcher(url);
        while (matcher.find()) {
            this.headers.put(matcher.group(1), matcher.group(2));
            matcher.appendReplacement(output, "");
        }
        matcher.appendTail(output);
        return output.toString();
    }


    /**
     * Call remote server and parse response to WMTSCapabilities object
     *
     * @param url of the getCapabilities document
     * @param headers HTTP headers to set when calling getCapabilities url
     * @return capabilities
     * @throws IOException in case of any I/O error
     * @throws WMTSGetCapabilitiesException when document didn't contain any layers
     * @throws IllegalArgumentException in case of any other error
     */
    public static WMTSCapabilities getCapabilities(String url, Map<String, String> headers) throws IOException, WMTSGetCapabilitiesException {
        try (CachedFile cf = new CachedFile(url); InputStream in = cf.setHttpHeaders(headers).
                setMaxAge(Config.getPref().getLong("wmts.capabilities.cache.max_age", 7 * CachedFile.DAYS)).
                setCachingStrategy(CachedFile.CachingStrategy.IfModifiedSince).
                getInputStream()) {
            byte[] data = Utils.readBytesFromStream(in);
            if (data.length == 0) {
                cf.clear();
                throw new IllegalArgumentException("Could not read data from: " + url);
            }

            try {
                XMLStreamReader reader = GetCapabilitiesParseHelper.getReader(new ByteArrayInputStream(data));
                WMTSCapabilities ret = null;
                Collection<Layer> layers = null;
                for (int event = reader.getEventType(); reader.hasNext(); event = reader.next()) {
                    if (event == XMLStreamReader.START_ELEMENT) {
                        if (GetCapabilitiesParseHelper.QN_OWS_OPERATIONS_METADATA.equals(reader.getName())) {
                            ret = parseOperationMetadata(reader);
                        }

                        if (QN_CONTENTS.equals(reader.getName())) {
                            layers = parseContents(reader);
                        }
                    }
                }
                if (ret == null) {
                    /*
                     *  see #12168 - create dummy operation metadata - not all WMTS services provide this information
                     *
                     *  WMTS Standard:
                     *  > Resource oriented architecture style HTTP encodings SHALL not be described in the OperationsMetadata section.
                     *
                     *  And OperationMetada is not mandatory element. So REST mode is justifiable
                     */
                    ret = new WMTSCapabilities(url, TransferMode.REST);
                }
                if (layers == null) {
                    throw new WMTSGetCapabilitiesException(tr("WMTS Capabilities document did not contain layers in url: {0}", url));
                }
                ret.addLayers(layers);
                return ret;
            } catch (XMLStreamException e) {
                cf.clear();
                Logging.warn(new String(data, StandardCharsets.UTF_8));
                throw new WMTSGetCapabilitiesException(tr("Error during parsing of WMTS Capabilities document: {0}", e.getMessage()), e);
            }
        } catch (InvalidPathException e) {
            throw new WMTSGetCapabilitiesException(tr("Invalid path for GetCapabilities document: {0}", e.getMessage()), e);
        }
    }

    /**
     * Parse Contents tag. Returns when reader reaches Contents closing tag
     *
     * @param reader StAX reader instance
     * @return collection of layers within contents with properly linked TileMatrixSets
     * @throws XMLStreamException See {@link XMLStreamReader}
     */
    private static Collection<Layer> parseContents(XMLStreamReader reader) throws XMLStreamException {
        Map<String, TileMatrixSet> matrixSetById = new ConcurrentHashMap<>();
        Collection<Layer> layers = new ArrayList<>();
        for (int event = reader.getEventType();
                reader.hasNext() && !(event == XMLStreamReader.END_ELEMENT && QN_CONTENTS.equals(reader.getName()));
                event = reader.next()) {
            if (event == XMLStreamReader.START_ELEMENT) {
                if (QN_LAYER.equals(reader.getName())) {
                    Layer l = parseLayer(reader);
                    if (l != null) {
                        layers.add(l);
                    }
                }
                if (QN_TILEMATRIXSET.equals(reader.getName())) {
                    TileMatrixSet entry = parseTileMatrixSet(reader);
                    matrixSetById.put(entry.identifier, entry);
                }
            }
        }
        Collection<Layer> ret = new ArrayList<>();
        // link layers to matrix sets
        for (Layer l: layers) {
            for (String tileMatrixId: l.tileMatrixSetLinks) {
                Layer newLayer = new Layer(l); // create a new layer object for each tile matrix set supported
                newLayer.tileMatrixSet = matrixSetById.get(tileMatrixId);
                ret.add(newLayer);
            }
        }
        return ret;
    }

    /**
     * Parse Layer tag. Returns when reader will reach Layer closing tag
     *
     * @param reader StAX reader instance
     * @return Layer object, with tileMatrixSetLinks and no tileMatrixSet attribute set.
     * @throws XMLStreamException See {@link XMLStreamReader}
     */
    private static Layer parseLayer(XMLStreamReader reader) throws XMLStreamException {
        Layer layer = new Layer();
        Deque<QName> tagStack = new LinkedList<>();
        List<String> supportedMimeTypes = new ArrayList<>(Arrays.asList(ImageIO.getReaderMIMETypes()));
        supportedMimeTypes.add("image/jpgpng");         // used by ESRI
        supportedMimeTypes.add("image/png8");           // used by geoserver
        if (supportedMimeTypes.contains("image/jpeg")) {
            supportedMimeTypes.add("image/jpg"); // sometimes misspelled by Arcgis
        }
        Collection<String> unsupportedFormats = new ArrayList<>();

        for (int event = reader.getEventType();
                reader.hasNext() && !(event == XMLStreamReader.END_ELEMENT && QN_LAYER.equals(reader.getName()));
                event = reader.next()) {
            if (event == XMLStreamReader.START_ELEMENT) {
                tagStack.push(reader.getName());
                if (tagStack.size() == 2) {
                    if (QN_FORMAT.equals(reader.getName())) {
                        String format = reader.getElementText();
                        if (supportedMimeTypes.contains(format)) {
                            layer.format = format;
                        } else {
                            unsupportedFormats.add(format);
                        }
                    } else if (GetCapabilitiesParseHelper.QN_OWS_IDENTIFIER.equals(reader.getName())) {
                        layer.identifier = reader.getElementText();
                    } else if (GetCapabilitiesParseHelper.QN_OWS_TITLE.equals(reader.getName())) {
                        layer.title = reader.getElementText();
                    } else if (QN_RESOURCE_URL.equals(reader.getName()) &&
                            "tile".equals(reader.getAttributeValue("", "resourceType"))) {
                        layer.baseUrl = reader.getAttributeValue("", "template");
                    } else if (QN_STYLE.equals(reader.getName()) &&
                            "true".equals(reader.getAttributeValue("", "isDefault"))) {
                        if (GetCapabilitiesParseHelper.moveReaderToTag(reader, GetCapabilitiesParseHelper.QN_OWS_IDENTIFIER)) {
                            layer.style = reader.getElementText();
                            tagStack.push(reader.getName()); // keep tagStack in sync
                        }
                    } else if (QN_DIMENSION.equals(reader.getName())) {
                        layer.dimensions.add(parseDimension(reader));
                    } else if (QN_TILEMATRIX_SET_LINK.equals(reader.getName())) {
                        layer.tileMatrixSetLinks.add(parseTileMatrixSetLink(reader));
                    } else {
                        GetCapabilitiesParseHelper.moveReaderToEndCurrentTag(reader);
                    }
                }
            }
            // need to get event type from reader, as parsing might have change position of reader
            if (reader.getEventType() == XMLStreamReader.END_ELEMENT) {
                QName start = tagStack.pop();
                if (!start.equals(reader.getName())) {
                    throw new IllegalStateException(tr("WMTS Parser error - start element {0} has different name than end element {2}",
                            start, reader.getName()));
                }
            }
        }
        if (layer.style == null) {
            layer.style = "";
        }
        if (layer.format == null) {
            // no format found - it's mandatory parameter - can't use this layer
            Logging.warn(tr("Can''t use layer {0} because no supported formats where found. Layer is available in formats: {1}",
                    layer.getUserTitle(),
                    String.join(", ", unsupportedFormats)));
            return null;
        }
        return layer;
    }

    /**
     * Gets Dimension value. Returns when reader is on Dimension closing tag
     *
     * @param reader StAX reader instance
     * @return dimension
     * @throws XMLStreamException See {@link XMLStreamReader}
     */
    private static Dimension parseDimension(XMLStreamReader reader) throws XMLStreamException {
        Dimension ret = new Dimension();
        for (int event = reader.getEventType();
                reader.hasNext() && !(event == XMLStreamReader.END_ELEMENT &&
                        QN_DIMENSION.equals(reader.getName()));
                event = reader.next()) {
            if (event == XMLStreamReader.START_ELEMENT) {
                if (GetCapabilitiesParseHelper.QN_OWS_IDENTIFIER.equals(reader.getName())) {
                    ret.identifier = reader.getElementText();
                } else if (QN_DEFAULT.equals(reader.getName())) {
                    ret.defaultValue = reader.getElementText();
                } else if (QN_VALUE.equals(reader.getName())) {
                    ret.values.add(reader.getElementText());
                }
            }
        }
        return ret;
    }

    /**
     * Gets TileMatrixSetLink value. Returns when reader is on TileMatrixSetLink closing tag
     *
     * @param reader StAX reader instance
     * @return TileMatrixSetLink identifier
     * @throws XMLStreamException See {@link XMLStreamReader}
     */
    private static String parseTileMatrixSetLink(XMLStreamReader reader) throws XMLStreamException {
        String ret = null;
        for (int event = reader.getEventType();
                reader.hasNext() && !(event == XMLStreamReader.END_ELEMENT &&
                        QN_TILEMATRIX_SET_LINK.equals(reader.getName()));
                event = reader.next()) {
            if (event == XMLStreamReader.START_ELEMENT && QN_TILEMATRIXSET.equals(reader.getName())) {
                ret = reader.getElementText();
            }
        }
        return ret;
    }

    /**
     * Parses TileMatrixSet section. Returns when reader is on TileMatrixSet closing tag
     * @param reader StAX reader instance
     * @return TileMatrixSet object
     * @throws XMLStreamException See {@link XMLStreamReader}
     */
    private static TileMatrixSet parseTileMatrixSet(XMLStreamReader reader) throws XMLStreamException {
        TileMatrixSetBuilder matrixSet = new TileMatrixSetBuilder();
        for (int event = reader.getEventType();
                reader.hasNext() && !(event == XMLStreamReader.END_ELEMENT && QN_TILEMATRIXSET.equals(reader.getName()));
                event = reader.next()) {
                    if (event == XMLStreamReader.START_ELEMENT) {
                        if (GetCapabilitiesParseHelper.QN_OWS_IDENTIFIER.equals(reader.getName())) {
                            matrixSet.identifier = reader.getElementText();
                        }
                        if (GetCapabilitiesParseHelper.QN_OWS_SUPPORTED_CRS.equals(reader.getName())) {
                            matrixSet.crs = GetCapabilitiesParseHelper.crsToCode(reader.getElementText());
                        }
                        if (QN_TILEMATRIX.equals(reader.getName())) {
                            matrixSet.tileMatrix.add(parseTileMatrix(reader, matrixSet.crs));
                        }
                    }
        }
        return matrixSet.build();
    }

    /**
     * Parses TileMatrix section. Returns when reader is on TileMatrix closing tag.
     * @param reader StAX reader instance
     * @param matrixCrs projection used by this matrix
     * @return TileMatrix object
     * @throws XMLStreamException See {@link XMLStreamReader}
     */
    private static TileMatrix parseTileMatrix(XMLStreamReader reader, String matrixCrs) throws XMLStreamException {
        Projection matrixProj = Optional.ofNullable(Projections.getProjectionByCode(matrixCrs))
                .orElseGet(ProjectionRegistry::getProjection); // use current projection if none found. Maybe user is using custom string
        TileMatrix ret = new TileMatrix();
        for (int event = reader.getEventType();
                reader.hasNext() && !(event == XMLStreamReader.END_ELEMENT && QN_TILEMATRIX.equals(reader.getName()));
                event = reader.next()) {
            if (event == XMLStreamReader.START_ELEMENT) {
                if (GetCapabilitiesParseHelper.QN_OWS_IDENTIFIER.equals(reader.getName())) {
                    ret.identifier = reader.getElementText();
                }
                if (QN_SCALE_DENOMINATOR.equals(reader.getName())) {
                    ret.scaleDenominator = Double.parseDouble(reader.getElementText());
                }
                if (QN_TOPLEFT_CORNER.equals(reader.getName())) {
                    String[] topLeftCorner = reader.getElementText().split(" ");
                    if (matrixProj.switchXY()) {
                        ret.topLeftCorner = new EastNorth(Double.parseDouble(topLeftCorner[1]), Double.parseDouble(topLeftCorner[0]));
                    } else {
                        ret.topLeftCorner = new EastNorth(Double.parseDouble(topLeftCorner[0]), Double.parseDouble(topLeftCorner[1]));
                    }
                }
                if (QN_TILE_HEIGHT.equals(reader.getName())) {
                    ret.tileHeight = Integer.parseInt(reader.getElementText());
                }
                if (QN_TILE_WIDTH.equals(reader.getName())) {
                    ret.tileWidth = Integer.parseInt(reader.getElementText());
                }
                if (QN_MATRIX_HEIGHT.equals(reader.getName())) {
                    ret.matrixHeight = Integer.parseInt(reader.getElementText());
                }
                if (QN_MATRIX_WIDTH.equals(reader.getName())) {
                    ret.matrixWidth = Integer.parseInt(reader.getElementText());
                }
            }
        }
        if (ret.tileHeight != ret.tileWidth) {
            throw new AssertionError(tr("Only square tiles are supported. {0}x{1} returned by server for TileMatrix identifier {2}",
                    ret.tileHeight, ret.tileWidth, ret.identifier));
        }
        return ret;
    }

    /**
     * Parses OperationMetadata section. Returns when reader is on OperationsMetadata closing tag.
     * return WMTSCapabilities with baseUrl and transferMode
     *
     * @param reader StAX reader instance
     * @return WMTSCapabilities with baseUrl and transferMode set
     * @throws XMLStreamException See {@link XMLStreamReader}
     */
    private static WMTSCapabilities parseOperationMetadata(XMLStreamReader reader) throws XMLStreamException {
        for (int event = reader.getEventType();
                reader.hasNext() && !(event == XMLStreamReader.END_ELEMENT &&
                        GetCapabilitiesParseHelper.QN_OWS_OPERATIONS_METADATA.equals(reader.getName()));
                event = reader.next()) {
            if (event == XMLStreamReader.START_ELEMENT &&
                    GetCapabilitiesParseHelper.QN_OWS_OPERATION.equals(reader.getName()) &&
                    "GetTile".equals(reader.getAttributeValue("", "name")) &&
                    GetCapabilitiesParseHelper.moveReaderToTag(reader,
                            GetCapabilitiesParseHelper.QN_OWS_DCP,
                            GetCapabilitiesParseHelper.QN_OWS_HTTP,
                            GetCapabilitiesParseHelper.QN_OWS_GET
                    )) {
                return new WMTSCapabilities(
                        reader.getAttributeValue(GetCapabilitiesParseHelper.XLINK_NS_URL, "href"),
                        GetCapabilitiesParseHelper.getTransferMode(reader)
                        );
            }
        }
        return null;
    }

    /**
     * Initializes projection for this TileSource with projection
     * @param proj projection to be used by this TileSource
     */
    public void initProjection(Projection proj) {
        if (proj.equals(tileProjection))
            return;
        List<Layer> matchingLayers = layers.stream().filter(
                l -> l.identifier.equals(defaultLayer.getLayerName()) && l.tileMatrixSet.crs.equals(proj.toCode()))
                .collect(Collectors.toList());
        if (matchingLayers.size() > 1) {
            this.currentLayer = matchingLayers.stream().filter(
                    l -> l.tileMatrixSet.identifier.equals(defaultLayer.getTileMatrixSet()))
                    .findFirst().orElse(matchingLayers.get(0));
            this.tileProjection = proj;
        } else if (matchingLayers.size() == 1) {
            this.currentLayer = matchingLayers.get(0);
            this.tileProjection = proj;
        } else {
            // no tile matrix sets with current projection
            if (this.currentLayer == null) {
                this.tileProjection = null;
                for (Layer layer : layers) {
                    if (!layer.identifier.equals(defaultLayer.getLayerName())) {
                        continue;
                    }
                    Projection pr = Projections.getProjectionByCode(layer.tileMatrixSet.crs);
                    if (pr != null) {
                        this.currentLayer = layer;
                        this.tileProjection = pr;
                        break;
                    }
                }
                if (this.currentLayer == null)
                    throw new IllegalArgumentException(
                            layers.stream().map(l -> l.tileMatrixSet).collect(Collectors.toList()).toString());
            } // else: keep currentLayer and tileProjection as is
        }
        if (this.currentLayer != null) {
            this.currentTileMatrixSet = this.currentLayer.tileMatrixSet;
            Collection<Double> scales = new ArrayList<>(currentTileMatrixSet.tileMatrix.size());
            for (TileMatrix tileMatrix : currentTileMatrixSet.tileMatrix) {
                scales.add(tileMatrix.scaleDenominator * 0.28e-03);
            }
            this.nativeScaleList = new ScaleList(scales);
        }
        this.crsScale = getTileSize() * 0.28e-03 / this.tileProjection.getMetersPerUnit();
    }

    @Override
    public int getTileSize() {
        if (cachedTileSize > 0) {
            return cachedTileSize;
        }
        if (currentTileMatrixSet != null) {
            // no support for non-square tiles (tileHeight != tileWidth)
            // and for different tile sizes at different zoom levels
            cachedTileSize = currentTileMatrixSet.tileMatrix.get(0).tileHeight;
            return cachedTileSize;
        }
        // Fallback to default mercator tile size. Maybe it will work
        Logging.warn("WMTS: Could not determine tile size. Using default tile size of: {0}", getDefaultTileSize());
        return getDefaultTileSize();
    }

    @Override
    public String getTileUrl(int zoom, int tilex, int tiley) {
        if (currentLayer == null) {
            return "";
        }

        String url;
        if (currentLayer.baseUrl != null && transferMode == null) {
            url = currentLayer.baseUrl;
        } else {
            switch (transferMode) {
            case KVP:
                url = baseUrl + URL_GET_ENCODING_PARAMS;
                break;
            case REST:
                url = currentLayer.baseUrl;
                break;
            default:
                url = "";
                break;
            }
        }

        TileMatrix tileMatrix = getTileMatrix(zoom);

        if (tileMatrix == null) {
            return ""; // no matrix, probably unsupported CRS selected.
        }

        url = url.replaceAll("\\{layer\\}", this.currentLayer.identifier)
                .replaceAll("\\{format\\}", this.currentLayer.format)
                .replaceAll("\\{TileMatrixSet\\}", this.currentTileMatrixSet.identifier)
                .replaceAll("\\{TileMatrix\\}", tileMatrix.identifier)
                .replaceAll("\\{TileRow\\}", Integer.toString(tiley))
                .replaceAll("\\{TileCol\\}", Integer.toString(tilex))
                .replaceAll("(?i)\\{style\\}", this.currentLayer.style);

        for (Dimension d : currentLayer.dimensions) {
            url = url.replaceAll("(?i)\\{"+d.identifier+"\\}", d.defaultValue);
        }

        return url;
    }

    /**
     *
     * @param zoom zoom level
     * @return TileMatrix that's working on this zoom level
     */
    private TileMatrix getTileMatrix(int zoom) {
        if (zoom > getMaxZoom()) {
            return null;
        }
        if (zoom < 0) {
            return null;
        }
        return this.currentTileMatrixSet.tileMatrix.get(zoom);
    }

    @Override
    public double getDistance(double lat1, double lon1, double lat2, double lon2) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ICoordinate tileXYToLatLon(Tile tile) {
        return tileXYToLatLon(tile.getXtile(), tile.getYtile(), tile.getZoom());
    }

    @Override
    public ICoordinate tileXYToLatLon(TileXY xy, int zoom) {
        return tileXYToLatLon(xy.getXIndex(), xy.getYIndex(), zoom);
    }

    @Override
    public ICoordinate tileXYToLatLon(int x, int y, int zoom) {
        TileMatrix matrix = getTileMatrix(zoom);
        if (matrix == null) {
            return CoordinateConversion.llToCoor(tileProjection.getWorldBoundsLatLon().getCenter());
        }
        double scale = matrix.scaleDenominator * this.crsScale;
        EastNorth ret = new EastNorth(matrix.topLeftCorner.east() + x * scale, matrix.topLeftCorner.north() - y * scale);
        return CoordinateConversion.llToCoor(tileProjection.eastNorth2latlon(ret));
    }

    @Override
    public TileXY latLonToTileXY(double lat, double lon, int zoom) {
        TileMatrix matrix = getTileMatrix(zoom);
        if (matrix == null) {
            return new TileXY(0, 0);
        }

        EastNorth enPoint = tileProjection.latlon2eastNorth(new LatLon(lat, lon));
        double scale = matrix.scaleDenominator * this.crsScale;
        return new TileXY(
                (enPoint.east() - matrix.topLeftCorner.east()) / scale,
                (matrix.topLeftCorner.north() - enPoint.north()) / scale
                );
    }

    @Override
    public TileXY latLonToTileXY(ICoordinate point, int zoom) {
        return latLonToTileXY(point.getLat(), point.getLon(), zoom);
    }

    @Override
    public int getTileXMax(int zoom) {
        return getTileXMax(zoom, tileProjection);
    }

    @Override
    public int getTileYMax(int zoom) {
        return getTileYMax(zoom, tileProjection);
    }

    @Override
    public Point latLonToXY(double lat, double lon, int zoom) {
        TileMatrix matrix = getTileMatrix(zoom);
        if (matrix == null) {
            return new Point(0, 0);
        }
        double scale = matrix.scaleDenominator * this.crsScale;
        EastNorth point = tileProjection.latlon2eastNorth(new LatLon(lat, lon));
        return new Point(
                    (int) Math.round((point.east() - matrix.topLeftCorner.east()) / scale),
                    (int) Math.round((matrix.topLeftCorner.north() - point.north()) / scale)
                );
    }

    @Override
    public Point latLonToXY(ICoordinate point, int zoom) {
        return latLonToXY(point.getLat(), point.getLon(), zoom);
    }

    @Override
    public Coordinate xyToLatLon(Point point, int zoom) {
        return xyToLatLon(point.x, point.y, zoom);
    }

    @Override
    public Coordinate xyToLatLon(int x, int y, int zoom) {
        TileMatrix matrix = getTileMatrix(zoom);
        if (matrix == null) {
            return new Coordinate(0, 0);
        }
        double scale = matrix.scaleDenominator * this.crsScale;
        EastNorth ret = new EastNorth(
                matrix.topLeftCorner.east() + x * scale,
                matrix.topLeftCorner.north() - y * scale
                );
        LatLon ll = tileProjection.eastNorth2latlon(ret);
        return new Coordinate(ll.lat(), ll.lon());
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public int getMaxZoom() {
        if (this.currentTileMatrixSet != null) {
            return this.currentTileMatrixSet.tileMatrix.size()-1;
        }
        return 0;
    }

    @Override
    public String getTileId(int zoom, int tilex, int tiley) {
        return getTileUrl(zoom, tilex, tiley);
    }

    /**
     * Checks if url is acceptable by this Tile Source
     * @param url URL to check
     */
    public static void checkUrl(String url) {
        CheckParameterUtil.ensureParameterNotNull(url, "url");
        Matcher m = Pattern.compile("\\{[^}]*\\}").matcher(url);
        while (m.find()) {
            boolean isSupportedPattern = false;
            for (String pattern : ALL_PATTERNS) {
                if (m.group().matches(pattern)) {
                    isSupportedPattern = true;
                    break;
                }
            }
            if (!isSupportedPattern) {
                throw new IllegalArgumentException(
                        tr("{0} is not a valid WMS argument. Please check this server URL:\n{1}", m.group(), url));
            }
        }
    }

    /**
     * @param layers to be grouped
     * @return list with entries - grouping identifier + list of layers
     */
    public static List<Entry<String, List<Layer>>> groupLayersByNameAndTileMatrixSet(Collection<Layer> layers) {
        Map<String, List<Layer>> layerByName = layers.stream().collect(
                Collectors.groupingBy(x -> x.identifier + '\u001c' + x.tileMatrixSet.identifier));
        return layerByName.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList());
    }


    /**
     * @return set of projection codes that this TileSource supports
     */
    public Collection<String> getSupportedProjections() {
        Collection<String> ret = new LinkedHashSet<>();
        if (currentLayer == null) {
            for (Layer layer: this.layers) {
                ret.add(layer.tileMatrixSet.crs);
            }
        } else {
            for (Layer layer: this.layers) {
                if (currentLayer.identifier.equals(layer.identifier)) {
                    ret.add(layer.tileMatrixSet.crs);
                }
            }
        }
        return ret;
    }

    private int getTileYMax(int zoom, Projection proj) {
        TileMatrix matrix = getTileMatrix(zoom);
        if (matrix == null) {
            return 0;
        }

        if (matrix.matrixHeight != -1) {
            return matrix.matrixHeight;
        }

        double scale = matrix.scaleDenominator * this.crsScale;
        EastNorth min = matrix.topLeftCorner;
        EastNorth max = proj.latlon2eastNorth(proj.getWorldBoundsLatLon().getMax());
        return (int) Math.ceil(Math.abs(max.north() - min.north()) / scale);
    }

    private int getTileXMax(int zoom, Projection proj) {
        TileMatrix matrix = getTileMatrix(zoom);
        if (matrix == null) {
            return 0;
        }
        if (matrix.matrixWidth != -1) {
            return matrix.matrixWidth;
        }

        double scale = matrix.scaleDenominator * this.crsScale;
        EastNorth min = matrix.topLeftCorner;
        EastNorth max = proj.latlon2eastNorth(proj.getWorldBoundsLatLon().getMax());
        return (int) Math.ceil(Math.abs(max.east() - min.east()) / scale);
    }

    /**
     * Get native scales of tile source.
     * @return {@link ScaleList} of native scales
     */
    public ScaleList getNativeScales() {
        return nativeScaleList;
    }

    /**
     * Returns the tile projection.
     * @return the tile projection
     */
    public Projection getTileProjection() {
        return tileProjection;
    }

    @Override
    public IProjected tileXYtoProjected(int x, int y, int zoom) {
        TileMatrix matrix = getTileMatrix(zoom);
        if (matrix == null) {
            return new Projected(0, 0);
        }
        double scale = matrix.scaleDenominator * this.crsScale;
        return new Projected(
                matrix.topLeftCorner.east() + x * scale,
                matrix.topLeftCorner.north() - y * scale);
    }

    @Override
    public TileXY projectedToTileXY(IProjected projected, int zoom) {
        TileMatrix matrix = getTileMatrix(zoom);
        if (matrix == null) {
            return new TileXY(0, 0);
        }
        double scale = matrix.scaleDenominator * this.crsScale;
        return new TileXY(
                (projected.getEast() - matrix.topLeftCorner.east()) / scale,
                -(projected.getNorth() - matrix.topLeftCorner.north()) / scale);
    }

    private EastNorth tileToEastNorth(int x, int y, int z) {
        return CoordinateConversion.projToEn(this.tileXYtoProjected(x, y, z));
    }

    private ProjectionBounds getTileProjectionBounds(Tile tile) {
        ProjectionBounds pb = new ProjectionBounds(tileToEastNorth(tile.getXtile(), tile.getYtile(), tile.getZoom()));
        pb.extend(tileToEastNorth(tile.getXtile() + 1, tile.getYtile() + 1, tile.getZoom()));
        return pb;
    }

    @Override
    public boolean isInside(Tile inner, Tile outer) {
        ProjectionBounds pbInner = getTileProjectionBounds(inner);
        ProjectionBounds pbOuter = getTileProjectionBounds(outer);
        // a little tolerance, for when inner tile touches the border of the outer tile
        double epsilon = 1e-7 * (pbOuter.maxEast - pbOuter.minEast);
        return pbOuter.minEast <= pbInner.minEast + epsilon &&
                pbOuter.minNorth <= pbInner.minNorth + epsilon &&
                pbOuter.maxEast >= pbInner.maxEast - epsilon &&
                pbOuter.maxNorth >= pbInner.maxNorth - epsilon;
    }

    @Override
    public TileRange getCoveringTileRange(Tile tile, int newZoom) {
        TileMatrix matrixNew = getTileMatrix(newZoom);
        if (matrixNew == null) {
            return new TileRange(new TileXY(0, 0), new TileXY(0, 0), newZoom);
        }
        IProjected p0 = tileXYtoProjected(tile.getXtile(), tile.getYtile(), tile.getZoom());
        IProjected p1 = tileXYtoProjected(tile.getXtile() + 1, tile.getYtile() + 1, tile.getZoom());
        TileXY tMin = projectedToTileXY(p0, newZoom);
        TileXY tMax = projectedToTileXY(p1, newZoom);
        // shrink the target tile a little, so we don't get neighboring tiles, that
        // share an edge, but don't actually cover the target tile
        double epsilon = 1e-7 * (tMax.getX() - tMin.getX());
        int minX = (int) Math.floor(tMin.getX() + epsilon);
        int minY = (int) Math.floor(tMin.getY() + epsilon);
        int maxX = (int) Math.ceil(tMax.getX() - epsilon) - 1;
        int maxY = (int) Math.ceil(tMax.getY() - epsilon) - 1;
        return new TileRange(new TileXY(minX, minY), new TileXY(maxX, maxY), newZoom);
    }

    @Override
    public String getServerCRS() {
        return tileProjection != null ? tileProjection.toCode() : null;
    }

    /**
     * Layers that can be used with this tile source
     * @return unmodifiable collection of layers available in this tile source
     * @since 13879
     */
    public Collection<Layer> getLayers() {
        return Collections.unmodifiableCollection(layers);
    }
}
