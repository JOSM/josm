// License: GPL. See LICENSE file for details.

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
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.event.ActionEvent;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.actions.SaveActionBase;
import org.openstreetmap.josm.actions.ToggleUploadDiscouragedLayerAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.conflict.ConflictCollection;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.ImmutableGpxTrack;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.DataIntegrityProblemException;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSetMerger;
import org.openstreetmap.josm.data.osm.DataSource;
import org.openstreetmap.josm.data.osm.DatasetConsistencyTest;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListenerAdapter;
import org.openstreetmap.josm.data.osm.event.DataSetListenerAdapter.Listener;
import org.openstreetmap.josm.data.osm.visitor.AbstractVisitor;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.osm.visitor.paint.MapRendererFactory;
import org.openstreetmap.josm.data.osm.visitor.paint.Rendering;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.MultipolygonCache;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.progress.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.tools.DateUtils;
import org.openstreetmap.josm.tools.FilteredCollection;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A layer that holds OSM data from a specific dataset.
 * The data can be fully edited.
 *
 * @author imi
 */
public class OsmDataLayer extends Layer implements Listener, SelectionChangedListener {
    static public final String REQUIRES_SAVE_TO_DISK_PROP = OsmDataLayer.class.getName() + ".requiresSaveToDisk";
    static public final String REQUIRES_UPLOAD_TO_SERVER_PROP = OsmDataLayer.class.getName() + ".requiresUploadToServer";

    private boolean requiresSaveToFile = false;
    private boolean requiresUploadToServer = false;
    private boolean isChanged = true;
    private int highlightUpdateCount;

    /**
     * List of validation errors in this layer.
     * @since 3669
     */
    public final List<TestError> validationErrors = new ArrayList<TestError>();

    protected void setRequiresSaveToFile(boolean newValue) {
        boolean oldValue = requiresSaveToFile;
        requiresSaveToFile = newValue;
        if (oldValue != newValue) {
            propertyChangeSupport.firePropertyChange(REQUIRES_SAVE_TO_DISK_PROP, oldValue, newValue);
        }
    }

    protected void setRequiresUploadToServer(boolean newValue) {
        boolean oldValue = requiresUploadToServer;
        requiresUploadToServer = newValue;
        if (oldValue != newValue) {
            propertyChangeSupport.firePropertyChange(REQUIRES_UPLOAD_TO_SERVER_PROP, oldValue, newValue);
        }
    }

    /** the global counter for created data layers */
    static private int dataLayerCounter = 0;

    /**
     * Replies a new unique name for a data layer
     *
     * @return a new unique name for a data layer
     */
    static public String createNewName() {
        dataLayerCounter++;
        return tr("Data Layer {0}", dataLayerCounter);
    }

    public final static class DataCountVisitor extends AbstractVisitor {
        public int nodes;
        public int ways;
        public int relations;
        public int deletedNodes;
        public int deletedWays;
        public int deletedRelations;

        @Override
        public void visit(final Node n) {
            nodes++;
            if (n.isDeleted()) {
                deletedNodes++;
            }
        }

        @Override
        public void visit(final Way w) {
            ways++;
            if (w.isDeleted()) {
                deletedWays++;
            }
        }

        @Override
        public void visit(final Relation r) {
            relations++;
            if (r.isDeleted()) {
                deletedRelations++;
            }
        }
    }

    public interface CommandQueueListener {
        void commandChanged(int queueSize, int redoSize);
    }

    /**
     * Listener called when a state of this layer has changed.
     */
    public interface LayerStateChangeListener {
        /**
         * Notifies that the "upload discouraged" (upload=no) state has changed.
         * @param layer The layer that has been modified
         * @param newValue The new value of the state
         */
        void uploadDiscouragedChanged(OsmDataLayer layer, boolean newValue);
    }

    private final CopyOnWriteArrayList<LayerStateChangeListener> layerStateChangeListeners = new CopyOnWriteArrayList<LayerStateChangeListener>();

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
     * Removes a layer property change listener
     *
     * @param listener the listener. Ignored if null or already registered.
     * @since 5519
     */
    public void removeLayerPropertyChangeListener(LayerStateChangeListener listener) {
        layerStateChangeListeners.remove(listener);
    }

    /**
     * The data behind this layer.
     */
    public final DataSet data;

    /**
     * the collection of conflicts detected in this layer
     */
    private ConflictCollection conflicts;

    /**
     * a paint texture for non-downloaded area
     */
    private static TexturePaint hatched;

    static {
        createHatchTexture();
    }

    public static Color getBackgroundColor() {
        return Main.pref.getColor(marktr("background"), Color.BLACK);
    }

    public static Color getOutsideColor() {
        return Main.pref.getColor(marktr("outside downloaded area"), Color.YELLOW);
    }

    /**
     * Initialize the hatch pattern used to paint the non-downloaded area
     */
    public static void createHatchTexture() {
        BufferedImage bi = new BufferedImage(15, 15, BufferedImage.TYPE_INT_ARGB);
        Graphics2D big = bi.createGraphics();
        big.setColor(getBackgroundColor());
        Composite comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f);
        big.setComposite(comp);
        big.fillRect(0,0,15,15);
        big.setColor(getOutsideColor());
        big.drawLine(0,15,15,0);
        Rectangle r = new Rectangle(0, 0, 15,15);
        hatched = new TexturePaint(bi, r);
    }

    /**
     * Construct a OsmDataLayer.
     */
    public OsmDataLayer(final DataSet data, final String name, final File associatedFile) {
        super(name);
        this.data = data;
        this.setAssociatedFile(associatedFile);
        conflicts = new ConflictCollection();
        data.addDataSetListener(new DataSetListenerAdapter(this));
        data.addDataSetListener(MultipolygonCache.getInstance());
        DataSet.addSelectionListener(this);
    }

    protected Icon getBaseIcon() {
        return ImageProvider.get("layer", "osmdata_small");
    }

    /**
     * TODO: @return Return a dynamic drawn icon of the map data. The icon is
     *         updated by a background thread to not disturb the running programm.
     */
    @Override public Icon getIcon() {
        Icon baseIcon = getBaseIcon();
        if (isUploadDiscouraged()) {
            return ImageProvider.overlay(baseIcon,
                    new ImageIcon(ImageProvider.get("warning-small").getImage().getScaledInstance(8, 8, Image.SCALE_SMOOTH)),
                    ImageProvider.OverlayPosition.SOUTHEAST);
        } else {
            return baseIcon;
        }
    }

    /**
     * Draw all primitives in this layer but do not draw modified ones (they
     * are drawn by the edit layer).
     * Draw nodes last to overlap the ways they belong to.
     */
    @Override public void paint(final Graphics2D g, final MapView mv, Bounds box) {
        isChanged = false;
        highlightUpdateCount = data.getHighlightUpdateCount();

        boolean active = mv.getActiveLayer() == this;
        boolean inactive = !active && Main.pref.getBoolean("draw.data.inactive_color", true);
        boolean virtual = !inactive && mv.isVirtualNodesEnabled();

        // draw the hatched area for non-downloaded region. only draw if we're the active
        // and bounds are defined; don't draw for inactive layers or loaded GPX files etc
        if (active && Main.pref.getBoolean("draw.data.downloaded_area", true) && !data.dataSources.isEmpty()) {
            // initialize area with current viewport
            Rectangle b = mv.getBounds();
            // on some platforms viewport bounds seem to be offset from the left,
            // over-grow it just to be sure
            b.grow(100, 100);
            Area a = new Area(b);

            // now successively subtract downloaded areas
            for (Bounds bounds : data.getDataSourceBounds()) {
                if (bounds.isCollapsed()) {
                    continue;
                }
                Point p1 = mv.getPoint(bounds.getMin());
                Point p2 = mv.getPoint(bounds.getMax());
                Rectangle r = new Rectangle(Math.min(p1.x, p2.x),Math.min(p1.y, p2.y),Math.abs(p2.x-p1.x),Math.abs(p2.y-p1.y));
                a.subtract(new Area(r));
            }

            // paint remainder
            g.setPaint(hatched);
            g.fill(a);
        }

        Rendering painter = MapRendererFactory.getInstance().createActiveRenderer(g, mv, inactive);
        painter.render(data, virtual, box);
        Main.map.conflictDialog.paintConflicts(g, mv);
    }

    @Override public String getToolTipText() {
        int nodes = new FilteredCollection<Node>(data.getNodes(), OsmPrimitive.nonDeletedPredicate).size();
        int ways = new FilteredCollection<Way>(data.getWays(), OsmPrimitive.nonDeletedPredicate).size();

        String tool = trn("{0} node", "{0} nodes", nodes, nodes)+", ";
        tool += trn("{0} way", "{0} ways", ways, ways);

        if (data.getVersion() != null) {
            tool += ", " + tr("version {0}", data.getVersion());
        }
        File f = getAssociatedFile();
        if (f != null) {
            tool = "<html>"+tool+"<br>"+f.getPath()+"</html>";
        }
        return tool;
    }

    @Override public void mergeFrom(final Layer from) {
        final PleaseWaitProgressMonitor monitor = new PleaseWaitProgressMonitor(tr("Merging layers"));
        monitor.setCancelable(false);
        if (from instanceof OsmDataLayer && ((OsmDataLayer)from).isUploadDiscouraged()) {
            setUploadDiscouraged(true);
        }
        mergeFrom(((OsmDataLayer)from).data, monitor);
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
     * merges the primitives in dataset <code>from</code> into the dataset of
     * this layer
     *
     * @param from  the source data set
     * @param progressMonitor the progress monitor, can be {@code null}
     */
    public void mergeFrom(final DataSet from, ProgressMonitor progressMonitor) {
        final DataSetMerger visitor = new DataSetMerger(data,from);
        try {
            visitor.merge(progressMonitor);
        } catch (DataIntegrityProblemException e) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    e.getHtmlMessage() != null ? e.getHtmlMessage() : e.getMessage(),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
            return;

        }

        Area a = data.getDataSourceArea();

        // copy the merged layer's data source info.
        // only add source rectangles if they are not contained in the layer already.
        for (DataSource src : from.dataSources) {
            if (a == null || !a.contains(src.bounds.asRect())) {
                data.dataSources.add(src);
            }
        }

        // copy the merged layer's API version, downgrade if required
        if (data.getVersion() == null) {
            data.setVersion(from.getVersion());
        } else if ("0.5".equals(data.getVersion()) ^ "0.5".equals(from.getVersion())) {
            Main.warn(tr("Mixing 0.6 and 0.5 data results in version 0.5"));
            data.setVersion("0.5");
        }

        int numNewConflicts = 0;
        for (Conflict<?> c : visitor.getConflicts()) {
            if (!conflicts.hasConflict(c)) {
                numNewConflicts++;
                conflicts.add(c);
            }
        }
        // repaint to make sure new data is displayed properly.
        if (Main.isDisplayingMapView()) {
            Main.map.mapView.repaint();
        }
        // warn about new conflicts
        if (numNewConflicts > 0 && Main.map != null && Main.map.conflictDialog != null) {
            Main.map.conflictDialog.warnNumNewConflicts(numNewConflicts);
        }
    }

    @Override public boolean isMergable(final Layer other) {
        // isUploadDiscouraged commented to allow merging between normal layers and discouraged layers with a warning (see #7684)
        return other instanceof OsmDataLayer;// && (isUploadDiscouraged() == ((OsmDataLayer)other).isUploadDiscouraged());
    }

    @Override public void visitBoundingBox(final BoundingXYVisitor v) {
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
    public void cleanupAfterUpload(final Collection<IPrimitive> processed) {
        // return immediately if an upload attempt failed
        if (processed == null || processed.isEmpty())
            return;

        Main.main.undoRedo.clean(this);

        // if uploaded, clean the modified flags as well
        data.cleanupDeletedPrimitives();
        for (OsmPrimitive p: data.allPrimitives()) {
            if (processed.contains(p)) {
                p.setModified(false);
            }
        }
    }


    @Override public Object getInfoComponent() {
        final DataCountVisitor counter = new DataCountVisitor();
        for (final OsmPrimitive osm : data.allPrimitives()) {
            osm.accept(counter);
        }
        final JPanel p = new JPanel(new GridBagLayout());

        String nodeText = trn("{0} node", "{0} nodes", counter.nodes, counter.nodes);
        if (counter.deletedNodes > 0) {
            nodeText += " ("+trn("{0} deleted", "{0} deleted", counter.deletedNodes, counter.deletedNodes)+")";
        }

        String wayText = trn("{0} way", "{0} ways", counter.ways, counter.ways);
        if (counter.deletedWays > 0) {
            wayText += " ("+trn("{0} deleted", "{0} deleted", counter.deletedWays, counter.deletedWays)+")";
        }

        String relationText = trn("{0} relation", "{0} relations", counter.relations, counter.relations);
        if (counter.deletedRelations > 0) {
            relationText += " ("+trn("{0} deleted", "{0} deleted", counter.deletedRelations, counter.deletedRelations)+")";
        }

        p.add(new JLabel(tr("{0} consists of:", getName())), GBC.eol());
        p.add(new JLabel(nodeText, ImageProvider.get("data", "node"), JLabel.HORIZONTAL), GBC.eop().insets(15,0,0,0));
        p.add(new JLabel(wayText, ImageProvider.get("data", "way"), JLabel.HORIZONTAL), GBC.eop().insets(15,0,0,0));
        p.add(new JLabel(relationText, ImageProvider.get("data", "relation"), JLabel.HORIZONTAL), GBC.eop().insets(15,0,0,0));
        p.add(new JLabel(tr("API version: {0}", (data.getVersion() != null) ? data.getVersion() : tr("unset"))), GBC.eop().insets(15,0,0,0));
        if (isUploadDiscouraged()) {
            p.add(new JLabel(tr("Upload is discouraged")), GBC.eop().insets(15,0,0,0));
        }

        return p;
    }

    @Override public Action[] getMenuEntries() {
        if (Main.applet)
            return new Action[]{
                LayerListDialog.getInstance().createActivateLayerAction(this),
                LayerListDialog.getInstance().createShowHideLayerAction(),
                LayerListDialog.getInstance().createDeleteLayerAction(),
                SeparatorLayerAction.INSTANCE,
                LayerListDialog.getInstance().createMergeLayerAction(this),
                SeparatorLayerAction.INSTANCE,
                new RenameLayerAction(getAssociatedFile(), this),
                new ConsistencyTestAction(),
                SeparatorLayerAction.INSTANCE,
                new LayerListPopup.InfoAction(this)};
        List<Action> actions = new ArrayList<Action>();
        actions.addAll(Arrays.asList(new Action[]{
                LayerListDialog.getInstance().createActivateLayerAction(this),
                LayerListDialog.getInstance().createShowHideLayerAction(),
                LayerListDialog.getInstance().createDeleteLayerAction(),
                SeparatorLayerAction.INSTANCE,
                LayerListDialog.getInstance().createMergeLayerAction(this),
                new LayerSaveAction(this),
                new LayerSaveAsAction(this),
        }));
        if (ExpertToggleAction.isExpert()) {
            actions.addAll(Arrays.asList(new Action[]{
                    new LayerGpxExportAction(this),
                    new ConvertToGpxLayerAction()}));
        }
        actions.addAll(Arrays.asList(new Action[]{
                SeparatorLayerAction.INSTANCE,
                new RenameLayerAction(getAssociatedFile(), this)}));
        if (ExpertToggleAction.isExpert() && Main.pref.getBoolean("data.layer.upload_discouragement.menu_item", false)) {
            actions.add(new ToggleUploadDiscouragedLayerAction(this));
        }
        actions.addAll(Arrays.asList(new Action[]{
                new ConsistencyTestAction(),
                SeparatorLayerAction.INSTANCE,
                new LayerListPopup.InfoAction(this)}));
        return actions.toArray(new Action[actions.size()]);
    }

    public static GpxData toGpxData(DataSet data, File file) {
        GpxData gpxData = new GpxData();
        gpxData.storageFile = file;
        HashSet<Node> doneNodes = new HashSet<Node>();
        for (Way w : data.getWays()) {
            if (!w.isUsable()) {
                continue;
            }
            Collection<Collection<WayPoint>> trk = new ArrayList<Collection<WayPoint>>();
            Map<String, Object> trkAttr = new HashMap<String, Object>();

            if (w.get("name") != null) {
                trkAttr.put("name", w.get("name"));
            }

            List<WayPoint> trkseg = null;
            for (Node n : w.getNodes()) {
                if (!n.isUsable()) {
                    trkseg = null;
                    continue;
                }
                if (trkseg == null) {
                    trkseg = new ArrayList<WayPoint>();
                    trk.add(trkseg);
                }
                if (!n.isTagged()) {
                    doneNodes.add(n);
                }
                WayPoint wpt = new WayPoint(n.getCoor());
                if (!n.isTimestampEmpty()) {
                    wpt.attr.put("time", DateUtils.fromDate(n.getTimestamp()));
                    wpt.setTime();
                }
                trkseg.add(wpt);
            }

            gpxData.tracks.add(new ImmutableGpxTrack(trk, trkAttr));
        }

        for (Node n : data.getNodes()) {
            if (n.isIncomplete() || n.isDeleted() || doneNodes.contains(n)) {
                continue;
            }
            WayPoint wpt = new WayPoint(n.getCoor());
            String name = n.get("name");
            if (name != null) {
                wpt.attr.put("name", name);
            }
            if (!n.isTimestampEmpty()) {
                wpt.attr.put("time", DateUtils.fromDate(n.getTimestamp()));
                wpt.setTime();
            }
            String desc = n.get("description");
            if (desc != null) {
                wpt.attr.put("desc", desc);
            }

            gpxData.waypoints.add(wpt);
        }
        return gpxData;
    }

    public GpxData toGpxData() {
        return toGpxData(data, getAssociatedFile());
    }

    public class ConvertToGpxLayerAction extends AbstractAction {
        public ConvertToGpxLayerAction() {
            super(tr("Convert to GPX layer"), ImageProvider.get("converttogpx"));
            putValue("help", ht("/Action/ConvertToGpxLayer"));
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            Main.main.addLayer(new GpxLayer(toGpxData(), tr("Converted from: {0}", getName())));
            Main.main.removeLayer(OsmDataLayer.this);
        }
    }

    public boolean containsPoint(LatLon coor) {
        // we'll assume that if this has no data sources
        // that it also has no borders
        if (this.data.dataSources.isEmpty())
            return true;

        boolean layer_bounds_point = false;
        for (DataSource src : this.data.dataSources) {
            if (src.bounds.contains(coor)) {
                layer_bounds_point = true;
                break;
            }
        }
        return layer_bounds_point;
    }

    /**
     * replies the set of conflicts currently managed in this layer
     *
     * @return the set of conflicts currently managed in this layer
     */
    public ConflictCollection getConflicts() {
        return conflicts;
    }

    /**
     * Replies true if the data managed by this layer needs to be uploaded to
     * the server because it contains at least one modified primitive.
     *
     * @return true if the data managed by this layer needs to be uploaded to
     * the server because it contains at least one modified primitive; false,
     * otherwise
     */
    public boolean requiresUploadToServer() {
        return requiresUploadToServer;
    }

    /**
     * Replies true if the data managed by this layer needs to be saved to
     * a file. Only replies true if a file is assigned to this layer and
     * if the data managed by this layer has been modified since the last
     * save operation to the file.
     *
     * @return true if the data managed by this layer needs to be saved to
     * a file
     */
    public boolean requiresSaveToFile() {
        return getAssociatedFile() != null && requiresSaveToFile;
    }

    @Override
    public void onPostLoadFromFile() {
        setRequiresSaveToFile(false);
        setRequiresUploadToServer(data.isModified());
    }

    public void onPostDownloadFromServer() {
        setRequiresSaveToFile(true);
        setRequiresUploadToServer(data.isModified());
    }

    @Override
    public boolean isChanged() {
        return isChanged || highlightUpdateCount != data.getHighlightUpdateCount();
    }

    /**
     * Initializes the layer after a successful save of OSM data to a file
     *
     */
    public void onPostSaveToFile() {
        setRequiresSaveToFile(false);
        setRequiresUploadToServer(data.isModified());
    }

    /**
     * Initializes the layer after a successful upload to the server
     *
     */
    public void onPostUploadToServer() {
        setRequiresUploadToServer(data.isModified());
        // keep requiresSaveToDisk unchanged
    }

    private class ConsistencyTestAction extends AbstractAction {

        public ConsistencyTestAction() {
            super(tr("Dataset consistency test"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String result = DatasetConsistencyTest.runTests(data);
            if (result.length() == 0) {
                JOptionPane.showMessageDialog(Main.parent, tr("No problems found"));
            } else {
                JPanel p = new JPanel(new GridBagLayout());
                p.add(new JLabel(tr("Following problems found:")), GBC.eol());
                JosmTextArea info = new JosmTextArea(result, 20, 60);
                info.setCaretPosition(0);
                info.setEditable(false);
                p.add(new JScrollPane(info), GBC.eop());

                JOptionPane.showMessageDialog(Main.parent, p, tr("Warning"), JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    @Override
    public void destroy() {
        DataSet.removeSelectionListener(this);
    }

    @Override
    public void processDatasetEvent(AbstractDatasetChangedEvent event) {
        isChanged = true;
        setRequiresSaveToFile(true);
        setRequiresUploadToServer(true);
    }

    @Override
    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        isChanged = true;
    }

    @Override
    public void projectionChanged(Projection oldValue, Projection newValue) {
        /*
         * No reprojection required. The dataset itself is registered as projection
         * change listener and already got notified.
         */
    }

    public final boolean isUploadDiscouraged() {
        return data.isUploadDiscouraged();
    }

    public final void setUploadDiscouraged(boolean uploadDiscouraged) {
        if (uploadDiscouraged ^ isUploadDiscouraged()) {
            data.setUploadDiscouraged(uploadDiscouraged);
            for (LayerStateChangeListener l : layerStateChangeListeners) {
                l.uploadDiscouragedChanged(this, uploadDiscouraged);
            }
        }
    }

    @Override
    public boolean isSavable() {
        return true; // With OsmExporter
    }

    @Override
    public boolean checkSaveConditions() {
        if (isDataSetEmpty()) {
            ExtendedDialog dialog = new ExtendedDialog(
                    Main.parent,
                    tr("Empty document"),
                    new String[] {tr("Save anyway"), tr("Cancel")}
            );
            dialog.setContent(tr("The document contains no data."));
            dialog.setButtonIcons(new String[] {"save.png", "cancel.png"});
            dialog.showDialog();
            if (dialog.getValue() != 1) return false;
        }

        ConflictCollection conflicts = getConflicts();
        if (conflicts != null && !conflicts.isEmpty()) {
            ExtendedDialog dialog = new ExtendedDialog(
                    Main.parent,
                    /* I18N: Display title of the window showing conflicts */
                    tr("Conflicts"),
                    new String[] {tr("Reject Conflicts and Save"), tr("Cancel")}
            );
            dialog.setContent(tr("There are unresolved conflicts. Conflicts will not be saved and handled as if you rejected all. Continue?"));
            dialog.setButtonIcons(new String[] {"save.png", "cancel.png"});
            dialog.showDialog();
            if (dialog.getValue() != 1) return false;
        }
        return true;
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
            for (OsmPrimitive osm : data.allNonDeletedPrimitives())
                if (!osm.isDeleted() || !osm.isNewOrUndeleted())
                    return false;
        }
        return true;
    }

    @Override
    public File createAndOpenSaveFileChooser() {
        return SaveActionBase.createAndOpenSaveFileChooser(tr("Save OSM file"), "osm");
    }
}
