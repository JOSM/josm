// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.event.ActionEvent;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.command.PurgePrimitivesCommand;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.conflict.ConflictCollection;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSetMerger;
import org.openstreetmap.josm.data.osm.DataSource;
import org.openstreetmap.josm.data.osm.DatasetConsistencyTest;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.AbstractVisitor;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.osm.visitor.MapPaintVisitor;
import org.openstreetmap.josm.data.osm.visitor.SimplePaintVisitor;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.tools.DateUtils;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A layer holding data from a specific dataset.
 * The data can be fully edited.
 *
 * @author imi
 */
public class OsmDataLayer extends Layer {
    static public final String REQUIRES_SAVE_TO_DISK_PROP = OsmDataLayer.class.getName() + ".requiresSaveToDisk";
    static public final String REQUIRES_UPLOAD_TO_SERVER_PROP = OsmDataLayer.class.getName() + ".requiresUploadToServer";

    private boolean requiresSaveToFile = false;
    private boolean requiresUploadToServer = false;

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

        public void visit(final Node n) {
            nodes++;
            if (n.isDeleted()) {
                deletedNodes++;
            }
        }

        public void visit(final Way w) {
            ways++;
            if (w.isDeleted()) {
                deletedWays++;
            }
        }

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
     * The data behind this layer.
     */
    public final DataSet data;

    /**
     * the collection of conflicts detected in this layer
     */
    private ConflictCollection conflicts;

    public final LinkedList<DataChangeListener> listenerDataChanged = new LinkedList<DataChangeListener>();

    /**
     * a paint texture for non-downloaded area
     */
    private static TexturePaint hatched;

    static {
        createHatchTexture();
    }

    /**
     * Initialize the hatch pattern used to paint the non-downloaded area
     */
    public static void createHatchTexture() {
        BufferedImage bi = new BufferedImage(15, 15, BufferedImage.TYPE_INT_ARGB);
        Graphics2D big = bi.createGraphics();
        big.setColor(Main.pref.getColor(marktr("background"), Color.BLACK));
        Composite comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f);
        big.setComposite(comp);
        big.fillRect(0,0,15,15);
        big.setColor(Main.pref.getColor(marktr("outside downloaded area"), Color.YELLOW));
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
    }

    /**
     * TODO: @return Return a dynamic drawn icon of the map data. The icon is
     *         updated by a background thread to not disturb the running programm.
     */
    @Override public Icon getIcon() {
        return ImageProvider.get("layer", "osmdata_small");
    }

    /**
     * Draw all primitives in this layer but do not draw modified ones (they
     * are drawn by the edit layer).
     * Draw nodes last to overlap the ways they belong to.
     */
    @Override public void paint(final Graphics2D g, final MapView mv, Bounds box) {
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

            // now succesively subtract downloaded areas
            for (DataSource src : data.dataSources) {
                if (src.bounds != null && !src.bounds.getMin().equals(src.bounds.getMax())) {
                    EastNorth en1 = mv.getProjection().latlon2eastNorth(src.bounds.getMin());
                    EastNorth en2 = mv.getProjection().latlon2eastNorth(src.bounds.getMax());
                    Point p1 = mv.getPoint(en1);
                    Point p2 = mv.getPoint(en2);
                    Rectangle r = new Rectangle(Math.min(p1.x, p2.x),Math.min(p1.y, p2.y),Math.abs(p2.x-p1.x),Math.abs(p2.y-p1.y));
                    a.subtract(new Area(r));
                }
            }

            // paint remainder
            g.setPaint(hatched);
            g.fill(a);
        }

        SimplePaintVisitor painter;
        if (Main.pref.getBoolean("draw.wireframe")) {
            painter = new SimplePaintVisitor();
        } else {
            painter = new MapPaintVisitor();
        }
        painter.setGraphics(g);
        painter.setNavigatableComponent(mv);
        painter.inactive = inactive;
        painter.visitAll(data, virtual, box);
        Main.map.conflictDialog.paintConflicts(g, mv);
    }

    @Override public String getToolTipText() {
        int nodes = undeletedSize(data.getNodes());
        int ways = undeletedSize(data.getWays());

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
        mergeFrom(((OsmDataLayer)from).data);
    }

    /**
     * merges the primitives in dataset <code>from</code> into the dataset of
     * this layer
     *
     * @param from  the source data set
     */
    public void mergeFrom(final DataSet from) {
        final DataSetMerger visitor = new DataSetMerger(data,from);
        visitor.merge();

        Area a = data.getDataSourceArea();

        // copy the merged layer's data source info;
        // only add source rectangles if they are not contained in the
        // layer already.
        for (DataSource src : from.dataSources) {
            if (a == null || !a.contains(src.bounds.asRect())) {
                data.dataSources.add(src);
            }
        }

        // copy the merged layer's API version, downgrade if required
        if (data.getVersion() == null) {
            data.setVersion(from.getVersion());
        } else if ("0.5".equals(data.getVersion()) ^ "0.5".equals(from.getVersion())) {
            System.err.println(tr("Warning: mixing 0.6 and 0.5 data results in version 0.5"));
            data.setVersion("0.5");
        }

        int numNewConflicts = 0;
        for (Conflict<?> c : visitor.getConflicts()) {
            if (!conflicts.hasConflict(c)) {
                numNewConflicts++;
                conflicts.add(c);
            }
        }
        PurgePrimitivesCommand cmd = buildPurgeCommand();
        if (cmd != null) {
            Main.main.undoRedo.add(cmd);
        }
        fireDataChange();
        // repaint to make sure new data is displayed properly.
        Main.map.mapView.repaint();
        warnNumNewConflicts(
                numNewConflicts,
                cmd == null ? 0 : cmd.getPurgedPrimitives().size()
        );
    }

    /**
     * Warns the user about the number of detected conflicts
     *
     * @param numNewConflicts the number of detected conflicts
     * @param numPurgedPrimitives the number of automatically purged objects
     */
    protected void warnNumNewConflicts(int numNewConflicts, int numPurgedPrimitives) {
        if (numNewConflicts == 0 && numPurgedPrimitives == 0) return;

        String msg1 = trn(
                "There was {0} conflict detected.",
                "There were {0} conflicts detected.",
                numNewConflicts,
                numNewConflicts
        );
        String msg2 = trn(
                "{0} conflict has been <strong>resolved automatically</strong> by purging {0} object<br>from the local dataset because it is deleted on the server.",
                "{0} conflicts have been <strong>resolved automatically</strong> by purging {0} objects<br> from the local dataset because they are deleted on the server.",
                numPurgedPrimitives,
                numPurgedPrimitives
        );
        int numRemainingConflicts = numNewConflicts - numPurgedPrimitives;
        String msg3 = "";
        if (numRemainingConflicts >0) {
            msg3 = trn(
                    "{0} conflict remains to be resolved.<br><br>Please open the Conflict List Dialog and manually resolve it.",
                    "{0} conflicts remain to be resolved.<br><br>Please open the Conflict List Dialog and manually resolve them.",
                    numRemainingConflicts,
                    numRemainingConflicts
            );
        }

        StringBuffer sb = new StringBuffer();
        sb.append("<html>").append(msg1);
        if (numPurgedPrimitives > 0) {
            sb.append("<br>").append(msg2);
        }
        if (numRemainingConflicts > 0) {
            sb.append("<br>").append(msg3);
        }
        sb.append("</html>");
        if (numNewConflicts > 0) {
            ButtonSpec[] options = new ButtonSpec[] {
                    new ButtonSpec(
                            tr("OK"),
                            ImageProvider.get("ok"),
                            tr("Click to close this dialog and continue editing"),
                            null /* no specific help */
                    )
            };
            HelpAwareOptionPane.showOptionDialog(
                    Main.parent,
                    sb.toString(),
                    tr("Conflicts detected"),
                    JOptionPane.WARNING_MESSAGE,
                    null, /* no icon */
                    options,
                    options[0],
                    ht("/Concepts/Conflict#WarningAboutDetectedConflicts")
            );
        }
    }

    /**
     * Builds the purge command for primitives which can be purged automatically
     * from the local dataset because they've been deleted on the
     * server.
     *
     * @return the purge command. <code>null</code> if no primitives have to
     * be purged
     */
    protected PurgePrimitivesCommand buildPurgeCommand() {
        ArrayList<OsmPrimitive> toPurge = new ArrayList<OsmPrimitive>();
        conflictLoop: for (Conflict<?> c: conflicts) {
            if (c.getMy().isDeleted() && !c.getTheir().isVisible()) {
                // Local and server version of the primitive are deleted. We
                // can purge it from the local dataset.
                //
                toPurge.add(c.getMy());
            } else if (!c.getMy().isModified() && ! c.getTheir().isVisible()) {
                // We purge deleted *ways* and *relations* automatically if they are
                // deleted on the server and if they aren't modified in the local
                // dataset.
                //
                if (c.getMy() instanceof Way || c.getMy() instanceof Relation) {
                    toPurge.add(c.getMy());
                    continue conflictLoop;
                }
                // We only purge nodes if they aren't part of a modified way.
                // Otherwise the number of nodes of a modified way could drop
                // below 2 and we would lose the modified data when the way
                // gets purged.
                //
                for (OsmPrimitive parent: c.getMy().getReferrers()) {
                    if (parent.isModified() && parent instanceof Way) {
                        continue conflictLoop;
                    }
                }
                toPurge.add(c.getMy());
            }
        }
        if (toPurge.isEmpty()) return null;
        PurgePrimitivesCommand cmd = new PurgePrimitivesCommand(this, toPurge);
        return cmd;
    }

    @Override public boolean isMergable(final Layer other) {
        return other instanceof OsmDataLayer;
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
    public void cleanupAfterUpload(final Collection<OsmPrimitive> processed) {
        // return immediately if an upload attempt failed
        if (processed == null || processed.isEmpty())
            return;

        Main.main.undoRedo.clean(this);

        // if uploaded, clean the modified flags as well
        final Set<OsmPrimitive> processedSet = new HashSet<OsmPrimitive>(processed);
        data.clenupDeletedPrimitives();
        for (final Iterator<Node> it = data.getNodes().iterator(); it.hasNext();) {
            cleanIterator(it, processedSet);
        }
        for (final Iterator<Way> it = data.getWays().iterator(); it.hasNext();) {
            cleanIterator(it, processedSet);
        }
        for (final Iterator<Relation> it = data.getRelations().iterator(); it.hasNext();) {
            cleanIterator(it, processedSet);
        }
    }

    /**
     * Clean the modified flag for the given iterator over a collection if it is in the
     * list of processed entries.
     *
     * @param it The iterator to change the modified and remove the items if deleted.
     * @param processed A list of all objects that have been successfully progressed.
     *         If the object in the iterator is not in the list, nothing will be changed on it.
     */
    private void cleanIterator(final Iterator<? extends OsmPrimitive> it, final Collection<OsmPrimitive> processed) {
        final OsmPrimitive osm = it.next();
        if (!processed.remove(osm))
            return;
        osm.setModified(false);
    }

    /**
     * @return The number of not-deleted and visible primitives in the list.
     */
    private int undeletedSize(final Collection<? extends OsmPrimitive> list) {
        int size = 0;
        for (final OsmPrimitive osm : list)
            if (!osm.isDeleted() && osm.isVisible()) {
                size++;
            }
        return size;
    }

    @Override public Object getInfoComponent() {
        final DataCountVisitor counter = new DataCountVisitor();
        for (final OsmPrimitive osm : data.allPrimitives()) {
            osm.visit(counter);
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
        p.add(new JLabel(tr("API version: {0}", (data.getVersion() != null) ? data.getVersion() : tr("unset"))));

        return p;
    }

    @Override public Component[] getMenuEntries() {
        if (Main.applet)
            return new Component[]{
                new JMenuItem(LayerListDialog.getInstance().createActivateLayerAction(this)),
                new JMenuItem(LayerListDialog.getInstance().createShowHideLayerAction(this)),
                new JMenuItem(LayerListDialog.getInstance().createDeleteLayerAction(this)),
                new JSeparator(),
                new JMenuItem(LayerListDialog.getInstance().createMergeLayerAction(this)),
                new JSeparator(),
                new JMenuItem(new RenameLayerAction(getAssociatedFile(), this)),
                new JMenuItem(new ConsistencyTestAction()),
                new JSeparator(),
                new JMenuItem(new LayerListPopup.InfoAction(this))};
        return new Component[]{
                new JMenuItem(LayerListDialog.getInstance().createActivateLayerAction(this)),
                new JMenuItem(LayerListDialog.getInstance().createShowHideLayerAction(this)),
                new JMenuItem(LayerListDialog.getInstance().createDeleteLayerAction(this)),
                new JSeparator(),
                new JMenuItem(LayerListDialog.getInstance().createMergeLayerAction(this)),
                new JMenuItem(new LayerSaveAction(this)),
                new JMenuItem(new LayerSaveAsAction(this)),
                new JMenuItem(new LayerGpxExportAction(this)),
                new JMenuItem(new ConvertToGpxLayerAction()),
                new JSeparator(),
                new JMenuItem(new RenameLayerAction(getAssociatedFile(), this)),
                new JMenuItem(new ConsistencyTestAction()),
                new JSeparator(),
                new JMenuItem(new LayerListPopup.InfoAction(this))};
    }

    public void fireDataChange() {
        setRequiresSaveToFile(true);
        setRequiresUploadToServer(true);
        for (DataChangeListener dcl : listenerDataChanged) {
            dcl.dataChanged(this);
        }
    }

    public static GpxData toGpxData(DataSet data, File file) {
        GpxData gpxData = new GpxData();
        gpxData.storageFile = file;
        HashSet<Node> doneNodes = new HashSet<Node>();
        for (Way w : data.getWays()) {
            if (!w.isUsable()) {
                continue;
            }
            GpxTrack trk = new GpxTrack();
            gpxData.tracks.add(trk);

            if (w.get("name") != null) {
                trk.attr.put("name", w.get("name"));
            }

            ArrayList<WayPoint> trkseg = null;
            for (Node n : w.getNodes()) {
                if (!n.isUsable()) {
                    trkseg = null;
                    continue;
                }
                if (trkseg == null) {
                    trkseg = new ArrayList<WayPoint>();
                    trk.trackSegs.add(trkseg);
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
        }

        // what is this loop meant to do? it creates waypoints but never
        // records them?
        for (Node n : data.getNodes()) {
            if (n.incomplete || n.isDeleted() || doneNodes.contains(n)) {
                continue;
            }
            WayPoint wpt = new WayPoint(n.getCoor());
            if (!n.isTimestampEmpty()) {
                wpt.attr.put("time", DateUtils.fromDate(n.getTimestamp()));
                wpt.setTime();
            }
            String name = n.get("name");
            if (name != null) {
                wpt.attr.put("name", name);
            }
        }
        return gpxData;
    }

    public GpxData toGpxData() {
        return toGpxData(data, getAssociatedFile());
    }

    public class ConvertToGpxLayerAction extends AbstractAction {
        public ConvertToGpxLayerAction() {
            super(tr("Convert to GPX layer"), ImageProvider.get("converttogpx"));
        }
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

    /**
     * Initializes the layer after a successful load of OSM data from a file
     *
     */
    public void onPostLoadFromFile() {
        setRequiresSaveToFile(false);
        setRequiresUploadToServer(data.isModified());
    }

    public void onPostDownloadFromServer() {
        setRequiresSaveToFile(true);
        setRequiresUploadToServer(data.isModified());
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

        public void actionPerformed(ActionEvent e) {
            String result = DatasetConsistencyTest.runTests(data);
            if (result.length() == 0) {
                JOptionPane.showMessageDialog(Main.parent, tr("No problems found"));
            } else {
                JPanel p = new JPanel(new GridBagLayout());
                p.add(new JLabel(tr("Following problems found:")), GBC.eol());
                JTextArea info = new JTextArea(result, 20, 60);
                info.setCaretPosition(0);
                info.setEditable(false);
                p.add(new JScrollPane(info), GBC.eop());

                JOptionPane.showMessageDialog(Main.parent, p, tr("Warning"), JOptionPane.WARNING_MESSAGE);
            }
        }

    }
}
