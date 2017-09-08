// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.NoteLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.gui.layer.WMSLayer;
import org.openstreetmap.josm.gui.layer.WMTSLayer;
import org.openstreetmap.josm.gui.layer.geoimage.GeoImageLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.gui.preferences.projection.ProjectionPreference;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 * Writes a .jos session file from current supported layers.
 * @since 4685
 */
public class SessionWriter {

    private static Map<Class<? extends Layer>, Class<? extends SessionLayerExporter>> sessionLayerExporters = new HashMap<>();

    private final List<Layer> layers;
    private final int active;
    private final Map<Layer, SessionLayerExporter> exporters;
    private final MultiMap<Layer, Layer> dependencies;
    private final boolean zip;

    private ZipOutputStream zipOut;

    static {
        registerSessionLayerExporter(OsmDataLayer.class, OsmDataSessionExporter.class);
        registerSessionLayerExporter(TMSLayer.class, ImagerySessionExporter.class);
        registerSessionLayerExporter(WMSLayer.class, ImagerySessionExporter.class);
        registerSessionLayerExporter(WMTSLayer.class, ImagerySessionExporter.class);
        registerSessionLayerExporter(GpxLayer.class, GpxTracksSessionExporter.class);
        registerSessionLayerExporter(GeoImageLayer.class, GeoImageSessionExporter.class);
        registerSessionLayerExporter(MarkerLayer.class, MarkerSessionExporter.class);
        registerSessionLayerExporter(NoteLayer.class, NoteSessionExporter.class);
    }

    /**
     * Register a session layer exporter.
     *
     * The exporter class must have a one-argument constructor with layerClass as formal parameter type.
     * @param layerClass layer class
     * @param exporter exporter for this layer class
     */
    public static void registerSessionLayerExporter(Class<? extends Layer> layerClass, Class<? extends SessionLayerExporter> exporter) {
        sessionLayerExporters.put(layerClass, exporter);
    }

    /**
     * Returns the session layer exporter for the given layer.
     * @param layer layer to export
     * @return session layer exporter for the given layer
     */
    public static SessionLayerExporter getSessionLayerExporter(Layer layer) {
        Class<? extends Layer> layerClass = layer.getClass();
        Class<? extends SessionLayerExporter> exporterClass = sessionLayerExporters.get(layerClass);
        if (exporterClass == null)
            return null;
        try {
            return exporterClass.getConstructor(layerClass).newInstance(layer);
        } catch (ReflectiveOperationException e) {
            throw new JosmRuntimeException(e);
        }
    }

    /**
     * Constructs a new {@code SessionWriter}.
     * @param layers The ordered list of layers to save
     * @param active The index of active layer in {@code layers} (starts at 0). Ignored if set to -1
     * @param exporters The exporters to use to save layers
     * @param dependencies layer dependencies
     * @param zip {@code true} if a joz archive has to be created, {@code false otherwise}
     * @since 6271
     */
    public SessionWriter(List<Layer> layers, int active, Map<Layer, SessionLayerExporter> exporters,
                MultiMap<Layer, Layer> dependencies, boolean zip) {
        this.layers = layers;
        this.active = active;
        this.exporters = exporters;
        this.dependencies = dependencies;
        this.zip = zip;
    }

    /**
     * A class that provides some context for the individual {@link SessionLayerExporter}
     * when doing the export.
     */
    public class ExportSupport {
        private final Document doc;
        private final int layerIndex;

        /**
         * Constructs a new {@code ExportSupport}.
         * @param doc XML document
         * @param layerIndex layer index
         */
        public ExportSupport(Document doc, int layerIndex) {
            this.doc = doc;
            this.layerIndex = layerIndex;
        }

        /**
         * Creates an element of the type specified.
         * @param name The name of the element type to instantiate
         * @return A new {@code Element} object
         * @see Document#createElement
         */
        public Element createElement(String name) {
            return doc.createElement(name);
        }

        /**
         * Creates a text node given the specified string.
         * @param text The data for the node.
         * @return The new {@code Text} object.
         * @see Document#createTextNode
         */
        public Text createTextNode(String text) {
            return doc.createTextNode(text);
        }

        /**
         * Get the index of the layer that is currently exported.
         * @return the index of the layer that is currently exported
         */
        public int getLayerIndex() {
            return layerIndex;
        }

        /**
         * Create a file inside the zip archive.
         *
         * @param zipPath the path inside the zip archive, e.g. "layers/03/data.xml"
         * @return the OutputStream you can write to. Never close the returned
         * output stream, but make sure to flush buffers.
         * @throws IOException if any I/O error occurs
         */
        public OutputStream getOutputStreamZip(String zipPath) throws IOException {
            if (!isZip()) throw new JosmRuntimeException("not zip");
            ZipEntry entry = new ZipEntry(zipPath);
            zipOut.putNextEntry(entry);
            return zipOut;
        }

        /**
         * Check, if the session is exported as a zip archive.
         *
         * @return true, if the session is exported as a zip archive (.joz file
         * extension). It will always return true, if one of the
         * {@link SessionLayerExporter} returns true for the
         * {@link SessionLayerExporter#requiresZip()} method. Otherwise, the
         * user can decide in the file chooser dialog.
         */
        public boolean isZip() {
            return zip;
        }
    }

    /**
     * Creates XML (.jos) session document.
     * @return new document
     * @throws IOException if any I/O error occurs
     */
    public Document createJosDocument() throws IOException {
        DocumentBuilder builder = null;
        try {
            builder = Utils.newSafeDOMBuilder();
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        }
        Document doc = builder.newDocument();

        Element root = doc.createElement("josm-session");
        root.setAttribute("version", "0.1");
        doc.appendChild(root);

        writeViewPort(root);
        writeProjection(root);

        Element layersEl = doc.createElement("layers");
        if (active >= 0) {
            layersEl.setAttribute("active", Integer.toString(active+1));
        }
        root.appendChild(layersEl);

        for (int index = 0; index < layers.size(); ++index) {
            Layer layer = layers.get(index);
            SessionLayerExporter exporter = exporters.get(layer);
            ExportSupport support = new ExportSupport(doc, index+1);
            Element el = exporter.export(support);
            el.setAttribute("index", Integer.toString(index+1));
            el.setAttribute("name", layer.getName());
            el.setAttribute("visible", Boolean.toString(layer.isVisible()));
            if (!Utils.equalsEpsilon(layer.getOpacity(), 1.0)) {
                el.setAttribute("opacity", Double.toString(layer.getOpacity()));
            }
            Set<Layer> deps = dependencies.get(layer);
            if (deps != null && !deps.isEmpty()) {
                List<Integer> depsInt = new ArrayList<>();
                for (Layer depLayer : deps) {
                    int depIndex = layers.indexOf(depLayer);
                    if (depIndex == -1) {
                        Logging.warn("Unable to find " + depLayer);
                    } else {
                        depsInt.add(depIndex+1);
                    }
                }
                if (!depsInt.isEmpty()) {
                    el.setAttribute("depends", Utils.join(",", depsInt));
                }
            }
            layersEl.appendChild(el);
        }
        return doc;
    }

    private static void writeViewPort(Element root) {
        Document doc = root.getOwnerDocument();
        Element viewportEl = doc.createElement("viewport");
        root.appendChild(viewportEl);
        Element centerEl = doc.createElement("center");
        viewportEl.appendChild(centerEl);
        MapView mapView = MainApplication.getMap().mapView;
        EastNorth center = mapView.getCenter();
        LatLon centerLL = Main.getProjection().eastNorth2latlon(center);
        centerEl.setAttribute("lat", Double.toString(centerLL.lat()));
        centerEl.setAttribute("lon", Double.toString(centerLL.lon()));
        Element scale = doc.createElement("scale");
        viewportEl.appendChild(scale);
        double dist100px = mapView.getDist100Pixel();
        scale.setAttribute("meter-per-pixel", Double.toString(dist100px / 100));
    }

    private static void writeProjection(Element root) {
        Document doc = root.getOwnerDocument();
        Element projectionEl = doc.createElement("projection");
        root.appendChild(projectionEl);
        String pcId = ProjectionPreference.getCurrentProjectionChoiceId();
        Element projectionChoiceEl = doc.createElement("projection-choice");
        projectionEl.appendChild(projectionChoiceEl);
        Element idEl = doc.createElement("id");
        projectionChoiceEl.appendChild(idEl);
        idEl.setTextContent(pcId);
        Collection<String> parameters = ProjectionPreference.getSubprojectionPreference(pcId);
        Element parametersEl = doc.createElement("parameters");
        projectionChoiceEl.appendChild(parametersEl);
        if (parameters != null) {
            for (String param : parameters) {
                Element paramEl = doc.createElement("param");
                parametersEl.appendChild(paramEl);
                paramEl.setTextContent(param);
            }
        }
        String code = Main.getProjection().toCode();
        if (code != null) {
            Element codeEl = doc.createElement("code");
            projectionEl.appendChild(codeEl);
            codeEl.setTextContent(code);
        }
    }

    /**
     * Writes given .jos document to an output stream.
     * @param doc session document
     * @param out output stream
     * @throws IOException if any I/O error occurs
     */
    public void writeJos(Document doc, OutputStream out) throws IOException {
        try {
            OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            TransformerFactory transfac = TransformerFactory.newInstance();
            Transformer trans = transfac.newTransformer();
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            StreamResult result = new StreamResult(writer);
            DOMSource source = new DOMSource(doc);
            trans.transform(source, result);
        } catch (TransformerException e) {
            throw new JosmRuntimeException(e);
        }
    }

    /**
     * Writes session to given file.
     * @param f output file
     * @throws IOException if any I/O error occurs
     */
    public void write(File f) throws IOException {
        try (OutputStream out = new FileOutputStream(f)) {
            write(out);
        } catch (FileNotFoundException e) {
            throw new IOException(e);
        }
    }

    /**
     * Writes session to given output stream.
     * @param out output stream
     * @throws IOException if any I/O error occurs
     */
    public void write(OutputStream out) throws IOException {
        if (zip) {
            zipOut = new ZipOutputStream(new BufferedOutputStream(out), StandardCharsets.UTF_8);
        }
        Document doc = createJosDocument(); // as side effect, files may be added to zipOut
        if (zip) {
            ZipEntry entry = new ZipEntry("session.jos");
            zipOut.putNextEntry(entry);
            writeJos(doc, zipOut);
            Utils.close(zipOut);
        } else {
            writeJos(doc, new BufferedOutputStream(out));
        }
    }
}
