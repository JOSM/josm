// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

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

    private static final long DIALOG_DELAY = 2000;

    private class MyKeyEventDispatcher implements KeyEventDispatcher {
        @Override
        public boolean dispatchKeyEvent(KeyEvent e) {

            if (e.getWhen() == lastTimestamp)
                return false;

            if (lastAction != null && e.getID() == KeyEvent.KEY_PRESSED) {
                if (e.getKeyCode() == lastAction.shortcut.getKeyCode()) {
                    lastAction.action.repeateLastMultikeyAction();
                } else {
                    int index = getIndex(e.getKeyChar());
                    if (index >= 0) {
                        lastAction.action.executeMultikeyAction(index);
                    }
                }
                lastAction = null;
                return true;
            }
            return false;
        }

        private int getIndex(char lastKey) {
            if (lastKey >= KeyEvent.VK_0 && lastKey <= KeyEvent.VK_9)
                return lastKey - KeyEvent.VK_0;
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


        for (final MultikeyInfo info: action.action.getMultikeyCombinations()) {
            JMenuItem item = new JMenuItem(formatMenuText(action.shortcut, String.valueOf(info.getShortcut()), info.getDescription()));
            item.setMnemonic(info.getShortcut());
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    action.action.executeMultikeyAction(info.getIndex());
                }
            });
            layers.add(item);
        }

        MultikeyInfo lastLayer = action.action.getLastMultikeyAction();
        if (lastLayer != null) {
            JMenuItem repeateItem = new JMenuItem(formatMenuText(action.shortcut,
                    KeyEvent.getKeyText(action.shortcut.getKeyCode()),
                    "Repeat " + lastLayer.getDescription()));
            repeateItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    action.action.repeateLastMultikeyAction();
                }
            });
            layers.add(repeateItem);
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
