// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.gui.layer.WMSLayer;
import org.openstreetmap.josm.gui.layer.geoimage.GeoImageLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.Utils;

public class SessionWriter {

    private static Map<Class<? extends Layer>, Class<? extends SessionLayerExporter>> sessionLayerExporters =
            new HashMap<Class<? extends Layer>, Class<? extends SessionLayerExporter>>();
    static {
        registerSessionLayerExporter(OsmDataLayer.class , OsmDataSessionExporter.class);
        registerSessionLayerExporter(TMSLayer.class , ImagerySessionExporter.class);
        registerSessionLayerExporter(WMSLayer.class , ImagerySessionExporter.class);
        registerSessionLayerExporter(GpxLayer.class , GpxTracksSessionExporter.class);
        registerSessionLayerExporter(GeoImageLayer.class , GeoImageSessionExporter.class);
        registerSessionLayerExporter(MarkerLayer.class, MarkerSessionExporter.class);
    }

    /**
     * Register a session layer exporter.
     *
     * The exporter class must have an one-argument constructor with layerClass as formal parameter type.
     */
    public static void registerSessionLayerExporter(Class<? extends Layer> layerClass, Class<? extends SessionLayerExporter> exporter) {
        sessionLayerExporters.put(layerClass, exporter);
    }

    public static SessionLayerExporter getSessionLayerExporter(Layer layer) {
        Class<? extends Layer> layerClass = layer.getClass();
        Class<? extends SessionLayerExporter> exporterClass = sessionLayerExporters.get(layerClass);
        if (exporterClass == null) return null;
        try {
            Constructor<? extends SessionLayerExporter> constructor = exporterClass.getConstructor(layerClass);
            return constructor.newInstance(layer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final List<Layer> layers;
    private final int active; 
    private final Map<Layer, SessionLayerExporter> exporters;
    private final MultiMap<Layer, Layer> dependencies;
    private final boolean zip;

    private ZipOutputStream zipOut;

    /**
     * Constructs a new {@code SessionWriter}.
     * @param layers The ordered list of layers to save
     * @param active The index of active layer in {@code layers} (starts to 0). Ignored if set to -1
     * @param exporters The exprters to use to save layers
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
        private Document doc;
        private int layerIndex;

        public ExportSupport(Document doc, int layerIndex) {
            this.doc = doc;
            this.layerIndex = layerIndex;
        }

        public Element createElement(String name) {
            return doc.createElement(name);
        }

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
         */
        public OutputStream getOutputStreamZip(String zipPath) throws IOException {
            if (!isZip()) throw new RuntimeException();
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

    public Document createJosDocument() throws IOException {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setValidating(false);
        builderFactory.setNamespaceAware(true);
        DocumentBuilder builder = null;
        try {
            builder = builderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        Document doc = builder.newDocument();

        Element root = doc.createElement("josm-session");
        root.setAttribute("version", "0.1");
        doc.appendChild(root);

        Element viewportEl = doc.createElement("viewport");
        root.appendChild(viewportEl);
        Element centerEl = doc.createElement("center");
        viewportEl.appendChild(centerEl);
        EastNorth center = Main.map.mapView.getCenter();
        LatLon centerLL = Projections.inverseProject(center);
        centerEl.setAttribute("lat", Double.toString(centerLL.lat()));
        centerEl.setAttribute("lon", Double.toString(centerLL.lon()));
        Element scale = doc.createElement("scale");
        viewportEl.appendChild(scale);
        double dist100px = Main.map.mapView.getDist100Pixel();
        scale.setAttribute("meter-per-pixel", Double.toString(dist100px / 100));

        Element layersEl = doc.createElement("layers");
        if (active >= 0) {
            layersEl.setAttribute("active", Integer.toString(active+1));
        }
        root.appendChild(layersEl);

        for (int index=0; index<layers.size(); ++index) {
            Layer layer = layers.get(index);
            SessionLayerExporter exporter = exporters.get(layer);
            ExportSupport support = new ExportSupport(doc, index+1);
            Element el = exporter.export(support);
            el.setAttribute("index", Integer.toString(index+1));
            el.setAttribute("name", layer.getName());
            el.setAttribute("visible", Boolean.toString(layer.isVisible()));
            if (layer.getOpacity() != 1.0) {
                el.setAttribute("opacity", Double.toString(layer.getOpacity()));
            }
            Set<Layer> deps = dependencies.get(layer);
            if (!deps.isEmpty()) {
                List<Integer> depsInt = new ArrayList<Integer>();
                for (Layer depLayer : deps) {
                    int depIndex = layers.indexOf(depLayer);
                    if (depIndex == -1) throw new AssertionError();
                    depsInt.add(depIndex+1);
                }
                el.setAttribute("depends", Utils.join(",", depsInt));
            }
            layersEl.appendChild(el);
        }
        return doc;
    }

    public void writeJos(Document doc, OutputStream out) throws IOException {
        try {
            OutputStreamWriter writer = new OutputStreamWriter(out, Utils.UTF_8);
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
            throw new RuntimeException(e);
        }
    }

    public void write(File f) throws IOException {
        OutputStream out = null;
        try {
            out = new FileOutputStream(f);
        } catch (FileNotFoundException e) {
            throw new IOException(e);
        }
        write(out);
    }

    public void write (OutputStream out) throws IOException {
        if (zip) {
            zipOut = new ZipOutputStream(new BufferedOutputStream(out));
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
        Utils.close(out);
    }
}
