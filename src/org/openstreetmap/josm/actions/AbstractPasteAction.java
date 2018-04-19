// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.gui.datatransfer.OsmTransferHandler;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * This is the base class for all actions that paste objects.
 * @author Michael Zangl
 * @since 10765
 */
public abstract class AbstractPasteAction extends JosmAction implements FlavorListener {

    protected final OsmTransferHandler transferHandler;

    /**
     * Constructs a new {@link AbstractPasteAction}.
     * @param name the action's text as displayed on the menu (if it is added to a menu)
     * @param iconName the filename of the icon to use
     * @param tooltip  a longer description of the action that will be displayed in the tooltip. Please note
     *           that html is not supported for menu actions on some platforms.
     * @param shortcut a ready-created shortcut object or null if you don't want a shortcut. But you always
     *            do want a shortcut, remember you can always register it with group=none, so you
     *            won't be assigned a shortcut unless the user configures one. If you pass null here,
     *            the user CANNOT configure a shortcut for your action.
     * @param registerInToolbar register this action for the toolbar preferences?
     */
    public AbstractPasteAction(String name, String iconName, String tooltip, Shortcut shortcut,
            boolean registerInToolbar) {
        this(name, iconName, tooltip, shortcut, registerInToolbar, null);
    }

    /**
     * Constructs a new {@link AbstractPasteAction}.
     * @param name the action's text as displayed on the menu (if it is added to a menu)
     * @param iconName the filename of the icon to use
     * @param tooltip  a longer description of the action that will be displayed in the tooltip. Please note
     *           that html is not supported for menu actions on some platforms.
     * @param shortcut a ready-created shortcut object or null if you don't want a shortcut. But you always
     *            do want a shortcut, remember you can always register it with group=none, so you
     *            won't be assigned a shortcut unless the user configures one. If you pass null here,
     *            the user CANNOT configure a shortcut for your action.
     * @param registerInToolbar register this action for the toolbar preferences?
     * @param toolbarId identifier for the toolbar preferences. The iconName is used, if this parameter is null
     */
    public AbstractPasteAction(String name, String iconName, String tooltip, Shortcut shortcut,
            boolean registerInToolbar, String toolbarId) {
        super(name, iconName, tooltip, shortcut, registerInToolbar, toolbarId, true);
        transferHandler = new OsmTransferHandler();
        ClipboardUtils.getClipboard().addFlavorListener(this);
    }

    /**
     * Compute the location the objects should be pasted at.
     * @param e The action event that triggered the paste
     * @return The paste position.
     */
    protected EastNorth computePastePosition(ActionEvent e) {
        // default to paste in center of map (pasted via menu or cursor not in MapView)
        MapView mapView = MainApplication.getMap().mapView;
        EastNorth mPosition = mapView.getCenter();
        // We previously checked for modifier to know if the action has been trigerred via shortcut or via menu
        // But this does not work if the shortcut is changed to a single key (see #9055)
        // Observed behaviour: getActionCommand() returns Action.NAME when triggered via menu, but shortcut text when triggered with it
        if (e != null && !getValue(NAME).equals(e.getActionCommand())) {
            try {
                final PointerInfo pointerInfo = MouseInfo.getPointerInfo();
                if (pointerInfo != null) {
                    final Point mp = pointerInfo.getLocation();
                    final Point tl = mapView.getLocationOnScreen();
                    final Point pos = new Point(mp.x-tl.x, mp.y-tl.y);
                    if (mapView.contains(pos)) {
                        mPosition = mapView.getEastNorth(pos.x, pos.y);
                    }
                }
            } catch (SecurityException ex) {
                Logging.log(Logging.LEVEL_ERROR, "Unable to get mouse pointer info", ex);
            }
        }
        return mPosition;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        doPaste(e, ClipboardUtils.getClipboardContent());
    }

    protected void doPaste(ActionEvent e, Transferable contents) {
        transferHandler.pasteOn(getLayerManager().getEditLayer(), computePastePosition(e), contents);
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getLayerManager().getEditDataSet() != null && transferHandler != null && transferHandler.isDataAvailable());
    }

    @Override
    public void flavorsChanged(FlavorEvent e) {
        updateEnabledState();
    }
}
