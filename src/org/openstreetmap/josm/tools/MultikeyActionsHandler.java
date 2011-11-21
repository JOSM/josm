// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.MultikeyShortcutAction.MultikeyInfo;

public class MultikeyActionsHandler {

    private static final long DIALOG_DELAY = 1000;
    private static final String STATUS_BAR_ID = new String("multikeyShortcut");

    private class MyKeyEventDispatcher implements KeyEventDispatcher {
        @Override
        public boolean dispatchKeyEvent(KeyEvent e) {

            if (e.getWhen() == lastTimestamp)
                return false;

            if (lastAction != null && e.getID() == KeyEvent.KEY_PRESSED) {
                int index = getIndex(e.getKeyCode());
                if (index >= 0) {
                    lastAction.action.executeMultikeyAction(index, e.getKeyCode() == lastAction.shortcut.getKeyCode());
                }
                lastAction = null;
                Main.map.statusLine.resetHelpText(STATUS_BAR_ID);
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

        final MultikeyShortcutAction action;
        final KeyStroke shortcut;

        MyAction(MultikeyShortcutAction action) {
            this.action = action;
            this.shortcut = (KeyStroke) action.getValue(ACCELERATOR_KEY);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            lastTimestamp = e.getWhen();
            lastAction = this;
            timer.schedule(new MyTimerTask(lastTimestamp, lastAction), DIALOG_DELAY);
            Main.map.statusLine.setHelpText(STATUS_BAR_ID, tr("{0}... [please type its number]", (String) action.getValue(SHORT_DESCRIPTION)));
        }

        @Override
        public String toString() {
            return "MultikeyAction" + action.toString();
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
                showLayersPopup(lastAction);
                MultikeyActionsHandler.this.lastAction = null;
            }
        }

    }

    private long lastTimestamp;
    private MyAction lastAction;
    private Timer timer;


    private MultikeyActionsHandler() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new MyKeyEventDispatcher());
        timer =new Timer();
    }

    private static MultikeyActionsHandler instance;

    public static MultikeyActionsHandler getInstance() {
        if (instance == null) {
            instance = new MultikeyActionsHandler();
        }
        return instance;
    }

    private String formatMenuText(KeyStroke keyStroke, String index, String description) {
        String shortcutText = KeyEvent.getKeyModifiersText(keyStroke.getModifiers()) + "+" + KeyEvent.getKeyText(keyStroke.getKeyCode()) + "," + index;

        return "<html><i>" + shortcutText + "</i>&nbsp;&nbsp;&nbsp;&nbsp;" + description;

    }

    private void showLayersPopup(final MyAction action) {
        JPopupMenu layers = new JPopupMenu();

        JMenuItem lbTitle = new JMenuItem((String) action.action.getValue(Action.SHORT_DESCRIPTION));
        lbTitle.setHorizontalAlignment(JMenuItem.CENTER);
        lbTitle.setEnabled(false);
        layers.add(lbTitle);

        char repeatKey = (char) action.shortcut.getKeyCode();
        boolean repeatKeyUsed = false;


        for (final MultikeyInfo info: action.action.getMultikeyCombinations()) {

            if (info.getShortcut() == repeatKey) {
                repeatKeyUsed = true;
            }

            JMenuItem item = new JMenuItem(formatMenuText(action.shortcut, String.valueOf(info.getShortcut()), info.getDescription()));
            item.setMnemonic(info.getShortcut());
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Main.map.statusLine.resetHelpText(STATUS_BAR_ID);
                    action.action.executeMultikeyAction(info.getIndex(), false);
                }
            });
            layers.add(item);
        }

        if (!repeatKeyUsed) {
            MultikeyInfo lastLayer = action.action.getLastMultikeyAction();
            if (lastLayer != null) {
                JMenuItem repeateItem = new JMenuItem(formatMenuText(action.shortcut,
                        KeyEvent.getKeyText(action.shortcut.getKeyCode()),
                        "Repeat " + lastLayer.getDescription()));
                repeateItem.setMnemonic(action.shortcut.getKeyCode());
                repeateItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Main.map.statusLine.resetHelpText(STATUS_BAR_ID);
                        action.action.executeMultikeyAction(-1, true);
                    }
                });
                layers.add(repeateItem);
            }
        }

        layers.show(Main.parent, Integer.MAX_VALUE, Integer.MAX_VALUE);
        layers.setLocation(Main.parent.getX() + Main.parent.getWidth() - layers.getWidth(), Main.parent.getY() + Main.parent.getHeight() - layers.getHeight());
    }

    public void addAction(MultikeyShortcutAction action) {
        if (!(action.getValue(Action.ACCELERATOR_KEY) instanceof KeyStroke))
            throw new IllegalArgumentException("Action must have shortcut set");
        MyAction myAction = new MyAction(action);
        Main.registerActionShortcut(myAction, myAction.shortcut);
    }


}
