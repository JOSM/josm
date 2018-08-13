// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.Layer.LayerAction;
import org.openstreetmap.josm.gui.layer.Layer.MultiLayerAction;
import org.openstreetmap.josm.gui.layer.Layer.SeparatorLayerAction;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Popup menu handler for the layer list.
 */
public class LayerListPopup extends JPopupMenu {

    /**
     * An action that displays the layer information.
     * @see Layer#getInfoComponent()
     */
    public static final class InfoAction extends AbstractAction {
        private final transient Layer layer;

        /**
         * Constructs a new {@code InfoAction} for the given layer.
         * @param layer The layer
         */
        public InfoAction(Layer layer) {
            super(tr("Info"));
            new ImageProvider("info").getResource().attachImageIcon(this, true);
            putValue("help", ht("/Action/LayerInfo"));
            this.layer = layer;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Object object = layer.getInfoComponent();
            if (object instanceof Component) {
                ExtendedDialog ed = new ExtendedDialog(
                        MainApplication.getMainFrame(), tr("Information about layer"),
                        tr("OK"));
                ed.setButtonIcons("ok");
                ed.setIcon(JOptionPane.INFORMATION_MESSAGE);
                ed.setContent((Component) object);
                ed.setResizable(layer.isInfoResizable());
                ed.setMinimumSize(new Dimension(270, 170));
                ed.showDialog();
            } else {
                JOptionPane.showMessageDialog(
                        MainApplication.getMainFrame(), object,
                        tr("Information about layer"),
                        JOptionPane.INFORMATION_MESSAGE
                        );
            }
        }
    }

    /**
     * Constructs a new {@code LayerListPopup}.
     * @param selectedLayers list of selected layers
     */
    public LayerListPopup(List<Layer> selectedLayers) {

        List<Action> actions;
        if (selectedLayers.size() == 1) {
            Action[] entries = selectedLayers.get(0).getMenuEntries();
            actions = entries != null ? Arrays.asList(entries) : Collections.emptyList();
        } else {
            // Very simple algorithm - first selected layer has actions order as in getMenuEntries, actions from other layers go to the end
            actions = new ArrayList<>();
            boolean separatorAdded = true;
            for (Action a: selectedLayers.get(0).getMenuEntries()) {
                if (!separatorAdded && a instanceof SeparatorLayerAction) {
                    separatorAdded = true;
                    actions.add(a);
                } else if (a instanceof LayerAction && ((LayerAction) a).supportLayers(selectedLayers)) {
                    separatorAdded = false;
                    if (a instanceof MultiLayerAction)
                        a = ((MultiLayerAction) a).getMultiLayerAction(selectedLayers);
                    actions.add(a);
                }
            }
            // This will usually add no action, because if some action support all selected layers then it was probably used also in first layer
            for (int i = 1; i < selectedLayers.size(); i++) {
                separatorAdded = false;
                for (Action a: selectedLayers.get(i).getMenuEntries()) {
                    if (a instanceof LayerAction && !(a instanceof MultiLayerAction)
                    && ((LayerAction) a).supportLayers(selectedLayers) && !actions.contains(a)) {
                        if (!separatorAdded) {
                            separatorAdded = true;
                            actions.add(SeparatorLayerAction.INSTANCE);
                        }
                        actions.add(a);
                    }
                }
            }
        }
        if (!actions.isEmpty() && actions.get(actions.size() - 1) instanceof SeparatorLayerAction) {
            actions.remove(actions.size() - 1);
        }
        for (Action a : actions) {
            if (a instanceof LayerAction) {
                add(((LayerAction) a).createMenuComponent());
            } else {
                add(new JMenuItem(a));
            }
        }
    }
}
