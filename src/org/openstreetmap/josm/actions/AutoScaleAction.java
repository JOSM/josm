// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
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
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapFrameListener;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.ValidatorDialog.ValidatorBoundingXYVisitor;
import org.openstreetmap.josm.gui.download.DownloadDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Toggles the autoScale feature of the mapView
 * @author imi
 */
public class AutoScaleAction extends JosmAction {

    public static final Collection<String> MODES = Collections.unmodifiableList(Arrays.asList(
        marktr("data"),
        marktr("layer"),
        marktr("selection"),
        marktr("conflict"),
        marktr("download"),
        marktr("problem"),
        marktr("previous"),
        marktr("next")));

    private final String mode;

    protected ZoomChangeAdapter zoomChangeAdapter;
    protected MapFrameAdapter mapFrameAdapter;

    /**
     * Zooms the current map view to the currently selected primitives.
     * Does nothing if there either isn't a current map view or if there isn't a current data
     * layer.
     *
     */
    public static void zoomToSelection() {
        if (Main.main == null || !Main.main.hasEditLayer()) return;
        Collection<OsmPrimitive> sel = Main.main.getEditLayer().data.getSelected();
        if (sel.isEmpty()) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("Nothing selected to zoom to."),
                    tr("Information"),
                    JOptionPane.INFORMATION_MESSAGE
            );
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
            Main.map.mapView.recalculateCenterScale(bboxCalculator);
        }
    }

    public static void autoScale(String mode) {
        new AutoScaleAction(mode, false).autoScale();
    }

    private static int getModeShortcut(String mode) {
        int shortcut = -1;

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
                Shortcut.registerShortcut("view:zoom"+mode, tr("View: {0}", tr("Zoom to {0}", tr(mode))), getModeShortcut(mode), Shortcut.DIRECT),
                true, null, false);
        String modeHelp = Character.toUpperCase(mode.charAt(0)) + mode.substring(1);
        putValue("help", "Action/AutoScale/" + modeHelp);
        this.mode = mode;
        if (mode.equals("data")) {
            putValue("help", ht("/Action/ZoomToData"));
        } else if (mode.equals("layer")) {
            putValue("help", ht("/Action/ZoomToLayer"));
        } else if (mode.equals("selection")) {
            putValue("help", ht("/Action/ZoomToSelection"));
        } else if (mode.equals("conflict")) {
            putValue("help", ht("/Action/ZoomToConflict"));
        } else if (mode.equals("problem")) {
            putValue("help", ht("/Action/ZoomToProblem"));
        } else if (mode.equals("download")) {
            putValue("help", ht("/Action/ZoomToDownload"));
        } else if (mode.equals("previous")) {
            putValue("help", ht("/Action/ZoomToPrevious"));
        } else if (mode.equals("next")) {
            putValue("help", ht("/Action/ZoomToNext"));
        } else {
            throw new IllegalArgumentException("Unknown mode: "+mode);
        }
        installAdapters();
    }

    public void autoScale()  {
        if (Main.isDisplayingMapView()) {
            if (mode.equals("previous")) {
                Main.map.mapView.zoomPrevious();
            } else if (mode.equals("next")) {
                Main.map.mapView.zoomNext();
            } else {
                BoundingXYVisitor bbox = getBoundingBox();
                if (bbox != null && bbox.getBounds() != null) {
                    Main.map.mapView.recalculateCenterScale(bbox);
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
        if (layers.isEmpty()) return null;
        return layers.get(0);
    }

    private BoundingXYVisitor getBoundingBox() {
        BoundingXYVisitor v = mode.equals("problem") ? new ValidatorBoundingXYVisitor() : new BoundingXYVisitor();

        if (mode.equals("problem")) {
            TestError error = Main.map.validatorDialog.getSelectedError();
            if (error == null) return null;
            ((ValidatorBoundingXYVisitor) v).visit(error);
            if (v.getBounds() == null) return null;
            v.enlargeBoundingBox(Main.pref.getDouble("validator.zoom-enlarge-bbox", 0.0002));
        } else if (mode.equals("data")) {
            for (Layer l : Main.map.mapView.getAllLayers()) {
                l.visitBoundingBox(v);
            }
        } else if (mode.equals("layer")) {
            if (Main.main.getActiveLayer() == null)
                return null;
            // try to zoom to the first selected layer
            Layer l = getFirstSelectedLayer();
            if (l == null) return null;
            l.visitBoundingBox(v);
        } else if (mode.equals("selection") || mode.equals("conflict")) {
            Collection<OsmPrimitive> sel = new HashSet<OsmPrimitive>();
            if (mode.equals("selection")) {
                sel = getCurrentDataSet().getSelected();
            } else if (mode.equals("conflict")) {
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
                        (mode.equals("selection") ? tr("Nothing selected to zoom to.") : tr("No conflicts to zoom to")),
                        tr("Information"),
                        JOptionPane.INFORMATION_MESSAGE
                );
                return null;
            }
            for (OsmPrimitive osm : sel) {
                osm.accept(v);
            }

            // Increase the bounding box by up to 100% to give more context.
            v.enlargeBoundingBoxLogarithmically(100);
            // Make the bounding box at least 0.0005 degrees (≈ 56 m) wide to
            // ensure reasonable zoom level when zooming onto single nodes.
            v.enlargeToMinDegrees(0.0005);
        } else if (mode.equals("download")) {
            Bounds bounds = DownloadDialog.getSavedDownloadBounds();
            if (bounds != null) {
                try {
                    v.visit(bounds);
                } catch (Exception e) {
                    Main.warn(e);
                }
            }
        }
        return v;
    }

    @Override
    protected void updateEnabledState() {
        if ("selection".equals(mode)) {
            setEnabled(getCurrentDataSet() != null && ! getCurrentDataSet().getSelected().isEmpty());
        }  else if ("layer".equals(mode)) {
            if (!Main.isDisplayingMapView() || Main.map.mapView.getAllLayersAsList().isEmpty()) {
                setEnabled(false);
            } else {
                // FIXME: should also check for whether a layer is selected in the layer list dialog
                setEnabled(true);
            }
        } else if ("conflict".equals(mode)) {
            setEnabled(Main.map != null && Main.map.conflictDialog.getSelectedConflict() != null);
        } else if ("problem".equals(mode)) {
            setEnabled(Main.map != null && Main.map.validatorDialog.getSelectedError() != null);
        } else if ("previous".equals(mode)) {
            setEnabled(Main.isDisplayingMapView() && Main.map.mapView.hasZoomUndoEntries());
        } else if ("next".equals(mode)) {
            setEnabled(Main.isDisplayingMapView() && Main.map.mapView.hasZoomRedoEntries());
        } else {
            setEnabled(
                    Main.isDisplayingMapView()
                    && Main.map.mapView.hasLayers()
            );
        }
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        if ("selection".equals(mode)) {
            setEnabled(selection != null && !selection.isEmpty());
        }
    }

    @Override
    protected void installAdapters() {
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
            if (mode.equals("conflict")) {
                conflictSelectionListener = new ListSelectionListener() {
                    @Override public void valueChanged(ListSelectionEvent e) {
                        updateEnabledState();
                    }
                };
            } else if (mode.equals("problem")) {
                validatorSelectionListener = new TreeSelectionListener() {
                    @Override public void valueChanged(TreeSelectionEvent e) {
                        updateEnabledState();
                    }
                };
            }
        }

        @Override public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
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
