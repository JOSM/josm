// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import java.awt.event.InputEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.tools.Destroyable;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Base class helper for all Actions in JOSM. Just to make the life easier.
 *
 * destroy() from interface Destroyable is called e.g. for MapModes, when the last layer has
 * been removed and so the mapframe will be destroyed. For other JosmActions, destroy() may never
 * be called (currently).
 *
 * @author imi
 */
abstract public class JosmAction extends AbstractAction implements Destroyable {

    protected Shortcut sc;

    public Shortcut getShortcut() {
        if (sc == null) {
            sc = Shortcut.registerShortcut("core:none", "No Shortcut", 0, Shortcut.GROUP_NONE);
            sc.setAutomatic(); // as this shortcut is shared by all action that don't want to have a shortcut,
                               // we shouldn't allow the user to change it...
        }
        return sc;
    }

    /**
     * The new super for all actions.
     *
     * Use this super constructor to setup your action. It takes 5 parameters:
     *
     * name - the action's text as displayed on the menu (if it is added to a menu)
     * iconName - the filename of the icon to use
     * tooltip - a longer description of the action that will be displayed in the tooltip. Please note
     *           that html is not supported for menu action on some platforms
     * shortcut - a ready-created shortcut object or null if you don't want a shortcut. But you always
     *            do want a shortcut, remember you can alway register it with group=none, so you
     *            won't be assigned a shurtcut unless the user configures one. If you pass null here,
     *            the user CANNOT configure a shortcut for your action.
     * register - register this action for the toolbar preferences?
     */
    public JosmAction(String name, String iconName, String tooltip, Shortcut shortcut, boolean register) {
        super(name, iconName == null ? null : ImageProvider.get(iconName));
        setHelpId();
        sc = shortcut;
        if (sc != null) {
            Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(sc.getKeyStroke(), name);
            Main.contentPane.getActionMap().put(name, this);
        }
        putValue(SHORT_DESCRIPTION, Main.platform.makeTooltip(tooltip, sc));
        putValue("toolbar", iconName);
    if (register)
        Main.toolbar.register(this);
    }

    public void destroy() {
        if (sc != null) {
            Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).remove(sc.getKeyStroke());
            Main.contentPane.getActionMap().remove(sc.getKeyStroke());
        }
    }

    public JosmAction() {
        setHelpId();
    }

    /**
     * needs to be overridden to be useful
     */
    public void pasteBufferChanged(DataSet newPasteBuffer) {
        return;
    }

    /**
     * needs to be overridden to be useful
     */
    public void addListener(JosmAction a) {
        return;
    }

    private void setHelpId() {
        String helpId = "Action/"+getClass().getName().substring(getClass().getName().lastIndexOf('.')+1);
        if (helpId.endsWith("Action"))
            helpId = helpId.substring(0, helpId.length()-6);
        putValue("help", helpId);
    }
}
