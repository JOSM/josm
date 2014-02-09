// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.dialogs.MapPaintDialog;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.MapPaintSylesUpdateListener;
import org.openstreetmap.josm.gui.util.StayOpenCheckBoxMenuItem;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * The View -&gt; Map Paint Styles menu
 * @since 5086
 */
public class MapPaintMenu extends JMenu implements MapPaintSylesUpdateListener {

    private static class MapPaintAction extends JosmAction {

        private StyleSource style;
        private JCheckBoxMenuItem button;

        public MapPaintAction(StyleSource style) {
            super(style.getDisplayString(), style.icon == null ? null : ImageProvider.getIfAvailable(style.icon),
                    tr("Select the map painting styles"), null, true, "mappaint/" + style.getDisplayString(), true);
            this.button = new StayOpenCheckBoxMenuItem(this);
            this.style = style;
            updateButton();
        }

        private void updateButton() {
            button.getModel().setSelected(style.active);
        }

        private void toggleStyle() {
            MapPaintStyles.toggleStyleActive(MapPaintStyles.getStyles().getStyleSources().indexOf(style));
            updateButton();
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            toggleStyle();
        }

        public JCheckBoxMenuItem getButton() {
            return button;
        }

        public void setStyle(StyleSource style) {
            this.style = style;
        }

        @Override
        public void updateEnabledState() {
            setEnabled(Main.isDisplayingMapView() && (Main.main.hasEditLayer() || mapHasGpxorMarkerLayer()));
        }

        private boolean mapHasGpxorMarkerLayer() {
            for (Layer layer : Main.map.mapView.getAllLayers()) {
                if (layer instanceof GpxLayer || layer instanceof MarkerLayer) {
                    return true;
                }
            }
            return false;
        }
    }
    private final Map<String, MapPaintAction> actions = new HashMap<String, MapPaintAction>();

    /**
     * Constructs a new {@code MapPaintMenu}
     */
    public MapPaintMenu() {
        super(tr("Map Paint Styles"));
        setIcon(ImageProvider.get("dialogs", "mapstyle"));
        MapPaintStyles.addMapPaintSylesUpdateListener(this);
    }

    @Override
    public void mapPaintStylesUpdated() {
        removeAll();
        for (StyleSource style : MapPaintStyles.getStyles().getStyleSources()) {
            final String k = style.getDisplayString();
            MapPaintAction a = actions.get(k);
            if (a == null) {
                actions.put(k, a = new MapPaintAction(style));
                add(a.getButton());
            } else {
                a.setStyle(style);
                add(a.getButton());
                a.updateButton();
            }
        }
        addSeparator();
        add(MapPaintDialog.PREFERENCE_ACTION);
    }

    @Override
    public void mapPaintStyleEntryUpdated(int idx) {
        mapPaintStylesUpdated();
    }
}
