// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JColorChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.layer.Layer.LayerAction;
import org.openstreetmap.josm.gui.layer.Layer.MultiLayerAction;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Action to show a dialog for picking a color.
 *
 * By calling this action, the user can choose a color to customize the painting
 * of a certain {@link GpxLayer} or {@link org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer}.
 */
public class CustomizeColor extends AbstractAction implements LayerAction, MultiLayerAction {
    private final transient List<Layer> colorLayers;

    /**
     * Constructs a new {@code CustomizeColor} for a given list of layers.
     * @param l list of layers
     */
    public CustomizeColor(List<Layer> l) {
        super(tr("Customize Color"));
        new ImageProvider("colorchooser").getResource().attachImageIcon(this, true);
        colorLayers = l.stream().filter(Objects::nonNull).filter(Layer::hasColor).collect(Collectors.toList());
        putValue("help", ht("/Action/LayerCustomizeColor"));
    }

    /**
     * Constructs a new {@code CustomizeColor} for a single layer.
     * @param l layer
     */
    public CustomizeColor(Layer l) {
        this(Collections.singletonList(l));
    }

    @Override
    public boolean supportLayers(List<Layer> layers) {
        return layers.stream().allMatch(Layer::hasColor);
    }

    @Override
    public Component createMenuComponent() {
        return new JMenuItem(this);
    }

    @Override
    public Action getMultiLayerAction(List<Layer> layers) {
        return new CustomizeColor(layers);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Color cl = colorLayers.stream().filter(Objects::nonNull).map(Layer::getColor).filter(Objects::nonNull).findAny().orElse(Color.GRAY);
        JColorChooser c = new JColorChooser(cl);
        Object[] options = {tr("OK"), tr("Cancel"), tr("Default")};
        int answer = JOptionPane.showOptionDialog(
                MainApplication.getMainFrame(),
                c,
                tr("Choose a color"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]
        );
        switch (answer) {
        case 0:
            colorLayers.forEach(l -> l.setColor(c.getColor()));
            break;
        case 1:
            return;
        case 2:
            colorLayers.forEach(l -> l.setColor(null));
            break;
        }
        // TODO: Make the layer dialog listen to property change events so that this is not needed any more.
        LayerListDialog.getInstance().repaint();
    }
}
