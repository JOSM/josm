// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.lang.ref.WeakReference;
import java.util.List;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.util.MultikeyActionsHandler;
import org.openstreetmap.josm.gui.util.MultikeyShortcutAction;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Manages actions to jump from one marker to the next for layers that show markers
 * ({@link org.openstreetmap.josm.gui.layer.geoimage.GeoImageLayer},
 * {@link org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer}).
 *
 * Registers global multi-key shortcuts and offers actions for the right-click menu of
 * the layers.
 */
public final class JumpToMarkerActions {

    /**
     * Interface for a layer that displays markers and supports jumping from
     * one marker to the next.
     */
    public interface JumpToMarkerLayer {
        /**
         * Jump (move the viewport) to the next marker.
         */
        void jumpToNextMarker();

        /**
         * Jump (move the viewport) to the previous marker.
         */
        void jumpToPreviousMarker();
    }

    private JumpToMarkerActions() {
        // Hide default constructor for utils classes
    }

    private static volatile JumpToNextMarker jumpToNextMarkerAction;
    private static volatile JumpToPreviousMarker jumpToPreviousMarkerAction;

    /**
     * Initialize the actions, register shortcuts.
     */
    public static void initialize() {
        jumpToNextMarkerAction = new JumpToNextMarker(null);
        jumpToPreviousMarkerAction = new JumpToPreviousMarker(null);
        MultikeyActionsHandler.getInstance().addAction(jumpToNextMarkerAction);
        MultikeyActionsHandler.getInstance().addAction(jumpToPreviousMarkerAction);
    }

    /**
     * Unregister the actions.
     */
    public static void unregisterActions() {
        MultikeyActionsHandler.getInstance().removeAction(jumpToNextMarkerAction);
        MultikeyActionsHandler.getInstance().removeAction(jumpToPreviousMarkerAction);
    }

    private abstract static class JumpToMarker extends AbstractAction implements MultikeyShortcutAction {

        private final transient JumpToMarkerLayer layer;
        private final transient Shortcut multikeyShortcut;
        private transient WeakReference<Layer> lastLayer;

        JumpToMarker(JumpToMarkerLayer layer, Shortcut shortcut) {
            this.layer = layer;
            this.multikeyShortcut = shortcut;
            this.multikeyShortcut.setAccelerator(this);
        }

        protected final void setLastLayer(Layer l) {
            lastLayer = new WeakReference<>(l);
        }

        @Override
        public Shortcut getMultikeyShortcut() {
            return multikeyShortcut;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            execute(layer);
        }

        @Override
        public void executeMultikeyAction(int index, boolean repeat) {
            Layer l = LayerListDialog.getLayerForIndex(index);
            if (l != null) {
                if (l instanceof JumpToMarkerLayer) {
                    execute((JumpToMarkerLayer) l);
                }
            } else if (repeat && lastLayer != null) {
                l = lastLayer.get();
                if (LayerListDialog.isLayerValid(l) && l instanceof JumpToMarkerLayer) {
                    execute((JumpToMarkerLayer) l);
                }
            }
        }

        protected abstract void execute(JumpToMarkerLayer l);

        @Override
        public List<MultikeyInfo> getMultikeyCombinations() {
            return LayerListDialog.getLayerInfoByClass(JumpToMarkerLayer.class);
        }

        @Override
        public MultikeyInfo getLastMultikeyAction() {
            if (lastLayer != null)
                return LayerListDialog.getLayerInfo(lastLayer.get());
            else
                return null;
        }
    }

    public static final class JumpToNextMarker extends JumpToMarker {

        public JumpToNextMarker(JumpToMarkerLayer layer) {
            super(layer, Shortcut.registerShortcut("core_multikey:nextMarker", tr("Multikey: {0}", tr("Next marker")),
                    KeyEvent.VK_J, Shortcut.ALT_CTRL));
            putValue(SHORT_DESCRIPTION, tr("Jump to next marker"));
            putValue(NAME, tr("Jump to next marker"));
        }

        @Override
        protected void execute(JumpToMarkerLayer l) {
            l.jumpToNextMarker();
            setLastLayer((Layer) l);
        }
    }

    public static final class JumpToPreviousMarker extends JumpToMarker {

        public JumpToPreviousMarker(JumpToMarkerLayer layer) {
            super(layer, Shortcut.registerShortcut("core_multikey:previousMarker", tr("Multikey: {0}", tr("Previous marker")),
                    KeyEvent.VK_P, Shortcut.ALT_CTRL));
            putValue(SHORT_DESCRIPTION, tr("Jump to previous marker"));
            putValue(NAME, tr("Jump to previous marker"));
        }

        @Override
        protected void execute(JumpToMarkerLayer l) {
            l.jumpToPreviousMarker();
            setLastLayer((Layer) l);
        }
    }
}
