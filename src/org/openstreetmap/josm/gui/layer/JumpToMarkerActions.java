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

public final class JumpToMarkerActions {

    public interface JumpToMarkerLayer {
        void jumpToNextMarker();
        void jumpToPreviousMarker();
    }
    
    private JumpToMarkerActions() {
        // Hide default constructor for utils classes
    }

    private static JumpToNextMarker jumpToNextMarkerAction;
    private static JumpToPreviousMarker jumpToPreviousMarkerAction;

    public static void initialize() {
        jumpToNextMarkerAction = new JumpToNextMarker(null);
        jumpToPreviousMarkerAction = new JumpToPreviousMarker(null);
        MultikeyActionsHandler.getInstance().addAction(jumpToNextMarkerAction);
        MultikeyActionsHandler.getInstance().addAction(jumpToPreviousMarkerAction);
    }

    public static void unregisterActions() {
        MultikeyActionsHandler.getInstance().removeAction(jumpToNextMarkerAction);
        MultikeyActionsHandler.getInstance().removeAction(jumpToPreviousMarkerAction);
    }

    private static abstract class JumpToMarker extends AbstractAction implements MultikeyShortcutAction {

        private final Layer layer;
        private final Shortcut multikeyShortcut;
        private WeakReference<Layer> lastLayer;
        
        public JumpToMarker(JumpToMarkerLayer layer, Shortcut shortcut) {
            this.layer = (Layer) layer;
            this.multikeyShortcut = shortcut;
            this.multikeyShortcut.setAccelerator(this);
        }
        
        protected final void setLastLayer(Layer l) {
            lastLayer = new WeakReference<Layer>(l);
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
                    execute(l);
                }
            } else if (repeat && lastLayer != null) {
                l = lastLayer.get();
                if (LayerListDialog.isLayerValid(l)) {
                    execute(l);
                }
            }
        }

        protected abstract void execute(Layer l);

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
        protected void execute(Layer l) {
            ((JumpToMarkerLayer)l).jumpToNextMarker();
            setLastLayer(l);
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
        protected void execute(Layer l) {
            ((JumpToMarkerLayer) l).jumpToPreviousMarker();
            setLastLayer(l);
        }
    }
}
