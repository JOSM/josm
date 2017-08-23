// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.MultikeyShortcutAction.MultikeyInfo;

public final class MultikeyActionsHandler {

    private static final long DIALOG_DELAY = 1000;
    private static final String STATUS_BAR_ID = "multikeyShortcut";

    private final Map<MultikeyShortcutAction, MyAction> myActions = new HashMap<>();

    static final class ShowLayersPopupWorker implements Runnable {
        static final class StatusLinePopupMenuListener implements PopupMenuListener {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                // Do nothing
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                MainApplication.getMap().statusLine.resetHelpText(STATUS_BAR_ID);
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                // Do nothing
            }
        }

        private final MyAction action;

        ShowLayersPopupWorker(MyAction action) {
            this.action = action;
        }

        @Override
        public void run() {
            JPopupMenu layers = new JPopupMenu();

            JMenuItem lbTitle = new JMenuItem((String) action.action.getValue(Action.SHORT_DESCRIPTION));
            lbTitle.setEnabled(false);
            JPanel pnTitle = new JPanel();
            pnTitle.add(lbTitle);
            layers.add(pnTitle);

            char repeatKey = (char) action.shortcut.getKeyStroke().getKeyCode();
            boolean repeatKeyUsed = false;

            for (final MultikeyInfo info: action.action.getMultikeyCombinations()) {

                if (info.getShortcut() == repeatKey) {
                    repeatKeyUsed = true;
                }

                JMenuItem item = new JMenuItem(formatMenuText(action.shortcut.getKeyStroke(),
                        String.valueOf(info.getShortcut()), info.getDescription()));
                item.setMnemonic(info.getShortcut());
                item.addActionListener(e -> action.action.executeMultikeyAction(info.getIndex(), false));
                layers.add(item);
            }

            if (!repeatKeyUsed) {
                MultikeyInfo lastLayer = action.action.getLastMultikeyAction();
                if (lastLayer != null) {
                    JMenuItem repeateItem = new JMenuItem(formatMenuText(action.shortcut.getKeyStroke(),
                            KeyEvent.getKeyText(action.shortcut.getKeyStroke().getKeyCode()),
                            "Repeat " + lastLayer.getDescription()));
                    repeateItem.setMnemonic(action.shortcut.getKeyStroke().getKeyCode());
                    repeateItem.addActionListener(e -> action.action.executeMultikeyAction(-1, true));
                    layers.add(repeateItem);
                }
            }
            layers.addPopupMenuListener(new StatusLinePopupMenuListener());
            layers.show(Main.parent, Integer.MAX_VALUE, Integer.MAX_VALUE);
            layers.setLocation(Main.parent.getX() + Main.parent.getWidth() - layers.getWidth(),
                               Main.parent.getY() + Main.parent.getHeight() - layers.getHeight());
        }
    }

    private class MyKeyEventDispatcher implements KeyEventDispatcher {
        @Override
        public boolean dispatchKeyEvent(KeyEvent e) {

            if (e.getWhen() == lastTimestamp)
                return false;

            if (lastAction != null && e.getID() == KeyEvent.KEY_PRESSED) {
                int index = getIndex(e.getKeyCode());
                if (index >= 0) {
                    lastAction.action.executeMultikeyAction(index, e.getKeyCode() == lastAction.shortcut.getKeyStroke().getKeyCode());
                }
                lastAction = null;
                MainApplication.getMap().statusLine.resetHelpText(STATUS_BAR_ID);
                return true;
            }
            return false;
        }

        private int getIndex(int lastKey) {
            if (lastKey >= KeyEvent.VK_1 && lastKey <= KeyEvent.VK_9)
                return lastKey - KeyEvent.VK_1;
            else if (lastKey == KeyEvent.VK_0)
                return 9;
            else if (lastKey >= KeyEvent.VK_A && lastKey <= KeyEvent.VK_Z)
                return lastKey - KeyEvent.VK_A + 10;
            else
                return -1;
        }
    }

    private class MyAction extends AbstractAction {

        private final transient MultikeyShortcutAction action;
        private final transient Shortcut shortcut;

        MyAction(MultikeyShortcutAction action) {
            this.action = action;
            this.shortcut = action.getMultikeyShortcut();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            lastTimestamp = e.getWhen();
            lastAction = this;
            timer.schedule(new MyTimerTask(lastTimestamp, lastAction), DIALOG_DELAY);
            MainApplication.getMap().statusLine.setHelpText(STATUS_BAR_ID, tr("{0}... [please type its number]", (String) action.getValue(SHORT_DESCRIPTION)));
        }

        @Override
        public String toString() {
            return "MultikeyAction" + action;
        }
    }

    private class MyTimerTask extends TimerTask {
        private final long lastTimestamp;
        private final MyAction lastAction;

        MyTimerTask(long lastTimestamp, MyAction lastAction) {
            this.lastTimestamp = lastTimestamp;
            this.lastAction = lastAction;
        }

        @Override
        public void run() {
            if (lastTimestamp == MultikeyActionsHandler.this.lastTimestamp &&
                    lastAction == MultikeyActionsHandler.this.lastAction) {
                SwingUtilities.invokeLater(new ShowLayersPopupWorker(lastAction));
                MultikeyActionsHandler.this.lastAction = null;
            }
        }
    }

    private long lastTimestamp;
    private MyAction lastAction;
    private final Timer timer;

    private MultikeyActionsHandler() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new MyKeyEventDispatcher());
        timer = new Timer();
    }

    private static MultikeyActionsHandler instance;

    /**
     * Replies the unique instance of this class.
     * @return The unique instance of this class
     */
    public static synchronized MultikeyActionsHandler getInstance() {
        if (instance == null) {
            instance = new MultikeyActionsHandler();
        }
        return instance;
    }

    private static String formatMenuText(KeyStroke keyStroke, String index, String description) {
        String shortcutText = Shortcut.getKeyText(keyStroke) + ',' + index;

        return "<html><i>" + shortcutText + "</i>&nbsp;&nbsp;&nbsp;&nbsp;" + description;
    }

    /**
     * Registers an action and its shortcut
     * @param action The action to add
     */
    public void addAction(MultikeyShortcutAction action) {
        if (action.getMultikeyShortcut() != null) {
            MyAction myAction = new MyAction(action);
            myActions.put(action, myAction);
            Main.registerActionShortcut(myAction, myAction.shortcut);
        }
    }

    /**
     * Unregisters an action and its shortcut completely
     * @param action The action to remove
     */
    public void removeAction(MultikeyShortcutAction action) {
        MyAction a = myActions.get(action);
        if (a != null) {
            Main.unregisterActionShortcut(a, a.shortcut);
            myActions.remove(action);
        }
    }
}
