// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
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
     * <code>TargetLayerDialogResult</code> returned by {@link #askTargetLayer}
     * containing the selectedTargetLayer and whether the checkbox is ticked
     * @param <T> type of layer
     * @since 14338
     */
    public static class TargetLayerDialogResult<T extends Layer> {
        /**
         * The selected target layer of type T
         */
        public T selectedTargetLayer;
        /**
         * Whether the checkbox is ticked
         */
        public boolean checkboxTicked;

        /**
         * Constructs a new {@link TargetLayerDialogResult}
         */
        public TargetLayerDialogResult() {
        }

        /**
         * Constructs a new {@link TargetLayerDialogResult}
         * @param sel the selected target layer of type T
         */
        public TargetLayerDialogResult(T sel) {
            selectedTargetLayer = sel;
        }

        /**
         * Constructs a new {@link TargetLayerDialogResult}
         * @param sel the selected target layer of type T
         * @param ch whether the checkbox was ticked
         */
        public TargetLayerDialogResult(T sel, boolean ch) {
            selectedTargetLayer = sel;
            checkboxTicked = ch;
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
    protected AbstractMergeAction(String name, String iconName, String tooltip, Shortcut shortcut, boolean register) {
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
    protected AbstractMergeAction(String name, String iconName, String tooltip, Shortcut shortcut,
    boolean register, String toolbar, boolean installAdapters) {
        super(name, iconName, tooltip, shortcut, register, toolbar, installAdapters);
    }

    /**
     * Ask user to choose the target layer and shows a checkbox.
     * @param targetLayers list of candidate target layers.
     * @param showCheckbox whether the checkbox is shown
     * @param checkbox The text of the checkbox shown to the user.
     * @param checkboxDefault whether the checkbox is ticked by default
     * @param buttonText text of button used to select target layer
     * @return The {@link TargetLayerDialogResult} containing the chosen target layer and the state of the checkbox
     * @since 15450
     */
    protected static TargetLayerDialogResult<Layer> askTargetLayer(List<? extends Layer> targetLayers, boolean showCheckbox,
            String checkbox, boolean checkboxDefault, String buttonText) {
        return askTargetLayer(targetLayers.toArray(new Layer[0]),
                tr("Please select the target layer."), checkbox,
                tr("Select target layer"),
                buttonText, "dialogs/mergedown", showCheckbox, checkboxDefault);
    }

    /**
     * Ask user to choose the target layer.
     * @param <T> type of layer
     * @param targetLayers array of proposed target layers
     * @param label label displayed in dialog
     * @param title title of dialog
     * @param buttonText text of button used to select target layer
     * @param buttonIcon icon name of button used to select target layer
     * @return chosen target layer
     */
    public static <T extends Layer> T askTargetLayer(T[] targetLayers, String label, String title, String buttonText, String buttonIcon) {
        return askTargetLayer(targetLayers, label, null, title, buttonText, buttonIcon, false, false).selectedTargetLayer;
    }

    /**
     * Ask user to choose the target layer. Can show a checkbox.
     * @param <T> type of layer
     * @param targetLayers array of proposed target layers
     * @param label label displayed in dialog
     * @param checkbox text of the checkbox displayed
     * @param title title of dialog
     * @param buttonText text of button used to select target layer
     * @param buttonIcon icon name of button used to select target layer
     * @param showCheckbox whether the checkbox is shown
     * @param checkboxDefault whether the checkbox is ticked by default
     * @return The {@link TargetLayerDialogResult} containing the chosen target layer and the state of the checkbox
     * @since 14338
     */
    @SuppressWarnings("unchecked")
    public static <T extends Layer> TargetLayerDialogResult<T> askTargetLayer(T[] targetLayers, String label, String checkbox, String title,
            String buttonText, String buttonIcon, boolean showCheckbox, boolean checkboxDefault) {
        JosmComboBox<T> layerList = new JosmComboBox<>(targetLayers);
        layerList.setRenderer(new LayerListCellRenderer());
        layerList.setSelectedIndex(0);

        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.add(new JLabel(label), GBC.eol());
        pnl.add(layerList, GBC.eol().fill(GBC.HORIZONTAL));

        JCheckBox cb = null;
        if (showCheckbox) {
            cb = new JCheckBox(checkbox);
            cb.setSelected(checkboxDefault);
            pnl.add(cb, GBC.eol());
        }

        ExtendedDialog ed = new ExtendedDialog(MainApplication.getMainFrame(), title, buttonText, tr("Cancel"));
        ed.setButtonIcons(buttonIcon, "cancel");
        ed.setContent(pnl);
        ed.showDialog();
        if (ed.getValue() != 1) {
            return new TargetLayerDialogResult<>();
        }
        return new TargetLayerDialogResult<>((T) layerList.getSelectedItem(), cb != null && cb.isSelected());
    }

    /**
     * Warns user when there no layers the source layer could be merged to.
     * @param sourceLayer source layer
     */
    protected void warnNoTargetLayersForSourceLayer(Layer sourceLayer) {
        String message = tr("<html>There are no layers the source layer<br>''{0}''<br>could be merged to.</html>",
                Utils.escapeReservedCharactersHTML(sourceLayer.getName()));
        JOptionPane.showMessageDialog(MainApplication.getMainFrame(), message, tr("No target layers"), JOptionPane.WARNING_MESSAGE);
    }
}
