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

import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.Utils;

public class SessionWriter {

    private static Map<Class<? extends Layer>, Class<? extends SessionLayerExporter>> sessionLayerExporters =
            new HashMap<Class<? extends Layer>, Class<? extends SessionLayerExporter>>();
    static {
        registerSessionLayerExporter(OsmDataLayer.class , OsmDataSessionExporter.class);
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
            @SuppressWarnings("unchecked")
            Constructor<? extends SessionLayerExporter> constructor = (Constructor) exporterClass.getConstructor(layerClass);
            return constructor.newInstance(layer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<Layer> layers;
    private Map<Layer, SessionLayerExporter> exporters;
    private MultiMap<Layer, Layer> dependencies;
    private boolean zip;

    private ZipOutputStream zipOut;

    public SessionWriter(List<Layer> layers, Map<Layer, SessionLayerExporter> exporters,
                MultiMap<Layer, Layer> dependencies, boolean zip) {
        this.layers = layers;
        this.exporters = exporters;
        this.dependencies = dependencies;
        this.zip = zip;
    }

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

        public int getLayerIndex() {
            return layerIndex;
        }

        /**
         * Create a file in the zip archive.
         *
         * @return never close the output stream, but make sure to flush buffers
         */
        public OutputStream getOutputStreamZip(String zipPath) throws IOException {
            if (!isZip()) throw new RuntimeException();
            ZipEntry entry = new ZipEntry(zipPath);
            zipOut.putNextEntry(entry);
            return zipOut;
        }

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

        Element layersEl = doc.createElement("layers");
        root.appendChild(layersEl);

        for (int index=0; index<layers.size(); ++index) {
            Layer layer = layers.get(index);
            SessionLayerExporter exporter = exporters.get(layer);
            ExportSupport support = new ExportSupport(doc, index+1);
            Element el = exporter.export(support);
            el.setAttribute("index", Integer.toString(index+1));
            el.setAttribute("name", layer.getName());
            Set<Layer> deps = dependencies.get(layer);
            if (deps.size() > 0) {
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
            OutputStreamWriter writer = new OutputStreamWriter(out, "utf-8");
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
            zipOut.close();
        } else {
            writeJos(doc, new BufferedOutputStream(out));
        }
        Utils.close(out);
    }
}
