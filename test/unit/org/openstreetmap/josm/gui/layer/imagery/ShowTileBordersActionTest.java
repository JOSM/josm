// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.imagery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JCheckBoxMenuItem;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.gui.layer.TMSLayerTest;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Unit tests of {@link ShowTileBordersAction}.
 */
@Main
@Projection
class ShowTileBordersActionTest {

    @AfterEach
    void resetPreferences() {
        Config.getPref().put("imagery.generic.default_showtileborders", null);
    }

    /**
     * Toggling the tile borders remembers the choice, so that a layer created later - in this session or after
     * a restart - shows them too. Non-regression test for the setting being forgotten between sessions.
     */
    @Test
    void testTogglePersistsTheChoice() {
        TMSLayer layer = TMSLayerTest.createTmsLayer();
        assertFalse(layer.getDisplaySettings().isShowTileBorders());
        ShowTileBordersAction action = new ShowTileBordersAction(layer);

        action.actionPerformed(new ActionEvent(this, 0, ""));
        assertTrue(layer.getDisplaySettings().isShowTileBorders());
        assertTrue(TileSourceDisplaySettings.PROP_SHOW_TILE_BORDERS.get());
        // a layer created afterwards picks the choice up, which is what happens after a restart
        assertTrue(TMSLayerTest.createTmsLayer().getDisplaySettings().isShowTileBorders());
        assertTrue(new JCheckBoxMenuItem(action).getModel().isEnabled());
        assertTrue(((JCheckBoxMenuItem) action.createMenuComponent()).isSelected());

        action.actionPerformed(new ActionEvent(this, 0, ""));
        assertFalse(layer.getDisplaySettings().isShowTileBorders());
        assertFalse(TileSourceDisplaySettings.PROP_SHOW_TILE_BORDERS.get());
        assertFalse(TMSLayerTest.createTmsLayer().getDisplaySettings().isShowTileBorders());
        assertFalse(((JCheckBoxMenuItem) action.createMenuComponent()).isSelected());
    }

    /**
     * Loading a session applies the setting to that layer only, it must not change the remembered default.
     */
    @Test
    void testSessionDoesNotOverwriteThePreference() {
        TMSLayer layer = TMSLayerTest.createTmsLayer();
        Map<String, String> session = new HashMap<>(Collections.singletonMap("show-tile-borders", "true"));
        layer.getDisplaySettings().applyFromPropertiesMap(session);

        assertTrue(layer.getDisplaySettings().isShowTileBorders());
        assertFalse(TileSourceDisplaySettings.PROP_SHOW_TILE_BORDERS.get());
        assertFalse(TMSLayerTest.createTmsLayer().getDisplaySettings().isShowTileBorders());
    }
}
