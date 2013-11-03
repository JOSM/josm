// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JColorChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.layer.Layer.LayerAction;
import org.openstreetmap.josm.gui.layer.Layer.MultiLayerAction;
import org.openstreetmap.josm.tools.ImageProvider;

public class CustomizeColor extends AbstractAction implements LayerAction, MultiLayerAction {
    List<Layer> layers;

    public CustomizeColor(List<Layer> l) {
        this();
        layers = l;
    }

    public CustomizeColor(Layer l) {
        this();
        layers = new LinkedList<Layer>();
        layers.add(l);
    }

    private CustomizeColor() {
        super(tr("Customize Color"), ImageProvider.get("colorchooser"));
        putValue("help", ht("/Action/LayerCustomizeColor"));
    }

    @Override
    public boolean supportLayers(List<Layer> layers) {
        for(Layer layer: layers) {
            if(layer.getColor(false) == null)
                return false;
        }
        return true;
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
        Color cl=layers.get(0).getColor(false); if (cl==null) cl=Color.gray;
        JColorChooser c = new JColorChooser(cl);
        Object[] options = new Object[]{tr("OK"), tr("Cancel"), tr("Default")};
        int answer = JOptionPane.showOptionDialog(
                Main.parent,
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
            for(Layer layer : layers)
                Main.pref.putColor("layer "+layer.getName(), c.getColor());
            break;
        case 1:
            return;
        case 2:
            for(Layer layer : layers)
                Main.pref.putColor("layer "+layer.getName(), null);
            break;
        }
        Main.map.repaint();
    }
}
