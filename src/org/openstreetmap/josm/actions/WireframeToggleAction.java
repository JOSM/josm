// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.data.osm.visitor.paint.MapRendererFactory;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.data.osm.visitor.paint.WireframeMapRenderer;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * This class toggles the wireframe rendering mode.
 * @since 2530
 */
public class WireframeToggleAction extends ToggleAction {

    /**
     * Constructs a new {@code WireframeToggleAction}.
     */
    public WireframeToggleAction() {
        super(tr("Wireframe View"),
              null, /* no icon */
              tr("Enable/disable rendering the map as wireframe only"),
              Shortcut.registerShortcut("menu:view:wireframe", tr("Toggle Wireframe view"), KeyEvent.VK_W, Shortcut.CTRL),
              false /* register toolbar */
        );
        putValue("toolbar", "wireframe");
        MainApplication.getToolbar().register(this);
        setSelected(MapRendererFactory.getInstance().isWireframeMapRendererActive());
        notifySelectedState();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getLayerManager().getActiveData() != null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        toggleSelectedState(e);
        if (isSelected()) {
            MapRendererFactory.getInstance().activate(WireframeMapRenderer.class);
        } else {
            MapRendererFactory.getInstance().activate(StyledMapRenderer.class);
        }

        notifySelectedState();
        getLayerManager().getLayersOfType(OsmDataLayer.class).forEach(OsmDataLayer::invalidate);
    }
}
