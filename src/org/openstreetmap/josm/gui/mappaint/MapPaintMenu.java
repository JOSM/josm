package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.MapPaintSylesUpdateListener;
import org.openstreetmap.josm.tools.ImageProvider;

public class MapPaintMenu extends JMenu implements MapPaintSylesUpdateListener {

    private static class MapPaintAction extends JosmAction {

        private StyleSource style;
        private JCheckBoxMenuItem button;

        public MapPaintAction(StyleSource style) {
            super(style.getDisplayString(), style.icon,
                    tr("Select the map painting styles"), null, style.icon != null);
            if (style.icon == null) {
                putValue("toolbar", "mappaint/" + style.getDisplayString());
                Main.toolbar.register(this);
            }
            this.button = new JCheckBoxMenuItem(this);
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

        @Override
        public void updateEnabledState() {
            setEnabled(Main.map != null && Main.main.getEditLayer() != null);
        }
    }
    private Map<String, MapPaintAction> actions = new HashMap<String, MapPaintAction>();

    public MapPaintMenu() {
        super(tr("Map Paint Styles"));
        setIcon(ImageProvider.get("dialogs", "mapstyle"));
        MapPaintStyles.addMapPaintSylesUpdateListener(this);
    }

    @Override
    public void mapPaintStylesUpdated() {
        final Set<String> actionsToRemove = new HashSet<String>(actions.keySet());
        for (StyleSource style : MapPaintStyles.getStyles().getStyleSources()) {
            final String k = style.getDisplayString();
            MapPaintAction a = actions.get(k);
            if (a == null) {
                a = new MapPaintAction(style);
                add(a.getButton());
                actions.put(k, a);
            } else {
                a.updateButton();
                actionsToRemove.remove(k);
            }
        }
        for (String k : actionsToRemove) {
            final MapPaintAction a = actions.get(k);
            if (a != null) {
                remove(a.getButton());
                actions.remove(k);
            }
        }
    }

    @Override
    public void mapPaintStyleEntryUpdated(int idx) {
        mapPaintStylesUpdated();
    }
}
