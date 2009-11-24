// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Shortcut;

public abstract class AbstractMergeAction extends JosmAction {

    /**
     * the list cell renderer used to render layer list entries
     *
     */
    static public class LayerListCellRenderer extends DefaultListCellRenderer {

        protected boolean isActiveLayer(Layer layer) {
            if (Main.isDisplayingMapView()) return false;
            return Main.map.mapView.getActiveLayer() == layer;
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            Layer layer = (Layer) value;
            JLabel label = (JLabel) super.getListCellRendererComponent(list, layer.getName(), index, isSelected,
                    cellHasFocus);
            Icon icon = layer.getIcon();
            label.setIcon(icon);
            label.setToolTipText(layer.getToolTipText());
            return label;
        }
    }

    public AbstractMergeAction() {
        super();
    }

    public AbstractMergeAction(String name, String iconName, String tooltip, Shortcut shortcut, boolean register) {
        super(name, iconName, tooltip, shortcut, register);
    }

    protected Layer askTargetLayer(List<Layer> targetLayers) {
        JComboBox layerList = new JComboBox();
        layerList.setRenderer(new LayerListCellRenderer());
        layerList.setModel(new DefaultComboBoxModel(targetLayers.toArray()));
        layerList.setSelectedIndex(0);

        JPanel pnl = new JPanel();
        pnl.setLayout(new GridBagLayout());
        pnl.add(new JLabel(tr("Please select the target layer.")), GBC.eol());
        pnl.add(layerList, GBC.eol());

        ExtendedDialog ed = new ExtendedDialog(Main.parent,
                tr("Select target layer"),
                new String[] { tr("Merge"), tr("Cancel") });
        ed.setButtonIcons(new String[] { "dialogs/mergedown", "cancel" });
        ed.setContent(pnl);
        ed.showDialog();
        if (ed.getValue() != 1)
            return null;

        Layer targetLayer = (Layer) layerList.getSelectedItem();
        return targetLayer;
    }

    protected void warnNoTargetLayersForSourceLayer(Layer sourceLayer) {
        JOptionPane.showMessageDialog(Main.parent,
                tr("<html>There are no layers the source layer<br>''{0}''<br>could be merged to.</html>"),
                tr("No target layers"), JOptionPane.WARNING_MESSAGE);
    }
}
