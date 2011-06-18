// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.visitor.paint.MapRendererFactory;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.data.osm.visitor.paint.WireframeMapRenderer;
import org.openstreetmap.josm.tools.Shortcut;

public class WireframeToggleAction extends JosmAction {
    public WireframeToggleAction() {
        super(
                tr("Wireframe View"),
                null, /* no icon */
                tr("Enable/disable rendering the map as wireframe only"),
                Shortcut.registerShortcut("menu:view:wireframe", tr("Toggle Wireframe view"),KeyEvent.VK_W, Shortcut.GROUP_MENU),
                false /* register toolbar */
        );
        putValue("toolbar", "wireframe");
        Main.toolbar.register(this);
        putValue(SELECTED_KEY, MapRendererFactory.getInstance().isWireframeMapRendererActive());
    }

    public void toggleSelectedState() {
        boolean selected = (Boolean)getValue(SELECTED_KEY);

        if (selected){
            MapRendererFactory.getInstance().activate(WireframeMapRenderer.class);
        } else {
            MapRendererFactory.getInstance().activate(StyledMapRenderer.class);
        }
        if (Main.map != null) {
            Main.map.mapView.repaint();
        }
    }

    public void actionPerformed(ActionEvent e) {
        toggleSelectedState();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(Main.main.getEditLayer() != null);
    }
}
