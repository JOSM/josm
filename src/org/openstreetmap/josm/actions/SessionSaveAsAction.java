// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.UserCancelException;

/**
 * Saves a JOSM session to a new file
 * @since 4685
 */
public class SessionSaveAsAction extends SessionSaveAction {

    /**
     * Constructs a new {@code SessionSaveAsAction}.
     */
    public SessionSaveAsAction() {
        this(true, false);
        updateEnabledState();
    }

    /**
     * Constructs a new {@code SessionSaveAsAction}.
     * @param toolbar Register this action for the toolbar preferences?
     * @param installAdapters False, if you don't want to install layer changed and selection changed adapters
     */
    protected SessionSaveAsAction(boolean toolbar, boolean installAdapters) {

        super(tr("Save Session As..."), "session", tr("Save the current session to a new file."),
                Shortcut.registerShortcut("system:savesessionas", tr("File: {0}", tr("Save Session As...")),
                        KeyEvent.VK_S, Shortcut.ALT_CTRL_SHIFT),
                toolbar, "save_as-session", installAdapters);

        setHelpId(ht("/Action/SessionSaveAs"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            saveSession(true, false);
        } catch (UserCancelException ignore) {
            Logging.trace(ignore);
        }
    }

    @Override
    protected void addListeners() {
        MainApplication.addMapFrameListener(this);
    }

    @Override
    protected void removeListeners() {
        MainApplication.removeMapFrameListener(this);
    }
}
