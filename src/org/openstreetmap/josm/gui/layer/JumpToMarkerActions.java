// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.lang.ref.WeakReference;
import java.util.List;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.tools.MultikeyActionsHandler;
import org.openstreetmap.josm.tools.MultikeyShortcutAction;
import org.openstreetmap.josm.tools.Shortcut;

public class JumpToMarkerActions {

    public interface JumpToMarkerLayer {
        void jumpToNextMarker();
        void jumpToPreviousMarker();
    }

    public static void initialize() {
        MultikeyActionsHandler.getInstance().addAction(new JumpToNextMarker(null));
        MultikeyActionsHandler.getInstance().addAction(new JumpToPreviousMarker(null));
    }

    public static final class JumpToNextMarker extends AbstractAction implements MultikeyShortcutAction {

        private final Layer layer;
        private WeakReference<Layer> lastLayer;

        public JumpToNextMarker(JumpToMarkerLayer layer) {
            putValue(ACCELERATOR_KEY, Shortcut.registerShortcut("core_multikey:nextMarker", "", 'J', Shortcut.GROUP_DIRECT, KeyEvent.ALT_DOWN_MASK + KeyEvent.CTRL_DOWN_MASK).getKeyStroke());
            putValue(SHORT_DESCRIPTION, tr("Jump to next marker"));
            putValue(NAME, tr("Jump to next marker"));

            this.layer = (Layer)layer;
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
                    execute(l);
                }
            } else if (repeat && lastLayer != null) {
                l = lastLayer.get();
                if (LayerListDialog.isLayerValid(l)) {
                    execute(l);
                }
            }
        }


        private void execute(Layer l) {
            ((JumpToMarkerLayer)l).jumpToNextMarker();
            lastLayer = new WeakReference<Layer>(l);
        }

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

    public static final class JumpToPreviousMarker extends AbstractAction implements MultikeyShortcutAction {

        private WeakReference<Layer> lastLayer;
        private final Layer layer;

        public JumpToPreviousMarker(JumpToMarkerLayer layer) {
            this.layer = (Layer)layer;

            putValue(ACCELERATOR_KEY, Shortcut.registerShortcut("core_multikey:previousMarker", "", 'P', Shortcut.GROUP_DIRECT, KeyEvent.ALT_DOWN_MASK + KeyEvent.CTRL_DOWN_MASK).getKeyStroke());
            putValue(SHORT_DESCRIPTION, tr("Jump to previous marker"));
            putValue(NAME, tr("Jump to previous marker"));
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
                    execute(l);
                }
            } else if (repeat && lastLayer != null) {
                l = lastLayer.get();
                if (LayerListDialog.isLayerValid(l)) {
                    execute(l);
                }
            }
        }

        private void execute(Layer l) {
            ((JumpToMarkerLayer) l).jumpToPreviousMarker();
            lastLayer = new WeakReference<Layer>(l);
        }

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


}
