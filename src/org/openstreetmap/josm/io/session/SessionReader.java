// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GraphicsEnvironment;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
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
import javax.xml.parsers.ParserConfigurationException;

import org.openstreetmap.josm.data.ViewportData;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Reads a .jos session file and loads the layers in the process.
 * @since 4668
 */
public class SessionReader {

    /**
     * Data class for projection saved in the session file.
     */
    public static class SessionProjectionChoiceData {
        private final String projectionChoiceId;
        private final Collection<String> subPreferences;

        /**
         * Construct a new SessionProjectionChoiceData.
         * @param projectionChoiceId projection choice id
         * @param subPreferences parameters for the projection choice
         */
        public SessionProjectionChoiceData(String projectionChoiceId, Collection<String> subPreferences) {
            this.projectionChoiceId = projectionChoiceId;
            this.subPreferences = subPreferences;
        }

        /**
         * Get the projection choice id.
         * @return the projection choice id
         */
        public String getProjectionChoiceId() {
            return projectionChoiceId;
        }

        /**
         * Get the parameters for the projection choice
         * @return parameters for the projection choice
         */
        public Collection<String> getSubPreferences() {
            return subPreferences;
        }
    }

    /**
     * Data class for viewport saved in the session file.
     */
    public static class SessionViewportData {
        private final LatLon center;
        private final double meterPerPixel;

        /**
         * Construct a new SessionViewportData.
         * @param center the lat/lon coordinates of the screen center
         * @param meterPerPixel scale in meters per pixel
         */
        public SessionViewportData(LatLon center, double meterPerPixel) {
            CheckParameterUtil.ensureParameterNotNull(center);
            this.center = center;
            this.meterPerPixel = meterPerPixel;
        }

        /**
         * Get the lat/lon coordinates of the screen center.
         * @return lat/lon coordinates of the screen center
         */
        public LatLon getCenter() {
            return center;
        }

        /**
         * Get the scale in meters per pixel.
         * @return scale in meters per pixel
         */
        public double getScale() {
            return meterPerPixel;
        }

        /**
         * Convert this viewport data to a {@link ViewportData} object (with projected coordinates).
         * @param proj the projection to convert from lat/lon to east/north
         * @return the corresponding ViewportData object
         */
        public ViewportData getEastNorthViewport(Projection proj) {
            EastNorth centerEN = proj.latlon2eastNorth(center);
            // Get a "typical" distance in east/north units that
            // corresponds to a couple of pixels. Shouldn't be too
            // large, to keep it within projection bounds and
            // not too small to avoid rounding errors.
            double dist = 0.01 * proj.getDefaultZoomInPPD();
            LatLon ll1 = proj.eastNorth2latlon(new EastNorth(centerEN.east() - dist, centerEN.north()));
            LatLon ll2 = proj.eastNorth2latlon(new EastNorth(centerEN.east() + dist, centerEN.north()));
            double meterPerEasting = ll1.greatCircleDistance(ll2) / dist / 2;
            double scale = meterPerPixel / meterPerEasting; // unit: easting per pixel
            return new ViewportData(centerEN, scale);
        }
    }

    private static final Map<String, Class<? extends SessionLayerImporter>> sessionLayerImporters = new HashMap<>();

    private URI sessionFileURI;
    private boolean zip; // true, if session file is a .joz file; false if it is a .jos file
    private ZipFile zipFile;
    private List<Layer> layers = new ArrayList<>();
    private int active = -1;
    private final List<Runnable> postLoadTasks = new ArrayList<>();
    private SessionViewportData viewport;
    private SessionProjectionChoiceData projectionChoice;

    static {
        registerSessionLayerImporter("osm-data", OsmDataSessionImporter.class);
        registerSessionLayerImporter("imagery", ImagerySessionImporter.class);
        registerSessionLayerImporter("tracks", GpxTracksSessionImporter.class);
        registerSessionLayerImporter("geoimage", GeoImageSessionImporter.class);
        registerSessionLayerImporter("markers", MarkerSessionImporter.class);
        registerSessionLayerImporter("osm-notes", NoteSessionImporter.class);
    }

    /**
     * Register a session layer importer.
     *
     * @param layerType layer type
     * @param importer importer for this layer class
     */
    public static void registerSessionLayerImporter(String layerType, Class<? extends SessionLayerImporter> importer) {
        sessionLayerImporters.put(layerType, importer);
    }

    /**
     * Returns the session layer importer for the given layer type.
     * @param layerType layer type to import
     * @return session layer importer for the given layer
     */
    public static SessionLayerImporter getSessionLayerImporter(String layerType) {
        Class<? extends SessionLayerImporter> importerClass = sessionLayerImporters.get(layerType);
        if (importerClass == null)
            return null;
        SessionLayerImporter importer = null;
        try {
            importer = importerClass.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new JosmRuntimeException(e);
        }
        return importer;
    }

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
     * @return the viewport; can be null when no viewport info is found in the file
     */
    public SessionViewportData getViewport() {
        return viewport;
    }

    /**
     * Return the projection choice data.
     * @return the projection; can be null when no projection info is found in the file
     */
    public SessionProjectionChoiceData getProjectionChoice() {
        return projectionChoice;
    }

    /**
     * A class that provides some context for the individual {@link SessionLayerImporter}
     * when doing the import.
     */
    public class ImportSupport {

        private final String layerName;
        private final int layerIndex;
        private final List<LayerDependency> layerDependencies;

        /**
         * Path of the file inside the zip archive.
         * Used as alternative return value for getFile method.
         */
        private String inZipPath;

        /**
         * Constructs a new {@code ImportSupport}.
         * @param layerName layer name
         * @param layerIndex layer index
         * @param layerDependencies layer dependencies
         */
        public ImportSupport(String layerName, int layerIndex, List<LayerDependency> layerDependencies) {
            this.layerName = layerName;
            this.layerIndex = layerIndex;
            this.layerDependencies = layerDependencies;
        }

        /**
         * Add a task, e.g. a message dialog, that should
         * be executed in EDT after all layers have been added.
         * @param task task to run in EDT
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
         * @param uriStr URI as string
         * @return the InputStream
         *
         * @throws IOException Thrown when no Stream can be opened for the given URI, e.g. when the linked file has been deleted.
         */
        public InputStream getInputStream(String uriStr) throws IOException {
            File file = getFile(uriStr);
            if (file != null) {
                try {
                    return new BufferedInputStream(Compression.getUncompressedFileInputStream(file));
                } catch (FileNotFoundException e) {
                    throw new IOException(tr("File ''{0}'' does not exist.", file.getPath()), e);
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
         * @param uriStr the URI as string
         * @return the resulting File
         * @throws IOException if any I/O error occurs
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
            } catch (URISyntaxException | IllegalArgumentException e) {
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
         * @return layer name
         */
        public String getLayerName() {
            return layerName;
        }

        /**
         * Index of the layer that is currently imported.
         * @return layer index
         */
        public int getLayerIndex() {
            return layerIndex;
        }

        /**
         * Dependencies - maps the layer index to the importer of the given
         * layer. All the dependent importers have loaded completely at this point.
         * @return layer dependencies
         */
        public List<LayerDependency> getLayerDependencies() {
            return layerDependencies;
        }

        @Override
        public String toString() {
            return "ImportSupport [layerName=" + layerName + ", layerIndex=" + layerIndex + ", layerDependencies="
                    + layerDependencies + ", inZipPath=" + inZipPath + ']';
        }
    }

    public static class LayerDependency {
        private final Integer index;
        private final Layer layer;
        private final SessionLayerImporter importer;

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
        if (!"josm-session".equals(root.getTagName())) {
            error(tr("Unexpected root element ''{0}'' in session file", root.getTagName()));
        }
        String version = root.getAttribute("version");
        if (!"0.1".equals(version)) {
            error(tr("Version ''{0}'' of session file is not supported. Expected: 0.1", version));
        }

        viewport = readViewportData(root);
        projectionChoice = readProjectionChoiceData(root);

        Element layersEl = getElementByTagName(root, "layers");
        if (layersEl == null) return;

        String activeAtt = layersEl.getAttribute("active");
        try {
            active = !activeAtt.isEmpty() ? (Integer.parseInt(activeAtt)-1) : -1;
        } catch (NumberFormatException e) {
            Logging.warn("Unsupported value for 'active' layer attribute. Ignoring it. Error was: "+e.getMessage());
            active = -1;
        }

        MultiMap<Integer, Integer> deps = new MultiMap<>();
        Map<Integer, Element> elems = new HashMap<>();

        NodeList nodes = layersEl.getChildNodes();

        for (int i = 0; i < nodes.getLength(); ++i) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) node;
                if ("layer".equals(e.getTagName())) {
                    if (!e.hasAttribute("index")) {
                        error(tr("missing mandatory attribute ''index'' for element ''layer''"));
                    }
                    Integer idx = null;
                    try {
                        idx = Integer.valueOf(e.getAttribute("index"));
                    } catch (NumberFormatException ex) {
                        Logging.warn(ex);
                    }
                    if (idx == null) {
                        error(tr("unexpected format of attribute ''index'' for element ''layer''"));
                    } else if (elems.containsKey(idx)) {
                        error(tr("attribute ''index'' ({0}) for element ''layer'' must be unique", Integer.toString(idx)));
                    }
                    elems.put(idx, e);

                    deps.putVoid(idx);
                    String depStr = e.getAttribute("depends");
                    if (!depStr.isEmpty()) {
                        for (String sd : depStr.split(",")) {
                            Integer d = null;
                            try {
                                d = Integer.valueOf(sd);
                            } catch (NumberFormatException ex) {
                                Logging.warn(ex);
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
        final Map<Integer, Layer> layersMap = new TreeMap<>(Collections.reverseOrder());
        final Map<Integer, SessionLayerImporter> importers = new HashMap<>();
        final Map<Integer, String> names = new HashMap<>();

        progressMonitor.setTicksCount(sorted.size());
        LAYER: for (int idx: sorted) {
            Element e = elems.get(idx);
            if (e == null) {
                error(tr("missing layer with index {0}", idx));
                return;
            } else if (!e.hasAttribute("name")) {
                error(tr("missing mandatory attribute ''name'' for element ''layer''"));
                return;
            }
            String name = e.getAttribute("name");
            names.put(idx, name);
            if (!e.hasAttribute("type")) {
                error(tr("missing mandatory attribute ''type'' for element ''layer''"));
                return;
            }
            String type = e.getAttribute("type");
            SessionLayerImporter imp = getSessionLayerImporter(type);
            if (imp == null && !GraphicsEnvironment.isHeadless()) {
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
            } else if (imp != null) {
                importers.put(idx, imp);
                List<LayerDependency> depsImp = new ArrayList<>();
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
                    if (layer == null) {
                        throw new IllegalStateException("Importer " + imp + " returned null for " + support);
                    }
                } catch (IllegalDataException | IllegalStateException | IOException ex) {
                    exception = ex;
                }
                if (exception != null) {
                    Logging.error(exception);
                    if (!GraphicsEnvironment.isHeadless()) {
                        CancelOrContinueDialog dialog = new CancelOrContinueDialog();
                        dialog.show(
                                tr("Error loading layer"),
                                tr("<html>Could not load layer {0} ''{1}''.<br>Error is:<br>{2}</html>", idx,
                                        Utils.escapeReservedCharactersHTML(name),
                                        Utils.escapeReservedCharactersHTML(exception.getMessage())),
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
                }

                layersMap.put(idx, layer);
            }
            progressMonitor.worked(1);
        }

        layers = new ArrayList<>();
        for (Entry<Integer, Layer> entry : layersMap.entrySet()) {
            Layer layer = entry.getValue();
            if (layer == null) {
                continue;
            }
            Element el = elems.get(entry.getKey());
            if (el.hasAttribute("visible")) {
                layer.setVisible(Boolean.parseBoolean(el.getAttribute("visible")));
            }
            if (el.hasAttribute("opacity")) {
                try {
                    double opacity = Double.parseDouble(el.getAttribute("opacity"));
                    layer.setOpacity(opacity);
                } catch (NumberFormatException ex) {
                    Logging.warn(ex);
                }
            }
            layer.setName(names.get(entry.getKey()));
            layers.add(layer);
        }
    }

    private static SessionViewportData readViewportData(Element root) {
        Element viewportEl = getElementByTagName(root, "viewport");
        if (viewportEl == null) return null;
        LatLon center = null;
        Element centerEl = getElementByTagName(viewportEl, "center");
        if (centerEl == null || !centerEl.hasAttribute("lat") || !centerEl.hasAttribute("lon")) return null;
        try {
            center = new LatLon(Double.parseDouble(centerEl.getAttribute("lat")),
                    Double.parseDouble(centerEl.getAttribute("lon")));
        } catch (NumberFormatException ex) {
            Logging.warn(ex);
        }
        if (center == null) return null;
        Element scaleEl = getElementByTagName(viewportEl, "scale");
        if (scaleEl == null || !scaleEl.hasAttribute("meter-per-pixel")) return null;
        try {
            double scale = Double.parseDouble(scaleEl.getAttribute("meter-per-pixel"));
            return new SessionViewportData(center, scale);
        } catch (NumberFormatException ex) {
            Logging.warn(ex);
            return null;
        }
    }

    private static SessionProjectionChoiceData readProjectionChoiceData(Element root) {
        Element projectionEl = getElementByTagName(root, "projection");
        if (projectionEl == null) return null;
        Element projectionChoiceEl = getElementByTagName(projectionEl, "projection-choice");
        if (projectionChoiceEl == null) return null;
        Element idEl = getElementByTagName(projectionChoiceEl, "id");
        if (idEl == null) return null;
        String id = idEl.getTextContent();
        Element parametersEl = getElementByTagName(projectionChoiceEl, "parameters");
        if (parametersEl == null) return null;
        Collection<String> parameters = new ArrayList<>();
        NodeList paramNl = parametersEl.getElementsByTagName("param");
        for (int i = 0; i < paramNl.getLength(); i++) {
            Element paramEl = (Element) paramNl.item(i);
            parameters.add(paramEl.getTextContent());
        }
        return new SessionProjectionChoiceData(id, parameters);
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
                SwingUtilities.invokeAndWait(() -> {
                    ExtendedDialog dlg = new ExtendedDialog(
                            MainApplication.getMainFrame(),
                            title,
                            tr("Cancel"), tr("Skip layer and continue"))
                        .setButtonIcons("cancel", "dialogs/next")
                        .setIcon(icon)
                        .setContent(message);
                    cancel = dlg.showDialog().getValue() != 2;
                });
            } catch (InvocationTargetException | InterruptedException ex) {
                throw new JosmRuntimeException(ex);
            }
        }

        public boolean isCancel() {
            return cancel;
        }
    }

    /**
     * Loads session from the given file.
     * @param sessionFile session file to load
     * @param zip {@code true} if it's a zipped session (.joz)
     * @param progressMonitor progress monitor
     * @throws IllegalDataException if invalid data is detected
     * @throws IOException if any I/O error occurs
     */
    public void loadSession(File sessionFile, boolean zip, ProgressMonitor progressMonitor) throws IllegalDataException, IOException {
        try (InputStream josIS = createInputStream(sessionFile, zip)) {
            loadSession(josIS, sessionFile.toURI(), zip, progressMonitor != null ? progressMonitor : NullProgressMonitor.INSTANCE);
        }
    }

    private InputStream createInputStream(File sessionFile, boolean zip) throws IOException, IllegalDataException {
        if (zip) {
            try {
                zipFile = new ZipFile(sessionFile, StandardCharsets.UTF_8);
                return getZipInputStream(zipFile);
            } catch (ZipException ex) {
                throw new IOException(ex);
            }
        } else {
            return Files.newInputStream(sessionFile.toPath());
        }
    }

    private static InputStream getZipInputStream(ZipFile zipFile) throws IOException, IllegalDataException {
        ZipEntry josEntry = null;
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (Utils.hasExtension(entry.getName(), "jos")) {
                josEntry = entry;
                break;
            }
        }
        if (josEntry == null) {
            error(tr("expected .jos file inside .joz archive"));
        }
        return zipFile.getInputStream(josEntry);
    }

    private void loadSession(InputStream josIS, URI sessionFileURI, boolean zip, ProgressMonitor progressMonitor)
            throws IOException, IllegalDataException {

        this.sessionFileURI = sessionFileURI;
        this.zip = zip;

        try {
            parseJos(XmlUtils.parseSafeDOM(josIS), progressMonitor);
        } catch (SAXException e) {
            throw new IllegalDataException(e);
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        }
    }

    private static Element getElementByTagName(Element root, String name) {
        NodeList els = root.getElementsByTagName(name);
        return els.getLength() > 0 ? (Element) els.item(0) : null;
    }
}
