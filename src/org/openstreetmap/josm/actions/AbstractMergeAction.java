// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * Abstract superclass of different "Merge" actions.
 * @since 1890
 */
public abstract class AbstractMergeAction extends JosmAction {

    /**
     * the list cell renderer used to render layer list entries
     */
    public static class LayerListCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Layer layer = (Layer) value;
            JLabel label = (JLabel) super.getListCellRendererComponent(list, layer.getName(), index, isSelected, cellHasFocus);
            label.setIcon(layer.getIcon());
            label.setToolTipText(layer.getToolTipText());
            return label;
        }
    }

    /**
     * Constructs a new {@code AbstractMergeAction}.
     * @param name the action's text as displayed on the menu (if it is added to a menu)
     * @param iconName the filename of the icon to use
     * @param tooltip  a longer description of the action that will be displayed in the tooltip. Please note
     *           that html is not supported for menu actions on some platforms.
     * @param shortcut a ready-created shortcut object or null if you don't want a shortcut. But you always
     *            do want a shortcut, remember you can always register it with group=none, so you
     *            won't be assigned a shortcut unless the user configures one. If you pass null here,
     *            the user CANNOT configure a shortcut for your action.
     * @param register register this action for the toolbar preferences?
     */
    public AbstractMergeAction(String name, String iconName, String tooltip, Shortcut shortcut, boolean register) {
        super(name, iconName, tooltip, shortcut, register);
    }

    /**
     * Constructs a new {@code AbstractMergeAction}.
     * @param name the action's text as displayed on the menu (if it is added to a menu)
     * @param iconName the filename of the icon to use
     * @param tooltip  a longer description of the action that will be displayed in the tooltip. Please note
     *           that html is not supported for menu actions on some platforms.
     * @param shortcut a ready-created shortcut object or null if you don't want a shortcut. But you always
     *            do want a shortcut, remember you can always register it with group=none, so you
     *            won't be assigned a shortcut unless the user configures one. If you pass null here,
     *            the user CANNOT configure a shortcut for your action.
     * @param register register this action for the toolbar preferences?
     * @param toolbar identifier for the toolbar preferences. The iconName is used, if this parameter is null
     * @param installAdapters false, if you don't want to install layer changed and selection changed adapters
     */
    public AbstractMergeAction(String name, String iconName, String tooltip, Shortcut shortcut,
    boolean register, String toolbar, boolean installAdapters) {
        super(name, iconName, tooltip, shortcut, register, toolbar, installAdapters);
    }

    /**
     * Ask user to choose the target layer.
     * @param targetLayers list of candidate target layers.
     * @return the chosen layer
     */
    protected static Layer askTargetLayer(List<Layer> targetLayers) {
        return askTargetLayer(targetLayers.toArray(new Layer[targetLayers.size()]),
                tr("Please select the target layer."),
                tr("Select target layer"),
                tr("Merge"), "dialogs/mergedown");
    }

    /**
     * Asks a target layer.
     * @param <T> type of layer
     * @param targetLayers array of proposed target layers
     * @param label label displayed in dialog
     * @param title title of dialog
     * @param buttonText text of button used to select target layer
     * @param buttonIcon icon name of button used to select target layer
     * @return choosen target layer
     */
    @SuppressWarnings("unchecked")
    public static <T extends Layer> T askTargetLayer(T[] targetLayers, String label, String title, String buttonText, String buttonIcon) {
        JosmComboBox<T> layerList = new JosmComboBox<>(targetLayers);
        layerList.setRenderer(new LayerListCellRenderer());
        layerList.setSelectedIndex(0);

        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.add(new JLabel(label), GBC.eol());
        pnl.add(layerList, GBC.eol().fill(GBC.HORIZONTAL));
        if (GraphicsEnvironment.isHeadless()) {
            // return first layer in headless mode, for unit tests
            return targetLayers[0];
        }
        ExtendedDialog ed = new ExtendedDialog(Main.parent, title, buttonText, tr("Cancel"));
        ed.setButtonIcons(buttonIcon, "cancel");
        ed.setContent(pnl);
        ed.showDialog();
        if (ed.getValue() != 1) {
            return null;
        }
        return (T) layerList.getSelectedItem();
    }

    /**
     * Warns user when there no layers the source layer could be merged to.
     * @param sourceLayer source layer
     */
    protected void warnNoTargetLayersForSourceLayer(Layer sourceLayer) {
        String message = tr("<html>There are no layers the source layer<br>''{0}''<br>could be merged to.</html>",
                Utils.escapeReservedCharactersHTML(sourceLayer.getName()));
        if (!GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(Main.parent, message, tr("No target layers"), JOptionPane.WARNING_MESSAGE);
        }
    }
}
