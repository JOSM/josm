// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.data.osm.visitor.paint.MapRendererFactory;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledTiledMapRenderer;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * This class enables and disables tiled rendering mode.
 * This is intended to be short-term until the tiled rendering
 * has no significant issues at high zoom levels.
 * @since 19176
 */
public class TiledRenderToggleAction extends ToggleAction implements ExpertToggleAction.ExpertModeChangeListener {
    /**
     * Create a new action for toggling render methods
     */
    public TiledRenderToggleAction() {
        super(tr("Tiled Rendering"),
                null,
                tr("Enable/disable rendering the map in tiles"),
                Shortcut.registerShortcut("menu:view:tiled", tr("View: {0}", tr("Tiled View")), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE),
                false /* register toolbar */
            );
        setToolbarId("tiledRendering");
        MainApplication.getToolbar().register(this);
        setSelected(false); // Always start disabled (until we are confident in the renderer)
        if (MapRendererFactory.getInstance().isMapRendererActive(StyledTiledMapRenderer.class)) {
            MapRendererFactory.getInstance().activate(StyledMapRenderer.class);
        }
        ExpertToggleAction.addExpertModeChangeListener(this, true);
    }

    @Override
    protected boolean listenToSelectionChange() {
        return false;
    }

    @Override
    public void expertChanged(boolean isExpert) {
        this.updateEnabledState();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getLayerManager().getActiveData() != null && ExpertToggleAction.isExpert());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        toggleSelectedState(e);
        if (isSelected()) {
            MapRendererFactory.getInstance().activate(StyledTiledMapRenderer.class);
        } else {
            MapRendererFactory.getInstance().activate(StyledMapRenderer.class);
        }

        notifySelectedState();
        getLayerManager().getLayersOfType(OsmDataLayer.class).forEach(OsmDataLayer::invalidate);
    }
}
