// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapFrameListener;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.ValidatorDialog.ValidatorBoundingXYVisitor;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Toggles the autoScale feature of the mapView
 * @author imi
 */
public class AutoScaleAction extends JosmAction {

    public static final Collection<String> MODES = Collections.unmodifiableList(Arrays.asList(
        marktr(/* ICON(dialogs/autoscale/) */ "data"),
        marktr(/* ICON(dialogs/autoscale/) */ "layer"),
        marktr(/* ICON(dialogs/autoscale/) */ "selection"),
        marktr(/* ICON(dialogs/autoscale/) */ "conflict"),
        marktr(/* ICON(dialogs/autoscale/) */ "download"),
        marktr(/* ICON(dialogs/autoscale/) */ "problem"),
        marktr(/* ICON(dialogs/autoscale/) */ "previous"),
        marktr(/* ICON(dialogs/autoscale/) */ "next")));

    private final String mode;

    protected transient ZoomChangeAdapter zoomChangeAdapter;
    protected transient MapFrameAdapter mapFrameAdapter;
    /** Time of last zoom to bounds action */
    protected long lastZoomTime = -1;
    /** Last zommed bounds */
    protected int lastZoomArea = -1;

    /**
     * Zooms the current map view to the currently selected primitives.
     * Does nothing if there either isn't a current map view or if there isn't a current data
     * layer.
     *
     */
    public static void zoomToSelection() {
        if (Main.main == null || !Main.main.hasEditLayer())
            return;
        Collection<OsmPrimitive> sel = Main.main.getEditLayer().data.getSelected();
        if (sel.isEmpty()) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("Nothing selected to zoom to."),
                    tr("Information"),
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        zoomTo(sel);
    }

    public static void zoomTo(Collection<OsmPrimitive> sel) {
        BoundingXYVisitor bboxCalculator = new BoundingXYVisitor();
        bboxCalculator.computeBoundingBox(sel);
        // increase bbox by 0.001 degrees on each side. this is required
        // especially if the bbox contains one single node, but helpful
        // in most other cases as well.
        bboxCalculator.enlargeBoundingBox();
        if (bboxCalculator.getBounds() != null) {
            Main.map.mapView.zoomTo(bboxCalculator);
        }
    }

    public static void autoScale(String mode) {
        new AutoScaleAction(mode, false).autoScale();
    }

    private static int getModeShortcut(String mode) {
        int shortcut = -1;

        // TODO: convert this to switch/case and make sure the parsing still works
        /* leave as single line for shortcut overview parsing! */
        if (mode.equals("data")) { shortcut = KeyEvent.VK_1; }
        else if (mode.equals("layer")) { shortcut = KeyEvent.VK_2; }
        else if (mode.equals("selection")) { shortcut = KeyEvent.VK_3; }
        else if (mode.equals("conflict")) { shortcut = KeyEvent.VK_4; }
        else if (mode.equals("download")) { shortcut = KeyEvent.VK_5; }
        else if (mode.equals("problem")) { shortcut = KeyEvent.VK_6; }
        else if (mode.equals("previous")) { shortcut = KeyEvent.VK_8; }
        else if (mode.equals("next")) { shortcut = KeyEvent.VK_9; }

        return shortcut;
    }

    /**
     * Constructs a new {@code AutoScaleAction}.
     * @param mode The autoscale mode (one of {@link AutoScaleAction#MODES})
     * @param marker Used only to differentiate from default constructor
     */
    private AutoScaleAction(String mode, boolean marker) {
        super(false);
        this.mode = mode;
    }

    /**
     * Constructs a new {@code AutoScaleAction}.
     * @param mode The autoscale mode (one of {@link AutoScaleAction#MODES})
     */
    public AutoScaleAction(final String mode) {
        super(tr("Zoom to {0}", tr(mode)), "dialogs/autoscale/" + mode, tr("Zoom the view to {0}.", tr(mode)),
                Shortcut.registerShortcut("view:zoom" + mode, tr("View: {0}", tr("Zoom to {0}", tr(mode))),
                        getModeShortcut(mode), Shortcut.DIRECT), true, null, false);
        String modeHelp = Character.toUpperCase(mode.charAt(0)) + mode.substring(1);
        putValue("help", "Action/AutoScale/" + modeHelp);
        this.mode = mode;
        switch (mode) {
        case "data":
            putValue("help", ht("/Action/ZoomToData"));
            break;
        case "layer":
            putValue("help", ht("/Action/ZoomToLayer"));
            break;
        case "selection":
            putValue("help", ht("/Action/ZoomToSelection"));
            break;
        case "conflict":
            putValue("help", ht("/Action/ZoomToConflict"));
            break;
        case "problem":
            putValue("help", ht("/Action/ZoomToProblem"));
            break;
        case "download":
            putValue("help", ht("/Action/ZoomToDownload"));
            break;
        case "previous":
            putValue("help", ht("/Action/ZoomToPrevious"));
            break;
        case "next":
            putValue("help", ht("/Action/ZoomToNext"));
            break;
        default:
            throw new IllegalArgumentException("Unknown mode: " + mode);
        }
        installAdapters();
    }

    public void autoScale() {
        if (Main.isDisplayingMapView()) {
            switch (mode) {
            case "previous":
                Main.map.mapView.zoomPrevious();
                break;
            case "next":
                Main.map.mapView.zoomNext();
                break;
            default:
                BoundingXYVisitor bbox = getBoundingBox();
                if (bbox != null && bbox.getBounds() != null) {
                    Main.map.mapView.zoomTo(bbox);
                }
            }
        }
        putValue("active", true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        autoScale();
    }

    /**
     * Replies the first selected layer in the layer list dialog. null, if no
     * such layer exists, either because the layer list dialog is not yet created
     * or because no layer is selected.
     *
     * @return the first selected layer in the layer list dialog
     */
    protected Layer getFirstSelectedLayer() {
        List<Layer> layers = LayerListDialog.getInstance().getModel().getSelectedLayers();
        if (layers.isEmpty())
            return null;
        return layers.get(0);
    }

    private BoundingXYVisitor getBoundingBox() {
        BoundingXYVisitor v = "problem".equals(mode) ? new ValidatorBoundingXYVisitor() : new BoundingXYVisitor();

        switch (mode) {
        case "problem":
            TestError error = Main.map.validatorDialog.getSelectedError();
            if (error == null)
                return null;
            ((ValidatorBoundingXYVisitor) v).visit(error);
            if (v.getBounds() == null)
                return null;
            v.enlargeBoundingBox(Main.pref.getDouble("validator.zoom-enlarge-bbox", 0.0002));
            break;
        case "data":
            for (Layer l : Main.map.mapView.getAllLayers()) {
                l.visitBoundingBox(v);
            }
            break;
        case "layer":
            if (Main.main.getActiveLayer() == null)
                return null;
            // try to zoom to the first selected layer
            Layer l = getFirstSelectedLayer();
            if (l == null)
                return null;
            l.visitBoundingBox(v);
            break;
        case "selection":
        case "conflict":
            Collection<OsmPrimitive> sel = new HashSet<>();
            if ("selection".equals(mode)) {
                sel = getCurrentDataSet().getSelected();
            } else {
                Conflict<? extends OsmPrimitive> c = Main.map.conflictDialog.getSelectedConflict();
                if (c != null) {
                    sel.add(c.getMy());
                } else if (Main.map.conflictDialog.getConflicts() != null) {
                    sel = Main.map.conflictDialog.getConflicts().getMyConflictParties();
                }
            }
            if (sel.isEmpty()) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        ("selection".equals(mode) ? tr("Nothing selected to zoom to.") : tr("No conflicts to zoom to")),
                        tr("Information"),
                        JOptionPane.INFORMATION_MESSAGE);
                return null;
            }
            for (OsmPrimitive osm : sel) {
                osm.accept(v);
            }

            // Increase the bounding box by up to 100% to give more context.
            v.enlargeBoundingBoxLogarithmically(100);
            // Make the bounding box at least 100 meter wide to
            // ensure reasonable zoom level when zooming onto single nodes.
            v.enlargeToMinSize(Main.pref.getDouble("zoom_to_selection_min_size_in_meter", 100));
            break;
        case "download":

            if (lastZoomTime > 0 && System.currentTimeMillis() - lastZoomTime > Main.pref.getLong("zoom.bounds.reset.time", 10*1000)) {
                lastZoomTime = -1;
            }
            DataSet dataset = Main.main.getCurrentDataSet();
            if(dataset != null) {
                List<DataSource> dataSources = new ArrayList<>(dataset.getDataSources());
                int s = dataSources.size();
                if(s > 0) {
                    if(lastZoomTime == -1 || lastZoomArea == -1 || lastZoomArea > s) {
                        lastZoomArea = s-1;
                        v.visit(dataSources.get(lastZoomArea).bounds);
                    } else if(lastZoomArea > 0) {
                        lastZoomArea -= 1;
                        v.visit(dataSources.get(lastZoomArea).bounds);
                    } else {
                        lastZoomArea = -1;
                        v.visit(new Bounds(Main.main.getCurrentDataSet().getDataSourceArea().getBounds2D()));
                    }
                    lastZoomTime = System.currentTimeMillis();
                } else {
                    lastZoomTime = -1;
                    lastZoomArea = -1;
                }
            }
            break;
        }
        return v;
    }

    @Override
    protected void updateEnabledState() {
        switch (mode) {
        case "selection":
            setEnabled(getCurrentDataSet() != null && !getCurrentDataSet().getSelected().isEmpty());
            break;
        case "layer":
            if (!Main.isDisplayingMapView() || Main.map.mapView.getAllLayersAsList().isEmpty()) {
                setEnabled(false);
            } else {
                // FIXME: should also check for whether a layer is selected in the layer list dialog
                setEnabled(true);
            }
            break;
        case "conflict":
            setEnabled(Main.map != null && Main.map.conflictDialog.getSelectedConflict() != null);
            break;
        case "problem":
            setEnabled(Main.map != null && Main.map.validatorDialog.getSelectedError() != null);
            break;
        case "previous":
            setEnabled(Main.isDisplayingMapView() && Main.map.mapView.hasZoomUndoEntries());
            break;
        case "next":
            setEnabled(Main.isDisplayingMapView() && Main.map.mapView.hasZoomRedoEntries());
            break;
        default:
            setEnabled(Main.isDisplayingMapView() && Main.map.mapView.hasLayers());
        }
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        if ("selection".equals(mode)) {
            setEnabled(selection != null && !selection.isEmpty());
        }
    }

    @Override
    protected final void installAdapters() {
        super.installAdapters();
        // make this action listen to zoom and mapframe change events
        //
        MapView.addZoomChangeListener(zoomChangeAdapter = new ZoomChangeAdapter());
        Main.addMapFrameListener(mapFrameAdapter = new MapFrameAdapter());
        initEnabledState();
    }

    /**
     * Adapter for zoom change events
     */
    private class ZoomChangeAdapter implements MapView.ZoomChangeListener {
        @Override
        public void zoomChanged() {
            updateEnabledState();
        }
    }

    /**
     * Adapter for MapFrame change events
     */
    private class MapFrameAdapter implements MapFrameListener {
        private ListSelectionListener conflictSelectionListener;
        private TreeSelectionListener validatorSelectionListener;

        public MapFrameAdapter() {
            if ("conflict".equals(mode)) {
                conflictSelectionListener = new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        updateEnabledState();
                    }
                };
            } else if ("problem".equals(mode)) {
                validatorSelectionListener = new TreeSelectionListener() {
                    @Override
                    public void valueChanged(TreeSelectionEvent e) {
                        updateEnabledState();
                    }
                };
            }
        }

        @Override
        public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
            if (conflictSelectionListener != null) {
                if (newFrame != null) {
                    newFrame.conflictDialog.addListSelectionListener(conflictSelectionListener);
                } else if (oldFrame != null) {
                    oldFrame.conflictDialog.removeListSelectionListener(conflictSelectionListener);
                }
            } else if (validatorSelectionListener != null) {
                if (newFrame != null) {
                    newFrame.validatorDialog.addTreeSelectionListener(validatorSelectionListener);
                } else if (oldFrame != null) {
                    oldFrame.validatorDialog.removeTreeSelectionListener(validatorSelectionListener);
                }
            }
        }
    }
}
