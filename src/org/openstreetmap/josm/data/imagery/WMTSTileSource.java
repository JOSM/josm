// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.TileXY;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.interfaces.TemplatedTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.TMSTileSource;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Tile Source handling WMS providers
 *
 * @author Wiktor Niesiobędzki
 * @since 8526
 */
public class WMTSTileSource extends TMSTileSource implements TemplatedTileSource {
    private static final String PATTERN_HEADER  = "\\{header\\(([^,]+),([^}]+)\\)\\}";

    private static final String URL_GET_ENCODING_PARAMS = "SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER={layer}&STYLE={Style}&"
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

    private static class TileMatrixSet {
        SortedSet<TileMatrix> tileMatrix = new TreeSet<>(new Comparator<TileMatrix>() {
            @Override
            public int compare(TileMatrix o1, TileMatrix o2) {
                // reverse the order, so it will be from greatest (lowest zoom level) to lowest value (highest zoom level)
                return -1 * Double.compare(o1.scaleDenominator, o2.scaleDenominator);
            }
        }); // sorted by zoom level
        private String crs;
        private String identifier;
    }

    private static class Layer {
        private String format;
        private String name;
        private TileMatrixSet tileMatrixSet;
        private String baseUrl;
        private String style;
    }

    private enum TransferMode {
        KVP("KVP"),
        REST("RESTful");

        private final String typeString;

        TransferMode(String urlString) {
            this.typeString = urlString;
        }

        private String getTypeString() {
            return typeString;
        }

        private static TransferMode fromString(String s) {
            for (TransferMode type : TransferMode.values()) {
                if (type.getTypeString().equals(s)) {
                    return type;
                }
            }
            return null;
        }
    }

    private static final class SelectLayerDialog extends ExtendedDialog {
        private final Layer[] layers;
        private final JTable list;

        public SelectLayerDialog(Collection<Layer> layers) {
            super(Main.parent, tr("Select WMTS layer"), new String[]{tr("Add layers"), tr("Cancel")});
            this.layers = layers.toArray(new Layer[]{});
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
            this.list.setPreferredSize(new Dimension(400, 400));
            this.list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            this.list.setRowSelectionAllowed(true);
            this.list.setColumnSelectionAllowed(false);
            JPanel panel = new JPanel(new GridBagLayout());
            panel.add(this.list, GBC.eol().fill());
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
    private Collection<Layer> layers;
    private Layer currentLayer;
    private TileMatrixSet currentTileMatrixSet;
    private double crsScale;
    private TransferMode transferMode;

    /**
     * Creates a tile source based on imagery info
     * @param info imagery info
     * @throws IOException if any I/O error occurs
     */
    public WMTSTileSource(ImageryInfo info) throws IOException {
        super(info);
        this.baseUrl = normalizeCapabilitiesUrl(handleTemplate(info.getUrl()));
        this.layers = getCapabilities();
        if (this.layers.isEmpty())
            throw new IllegalArgumentException(tr("No layers defined by getCapabilities document: {0}", info.getUrl()));

        // Not needed ? initProjection();
    }

    private Layer userSelectLayer(Collection<Layer> layers) {
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

    private Collection<Layer> getCapabilities() throws IOException {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setValidating(false);
        builderFactory.setNamespaceAware(false);
        try {
            builderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (ParserConfigurationException e) {
            //this should not happen
            throw new IllegalArgumentException(e);
        }
        DocumentBuilder builder = null;
        InputStream in = new CachedFile(baseUrl).
                setHttpHeaders(headers).
                setMaxAge(7 * CachedFile.DAYS).
                setCachingStrategy(CachedFile.CachingStrategy.IfModifiedSince).
                getInputStream();
        try {
            builder = builderFactory.newDocumentBuilder();
            byte[] data = Utils.readBytesFromStream(in);
            if (data == null || data.length == 0) {
                throw new IllegalArgumentException("Could not read data from: " + baseUrl);
            }
            Document document = builder.parse(new ByteArrayInputStream(data));
            Node getTileOperation = getByXpath(document,
                    "/Capabilities/OperationsMetadata/Operation[@name=\"GetTile\"]/DCP/HTTP/Get").item(0);
            this.baseUrl = getStringByXpath(getTileOperation, "@href");
            this.transferMode = TransferMode.fromString(getStringByXpath(getTileOperation,
                    "Constraint[@name=\"GetEncoding\"]/AllowedValues/Value"));
            NodeList layersNodeList = getByXpath(document, "/Capabilities/Contents/Layer");
            Map<String, TileMatrixSet> matrixSetById = parseMatrices(getByXpath(document, "/Capabilities/Contents/TileMatrixSet"));
            return parseLayer(layersNodeList, matrixSetById);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static String normalizeCapabilitiesUrl(String url) throws MalformedURLException {
        URL inUrl = new URL(url);
        URL ret = new URL(inUrl.getProtocol(), inUrl.getHost(), inUrl.getPort(), inUrl.getFile());
        return ret.toExternalForm();
    }

    private Collection<Layer> parseLayer(NodeList nodeList, Map<String, TileMatrixSet> matrixSetById) throws XPathExpressionException {
        Collection<Layer> ret = new ArrayList<>();
        for (int layerId = 0; layerId < nodeList.getLength(); layerId++) {
            Node layerNode = nodeList.item(layerId);
            NodeList tileMatrixSetLinks = getByXpath(layerNode, "TileMatrixSetLink");

            // we add an layer for all matrix sets to allow user to choose, with which tileset he wants to work
            for (int tileMatrixId = 0; tileMatrixId < tileMatrixSetLinks.getLength(); tileMatrixId++) {
                Layer layer = new Layer();
                layer.format = getStringByXpath(layerNode, "Format");
                layer.name = getStringByXpath(layerNode, "Identifier");
                layer.baseUrl = getStringByXpath(layerNode, "ResourceURL[@resourceType='tile']/@template");
                layer.style = getStringByXpath(layerNode, "Style[@isDefault='true']/Identifier");
                if (layer.style == null) {
                    layer.style = "";
                }
                Node tileMatrixLink = tileMatrixSetLinks.item(tileMatrixId);
                TileMatrixSet tms = matrixSetById.get(getStringByXpath(tileMatrixLink, "TileMatrixSet"));
                layer.tileMatrixSet = tms;
                ret.add(layer);
            }
        }
        return ret;

    }

    private Map<String, TileMatrixSet> parseMatrices(NodeList nodeList) throws XPathExpressionException {
        Map<String, TileMatrixSet> ret = new ConcurrentHashMap<>();
        for (int matrixSetId = 0; matrixSetId < nodeList.getLength(); matrixSetId++) {
            Node matrixSetNode = nodeList.item(matrixSetId);
            TileMatrixSet matrixSet = new TileMatrixSet();
            matrixSet.identifier = getStringByXpath(matrixSetNode, "Identifier");
            matrixSet.crs = crsToCode(getStringByXpath(matrixSetNode, "SupportedCRS"));
            NodeList tileMatrixList = getByXpath(matrixSetNode, "TileMatrix");
            Projection matrixProj = Projections.getProjectionByCode(matrixSet.crs);
            if (matrixProj == null) {
                // use current projection if none found. Maybe user is using custom string
                matrixProj = Main.getProjection();
            }
            for (int matrixId = 0; matrixId < tileMatrixList.getLength(); matrixId++) {
                Node tileMatrixNode = tileMatrixList.item(matrixId);
                TileMatrix tileMatrix = new TileMatrix();
                tileMatrix.identifier = getStringByXpath(tileMatrixNode, "Identifier");
                tileMatrix.scaleDenominator = Double.parseDouble(getStringByXpath(tileMatrixNode, "ScaleDenominator"));
                String[] topLeftCorner = getStringByXpath(tileMatrixNode, "TopLeftCorner").split(" ");

                if (matrixProj.switchXY()) {
                    tileMatrix.topLeftCorner = new EastNorth(Double.parseDouble(topLeftCorner[1]), Double.parseDouble(topLeftCorner[0]));
                } else {
                    tileMatrix.topLeftCorner = new EastNorth(Double.parseDouble(topLeftCorner[0]), Double.parseDouble(topLeftCorner[1]));
                }
                tileMatrix.tileHeight = Integer.parseInt(getStringByXpath(tileMatrixNode, "TileHeight"));
                tileMatrix.tileWidth = Integer.parseInt(getStringByXpath(tileMatrixNode, "TileHeight"));
                tileMatrix.matrixWidth = getOptionalIntegerByXpath(tileMatrixNode, "MatrixWidth");
                tileMatrix.matrixHeight = getOptionalIntegerByXpath(tileMatrixNode, "MatrixHeight");
                if (tileMatrix.tileHeight != tileMatrix.tileWidth) {
                    throw new AssertionError(tr("Only square tiles are supported. {0}x{1} returned by server for TileMatrix identifier {2}",
                            tileMatrix.tileHeight, tileMatrix.tileWidth, tileMatrix.identifier));
                }

                matrixSet.tileMatrix.add(tileMatrix);
            }
            ret.put(matrixSet.identifier, matrixSet);
        }
        return ret;
    }

    private static String crsToCode(String crsIdentifier) {
        if (crsIdentifier.startsWith("urn:ogc:def:crs:")) {
            return crsIdentifier.replaceFirst("urn:ogc:def:crs:([^:]*):.*:(.*)$", "$1:$2");
        }
        return crsIdentifier;
    }

    private static int getOptionalIntegerByXpath(Node document, String xpathQuery) throws XPathExpressionException {
        String ret = getStringByXpath(document, xpathQuery);
        if (ret == null || "".equals(ret)) {
            return -1;
        }
        return Integer.parseInt(ret);
    }

    private static String getStringByXpath(Node document, String xpathQuery) throws XPathExpressionException {
        return (String) getByXpath(document, xpathQuery, XPathConstants.STRING);
    }

    private static NodeList getByXpath(Node document, String xpathQuery) throws XPathExpressionException {
        return (NodeList) getByXpath(document, xpathQuery, XPathConstants.NODESET);
    }

    private static Object getByXpath(Node document, String xpathQuery, QName returnType) throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression expr = xpath.compile(xpathQuery);
        return expr.evaluate(document, returnType);
    }

    /**
     * Initializes projection for this TileSource with current projection
     */
    protected void initProjection() {
        initProjection(Main.getProjection());
    }

    /**
     * Initializes projection for this TileSource with projection
     * @param proj projection to be used by this TileSource
     */
    public void initProjection(Projection proj) {
        String layerName = null;
        if (currentLayer != null) {
            layerName = currentLayer.name;
        }
        Collection<Layer> candidates = getLayers(layerName, proj.toCode());
        if (!candidates.isEmpty()) {
            Layer newLayer = userSelectLayer(candidates);
            if (newLayer != null) {
                this.currentTileMatrixSet = newLayer.tileMatrixSet;
                this.currentLayer = newLayer;
            }
        }

        this.crsScale = getTileSize() * 0.28e-03 / proj.getMetersPerUnit();
    }

    private Collection<Layer> getLayers(String name, String projectionCode) {
        Collection<Layer> ret = new ArrayList<>();
        for (Layer layer: this.layers) {
            if ((name == null || name.equals(layer.name)) && (projectionCode == null || projectionCode.equals(layer.tileMatrixSet.crs))) {
                ret.add(layer);
            }
        }
        return ret;
    }

    @Override
    public int getDefaultTileSize() {
        return getTileSize();
    }

    // FIXME: remove in September 2015, when ImageryPreferenceEntry.tileSize will be initialized to -1 instead to 256
    // need to leave it as it is to keep compatiblity between tested and latest JOSM versions
    @Override
    public int getTileSize() {
        TileMatrix matrix = getTileMatrix(1);
        if (matrix == null) {
            return 1;
        }
        return matrix.tileHeight;
    }

    @Override
    public String getTileUrl(int zoom, int tilex, int tiley) {
        String url;
        if (currentLayer == null) {
            return "";
        }

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
                .replaceAll("\\{Style\\}", this.currentLayer.style);
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
        if (zoom < 1) {
            return null;
        }
        return this.currentTileMatrixSet.tileMatrix.toArray(new TileMatrix[]{})[zoom - 1];
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
        return latLonToTileXY(point.getLat(),  point.getLon(), zoom);
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
                    (int) Math.round((point.east() - matrix.topLeftCorner.east())   / scale),
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
            return this.currentTileMatrixSet.tileMatrix.size();
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
}
