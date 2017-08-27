// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Predicate;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.history.History;
import org.openstreetmap.josm.data.osm.history.HistoryDataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.util.WindowGeometry;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;
import org.openstreetmap.josm.tools.bugreport.BugReportExceptionHandler;

/**
 * Manager allowing to show/hide history dialogs.
 * @since 2019
 */
public final class HistoryBrowserDialogManager implements LayerChangeListener {

    static final class UnloadedHistoryPredicate implements Predicate<PrimitiveId> {
        private final HistoryDataSet hds = HistoryDataSet.getInstance();

        @Override
        public boolean test(PrimitiveId p) {
            History h = hds.getHistory(p);
            if (h == null)
                // reload if the history is not in the cache yet
                return true;
            else
                // reload if the history object of the selected object is not in the cache yet
                return !p.isNew() && h.getByVersion(p.getUniqueId()) == null;
        }
    }

    private static final String WINDOW_GEOMETRY_PREF = HistoryBrowserDialogManager.class.getName() + ".geometry";

    private static HistoryBrowserDialogManager instance;

    private final Map<Long, HistoryBrowserDialog> dialogs;

    private final Predicate<PrimitiveId> unloadedHistoryPredicate = new UnloadedHistoryPredicate();

    private final Predicate<PrimitiveId> notNewPredicate = p -> !p.isNew();

    protected HistoryBrowserDialogManager() {
        dialogs = new HashMap<>();
        MainApplication.getLayerManager().addLayerChangeListener(this);
    }

    /**
     * Replies the unique instance.
     * @return the unique instance
     */
    public static synchronized HistoryBrowserDialogManager getInstance() {
        if (instance == null) {
            instance = new HistoryBrowserDialogManager();
        }
        return instance;
    }

    /**
     * Determines if an history dialog exists for the given object id.
     * @param id the object id
     * @return {@code true} if an history dialog exists for the given object id, {@code false} otherwise
     */
    public boolean existsDialog(long id) {
        return dialogs.containsKey(id);
    }

    private void show(long id, HistoryBrowserDialog dialog) {
        if (dialogs.containsValue(dialog)) {
            show(id);
        } else {
            placeOnScreen(dialog);
            dialog.setVisible(true);
            dialogs.put(id, dialog);
        }
    }

    private void show(long id) {
        if (dialogs.containsKey(id)) {
            dialogs.get(id).toFront();
        }
    }

    private boolean hasDialogWithCloseUpperLeftCorner(Point p) {
        for (HistoryBrowserDialog dialog: dialogs.values()) {
            Point corner = dialog.getLocation();
            if (p.x >= corner.x -5 && corner.x + 5 >= p.x
                    && p.y >= corner.y -5 && corner.y + 5 >= p.y)
                return true;
        }
        return false;
    }

    private void placeOnScreen(HistoryBrowserDialog dialog) {
        WindowGeometry geometry = new WindowGeometry(WINDOW_GEOMETRY_PREF, WindowGeometry.centerOnScreen(new Dimension(850, 500)));
        geometry.applySafe(dialog);
        Point p = dialog.getLocation();
        while (hasDialogWithCloseUpperLeftCorner(p)) {
            p.x += 20;
            p.y += 20;
        }
        dialog.setLocation(p);
    }

    /**
     * Hides the specified history dialog and cleans associated resources.
     * @param dialog History dialog to hide
     */
    public void hide(HistoryBrowserDialog dialog) {
        for (Iterator<Entry<Long, HistoryBrowserDialog>> it = dialogs.entrySet().iterator(); it.hasNext();) {
            if (Objects.equals(it.next().getValue(), dialog)) {
                it.remove();
                if (dialogs.isEmpty()) {
                    new WindowGeometry(dialog).remember(WINDOW_GEOMETRY_PREF);
                }
                break;
            }
        }
        dialog.setVisible(false);
        dialog.dispose();
    }

    /**
     * Hides and destroys all currently visible history browser dialogs
     *
     */
    public void hideAll() {
        List<HistoryBrowserDialog> dialogs = new ArrayList<>();
        dialogs.addAll(this.dialogs.values());
        for (HistoryBrowserDialog dialog: dialogs) {
            dialog.unlinkAsListener();
            hide(dialog);
        }
    }

    /**
     * Show history dialog for the given history.
     * @param h History to show
     */
    public void show(History h) {
        if (h == null)
            return;
        if (existsDialog(h.getId())) {
            show(h.getId());
        } else {
            HistoryBrowserDialog dialog = new HistoryBrowserDialog(h);
            show(h.getId(), dialog);
        }
    }

    /* ----------------------------------------------------------------------------- */
    /* LayerChangeListener                                                           */
    /* ----------------------------------------------------------------------------- */
    @Override
    public void layerAdded(LayerAddEvent e) {
        // Do nothing
    }

    @Override
    public void layerRemoving(LayerRemoveEvent e) {
        // remove all history browsers if the number of layers drops to 0
        if (e.getSource().getLayers().isEmpty()) {
            hideAll();
        }
    }

    @Override
    public void layerOrderChanged(LayerOrderChangeEvent e) {
        // Do nothing
    }

    /**
     * Show history dialog(s) for the given primitive(s).
     * @param primitives The primitive(s) for which history will be displayed
     */
    public void showHistory(final Collection<? extends PrimitiveId> primitives) {
        final Collection<? extends PrimitiveId> notNewPrimitives = SubclassFilteredCollection.filter(primitives, notNewPredicate);
        if (notNewPrimitives.isEmpty()) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("Please select at least one already uploaded node, way, or relation."),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Collection<? extends PrimitiveId> toLoad = SubclassFilteredCollection.filter(primitives, unloadedHistoryPredicate);
        if (!toLoad.isEmpty()) {
            HistoryLoadTask task = new HistoryLoadTask();
            for (PrimitiveId p : notNewPrimitives) {
                task.add(p);
            }
            MainApplication.worker.submit(task);
        }

        Runnable r = () -> {
            try {
                for (PrimitiveId p : notNewPrimitives) {
                    final History h = HistoryDataSet.getInstance().getHistory(p);
                    if (h == null) {
                        continue;
                    }
                    SwingUtilities.invokeLater(() -> show(h));
                }
            } catch (final JosmRuntimeException | IllegalArgumentException | IllegalStateException e) {
                BugReportExceptionHandler.handleException(e);
            }
        };
        MainApplication.worker.submit(r);
    }
}
