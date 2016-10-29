// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.TileXY;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.interfaces.TemplatedTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.AbstractTMSTileSource;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.layer.NativeScaleLayer.ScaleList;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Utils;

/**
 * Tile Source handling WMS providers
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
    // CHECKSTYLE.ON: SingleSpaceSeparator

    private static final String PATTERN_HEADER = "\\{header\\(([^,]+),([^}]+)\\)\\}";

    private static final String URL_GET_ENCODING_PARAMS = "SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER={layer}&STYLE={style}&"
            + "FORMAT={format}&tileMatrixSet={TileMatrixSet}&tileMatrix={TileMatrix}&tileRow={TileRow}&tileCol={TileCol}";

    private static final String[] ALL_PATTERNS = {
        PATTERN_HEADER,
    };

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

    private static class TileMatrixSet {

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

    }

    private static class Layer {
        private String format;
        private String name;
        private TileMatrixSet tileMatrixSet;
        private String baseUrl;
        private String style;
        private final Collection<String> tileMatrixSetLinks = new ArrayList<>();

        Layer(Layer l) {
            if (l != null) {
                format = l.format;
                name = l.name;
                baseUrl = l.baseUrl;
                style = l.style;
                tileMatrixSet = new TileMatrixSet(l.tileMatrixSet);
            }
        }

        Layer() {
        }
    }

    private static final class SelectLayerDialog extends ExtendedDialog {
        private final transient Layer[] layers;
        private final JTable list;

        SelectLayerDialog(Collection<Layer> layers) {
            super(Main.parent, tr("Select WMTS layer"), new String[]{tr("Add layers"), tr("Cancel")});
            this.layers = layers.toArray(new Layer[layers.size()]);
            //getLayersTable(layers, Main.getProjection())
            this.list = new JTable(
                    new AbstractTableModel() {
                        @Override
                        public Object getValueAt(int rowIndex, int columnIndex) {
                            switch (columnIndex) {
                            case 0:
                                return SelectLayerDialog.this.layers[rowIndex].name;
                            case 1:
                                return SelectLayerDialog.this.layers[rowIndex].tileMatrixSet.crs;
                            case 2:
                                return SelectLayerDialog.this.layers[rowIndex].tileMatrixSet.identifier;
                            default:
                                throw new IllegalArgumentException();
                            }
                        }

                        @Override
                        public int getRowCount() {
                            return SelectLayerDialog.this.layers.length;
                        }

                        @Override
                        public int getColumnCount() {
                            return 3;
                        }

                        @Override
                        public String getColumnName(int column) {
                            switch (column) {
                            case 0: return tr("Layer name");
                            case 1: return tr("Projection");
                            case 2: return tr("Matrix set identifier");
                            default:
                                throw new IllegalArgumentException();
                            }
                        }

                        @Override
                        public boolean isCellEditable(int row, int column) {
                            return false;
                        }
                    });
            this.list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            this.list.setRowSelectionAllowed(true);
            this.list.setColumnSelectionAllowed(false);
            JPanel panel = new JPanel(new GridBagLayout());
            panel.add(new JScrollPane(this.list), GBC.eol().fill());
            setContent(panel);
        }

        public Layer getSelectedLayer() {
            int index = list.getSelectedRow();
            if (index < 0) {
                return null; //nothing selected
            }
            return layers[index];
        }
    }

    private final Map<String, String> headers = new ConcurrentHashMap<>();
    private final Collection<Layer> layers;
    private Layer currentLayer;
    private TileMatrixSet currentTileMatrixSet;
    private double crsScale;
    private GetCapabilitiesParseHelper.TransferMode transferMode;

    private ScaleList nativeScaleList;

    /**
     * Creates a tile source based on imagery info
     * @param info imagery info
     * @throws IOException if any I/O error occurs
     * @throws IllegalArgumentException if any other error happens for the given imagery info
     */
    public WMTSTileSource(ImageryInfo info) throws IOException {
        super(info);
        this.baseUrl = GetCapabilitiesParseHelper.normalizeCapabilitiesUrl(handleTemplate(info.getUrl()));
        this.layers = getCapabilities();
        if (this.layers.isEmpty())
            throw new IllegalArgumentException(tr("No layers defined by getCapabilities document: {0}", info.getUrl()));
    }

    private static Layer userSelectLayer(Collection<Layer> layers) {
        if (layers.size() == 1)
            return layers.iterator().next();
        Layer ret = null;

        final SelectLayerDialog layerSelection = new SelectLayerDialog(layers);
        if (layerSelection.showDialog().getValue() == 1) {
            ret = layerSelection.getSelectedLayer();
            // TODO: save layer information into ImageryInfo / ImageryPreferences?
        }
        if (ret == null) {
            // user canceled operation or did not choose any layer
            throw new IllegalArgumentException(tr("No layer selected"));
        }
        return ret;
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
     * @return capabilities
     * @throws IOException in case of any I/O error
     * @throws IllegalArgumentException in case of any other error
     */
    private Collection<Layer> getCapabilities() throws IOException {
        try (CachedFile cf = new CachedFile(baseUrl); InputStream in = cf.setHttpHeaders(headers).
                setMaxAge(7 * CachedFile.DAYS).
                setCachingStrategy(CachedFile.CachingStrategy.IfModifiedSince).
                getInputStream()) {
            byte[] data = Utils.readBytesFromStream(in);
            if (data == null || data.length == 0) {
                cf.clear();
                throw new IllegalArgumentException("Could not read data from: " + baseUrl);
            }

            try {
                XMLStreamReader reader = GetCapabilitiesParseHelper.getReader(new ByteArrayInputStream(data));
                Collection<Layer> ret = new ArrayList<>();
                for (int event = reader.getEventType(); reader.hasNext(); event = reader.next()) {
                    if (event == XMLStreamReader.START_ELEMENT) {
                        if (GetCapabilitiesParseHelper.QN_OWS_OPERATIONS_METADATA.equals(reader.getName())) {
                            parseOperationMetadata(reader);
                        }

                        if (QN_CONTENTS.equals(reader.getName())) {
                            ret = parseContents(reader);
                        }
                    }
                }
                return ret;
            } catch (XMLStreamException e) {
                cf.clear();
                Main.warn(new String(data, StandardCharsets.UTF_8));
                throw new IllegalArgumentException(e);
            }
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
                    layers.add(parseLayer(reader));
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
        Stack<QName> tagStack = new Stack<>();

        for (int event = reader.getEventType();
                reader.hasNext() && !(event == XMLStreamReader.END_ELEMENT && QN_LAYER.equals(reader.getName()));
                event = reader.next()) {
            if (event == XMLStreamReader.START_ELEMENT) {
                tagStack.push(reader.getName());
                if (tagStack.size() == 2) {
                    if (QN_FORMAT.equals(reader.getName())) {
                        layer.format = reader.getElementText();
                    } else if (GetCapabilitiesParseHelper.QN_OWS_IDENTIFIER.equals(reader.getName())) {
                        layer.name = reader.getElementText();
                    } else if (QN_RESOURCE_URL.equals(reader.getName()) &&
                            "tile".equals(reader.getAttributeValue("", "resourceType"))) {
                        layer.baseUrl = reader.getAttributeValue("", "template");
                    } else if (QN_STYLE.equals(reader.getName()) &&
                            "true".equals(reader.getAttributeValue("", "isDefault"))) {
                        if (GetCapabilitiesParseHelper.moveReaderToTag(reader, new QName[] {GetCapabilitiesParseHelper.QN_OWS_IDENTIFIER})) {
                            layer.style = reader.getElementText();
                            tagStack.push(reader.getName()); // keep tagStack in sync
                        }
                    } else if (QN_TILEMATRIX_SET_LINK.equals(reader.getName())) {
                        layer.tileMatrixSetLinks.add(praseTileMatrixSetLink(reader));
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
        return layer;
    }

    /**
     * Gets TileMatrixSetLink value. Returns when reader is on TileMatrixSetLink closing tag
     *
     * @param reader StAX reader instance
     * @return TileMatrixSetLink identifier
     * @throws XMLStreamException See {@link XMLStreamReader}
     */
    private static String praseTileMatrixSetLink(XMLStreamReader reader) throws XMLStreamException {
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
        Projection matrixProj = Projections.getProjectionByCode(matrixCrs);
        TileMatrix ret = new TileMatrix();

        if (matrixProj == null) {
            // use current projection if none found. Maybe user is using custom string
            matrixProj = Main.getProjection();
        }
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
     * Sets this.baseUrl and this.transferMode
     *
     * @param reader StAX reader instance
     * @throws XMLStreamException See {@link XMLStreamReader}
     */
    private void parseOperationMetadata(XMLStreamReader reader) throws XMLStreamException {
        for (int event = reader.getEventType();
                reader.hasNext() && !(event == XMLStreamReader.END_ELEMENT &&
                        GetCapabilitiesParseHelper.QN_OWS_OPERATIONS_METADATA.equals(reader.getName()));
                event = reader.next()) {
            if (event == XMLStreamReader.START_ELEMENT &&
                    GetCapabilitiesParseHelper.QN_OWS_OPERATION.equals(reader.getName()) &&
                    "GetTile".equals(reader.getAttributeValue("", "name")) &&
                    GetCapabilitiesParseHelper.moveReaderToTag(reader, new QName[] {
                            GetCapabilitiesParseHelper.QN_OWS_DCP,
                            GetCapabilitiesParseHelper.QN_OWS_HTTP,
                            GetCapabilitiesParseHelper.QN_OWS_GET,
                    })) {
                this.baseUrl = reader.getAttributeValue(GetCapabilitiesParseHelper.XLINK_NS_URL, "href");
                this.transferMode = GetCapabilitiesParseHelper.getTransferMode(reader);
            }
        }
    }

    /**
     * Initializes projection for this TileSource with projection
     * @param proj projection to be used by this TileSource
     */
    public void initProjection(Projection proj) {
        // getLayers will return only layers matching the name, if the user already choose the layer
        // so we will not ask the user again to chose the layer, if he just changes projection
        Collection<Layer> candidates = getLayers(currentLayer != null ? currentLayer.name : null, proj.toCode());
        if (!candidates.isEmpty()) {
            Layer newLayer = userSelectLayer(candidates);
            if (newLayer != null) {
                this.currentTileMatrixSet = newLayer.tileMatrixSet;
                this.currentLayer = newLayer;
                Collection<Double> scales = new ArrayList<>(currentTileMatrixSet.tileMatrix.size());
                for (TileMatrix tileMatrix : currentTileMatrixSet.tileMatrix) {
                    scales.add(tileMatrix.scaleDenominator * 0.28e-03);
                }
                this.nativeScaleList = new ScaleList(scales);
            }
        }
        this.crsScale = getTileSize() * 0.28e-03 / proj.getMetersPerUnit();
    }

    /**
     *
     * @param name of the layer to match
     * @param projectionCode projection code to match
     * @return Collection of layers matching the name of the layer and projection, or only projection if name is not provided
     */
    private Collection<Layer> getLayers(String name, String projectionCode) {
        Collection<Layer> ret = new ArrayList<>();
        if (this.layers != null) {
            for (Layer layer: this.layers) {
                if ((name == null || name.equals(layer.name)) && (projectionCode == null || projectionCode.equals(layer.tileMatrixSet.crs))) {
                    ret.add(layer);
                }
            }
        }
        return ret;
    }

    @Override
    public int getTileSize() {
        // no support for non-square tiles (tileHeight != tileWidth)
        // and for different tile sizes at different zoom levels
        Collection<Layer> projLayers = getLayers(null, Main.getProjection().toCode());
        if (!projLayers.isEmpty()) {
            return projLayers.iterator().next().tileMatrixSet.tileMatrix.get(0).tileHeight;
        }
        // if no layers is found, fallback to default mercator tile size. Maybe it will work
        Main.warn("WMTS: Could not determine tile size. Using default tile size of: {0}", getDefaultTileSize());
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

        return url.replaceAll("\\{layer\\}", this.currentLayer.name)
                .replaceAll("\\{format\\}", this.currentLayer.format)
                .replaceAll("\\{TileMatrixSet\\}", this.currentTileMatrixSet.identifier)
                .replaceAll("\\{TileMatrix\\}", tileMatrix.identifier)
                .replaceAll("\\{TileRow\\}", Integer.toString(tiley))
                .replaceAll("\\{TileCol\\}", Integer.toString(tilex))
                .replaceAll("(?i)\\{style\\}", this.currentLayer.style);
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
            return Main.getProjection().getWorldBoundsLatLon().getCenter().toCoordinate();
        }
        double scale = matrix.scaleDenominator * this.crsScale;
        EastNorth ret = new EastNorth(matrix.topLeftCorner.east() + x * scale, matrix.topLeftCorner.north() - y * scale);
        return Main.getProjection().eastNorth2latlon(ret).toCoordinate();
    }

    @Override
    public TileXY latLonToTileXY(double lat, double lon, int zoom) {
        TileMatrix matrix = getTileMatrix(zoom);
        if (matrix == null) {
            return new TileXY(0, 0);
        }

        Projection proj = Main.getProjection();
        EastNorth enPoint = proj.latlon2eastNorth(new LatLon(lat, lon));
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
        return getTileXMax(zoom, Main.getProjection());
    }

    @Override
    public int getTileXMin(int zoom) {
        return 0;
    }

    @Override
    public int getTileYMax(int zoom) {
        return getTileYMax(zoom, Main.getProjection());
    }

    @Override
    public int getTileYMin(int zoom) {
        return 0;
    }

    @Override
    public Point latLonToXY(double lat, double lon, int zoom) {
        TileMatrix matrix = getTileMatrix(zoom);
        if (matrix == null) {
            return new Point(0, 0);
        }
        double scale = matrix.scaleDenominator * this.crsScale;
        EastNorth point = Main.getProjection().latlon2eastNorth(new LatLon(lat, lon));
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
        Projection proj = Main.getProjection();
        EastNorth ret = new EastNorth(
                matrix.topLeftCorner.east() + x * scale,
                matrix.topLeftCorner.north() - y * scale
                );
        LatLon ll = proj.eastNorth2latlon(ret);
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
     * @return set of projection codes that this TileSource supports
     */
    public Set<String> getSupportedProjections() {
        Set<String> ret = new HashSet<>();
        if (currentLayer == null) {
            for (Layer layer: this.layers) {
                ret.add(layer.tileMatrixSet.crs);
            }
        } else {
            for (Layer layer: this.layers) {
                if (currentLayer.name.equals(layer.name)) {
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

}
