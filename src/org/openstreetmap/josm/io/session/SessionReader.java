// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.Utils.equal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.NavigatableComponent.ViewportData;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Reads a .jos session file and loads the layers in the process.
 *
 */
public class SessionReader {

    private static Map<String, Class<? extends SessionLayerImporter>> sessionLayerImporters = new HashMap<String, Class<? extends SessionLayerImporter>>();
    static {
        registerSessionLayerImporter("osm-data", OsmDataSessionImporter.class);
        registerSessionLayerImporter("imagery", ImagerySessionImporter.class);
        registerSessionLayerImporter("tracks", GpxTracksSessionImporter.class);
        registerSessionLayerImporter("geoimage", GeoImageSessionImporter.class);
        registerSessionLayerImporter("markers", MarkerSessionImporter.class);
    }

    public static void registerSessionLayerImporter(String layerType, Class<? extends SessionLayerImporter> importer) {
        sessionLayerImporters.put(layerType, importer);
    }

    public static SessionLayerImporter getSessionLayerImporter(String layerType) {
        Class<? extends SessionLayerImporter> importerClass = sessionLayerImporters.get(layerType);
        if (importerClass == null)
            return null;
        SessionLayerImporter importer = null;
        try {
            importer = importerClass.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return importer;
    }

    private URI sessionFileURI;
    private boolean zip; // true, if session file is a .joz file; false if it is a .jos file
    private ZipFile zipFile;
    private List<Layer> layers = new ArrayList<Layer>();
    private int active = -1;
    private List<Runnable> postLoadTasks = new ArrayList<Runnable>();
    private ViewportData viewport;

    /**
     * @return list of layers that are later added to the mapview
     */
    public List<Layer> getLayers() {
        return layers;
    }

    /**
     * @return active layer, or {@code null} if not set
     * @since 6271
     */
    public Layer getActive() {
        // layers is in reverse order because of the way TreeMap is built
        return (active >= 0 && active < layers.size()) ? layers.get(layers.size()-1-active) : null;
    }

    /**
     * @return actions executed in EDT after layers have been added (message dialog, etc.)
     */
    public List<Runnable> getPostLoadTasks() {
        return postLoadTasks;
    }

    /**
     * Return the viewport (map position and scale).
     * @return The viewport. Can be null when no viewport info is found in the file.
     */
    public ViewportData getViewport() {
        return viewport;
    }

    public class ImportSupport {

        private String layerName;
        private int layerIndex;
        private List<LayerDependency> layerDependencies;

        public ImportSupport(String layerName, int layerIndex, List<LayerDependency> layerDependencies) {
            this.layerName = layerName;
            this.layerIndex = layerIndex;
            this.layerDependencies = layerDependencies;
        }

        /**
         * Path of the file inside the zip archive.
         * Used as alternative return value for getFile method.
         */
        private String inZipPath;

        /**
         * Add a task, e.g. a message dialog, that should
         * be executed in EDT after all layers have been added.
         */
        public void addPostLayersTask(Runnable task) {
            postLoadTasks.add(task);
        }

        /**
         * Return an InputStream for a URI from a .jos/.joz file.
         *
         * The following forms are supported:
         *
         * - absolute file (both .jos and .joz):
         *         "file:///home/user/data.osm"
         *         "file:/home/user/data.osm"
         *         "file:///C:/files/data.osm"
         *         "file:/C:/file/data.osm"
         *         "/home/user/data.osm"
         *         "C:\files\data.osm"          (not a URI, but recognized by File constructor on Windows systems)
         * - standalone .jos files:
         *     - relative uri:
         *         "save/data.osm"
         *         "../project2/data.osm"
         * - for .joz files:
         *     - file inside zip archive:
         *         "layers/01/data.osm"
         *     - relativ to the .joz file:
         *         "../save/data.osm"           ("../" steps out of the archive)
         *
         * @throws IOException Thrown when no Stream can be opened for the given URI, e.g. when the linked file has been deleted.
         */
        public InputStream getInputStream(String uriStr) throws IOException {
            File file = getFile(uriStr);
            if (file != null) {
                try {
                    return new BufferedInputStream(new FileInputStream(file));
                } catch (FileNotFoundException e) {
                    throw new IOException(tr("File ''{0}'' does not exist.", file.getPath()));
                }
            } else if (inZipPath != null) {
                ZipEntry entry = zipFile.getEntry(inZipPath);
                if (entry != null) {
                    return zipFile.getInputStream(entry);
                }
            }
            throw new IOException(tr("Unable to locate file  ''{0}''.", uriStr));
        }

        /**
         * Return a File for a URI from a .jos/.joz file.
         *
         * Returns null if the URI points to a file inside the zip archive.
         * In this case, inZipPath will be set to the corresponding path.
         */
        public File getFile(String uriStr) throws IOException {
            inZipPath = null;
            try {
                URI uri = new URI(uriStr);
                if ("file".equals(uri.getScheme()))
                    // absolute path
                    return new File(uri);
                else if (uri.getScheme() == null) {
                    // Check if this is an absolute path without 'file:' scheme part.
                    // At this point, (as an exception) platform dependent path separator will be recognized.
                    // (This form is discouraged, only for users that like to copy and paste a path manually.)
                    File file = new File(uriStr);
                    if (file.isAbsolute())
                        return file;
                    else {
                        // for relative paths, only forward slashes are permitted
                        if (isZip()) {
                            if (uri.getPath().startsWith("../")) {
                                // relative to session file - "../" step out of the archive
                                String relPath = uri.getPath().substring(3);
                                return new File(sessionFileURI.resolve(relPath));
                            } else {
                                // file inside zip archive
                                inZipPath = uriStr;
                                return null;
                            }
                        } else
                            return new File(sessionFileURI.resolve(uri));
                    }
                } else
                    throw new IOException(tr("Unsupported scheme ''{0}'' in URI ''{1}''.", uri.getScheme(), uriStr));
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
        }

        /**
         * Determines if we are reading from a .joz file.
         * @return {@code true} if we are reading from a .joz file, {@code false} otherwise
         */
        public boolean isZip() {
            return zip;
        }

        /**
         * Name of the layer that is currently imported.
         */
        public String getLayerName() {
            return layerName;
        }

        /**
         * Index of the layer that is currently imported.
         */
        public int getLayerIndex() {
            return layerIndex;
        }

        /**
         * Dependencies - maps the layer index to the importer of the given
         * layer. All the dependent importers have loaded completely at this point.
         */
        public List<LayerDependency> getLayerDependencies() {
            return layerDependencies;
        }
    }

    public static class LayerDependency {
        private Integer index;
        private Layer layer;
        private SessionLayerImporter importer;

        public LayerDependency(Integer index, Layer layer, SessionLayerImporter importer) {
            this.index = index;
            this.layer = layer;
            this.importer = importer;
        }

        public SessionLayerImporter getImporter() {
            return importer;
        }

        public Integer getIndex() {
            return index;
        }

        public Layer getLayer() {
            return layer;
        }
    }

    private static void error(String msg) throws IllegalDataException {
        throw new IllegalDataException(msg);
    }

    private void parseJos(Document doc, ProgressMonitor progressMonitor) throws IllegalDataException {
        Element root = doc.getDocumentElement();
        if (!equal(root.getTagName(), "josm-session")) {
            error(tr("Unexpected root element ''{0}'' in session file", root.getTagName()));
        }
        String version = root.getAttribute("version");
        if (!"0.1".equals(version)) {
            error(tr("Version ''{0}'' of session file is not supported. Expected: 0.1", version));
        }

        Element viewportEl = getElementByTagName(root, "viewport");
        if (viewportEl != null) {
            EastNorth center = null;
            Element centerEl = getElementByTagName(viewportEl, "center");
            if (centerEl != null && centerEl.hasAttribute("lat") && centerEl.hasAttribute("lon")) {
                try {
                    LatLon centerLL = new LatLon(Double.parseDouble(centerEl.getAttribute("lat")), Double.parseDouble(centerEl.getAttribute("lon")));
                    center = Projections.project(centerLL);
                } catch (NumberFormatException ex) {
                    Main.warn(ex);
                }
            }
            if (center != null) {
                Element scaleEl = getElementByTagName(viewportEl, "scale");
                if (scaleEl != null && scaleEl.hasAttribute("meter-per-pixel")) {
                    try {
                        double meterPerPixel = Double.parseDouble(scaleEl.getAttribute("meter-per-pixel"));
                        Projection proj = Main.getProjection();
                        // Get a "typical" distance in east/north units that
                        // corresponds to a couple of pixels. Shouldn't be too
                        // large, to keep it within projection bounds and
                        // not too small to avoid rounding errors.
                        double dist = 0.01 * proj.getDefaultZoomInPPD();
                        LatLon ll1 = proj.eastNorth2latlon(new EastNorth(center.east() - dist, center.north()));
                        LatLon ll2 = proj.eastNorth2latlon(new EastNorth(center.east() + dist, center.north()));
                        double meterPerEasting = ll1.greatCircleDistance(ll2) / dist / 2;
                        double scale = meterPerPixel / meterPerEasting; // unit: easting per pixel
                        viewport = new ViewportData(center, scale);
                    } catch (NumberFormatException ex) {
                        Main.warn(ex);
                    }
                }
            }
        }

        Element layersEl = getElementByTagName(root, "layers");
        if (layersEl == null) return;

        String activeAtt = layersEl.getAttribute("active");
        try {
            active = (activeAtt != null && !activeAtt.isEmpty()) ? Integer.parseInt(activeAtt)-1 : -1;
        } catch (NumberFormatException e) {
            Main.warn("Unsupported value for 'active' layer attribute. Ignoring it. Error was: "+e.getMessage());
            active = -1;
        }

        MultiMap<Integer, Integer> deps = new MultiMap<Integer, Integer>();
        Map<Integer, Element> elems = new HashMap<Integer, Element>();

        NodeList nodes = layersEl.getChildNodes();

        for (int i=0; i<nodes.getLength(); ++i) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) node;
                if (equal(e.getTagName(), "layer")) {

                    if (!e.hasAttribute("index")) {
                        error(tr("missing mandatory attribute ''index'' for element ''layer''"));
                    }
                    Integer idx = null;
                    try {
                        idx = Integer.parseInt(e.getAttribute("index"));
                    } catch (NumberFormatException ex) {
                        Main.warn(ex);
                    }
                    if (idx == null) {
                        error(tr("unexpected format of attribute ''index'' for element ''layer''"));
                    }
                    if (elems.containsKey(idx)) {
                        error(tr("attribute ''index'' ({0}) for element ''layer'' must be unique", Integer.toString(idx)));
                    }
                    elems.put(idx, e);

                    deps.putVoid(idx);
                    String depStr = e.getAttribute("depends");
                    if (depStr != null && !depStr.isEmpty()) {
                        for (String sd : depStr.split(",")) {
                            Integer d = null;
                            try {
                                d = Integer.parseInt(sd);
                            } catch (NumberFormatException ex) {
                                Main.warn(ex);
                            }
                            if (d != null) {
                                deps.put(idx, d);
                            }
                        }
                    }
                }
            }
        }

        List<Integer> sorted = Utils.topologicalSort(deps);
        final Map<Integer, Layer> layersMap = new TreeMap<Integer, Layer>(Collections.reverseOrder());
        final Map<Integer, SessionLayerImporter> importers = new HashMap<Integer, SessionLayerImporter>();
        final Map<Integer, String> names = new HashMap<Integer, String>();

        progressMonitor.setTicksCount(sorted.size());
        LAYER: for (int idx: sorted) {
            Element e = elems.get(idx);
            if (e == null) {
                error(tr("missing layer with index {0}", idx));
            }
            if (!e.hasAttribute("name")) {
                error(tr("missing mandatory attribute ''name'' for element ''layer''"));
            }
            String name = e.getAttribute("name");
            names.put(idx, name);
            if (!e.hasAttribute("type")) {
                error(tr("missing mandatory attribute ''type'' for element ''layer''"));
            }
            String type = e.getAttribute("type");
            SessionLayerImporter imp = getSessionLayerImporter(type);
            if (imp == null) {
                CancelOrContinueDialog dialog = new CancelOrContinueDialog();
                dialog.show(
                        tr("Unable to load layer"),
                        tr("Cannot load layer of type ''{0}'' because no suitable importer was found.", type),
                        JOptionPane.WARNING_MESSAGE,
                        progressMonitor
                        );
                if (dialog.isCancel()) {
                    progressMonitor.cancel();
                    return;
                } else {
                    continue;
                }
            } else {
                importers.put(idx, imp);
                List<LayerDependency> depsImp = new ArrayList<LayerDependency>();
                for (int d : deps.get(idx)) {
                    SessionLayerImporter dImp = importers.get(d);
                    if (dImp == null) {
                        CancelOrContinueDialog dialog = new CancelOrContinueDialog();
                        dialog.show(
                                tr("Unable to load layer"),
                                tr("Cannot load layer {0} because it depends on layer {1} which has been skipped.", idx, d),
                                JOptionPane.WARNING_MESSAGE,
                                progressMonitor
                                );
                        if (dialog.isCancel()) {
                            progressMonitor.cancel();
                            return;
                        } else {
                            continue LAYER;
                        }
                    }
                    depsImp.add(new LayerDependency(d, layersMap.get(d), dImp));
                }
                ImportSupport support = new ImportSupport(name, idx, depsImp);
                Layer layer = null;
                Exception exception = null;
                try {
                    layer = imp.load(e, support, progressMonitor.createSubTaskMonitor(1, false));
                } catch (IllegalDataException ex) {
                    exception = ex;
                } catch (IOException ex) {
                    exception = ex;
                }
                if (exception != null) {
                    Main.error(exception);
                    CancelOrContinueDialog dialog = new CancelOrContinueDialog();
                    dialog.show(
                            tr("Error loading layer"),
                            tr("<html>Could not load layer {0} ''{1}''.<br>Error is:<br>{2}</html>", idx, name, exception.getMessage()),
                            JOptionPane.ERROR_MESSAGE,
                            progressMonitor
                            );
                    if (dialog.isCancel()) {
                        progressMonitor.cancel();
                        return;
                    } else {
                        continue;
                    }
                }

                if (layer == null) throw new RuntimeException();
                layersMap.put(idx, layer);
            }
            progressMonitor.worked(1);
        }

        layers = new ArrayList<Layer>();
        for (int idx : layersMap.keySet()) {
            Layer layer = layersMap.get(idx);
            if (layer == null) {
                continue;
            }
            Element el = elems.get(idx);
            if (el.hasAttribute("visible")) {
                layer.setVisible(Boolean.parseBoolean(el.getAttribute("visible")));
            }
            if (el.hasAttribute("opacity")) {
                try {
                    double opacity = Double.parseDouble(el.getAttribute("opacity"));
                    layer.setOpacity(opacity);
                } catch (NumberFormatException ex) {
                    Main.warn(ex);
                }
            }
        }
        for (Entry<Integer, Layer> e : layersMap.entrySet()) {
            Layer l = e.getValue();
            if (l == null) {
                continue;
            }

            l.setName(names.get(e.getKey()));
            layers.add(l);
        }
    }

    /**
     * Show Dialog when there is an error for one layer.
     * Ask the user whether to cancel the complete session loading or just to skip this layer.
     *
     * This is expected to run in a worker thread (PleaseWaitRunnable), so invokeAndWait is
     * needed to block the current thread and wait for the result of the modal dialog from EDT.
     */
    private static class CancelOrContinueDialog {

        private boolean cancel;

        public void show(final String title, final String message, final int icon, final ProgressMonitor progressMonitor) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override public void run() {
                        ExtendedDialog dlg = new ExtendedDialog(
                                Main.parent,
                                title,
                                new String[] { tr("Cancel"), tr("Skip layer and continue") }
                                );
                        dlg.setButtonIcons(new String[] {"cancel", "dialogs/next"});
                        dlg.setIcon(icon);
                        dlg.setContent(message);
                        dlg.showDialog();
                        cancel = dlg.getValue() != 2;
                    }
                });
            } catch (InvocationTargetException ex) {
                throw new RuntimeException(ex);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }

        public boolean isCancel() {
            return cancel;
        }
    }

    public void loadSession(File sessionFile, boolean zip, ProgressMonitor progressMonitor) throws IllegalDataException, IOException {
        if (progressMonitor == null) {
            progressMonitor = NullProgressMonitor.INSTANCE;
        }

        InputStream josIS = null;

        if (zip) {
            try {
                zipFile = new ZipFile(sessionFile);
                josIS = getZipInputStream(zipFile);
            } catch (ZipException ze) {
                throw new IOException(ze);
            }
        } else {
            try {
                josIS = new FileInputStream(sessionFile);
            } catch (FileNotFoundException ex) {
                throw new IOException(ex);
            }
        }

        loadSession(josIS, sessionFile.toURI(), zip, progressMonitor);
    }

    private static InputStream getZipInputStream(ZipFile zipFile) throws ZipException, IOException, IllegalDataException {
        ZipEntry josEntry = null;
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.getName().toLowerCase().endsWith(".jos")) {
                josEntry = entry;
                break;
            }
        }
        if (josEntry == null) {
            error(tr("expected .jos file inside .joz archive"));
        }
        return zipFile.getInputStream(josEntry);
    }

    private void loadSession(InputStream josIS, URI sessionFileURI, boolean zip, ProgressMonitor progressMonitor) throws IOException, IllegalDataException {

        this.sessionFileURI = sessionFileURI;
        this.zip = zip;

        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setValidating(false);
            builderFactory.setNamespaceAware(true);
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document document = builder.parse(josIS);
            parseJos(document, progressMonitor);
        } catch (SAXException e) {
            throw new IllegalDataException(e);
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        }
    }

    private static Element getElementByTagName(Element root, String name) {
        NodeList els = root.getElementsByTagName(name);
        if (els.getLength() == 0) return null;
        return (Element) els.item(0);
    }
}
