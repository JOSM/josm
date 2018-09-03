// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionListener;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmData;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapFrameListener;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.ConflictDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.ValidatorDialog.ValidatorBoundingXYVisitor;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Toggles the autoScale feature of the mapView
 * @author imi
 * @since 17
 */
public class AutoScaleAction extends JosmAction {

    /**
     * A list of things we can zoom to. The zoom target is given depending on the mode.
     * @since 14221
     */
    public enum AutoScaleMode {
        /** Zoom the window so that all the data fills the window area */
        DATA(marktr(/* ICON(dialogs/autoscale/) */ "data")),
        /** Zoom the window so that all the data on the currently selected layer fills the window area */
        LAYER(marktr(/* ICON(dialogs/autoscale/) */ "layer")),
        /** Zoom the window so that only data which is currently selected fills the window area */
        SELECTION(marktr(/* ICON(dialogs/autoscale/) */ "selection")),
        /** Zoom to the first selected conflict */
        CONFLICT(marktr(/* ICON(dialogs/autoscale/) */ "conflict")),
        /** Zoom the view to last downloaded data */
        DOWNLOAD(marktr(/* ICON(dialogs/autoscale/) */ "download")),
        /** Zoom the view to problem */
        PROBLEM(marktr(/* ICON(dialogs/autoscale/) */ "problem")),
        /** Zoom to the previous zoomed to scale and location (zoom undo) */
        PREVIOUS(marktr(/* ICON(dialogs/autoscale/) */ "previous")),
        /** Zoom to the next zoomed to scale and location (zoom redo) */
        NEXT(marktr(/* ICON(dialogs/autoscale/) */ "next"));

        private final String label;

        AutoScaleMode(String label) {
            this.label = label;
        }

        /**
         * Returns the English label. Used for retrieving icons.
         * @return the English label
         */
        public String getEnglishLabel() {
            return label;
        }

        /**
         * Returns the localized label. Used for display
         * @return the localized label
         */
        public String getLocalizedLabel() {
            return tr(label);
        }

        /**
         * Returns {@code AutoScaleMode} for a given English label
         * @param englishLabel English label
         * @return {@code AutoScaleMode} for given English label
         * @throws IllegalArgumentException if Engligh label is unknown
         */
        public static AutoScaleMode of(String englishLabel) {
            for (AutoScaleMode v : values()) {
                if (Objects.equals(v.label, englishLabel)) {
                    return v;
                }
            }
            throw new IllegalArgumentException(englishLabel);
        }
    }

    /**
     * A list of things we can zoom to. The zoom target is given depending on the mode.
     * @deprecated Use {@link AutoScaleMode} enum instead
     */
    @Deprecated
    public static final Collection<String> MODES = Collections.unmodifiableList(Arrays.asList(
        marktr(/* ICON(dialogs/autoscale/) */ "data"),
        marktr(/* ICON(dialogs/autoscale/) */ "layer"),
        marktr(/* ICON(dialogs/autoscale/) */ "selection"),
        marktr(/* ICON(dialogs/autoscale/) */ "conflict"),
        marktr(/* ICON(dialogs/autoscale/) */ "download"),
        marktr(/* ICON(dialogs/autoscale/) */ "problem"),
        marktr(/* ICON(dialogs/autoscale/) */ "previous"),
        marktr(/* ICON(dialogs/autoscale/) */ "next")));

    /**
     * One of {@link AutoScaleMode}. Defines what we are zooming to.
     */
    private final AutoScaleMode mode;

    /** Time of last zoom to bounds action */
    protected long lastZoomTime = -1;
    /** Last zommed bounds */
    protected int lastZoomArea = -1;

    /**
     * Zooms the current map view to the currently selected primitives.
     * Does nothing if there either isn't a current map view or if there isn't a current data layer.
     *
     */
    public static void zoomToSelection() {
        OsmData<?, ?, ?, ?> dataSet = MainApplication.getLayerManager().getActiveData();
        if (dataSet == null) {
            return;
        }
        Collection<? extends IPrimitive> sel = dataSet.getSelected();
        if (sel.isEmpty()) {
            JOptionPane.showMessageDialog(
                    MainApplication.getMainFrame(),
                    tr("Nothing selected to zoom to."),
                    tr("Information"),
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        zoomTo(sel);
    }

    /**
     * Zooms the view to display the given set of primitives.
     * @param sel The primitives to zoom to, e.g. the current selection.
     */
    public static void zoomTo(Collection<? extends IPrimitive> sel) {
        BoundingXYVisitor bboxCalculator = new BoundingXYVisitor();
        bboxCalculator.computeBoundingBox(sel);
        // increase bbox. This is required
        // especially if the bbox contains one single node, but helpful
        // in most other cases as well.
        bboxCalculator.enlargeBoundingBox();
        if (bboxCalculator.getBounds() != null) {
            MainApplication.getMap().mapView.zoomTo(bboxCalculator);
        }
    }

    /**
     * Performs the auto scale operation of the given mode without the need to create a new action.
     * @param mode One of {@link #MODES}.
     * @since 14221
     */
    public static void autoScale(AutoScaleMode mode) {
        new AutoScaleAction(mode, false).autoScale();
    }

    /**
     * Performs the auto scale operation of the given mode without the need to create a new action.
     * @param mode One of {@link #MODES}.
     * @deprecated Use {@link #autoScale(AutoScaleMode)} instead
     */
    @Deprecated
    public static void autoScale(String mode) {
        autoScale(AutoScaleMode.of(mode));
    }

    private static int getModeShortcut(String mode) {
        int shortcut = -1;

        // TODO: convert this to switch/case and make sure the parsing still works
        // CHECKSTYLE.OFF: LeftCurly
        // CHECKSTYLE.OFF: RightCurly
        /* leave as single line for shortcut overview parsing! */
        if (mode.equals("data")) { shortcut = KeyEvent.VK_1; }
        else if (mode.equals("layer")) { shortcut = KeyEvent.VK_2; }
        else if (mode.equals("selection")) { shortcut = KeyEvent.VK_3; }
        else if (mode.equals("conflict")) { shortcut = KeyEvent.VK_4; }
        else if (mode.equals("download")) { shortcut = KeyEvent.VK_5; }
        else if (mode.equals("problem")) { shortcut = KeyEvent.VK_6; }
        else if (mode.equals("previous")) { shortcut = KeyEvent.VK_8; }
        else if (mode.equals("next")) { shortcut = KeyEvent.VK_9; }
        // CHECKSTYLE.ON: LeftCurly
        // CHECKSTYLE.ON: RightCurly

        return shortcut;
    }

    /**
     * Constructs a new {@code AutoScaleAction}.
     * @param mode The autoscale mode (one of {@link AutoScaleMode})
     * @param marker Must be set to false. Used only to differentiate from default constructor
     */
    private AutoScaleAction(AutoScaleMode mode, boolean marker) {
        super(marker);
        this.mode = mode;
    }

    /**
     * Constructs a new {@code AutoScaleAction}.
     * @param mode The autoscale mode (one of {@link AutoScaleAction#MODES})
     * @deprecated Use {@link #AutoScaleAction(AutoScaleMode)} instead
     */
    @Deprecated
    public AutoScaleAction(final String mode) {
        this(AutoScaleMode.of(mode));
    }

    /**
     * Constructs a new {@code AutoScaleAction}.
     * @param mode The autoscale mode (one of {@link AutoScaleAction#MODES})
     * @since 14221
     */
    public AutoScaleAction(final AutoScaleMode mode) {
        super(tr("Zoom to {0}", mode.getLocalizedLabel()), "dialogs/autoscale/" + mode.getEnglishLabel(),
              tr("Zoom the view to {0}.", mode.getLocalizedLabel()),
              Shortcut.registerShortcut("view:zoom" + mode.getEnglishLabel(),
                        tr("View: {0}", tr("Zoom to {0}", mode.getLocalizedLabel())),
                        getModeShortcut(mode.getEnglishLabel()), Shortcut.DIRECT), true, null, false);
        String label = mode.getEnglishLabel();
        String modeHelp = Character.toUpperCase(label.charAt(0)) + label.substring(1);
        putValue("help", "Action/AutoScale/" + modeHelp);
        this.mode = mode;
        switch (mode) {
        case DATA:
            putValue("help", ht("/Action/ZoomToData"));
            break;
        case LAYER:
            putValue("help", ht("/Action/ZoomToLayer"));
            break;
        case SELECTION:
            putValue("help", ht("/Action/ZoomToSelection"));
            break;
        case CONFLICT:
            putValue("help", ht("/Action/ZoomToConflict"));
            break;
        case PROBLEM:
            putValue("help", ht("/Action/ZoomToProblem"));
            break;
        case DOWNLOAD:
            putValue("help", ht("/Action/ZoomToDownload"));
            break;
        case PREVIOUS:
            putValue("help", ht("/Action/ZoomToPrevious"));
            break;
        case NEXT:
            putValue("help", ht("/Action/ZoomToNext"));
            break;
        default:
            throw new IllegalArgumentException("Unknown mode: " + mode);
        }
        installAdapters();
    }

    /**
     * Performs this auto scale operation for the mode this action is in.
     */
    public void autoScale() {
        if (MainApplication.isDisplayingMapView()) {
            MapView mapView = MainApplication.getMap().mapView;
            switch (mode) {
            case PREVIOUS:
                mapView.zoomPrevious();
                break;
            case NEXT:
                mapView.zoomNext();
                break;
            default:
                BoundingXYVisitor bbox = getBoundingBox();
                if (bbox != null && bbox.getBounds() != null) {
                    mapView.zoomTo(bbox);
                }
            }
        }
        putValue("active", Boolean.TRUE);
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
        if (getLayerManager().getActiveLayer() == null) {
            return null;
        }
        try {
            List<Layer> layers = LayerListDialog.getInstance().getModel().getSelectedLayers();
            if (!layers.isEmpty())
                return layers.get(0);
        } catch (IllegalStateException e) {
            Logging.error(e);
        }
        return null;
    }

    private BoundingXYVisitor getBoundingBox() {
        switch (mode) {
        case PROBLEM:
            return modeProblem(new ValidatorBoundingXYVisitor());
        case DATA:
            return modeData(new BoundingXYVisitor());
        case LAYER:
            return modeLayer(new BoundingXYVisitor());
        case SELECTION:
        case CONFLICT:
            return modeSelectionOrConflict(new BoundingXYVisitor());
        case DOWNLOAD:
            return modeDownload(new BoundingXYVisitor());
        default:
            return new BoundingXYVisitor();
        }
    }

    private static BoundingXYVisitor modeProblem(ValidatorBoundingXYVisitor v) {
        TestError error = MainApplication.getMap().validatorDialog.getSelectedError();
        if (error == null)
            return null;
        v.visit(error);
        if (v.getBounds() == null)
            return null;
        v.enlargeBoundingBox(Config.getPref().getDouble("validator.zoom-enlarge-bbox", 0.0002));
        return v;
    }

    private static BoundingXYVisitor modeData(BoundingXYVisitor v) {
        for (Layer l : MainApplication.getLayerManager().getLayers()) {
            l.visitBoundingBox(v);
        }
        return v;
    }

    private BoundingXYVisitor modeLayer(BoundingXYVisitor v) {
        // try to zoom to the first selected layer
        Layer l = getFirstSelectedLayer();
        if (l == null)
            return null;
        l.visitBoundingBox(v);
        return v;
    }

    private BoundingXYVisitor modeSelectionOrConflict(BoundingXYVisitor v) {
        Collection<IPrimitive> sel = new HashSet<>();
        if (AutoScaleMode.SELECTION == mode) {
            OsmData<?, ?, ?, ?> dataSet = getLayerManager().getActiveData();
            if (dataSet != null) {
                sel.addAll(dataSet.getSelected());
            }
        } else {
            ConflictDialog conflictDialog = MainApplication.getMap().conflictDialog;
            Conflict<? extends IPrimitive> c = conflictDialog.getSelectedConflict();
            if (c != null) {
                sel.add(c.getMy());
            } else if (conflictDialog.getConflicts() != null) {
                sel.addAll(conflictDialog.getConflicts().getMyConflictParties());
            }
        }
        if (sel.isEmpty()) {
            JOptionPane.showMessageDialog(
                    MainApplication.getMainFrame(),
                    AutoScaleMode.SELECTION == mode ? tr("Nothing selected to zoom to.") : tr("No conflicts to zoom to"),
                    tr("Information"),
                    JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        for (IPrimitive osm : sel) {
            osm.accept(v);
        }

        // Increase the bounding box by up to 100% to give more context.
        v.enlargeBoundingBoxLogarithmically(100);
        // Make the bounding box at least 100 meter wide to
        // ensure reasonable zoom level when zooming onto single nodes.
        v.enlargeToMinSize(Config.getPref().getDouble("zoom_to_selection_min_size_in_meter", 100));
        return v;
    }

    private BoundingXYVisitor modeDownload(BoundingXYVisitor v) {
        if (lastZoomTime > 0 &&
                System.currentTimeMillis() - lastZoomTime > Config.getPref().getLong("zoom.bounds.reset.time", TimeUnit.SECONDS.toMillis(10))) {
            lastZoomTime = -1;
        }
        final DataSet dataset = getLayerManager().getActiveDataSet();
        if (dataset != null) {
            List<DataSource> dataSources = new ArrayList<>(dataset.getDataSources());
            int s = dataSources.size();
            if (s > 0) {
                if (lastZoomTime == -1 || lastZoomArea == -1 || lastZoomArea > s) {
                    lastZoomArea = s-1;
                    v.visit(dataSources.get(lastZoomArea).bounds);
                } else if (lastZoomArea > 0) {
                    lastZoomArea -= 1;
                    v.visit(dataSources.get(lastZoomArea).bounds);
                } else {
                    lastZoomArea = -1;
                    Area sourceArea = getLayerManager().getActiveDataSet().getDataSourceArea();
                    if (sourceArea != null) {
                        v.visit(new Bounds(sourceArea.getBounds2D()));
                    }
                }
                lastZoomTime = System.currentTimeMillis();
            } else {
                lastZoomTime = -1;
                lastZoomArea = -1;
            }
        }
        return v;
    }

    @Override
    protected void updateEnabledState() {
        OsmData<?, ?, ?, ?> ds = getLayerManager().getActiveData();
        MapFrame map = MainApplication.getMap();
        switch (mode) {
        case SELECTION:
            setEnabled(ds != null && !ds.selectionEmpty());
            break;
        case LAYER:
            setEnabled(getFirstSelectedLayer() != null);
            break;
        case CONFLICT:
            setEnabled(map != null && map.conflictDialog.getSelectedConflict() != null);
            break;
        case DOWNLOAD:
            setEnabled(ds != null && !ds.getDataSources().isEmpty());
            break;
        case PROBLEM:
            setEnabled(map != null && map.validatorDialog.getSelectedError() != null);
            break;
        case PREVIOUS:
            setEnabled(MainApplication.isDisplayingMapView() && map.mapView.hasZoomUndoEntries());
            break;
        case NEXT:
            setEnabled(MainApplication.isDisplayingMapView() && map.mapView.hasZoomRedoEntries());
            break;
        default:
            setEnabled(!getLayerManager().getLayers().isEmpty());
        }
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        if (AutoScaleMode.SELECTION == mode) {
            setEnabled(selection != null && !selection.isEmpty());
        }
    }

    @Override
    protected final void installAdapters() {
        super.installAdapters();
        // make this action listen to zoom and mapframe change events
        //
        MapView.addZoomChangeListener(new ZoomChangeAdapter());
        MainApplication.addMapFrameListener(new MapFrameAdapter());
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

        MapFrameAdapter() {
            if (AutoScaleMode.CONFLICT == mode) {
                conflictSelectionListener = e -> updateEnabledState();
            } else if (AutoScaleMode.PROBLEM == mode) {
                validatorSelectionListener = e -> updateEnabledState();
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
            updateEnabledState();
        }
    }
}
