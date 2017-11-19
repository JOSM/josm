// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.Layer.LayerAction;
import org.openstreetmap.josm.gui.layer.Layer.MultiLayerAction;
import org.openstreetmap.josm.gui.preferences.display.GPXSettingsPanel;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * An action that is displayed in the popup menu for the layer to change the drawing of the GPX layer
 */
public class CustomizeDrawingAction extends AbstractAction implements LayerAction, MultiLayerAction {
    private transient List<Layer> layers;

    /**
     * Create a new {@link CustomizeDrawingAction}
     * @param l The layers that should be customized
     */
    public CustomizeDrawingAction(List<Layer> l) {
        this();
        layers = l;
    }

    /**
     * Create a new {@link CustomizeDrawingAction}
     * @param l The layer that should be customized
     */
    public CustomizeDrawingAction(Layer l) {
        this();
        layers = new LinkedList<>();
        layers.add(l);
    }

    private CustomizeDrawingAction() {
        super(tr("Customize track drawing"));
        new ImageProvider("mapmode/addsegment").getResource().attachImageIcon(this, true);
        putValue("help", ht("/Action/GPXLayerCustomizeLineDrawing"));
    }

    @Override
    public boolean supportLayers(List<Layer> layers) {
        for (Layer layer : layers) {
            if (!(layer instanceof GpxLayer)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Component createMenuComponent() {
        return new JMenuItem(this);
    }

    @Override
    public Action getMultiLayerAction(List<Layer> layers) {
        return new CustomizeDrawingAction(layers);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        boolean hasLocal = false;
        boolean hasNonlocal = false;
        for (Layer layer : layers) {
            if (layer instanceof GpxLayer) {
                if (((GpxLayer) layer).isLocalFile()) {
                    hasLocal = true;
                } else {
                    hasNonlocal = true;
                }
            }
        }
        GPXSettingsPanel panel = new GPXSettingsPanel(layers.get(0).getName(), hasLocal, hasNonlocal);
        JScrollPane scrollpane = GuiHelper.embedInVerticalScrollPane(panel);
        scrollpane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        int screenHeight = GuiHelper.getScreenSize().height;
        if (screenHeight < 700) {
            // to fit on screen 800x600
            scrollpane.setPreferredSize(new Dimension(panel.getPreferredSize().width, Math.min(panel.getPreferredSize().height, 450)));
        }
        int answer = JOptionPane.showConfirmDialog(Main.parent, scrollpane, tr("Customize track drawing"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer == JOptionPane.CANCEL_OPTION || answer == JOptionPane.CLOSED_OPTION) {
            return;
        }
        for (Layer layer : layers) {
            // save preferences for all layers
            boolean f = false;
            if (layer instanceof GpxLayer) {
                f = ((GpxLayer) layer).isLocalFile();
            }
            panel.savePreferences(layer.getName(), f);
        }
        MainApplication.getMap().repaint();
    }

}
