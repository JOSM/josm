// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.actions.ToggleUploadDiscouragedLayerAction;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.conflict.ConflictCollection;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxExtensionCollection;
import org.openstreetmap.josm.data.gpx.GpxLink;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.GpxTrackSegment;
import org.openstreetmap.josm.data.gpx.IGpxTrackSegment;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.DataIntegrityProblemException;
import org.openstreetmap.josm.data.osm.DataSelectionListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSetMerger;
import org.openstreetmap.josm.data.osm.DatasetConsistencyTest;
import org.openstreetmap.josm.data.osm.DownloadPolicy;
import org.openstreetmap.josm.data.osm.HighlightUpdateListener;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveComparator;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.data.osm.UploadPolicy;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListenerAdapter;
import org.openstreetmap.josm.data.osm.event.DataSetListenerAdapter.Listener;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.osm.visitor.OsmPrimitiveVisitor;
import org.openstreetmap.josm.data.osm.visitor.paint.AbstractMapRenderer;
import org.openstreetmap.josm.data.osm.visitor.paint.MapRendererFactory;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.MultipolygonCache;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.data.preferences.NamedColorProperty;
import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapViewState.MapViewPoint;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.gui.datatransfer.data.OsmLayerTransferData;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.io.AbstractIOTask;
import org.openstreetmap.josm.gui.io.AbstractUploadDialog;
import org.openstreetmap.josm.gui.io.UploadDialog;
import org.openstreetmap.josm.gui.io.UploadLayerTask;
import org.openstreetmap.josm.gui.io.importexport.NoteExporter;
import org.openstreetmap.josm.gui.io.importexport.OsmImporter;
import org.openstreetmap.josm.gui.io.importexport.ValidatorErrorExporter;
import org.openstreetmap.josm.gui.io.importexport.WMSLayerImporter;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.gui.preferences.display.DrawingPreference;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.progress.swing.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.LruCache;
import org.openstreetmap.josm.gui.widgets.FileChooserManager;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.AlphanumComparator;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageOverlay;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.UncheckedParseException;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * A layer that holds OSM data from a specific dataset.
 * The data can be fully edited.
 *
 * @author imi
 * @since 17
 */
public class OsmDataLayer extends AbstractOsmDataLayer implements Listener, DataSelectionListener, HighlightUpdateListener {
    private static final int HATCHED_SIZE = 15;
    /** Property used to know if this layer has to be saved on disk */
    public static final String REQUIRES_SAVE_TO_DISK_PROP = OsmDataLayer.class.getName() + ".requiresSaveToDisk";
    /** Property used to know if this layer has to be uploaded */
    public static final String REQUIRES_UPLOAD_TO_SERVER_PROP = OsmDataLayer.class.getName() + ".requiresUploadToServer";

    private boolean requiresSaveToFile;
    private boolean requiresUploadToServer;
    /** Flag used to know if the layer is being uploaded */
    private final AtomicBoolean isUploadInProgress = new AtomicBoolean(false);

    /**
     * List of validation errors in this layer.
     * @since 3669
     */
    public final List<TestError> validationErrors = new ArrayList<>();

    /**
     * The default number of relations in the recent relations cache.
     * @see #getRecentRelations()
     */
    public static final int DEFAULT_RECENT_RELATIONS_NUMBER = 20;
    /**
     * The number of relations to use in the recent relations cache.
     * @see #getRecentRelations()
     */
    public static final IntegerProperty PROPERTY_RECENT_RELATIONS_NUMBER = new IntegerProperty("properties.last-closed-relations-size",
            DEFAULT_RECENT_RELATIONS_NUMBER);
    /**
     * The extension that should be used when saving the OSM file.
     */
    public static final StringProperty PROPERTY_SAVE_EXTENSION = new StringProperty("save.extension.osm", "osm");

    /**
     * Property to determine if labels must be hidden while dragging the map.
     */
    public static final BooleanProperty PROPERTY_HIDE_LABELS_WHILE_DRAGGING = new BooleanProperty("mappaint.hide.labels.while.dragging", true);

    private static final NamedColorProperty PROPERTY_BACKGROUND_COLOR = new NamedColorProperty(marktr("background"), Color.BLACK);
    private static final NamedColorProperty PROPERTY_OUTSIDE_COLOR = new NamedColorProperty(marktr("outside downloaded area"), Color.YELLOW);

    /** List of recent relations */
    private final Map<Relation, Void> recentRelations = new LruCache<>(PROPERTY_RECENT_RELATIONS_NUMBER.get());

    /**
     * Returns list of recently closed relations or null if none.
     * @return list of recently closed relations or <code>null</code> if none
     * @since 12291 (signature)
     * @since 9668
     */
    public List<Relation> getRecentRelations() {
        ArrayList<Relation> list = new ArrayList<>(recentRelations.keySet());
        Collections.reverse(list);
        return list;
    }

    /**
     * Adds recently closed relation.
     * @param relation new entry for the list of recently closed relations
     * @see #PROPERTY_RECENT_RELATIONS_NUMBER
     * @since 9668
     */
    public void setRecentRelation(Relation relation) {
        recentRelations.put(relation, null);
        MapFrame map = MainApplication.getMap();
        if (map != null && map.relationListDialog != null) {
            map.relationListDialog.enableRecentRelations();
        }
    }

    /**
     * Remove relation from list of recent relations.
     * @param relation relation to remove
     * @since 9668
     */
    public void removeRecentRelation(Relation relation) {
        recentRelations.remove(relation);
        MapFrame map = MainApplication.getMap();
        if (map != null && map.relationListDialog != null) {
            map.relationListDialog.enableRecentRelations();
        }
    }

    protected void setRequiresSaveToFile(boolean newValue) {
        boolean oldValue = requiresSaveToFile;
        requiresSaveToFile = newValue;
        if (oldValue != newValue) {
            GuiHelper.runInEDT(() ->
                propertyChangeSupport.firePropertyChange(REQUIRES_SAVE_TO_DISK_PROP, oldValue, newValue)
            );
        }
    }

    protected void setRequiresUploadToServer(boolean newValue) {
        boolean oldValue = requiresUploadToServer;
        requiresUploadToServer = newValue;
        if (oldValue != newValue) {
            GuiHelper.runInEDT(() ->
                propertyChangeSupport.firePropertyChange(REQUIRES_UPLOAD_TO_SERVER_PROP, oldValue, newValue)
            );
        }
    }

    /** the global counter for created data layers */
    private static final AtomicInteger dataLayerCounter = new AtomicInteger();

    /**
     * Replies a new unique name for a data layer
     *
     * @return a new unique name for a data layer
     */
    public static String createNewName() {
        return createLayerName(dataLayerCounter.incrementAndGet());
    }

    static String createLayerName(Object arg) {
        return tr("Data Layer {0}", arg);
    }

    /**
     * A listener that counts the number of primitives it encounters
     */
    public static final class DataCountVisitor implements OsmPrimitiveVisitor {
        /**
         * Nodes that have been visited
         */
        public int nodes;
        /**
         * Ways that have been visited
         */
        public int ways;
        /**
         * Relations that have been visited
         */
        public int relations;
        /**
         * Deleted nodes that have been visited
         */
        public int deletedNodes;
        /**
         * Deleted ways that have been visited
         */
        public int deletedWays;
        /**
         * Deleted relations that have been visited
         */
        public int deletedRelations;
        /**
         * Incomplete nodes that have been visited
         */
        public int incompleteNodes;
        /**
         * Incomplete ways that have been visited
         */
        public int incompleteWays;
        /**
         * Incomplete relations that have been visited
         */
        public int incompleteRelations;

        @Override
        public void visit(final Node n) {
            nodes++;
            if (n.isDeleted()) {
                deletedNodes++;
            }
            if (n.isIncomplete()) {
                incompleteNodes++;
            }
        }

        @Override
        public void visit(final Way w) {
            ways++;
            if (w.isDeleted()) {
                deletedWays++;
            }
            if (w.isIncomplete()) {
                incompleteWays++;
            }
        }

        @Override
        public void visit(final Relation r) {
            relations++;
            if (r.isDeleted()) {
                deletedRelations++;
            }
            if (r.isIncomplete()) {
                incompleteRelations++;
            }
        }
    }

    /**
     * Listener called when a state of this layer has changed.
     * @since 10600 (functional interface)
     */
    @FunctionalInterface
    public interface LayerStateChangeListener {
        /**
         * Notifies that the "upload discouraged" (upload=no) state has changed.
         * @param layer The layer that has been modified
         * @param newValue The new value of the state
         */
        void uploadDiscouragedChanged(OsmDataLayer layer, boolean newValue);
    }

    private final CopyOnWriteArrayList<LayerStateChangeListener> layerStateChangeListeners = new CopyOnWriteArrayList<>();

    /**
     * Adds a layer state change listener
     *
     * @param listener the listener. Ignored if null or already registered.
     * @since 5519
     */
    public void addLayerStateChangeListener(LayerStateChangeListener listener) {
        if (listener != null) {
            layerStateChangeListeners.addIfAbsent(listener);
        }
    }

    /**
     * Removes a layer state change listener
     *
     * @param listener the listener. Ignored if null or already registered.
     * @since 10340
     */
    public void removeLayerStateChangeListener(LayerStateChangeListener listener) {
        layerStateChangeListeners.remove(listener);
    }

    /**
     * The data behind this layer.
     */
    public final DataSet data;
    private DataSetListenerAdapter dataSetListenerAdapter;

    /**
     * a texture for non-downloaded area
     */
    private static volatile BufferedImage hatched;

    static {
        createHatchTexture();
    }

    /**
     * Replies background color for downloaded areas.
     * @return background color for downloaded areas. Black by default
     */
    public static Color getBackgroundColor() {
        return PROPERTY_BACKGROUND_COLOR.get();
    }

    /**
     * Replies background color for non-downloaded areas.
     * @return background color for non-downloaded areas. Yellow by default
     */
    public static Color getOutsideColor() {
        return PROPERTY_OUTSIDE_COLOR.get();
    }

    /**
     * Initialize the hatch pattern used to paint the non-downloaded area
     */
    public static void createHatchTexture() {
        BufferedImage bi = new BufferedImage(HATCHED_SIZE, HATCHED_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D big = bi.createGraphics();
        big.setColor(getBackgroundColor());
        Composite comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f);
        big.setComposite(comp);
        big.fillRect(0, 0, HATCHED_SIZE, HATCHED_SIZE);
        big.setColor(getOutsideColor());
        big.drawLine(-1, 6, 6, -1);
        big.drawLine(4, 16, 16, 4);
        hatched = bi;
    }

    /**
     * Construct a new {@code OsmDataLayer}.
     * @param data OSM data
     * @param name Layer name
     * @param associatedFile Associated .osm file (can be null)
     */
    public OsmDataLayer(final DataSet data, final String name, final File associatedFile) {
        super(name);
        CheckParameterUtil.ensureParameterNotNull(data, "data");
        this.data = data;
        this.data.setName(name);
        this.dataSetListenerAdapter = new DataSetListenerAdapter(this);
        this.setAssociatedFile(associatedFile);
        data.addDataSetListener(dataSetListenerAdapter);
        data.addDataSetListener(MultipolygonCache.getInstance());
        data.addHighlightUpdateListener(this);
        data.addSelectionListener(this);
        if (name != null && name.startsWith(createLayerName("")) && Character.isDigit(
                (name.substring(createLayerName("").length()) + "XX" /*avoid StringIndexOutOfBoundsException*/).charAt(1))) {
            while (AlphanumComparator.getInstance().compare(createLayerName(dataLayerCounter), name) < 0) {
                final int i = dataLayerCounter.incrementAndGet();
                if (i > 1_000_000) {
                    break; // to avoid looping in unforeseen case
                }
            }
        }
    }

    /**
     * Returns the {@link DataSet} behind this layer.
     * @return the {@link DataSet} behind this layer.
     * @since 13558
     */
    @Override
    public DataSet getDataSet() {
        return data;
    }

    /**
     * Return the image provider to get the base icon
     * @return image provider class which can be modified
     * @since 8323
     */
    protected ImageProvider getBaseIconProvider() {
        return new ImageProvider("layer", "osmdata_small");
    }

    @Override
    public Icon getIcon() {
        ImageProvider base = getBaseIconProvider().setMaxSize(ImageSizes.LAYER);
        if (data.getDownloadPolicy() != null && data.getDownloadPolicy() != DownloadPolicy.NORMAL) {
            base.addOverlay(new ImageOverlay(new ImageProvider("warning-small"), 0.5, 0.0, 1.0, 0.5));
        }
        if (data.getUploadPolicy() != null && data.getUploadPolicy() != UploadPolicy.NORMAL) {
            base.addOverlay(new ImageOverlay(new ImageProvider("warning-small"), 0.5, 0.5, 1.0, 1.0));
        }

        if (isUploadInProgress()) {
            // If the layer is being uploaded then change the default icon to a clock
            base = new ImageProvider("clock").setMaxSize(ImageSizes.LAYER);
        } else if (isLocked()) {
            // If the layer is read only then change the default icon to a lock
            base = new ImageProvider("lock").setMaxSize(ImageSizes.LAYER);
        }
        return base.get();
    }

    /**
     * Draw all primitives in this layer but do not draw modified ones (they
     * are drawn by the edit layer).
     * Draw nodes last to overlap the ways they belong to.
     */
    @Override public void paint(final Graphics2D g, final MapView mv, Bounds box) {
        boolean active = mv.getLayerManager().getActiveLayer() == this;
        boolean inactive = !active && Config.getPref().getBoolean("draw.data.inactive_color", true);
        boolean virtual = !inactive && mv.isVirtualNodesEnabled();

        // draw the hatched area for non-downloaded region. only draw if we're the active
        // and bounds are defined; don't draw for inactive layers or loaded GPX files etc
        if (active && DrawingPreference.SOURCE_BOUNDS_PROP.get() && !data.getDataSources().isEmpty()) {
            // initialize area with current viewport
            Rectangle b = mv.getBounds();
            // on some platforms viewport bounds seem to be offset from the left,
            // over-grow it just to be sure
            b.grow(100, 100);
            Path2D p = new Path2D.Double();

            // combine successively downloaded areas
            for (Bounds bounds : data.getDataSourceBounds()) {
                if (bounds.isCollapsed()) {
                    continue;
                }
                p.append(mv.getState().getArea(bounds), false);
            }
            // subtract combined areas
            Area a = new Area(b);
            a.subtract(new Area(p));

            // paint remainder
            MapViewPoint anchor = mv.getState().getPointFor(new EastNorth(0, 0));
            Rectangle2D anchorRect = new Rectangle2D.Double(anchor.getInView().getX() % HATCHED_SIZE,
                    anchor.getInView().getY() % HATCHED_SIZE, HATCHED_SIZE, HATCHED_SIZE);
            if (hatched != null) {
                g.setPaint(new TexturePaint(hatched, anchorRect));
            }
            try {
                g.fill(a);
            } catch (ArrayIndexOutOfBoundsException e) {
                // #16686 - AIOOBE in java.awt.TexturePaintContext$Int.setRaster
                Logging.error(e);
            }
        }

        AbstractMapRenderer painter = MapRendererFactory.getInstance().createActiveRenderer(g, mv, inactive);
        painter.enableSlowOperations(mv.getMapMover() == null || !mv.getMapMover().movementInProgress()
                || !PROPERTY_HIDE_LABELS_WHILE_DRAGGING.get());
        painter.render(data, virtual, box);
        MainApplication.getMap().conflictDialog.paintConflicts(g, mv);
    }

    @Override public String getToolTipText() {
        DataCountVisitor counter = new DataCountVisitor();
        for (final OsmPrimitive osm : data.allPrimitives()) {
            osm.accept(counter);
        }
        int nodes = counter.nodes - counter.deletedNodes;
        int ways = counter.ways - counter.deletedWays;
        int rels = counter.relations - counter.deletedRelations;

        StringBuilder tooltip = new StringBuilder("<html>")
                .append(trn("{0} node", "{0} nodes", nodes, nodes))
                .append("<br>")
                .append(trn("{0} way", "{0} ways", ways, ways))
                .append("<br>")
                .append(trn("{0} relation", "{0} relations", rels, rels));

        File f = getAssociatedFile();
        if (f != null) {
            tooltip.append("<br>").append(f.getPath());
        }
        tooltip.append("</html>");
        return tooltip.toString();
    }

    @Override public void mergeFrom(final Layer from) {
        final PleaseWaitProgressMonitor monitor = new PleaseWaitProgressMonitor(tr("Merging layers"));
        monitor.setCancelable(false);
        if (from instanceof OsmDataLayer && ((OsmDataLayer) from).isUploadDiscouraged()) {
            setUploadDiscouraged(true);
        }
        mergeFrom(((OsmDataLayer) from).data, monitor);
        monitor.close();
    }

    /**
     * merges the primitives in dataset <code>from</code> into the dataset of
     * this layer
     *
     * @param from  the source data set
     */
    public void mergeFrom(final DataSet from) {
        mergeFrom(from, null);
    }

    /**
     * merges the primitives in dataset <code>from</code> into the dataset of this layer
     *
     * @param from  the source data set
     * @param progressMonitor the progress monitor, can be {@code null}
     */
    public void mergeFrom(final DataSet from, ProgressMonitor progressMonitor) {
        final DataSetMerger visitor = new DataSetMerger(data, from);
        try {
            visitor.merge(progressMonitor);
        } catch (DataIntegrityProblemException e) {
            Logging.error(e);
            JOptionPane.showMessageDialog(
                    MainApplication.getMainFrame(),
                    e.getHtmlMessage() != null ? e.getHtmlMessage() : e.getMessage(),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        int numNewConflicts = 0;
        for (Conflict<?> c : visitor.getConflicts()) {
            if (!data.getConflicts().hasConflict(c)) {
                numNewConflicts++;
                data.getConflicts().add(c);
            }
        }
        // repaint to make sure new data is displayed properly.
        invalidate();
        // warn about new conflicts
        MapFrame map = MainApplication.getMap();
        if (numNewConflicts > 0 && map != null && map.conflictDialog != null) {
            map.conflictDialog.warnNumNewConflicts(numNewConflicts);
        }
    }

    @Override
    public boolean isMergable(final Layer other) {
        // allow merging between normal layers and discouraged layers with a warning (see #7684)
        return other instanceof OsmDataLayer;
    }

    @Override
    public void visitBoundingBox(final BoundingXYVisitor v) {
        for (final Node n: data.getNodes()) {
            if (n.isUsable()) {
                v.visit(n);
            }
        }
    }

    /**
     * Clean out the data behind the layer. This means clearing the redo/undo lists,
     * really deleting all deleted objects and reset the modified flags. This should
     * be done after an upload, even after a partial upload.
     *
     * @param processed A list of all objects that were actually uploaded.
     *         May be <code>null</code>, which means nothing has been uploaded
     */
    public void cleanupAfterUpload(final Collection<? extends IPrimitive> processed) {
        // return immediately if an upload attempt failed
        if (processed == null || processed.isEmpty())
            return;

        UndoRedoHandler.getInstance().clean(data);

        // if uploaded, clean the modified flags as well
        data.cleanupDeletedPrimitives();
        data.beginUpdate();
        try {
            for (OsmPrimitive p: data.allPrimitives()) {
                if (processed.contains(p)) {
                    p.setModified(false);
                }
            }
        } finally {
            data.endUpdate();
        }
    }

    private static String counterText(String text, int deleted, int incomplete) {
        StringBuilder sb = new StringBuilder(text);
        if (deleted > 0 || incomplete > 0) {
            sb.append(" (");
            if (deleted > 0) {
                sb.append(trn("{0} deleted", "{0} deleted", deleted, deleted));
            }
            if (deleted > 0 && incomplete > 0) {
                sb.append(", ");
            }
            if (incomplete > 0) {
                sb.append(trn("{0} incomplete", "{0} incomplete", incomplete, incomplete));
            }
            sb.append(')');
        }
        return sb.toString();
    }

    @Override
    public Object getInfoComponent() {
        final DataCountVisitor counter = new DataCountVisitor();
        for (final OsmPrimitive osm : data.allPrimitives()) {
            osm.accept(counter);
        }
        final JPanel p = new JPanel(new GridBagLayout());

        String nodeText = counterText(trn("{0} node", "{0} nodes", counter.nodes, counter.nodes),
                counter.deletedNodes, counter.incompleteNodes);
        String wayText = counterText(trn("{0} way", "{0} ways", counter.ways, counter.ways),
                counter.deletedWays, counter.incompleteWays);
        String relationText = counterText(trn("{0} relation", "{0} relations", counter.relations, counter.relations),
                counter.deletedRelations, counter.incompleteRelations);

        p.add(new JLabel(tr("{0} consists of:", getName())), GBC.eol());
        p.add(new JLabel(nodeText, ImageProvider.get("data", "node"), JLabel.HORIZONTAL), GBC.eop().insets(15, 0, 0, 0));
        p.add(new JLabel(wayText, ImageProvider.get("data", "way"), JLabel.HORIZONTAL), GBC.eop().insets(15, 0, 0, 0));
        p.add(new JLabel(relationText, ImageProvider.get("data", "relation"), JLabel.HORIZONTAL), GBC.eop().insets(15, 0, 0, 0));
        p.add(new JLabel(tr("API version: {0}", (data.getVersion() != null) ? data.getVersion() : tr("unset"))),
                GBC.eop().insets(15, 0, 0, 0));
        addConditionalInformation(p, tr("Layer is locked"), isLocked());
        addConditionalInformation(p, tr("Download is blocked"), data.getDownloadPolicy() == DownloadPolicy.BLOCKED);
        addConditionalInformation(p, tr("Upload is discouraged"), isUploadDiscouraged());
        addConditionalInformation(p, tr("Upload is blocked"), data.getUploadPolicy() == UploadPolicy.BLOCKED);

        return p;
    }

    private static void addConditionalInformation(JPanel p, String text, boolean condition) {
        if (condition) {
            p.add(new JLabel(text), GBC.eop().insets(15, 0, 0, 0));
        }
    }

    @Override
    public Action[] getMenuEntries() {
        List<Action> actions = new ArrayList<>();
        actions.addAll(Arrays.asList(
                LayerListDialog.getInstance().createActivateLayerAction(this),
                LayerListDialog.getInstance().createShowHideLayerAction(),
                LayerListDialog.getInstance().createDeleteLayerAction(),
                SeparatorLayerAction.INSTANCE,
                LayerListDialog.getInstance().createMergeLayerAction(this),
                LayerListDialog.getInstance().createDuplicateLayerAction(this),
                new LayerSaveAction(this),
                new LayerSaveAsAction(this)));
        if (ExpertToggleAction.isExpert()) {
            actions.addAll(Arrays.asList(
                    new LayerGpxExportAction(this),
                    new ConvertToGpxLayerAction()));
        }
        actions.addAll(Arrays.asList(
                SeparatorLayerAction.INSTANCE,
                new RenameLayerAction(getAssociatedFile(), this)));
        if (ExpertToggleAction.isExpert()) {
            actions.add(new ToggleUploadDiscouragedLayerAction(this));
        }
        actions.addAll(Arrays.asList(
                new ConsistencyTestAction(),
                SeparatorLayerAction.INSTANCE,
                new LayerListPopup.InfoAction(this)));
        return actions.toArray(new Action[0]);
    }

    /**
     * Converts given OSM dataset to GPX data.
     * @param data OSM dataset
     * @param file output .gpx file
     * @return GPX data
     */
    public static GpxData toGpxData(DataSet data, File file) {
        GpxData gpxData = new GpxData();
        if (data.getGPXNamespaces() != null) {
            gpxData.getNamespaces().addAll(data.getGPXNamespaces());
        }
        gpxData.storageFile = file;
        Set<Node> doneNodes = new HashSet<>();
        waysToGpxData(data.getWays(), gpxData, doneNodes);
        nodesToGpxData(data.getNodes(), gpxData, doneNodes);
        return gpxData;
    }

    private static void waysToGpxData(Collection<Way> ways, GpxData gpxData, Set<Node> doneNodes) {
        /* When the dataset has been obtained from a gpx layer and now is being converted back,
         * the ways have negative ids. The first created way corresponds to the first gpx segment,
         * and has the highest id (i.e., closest to zero).
         * Thus, sorting by OsmPrimitive#getUniqueId gives the original order.
         * (Only works if the data layer has not been saved to and been loaded from an osm file before.)
         */
        ways.stream()
                .sorted(OsmPrimitiveComparator.comparingUniqueId().reversed())
                .forEachOrdered(w -> {
            if (!w.isUsable()) {
                return;
            }
            List<IGpxTrackSegment> trk = new ArrayList<>();
            Map<String, Object> trkAttr = new HashMap<>();

            GpxExtensionCollection trkExts = new GpxExtensionCollection();
            GpxExtensionCollection segExts = new GpxExtensionCollection();
            for (Entry<String, String> e : w.getKeys().entrySet()) {
                String k = e.getKey().startsWith(GpxConstants.GPX_PREFIX) ? e.getKey().substring(GpxConstants.GPX_PREFIX.length()) : e.getKey();
                String v = e.getValue();
                if (GpxConstants.RTE_TRK_KEYS.contains(k)) {
                    trkAttr.put(k, v);
                } else {
                    k = GpxConstants.EXTENSION_ABBREVIATIONS.entrySet()
                            .stream()
                            .filter(s -> s.getValue().equals(e.getKey()))
                            .map(s -> s.getKey().substring(GpxConstants.GPX_PREFIX.length()))
                            .findAny()
                            .orElse(k);
                    if (k.startsWith("extension")) {
                        String[] chain = k.split(":");
                        if (chain.length >= 3 && "segment".equals(chain[2])) {
                            segExts.addFlat(chain, v);
                        } else {
                            trkExts.addFlat(chain, v);
                        }
                    }

                }
            }
            List<WayPoint> trkseg = new ArrayList<>();
            for (Node n : w.getNodes()) {
                if (!n.isUsable()) {
                    if (!trkseg.isEmpty()) {
                        trk.add(new GpxTrackSegment(trkseg));
                        trkseg.clear();
                    }
                    continue;
                }
                if (!n.isTagged() || containsOnlyGpxTags(n)) {
                    doneNodes.add(n);
                }
                trkseg.add(nodeToWayPoint(n));
            }
            trk.add(new GpxTrackSegment(trkseg));
            trk.forEach(gpxseg -> gpxseg.getExtensions().addAll(segExts));
            GpxTrack gpxtrk = new GpxTrack(trk, trkAttr);
            gpxtrk.getExtensions().addAll(trkExts);
            gpxData.addTrack(gpxtrk);
        });
    }

    private static boolean containsOnlyGpxTags(Tagged t) {
        for (String key : t.getKeys().keySet()) {
            if (!GpxConstants.WPT_KEYS.contains(key) && !key.startsWith(GpxConstants.GPX_PREFIX)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Reads the Gpx key from the given {@link OsmPrimitive}, with or without &quot;gpx:&quot; prefix
     * @param prim OSM primitive
     * @param key GPX key without prefix
     * @return the value or <code>null</code> if not present
     * @since 15419
     */
    public static String gpxVal(OsmPrimitive prim, String key) {
        return Optional.ofNullable(prim.get(GpxConstants.GPX_PREFIX + key)).orElse(prim.get(key));
    }

    /**
     * @param n the {@code Node} to convert
     * @return {@code WayPoint} object
     * @since 13210
     */
    public static WayPoint nodeToWayPoint(Node n) {
        return nodeToWayPoint(n, Long.MIN_VALUE);
    }

    /**
     * @param n the {@code Node} to convert
     * @param time a timestamp value in milliseconds from the epoch.
     * @return {@code WayPoint} object
     * @since 13210
     */
    public static WayPoint nodeToWayPoint(Node n, long time) {
        WayPoint wpt = new WayPoint(n.getCoor());

        // Position info

        addDoubleIfPresent(wpt, n, GpxConstants.PT_ELE);

        try {
            String v;
            if (time > Long.MIN_VALUE) {
                wpt.setTimeInMillis(time);
            } else if ((v = gpxVal(n, GpxConstants.PT_TIME)) != null) {
                wpt.setTimeInMillis(DateUtils.tsFromString(v));
            } else if (!n.isTimestampEmpty()) {
                wpt.setTime(Integer.toUnsignedLong(n.getRawTimestamp()));
            }
        } catch (UncheckedParseException e) {
            Logging.error(e);
        }

        addDoubleIfPresent(wpt, n, GpxConstants.PT_MAGVAR);
        addDoubleIfPresent(wpt, n, GpxConstants.PT_GEOIDHEIGHT);

        // Description info

        addStringIfPresent(wpt, n, GpxConstants.GPX_NAME);
        addStringIfPresent(wpt, n, GpxConstants.GPX_DESC, "description");
        addStringIfPresent(wpt, n, GpxConstants.GPX_CMT, "comment");
        addStringIfPresent(wpt, n, GpxConstants.GPX_SRC, "source", "source:position");

        Collection<GpxLink> links = new ArrayList<>();
        for (String key : new String[]{"link", "url", "website", "contact:website"}) {
            String value = gpxVal(n, key);
            if (value != null) {
                links.add(new GpxLink(value));
            }
        }
        wpt.put(GpxConstants.META_LINKS, links);

        addStringIfPresent(wpt, n, GpxConstants.PT_SYM, "wpt_symbol");
        addStringIfPresent(wpt, n, GpxConstants.PT_TYPE);

        // Accuracy info
        addStringIfPresent(wpt, n, GpxConstants.PT_FIX, "gps:fix");
        addIntegerIfPresent(wpt, n, GpxConstants.PT_SAT, "gps:sat");
        addDoubleIfPresent(wpt, n, GpxConstants.PT_HDOP, "gps:hdop");
        addDoubleIfPresent(wpt, n, GpxConstants.PT_VDOP, "gps:vdop");
        addDoubleIfPresent(wpt, n, GpxConstants.PT_PDOP, "gps:pdop");
        addDoubleIfPresent(wpt, n, GpxConstants.PT_AGEOFDGPSDATA, "gps:ageofdgpsdata");
        addIntegerIfPresent(wpt, n, GpxConstants.PT_DGPSID, "gps:dgpsid");

        return wpt;
    }

    private static void nodesToGpxData(Collection<Node> nodes, GpxData gpxData, Set<Node> doneNodes) {
        List<Node> sortedNodes = new ArrayList<>(nodes);
        sortedNodes.removeAll(doneNodes);
        Collections.sort(sortedNodes);
        for (Node n : sortedNodes) {
            if (n.isIncomplete() || n.isDeleted()) {
                continue;
            }
            gpxData.waypoints.add(nodeToWayPoint(n));
        }
    }

    private static void addIntegerIfPresent(WayPoint wpt, OsmPrimitive p, String gpxKey, String... osmKeys) {
        List<String> possibleKeys = new ArrayList<>(Arrays.asList(osmKeys));
        possibleKeys.add(0, gpxKey);
        for (String key : possibleKeys) {
            String value = gpxVal(p, key);
            if (value != null) {
                try {
                    int i = Integer.parseInt(value);
                    // Sanity checks
                    if ((!GpxConstants.PT_SAT.equals(gpxKey) || i >= 0) &&
                        (!GpxConstants.PT_DGPSID.equals(gpxKey) || (0 <= i && i <= 1023))) {
                        wpt.put(gpxKey, value);
                        break;
                    }
                } catch (NumberFormatException e) {
                    Logging.trace(e);
                }
            }
        }
    }

    private static void addDoubleIfPresent(WayPoint wpt, OsmPrimitive p, String gpxKey, String... osmKeys) {
        List<String> possibleKeys = new ArrayList<>(Arrays.asList(osmKeys));
        possibleKeys.add(0, gpxKey);
        for (String key : possibleKeys) {
            String value = gpxVal(p, key);
            if (value != null) {
                try {
                    double d = Double.parseDouble(value);
                    // Sanity checks
                    if (!GpxConstants.PT_MAGVAR.equals(gpxKey) || (0.0 <= d && d < 360.0)) {
                        wpt.put(gpxKey, value);
                        break;
                    }
                } catch (NumberFormatException e) {
                    Logging.trace(e);
                }
            }
        }
    }

    private static void addStringIfPresent(WayPoint wpt, OsmPrimitive p, String gpxKey, String... osmKeys) {
        List<String> possibleKeys = new ArrayList<>(Arrays.asList(osmKeys));
        possibleKeys.add(0, gpxKey);
        for (String key : possibleKeys) {
            String value = gpxVal(p, key);
            // Sanity checks
            if (value != null && (!GpxConstants.PT_FIX.equals(gpxKey) || GpxConstants.FIX_VALUES.contains(value))) {
                wpt.put(gpxKey, value);
                break;
            }
        }
    }

    /**
     * Converts OSM data behind this layer to GPX data.
     * @return GPX data
     */
    public GpxData toGpxData() {
        return toGpxData(data, getAssociatedFile());
    }

    /**
     * Action that converts this OSM layer to a GPX layer.
     */
    public class ConvertToGpxLayerAction extends AbstractAction {
        /**
         * Constructs a new {@code ConvertToGpxLayerAction}.
         */
        public ConvertToGpxLayerAction() {
            super(tr("Convert to GPX layer"));
            new ImageProvider("converttogpx").getResource().attachImageIcon(this, true);
            putValue("help", ht("/Action/ConvertToGpxLayer"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final GpxData gpxData = toGpxData();
            final GpxLayer gpxLayer = new GpxLayer(gpxData, tr("Converted from: {0}", getName()));
            if (getAssociatedFile() != null) {
                String filename = getAssociatedFile().getName().replaceAll(Pattern.quote(".gpx.osm") + '$', "") + ".gpx";
                gpxLayer.setAssociatedFile(new File(getAssociatedFile().getParentFile(), filename));
            }
            MainApplication.getLayerManager().addLayer(gpxLayer, false);
            if (Config.getPref().getBoolean("marker.makeautomarkers", true) && !gpxData.waypoints.isEmpty()) {
                MainApplication.getLayerManager().addLayer(
                        new MarkerLayer(gpxData, tr("Converted from: {0}", getName()), null, gpxLayer), false);
            }
            MainApplication.getLayerManager().removeLayer(OsmDataLayer.this);
        }
    }

    /**
     * Determines if this layer contains data at the given coordinate.
     * @param coor the coordinate
     * @return {@code true} if data sources bounding boxes contain {@code coor}
     */
    public boolean containsPoint(LatLon coor) {
        // we'll assume that if this has no data sources
        // that it also has no borders
        if (this.data.getDataSources().isEmpty())
            return true;

        boolean layerBoundsPoint = false;
        for (DataSource src : this.data.getDataSources()) {
            if (src.bounds.contains(coor)) {
                layerBoundsPoint = true;
                break;
            }
        }
        return layerBoundsPoint;
    }

    /**
     * Replies the set of conflicts currently managed in this layer.
     *
     * @return the set of conflicts currently managed in this layer
     */
    public ConflictCollection getConflicts() {
        return data.getConflicts();
    }

    @Override
    public boolean isDownloadable() {
        return data.getDownloadPolicy() != DownloadPolicy.BLOCKED && !isLocked();
    }

    @Override
    public boolean isUploadable() {
        return data.getUploadPolicy() != UploadPolicy.BLOCKED && !isLocked();
    }

    @Override
    public boolean requiresUploadToServer() {
        return isUploadable() && requiresUploadToServer;
    }

    @Override
    public boolean requiresSaveToFile() {
        return getAssociatedFile() != null && requiresSaveToFile;
    }

    @Override
    public void onPostLoadFromFile() {
        setRequiresSaveToFile(false);
        setRequiresUploadToServer(isModified());
        invalidate();
    }

    /**
     * Actions run after data has been downloaded to this layer.
     */
    public void onPostDownloadFromServer() {
        setRequiresSaveToFile(true);
        setRequiresUploadToServer(isModified());
        invalidate();
    }

    @Override
    public void onPostSaveToFile() {
        setRequiresSaveToFile(false);
        setRequiresUploadToServer(isModified());
    }

    @Override
    public void onPostUploadToServer() {
        setRequiresUploadToServer(isModified());
        // keep requiresSaveToDisk unchanged
    }

    private class ConsistencyTestAction extends AbstractAction {

        ConsistencyTestAction() {
            super(tr("Dataset consistency test"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String result = DatasetConsistencyTest.runTests(data);
            if (result.isEmpty()) {
                JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("No problems found"));
            } else {
                JPanel p = new JPanel(new GridBagLayout());
                p.add(new JLabel(tr("Following problems found:")), GBC.eol());
                JosmTextArea info = new JosmTextArea(result, 20, 60);
                info.setCaretPosition(0);
                info.setEditable(false);
                p.add(new JScrollPane(info), GBC.eop());

                JOptionPane.showMessageDialog(MainApplication.getMainFrame(), p, tr("Warning"), JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    @Override
    public synchronized void destroy() {
        super.destroy();
        data.removeSelectionListener(this);
        data.removeHighlightUpdateListener(this);
        data.removeDataSetListener(dataSetListenerAdapter);
        data.removeDataSetListener(MultipolygonCache.getInstance());
        removeClipboardDataFor(this);
        recentRelations.clear();
    }

    protected static void removeClipboardDataFor(OsmDataLayer osm) {
        Transferable clipboardContents = ClipboardUtils.getClipboardContent();
        if (clipboardContents != null && clipboardContents.isDataFlavorSupported(OsmLayerTransferData.OSM_FLAVOR)) {
            try {
                Object o = clipboardContents.getTransferData(OsmLayerTransferData.OSM_FLAVOR);
                if (o instanceof OsmLayerTransferData && osm.equals(((OsmLayerTransferData) o).getLayer())) {
                    ClipboardUtils.clear();
                }
            } catch (UnsupportedFlavorException | IOException e) {
                Logging.error(e);
            }
        }
    }

    @Override
    public void processDatasetEvent(AbstractDatasetChangedEvent event) {
        invalidate();
        setRequiresSaveToFile(true);
        setRequiresUploadToServer(event.getDataset().requiresUploadToServer());
    }

    @Override
    public void selectionChanged(SelectionChangeEvent event) {
        invalidate();
    }

    @Override
    public void projectionChanged(Projection oldValue, Projection newValue) {
         // No reprojection required. The dataset itself is registered as projection
         // change listener and already got notified.
    }

    @Override
    public final boolean isUploadDiscouraged() {
        return data.getUploadPolicy() == UploadPolicy.DISCOURAGED;
    }

    /**
     * Sets the "discouraged upload" flag.
     * @param uploadDiscouraged {@code true} if upload of data managed by this layer is discouraged.
     * This feature allows to use "private" data layers.
     */
    public final void setUploadDiscouraged(boolean uploadDiscouraged) {
        if (data.getUploadPolicy() != UploadPolicy.BLOCKED &&
                (uploadDiscouraged ^ isUploadDiscouraged())) {
            data.setUploadPolicy(uploadDiscouraged ? UploadPolicy.DISCOURAGED : UploadPolicy.NORMAL);
            for (LayerStateChangeListener l : layerStateChangeListeners) {
                l.uploadDiscouragedChanged(this, uploadDiscouraged);
            }
        }
    }

    @Override
    public final boolean isModified() {
        return data.isModified();
    }

    @Override
    public boolean isSavable() {
        return true; // With OsmExporter
    }

    @Override
    public boolean checkSaveConditions() {
        if (isDataSetEmpty() && 1 != GuiHelper.runInEDTAndWaitAndReturn(() ->
            new ExtendedDialog(
                    MainApplication.getMainFrame(),
                    tr("Empty document"),
                    tr("Save anyway"), tr("Cancel"))
                .setContent(tr("The document contains no data."))
                .setButtonIcons("save", "cancel")
                .showDialog().getValue()
        )) {
            return false;
        }

        ConflictCollection conflictsCol = getConflicts();
        return conflictsCol == null || conflictsCol.isEmpty() || 1 == GuiHelper.runInEDTAndWaitAndReturn(() ->
            new ExtendedDialog(
                    MainApplication.getMainFrame(),
                    /* I18N: Display title of the window showing conflicts */
                    tr("Conflicts"),
                    tr("Reject Conflicts and Save"), tr("Cancel"))
                .setContent(
                    tr("There are unresolved conflicts. Conflicts will not be saved and handled as if you rejected all. Continue?"))
                .setButtonIcons("save", "cancel")
                .showDialog().getValue()
        );
    }

    /**
     * Check the data set if it would be empty on save. It is empty, if it contains
     * no objects (after all objects that are created and deleted without being
     * transferred to the server have been removed).
     *
     * @return <code>true</code>, if a save result in an empty data set.
     */
    private boolean isDataSetEmpty() {
        if (data != null) {
            for (OsmPrimitive osm : data.allNonDeletedPrimitives()) {
                if (!osm.isDeleted() || !osm.isNewOrUndeleted())
                    return false;
            }
        }
        return true;
    }

    @Override
    public File createAndOpenSaveFileChooser() {
        String extension = PROPERTY_SAVE_EXTENSION.get();
        File file = getAssociatedFile();
        if (file == null && isRenamed()) {
            StringBuilder filename = new StringBuilder(Config.getPref().get("lastDirectory")).append('/').append(getName());
            if (!OsmImporter.FILE_FILTER.acceptName(filename.toString())) {
                filename.append('.').append(extension);
            }
            file = new File(filename.toString());
        }
        return new FileChooserManager()
            .title(tr("Save OSM file"))
            .extension(extension)
            .file(file)
            .additionalTypes(t -> t != WMSLayerImporter.FILE_FILTER && t != NoteExporter.FILE_FILTER && t != ValidatorErrorExporter.FILE_FILTER)
            .getFileForSave();
    }

    @Override
    public AbstractIOTask createUploadTask(final ProgressMonitor monitor) {
        UploadDialog dialog = UploadDialog.getUploadDialog();
        return new UploadLayerTask(
                dialog.getUploadStrategySpecification(),
                this,
                monitor,
                dialog.getChangeset());
    }

    @Override
    public AbstractUploadDialog getUploadDialog() {
        UploadDialog dialog = UploadDialog.getUploadDialog();
        dialog.setUploadedPrimitives(new APIDataSet(data));
        return dialog;
    }

    @Override
    public ProjectionBounds getViewProjectionBounds() {
        BoundingXYVisitor v = new BoundingXYVisitor();
        v.visit(data.getDataSourceBoundingBox());
        if (!v.hasExtend()) {
            v.computeBoundingBox(data.getNodes());
        }
        return v.getBounds();
    }

    @Override
    public void highlightUpdated(HighlightUpdateEvent e) {
        invalidate();
    }

    @Override
    public void setName(String name) {
        if (data != null) {
            data.setName(name);
        }
        super.setName(name);
    }

    /**
     * Sets the "upload in progress" flag, which will result in displaying a new icon and forbid to remove the layer.
     * @since 13434
     */
    public void setUploadInProgress() {
        if (!isUploadInProgress.compareAndSet(false, true)) {
            Logging.warn("Trying to set uploadInProgress flag on layer already being uploaded ", getName());
        }
    }

    /**
     * Unsets the "upload in progress" flag, which will result in displaying the standard icon and allow to remove the layer.
     * @since 13434
     */
    public void unsetUploadInProgress() {
        if (!isUploadInProgress.compareAndSet(true, false)) {
            Logging.warn("Trying to unset uploadInProgress flag on layer not being uploaded ", getName());
        }
    }

    @Override
    public boolean isUploadInProgress() {
        return isUploadInProgress.get();
    }
}
