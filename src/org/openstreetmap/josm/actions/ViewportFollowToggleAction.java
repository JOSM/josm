// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.actions.mapmode.DrawAction;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * This action toggles automatic moving of the map view to last placed node
 * @since 3837
 */
public class ViewportFollowToggleAction extends ToggleAction {

    /**
     * Defines if a notification should be displayed after enabling and disabling
     */
    public static final BooleanProperty PROP_NOTIFICATION = new BooleanProperty("viewportfollow.notification", true);

    /**
     * Constructs a new {@code ViewportFollowToggleAction}.
     */
    public ViewportFollowToggleAction() {
        super(tr("Viewport Following"),
              "viewport-follow",
              tr("Enable/disable automatic moving of the map view to last placed node"),
              Shortcut.registerShortcut("menu:view:viewportfollow", tr("Toggle Viewport Following"),
              KeyEvent.VK_F, Shortcut.CTRL_SHIFT),
              true /* register shortcut */
        );
        setHelpId(ht("/Action/ViewportFollowing"));
        setSelected(DrawAction.VIEWPORT_FOLLOWING.get());
        notifySelectedState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        toggleSelectedState(e);
        DrawAction.VIEWPORT_FOLLOWING.put(isSelected());
        if (!getShortcut().getKeyText().isEmpty() && PROP_NOTIFICATION.get()) {
            String msg = isSelected()
                    ? tr("Viewport following is enabled, press {0} to disable it", getShortcut().getKeyText())
                    : tr("Viewport following is disabled");
            GuiHelper.runInEDT(() -> new Notification(msg).show());
        }
        notifySelectedState();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getLayerManager().getEditDataSet() != null);
    }
}
