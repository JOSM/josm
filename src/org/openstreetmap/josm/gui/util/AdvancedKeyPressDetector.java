// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.openstreetmap.josm.Main;

/**
 * Helper object that allows cross-platform detection of key press and release events
 * instance is available globally as {@code Main.map.keyDetector}.
 * @since 7217
 */
public class AdvancedKeyPressDetector implements AWTEventListener {

    // events for crossplatform key holding processing
    // thanks to http://www.arco.in-berlin.de/keyevent.html
    private final Set<Integer> set = new TreeSet<>();
    private KeyEvent releaseEvent;
    private Timer timer;

    private final List<KeyPressReleaseListener> keyListeners = new ArrayList<>();
    private final List<ModifierListener> modifierListeners = new ArrayList<>();
    private int previousModifiers;

    private boolean enabled = true;

    /**
     * Adds an object that wants to receive key press and release events.
     * @param l listener to add
     */
    public synchronized void addKeyListener(KeyPressReleaseListener l) {
        keyListeners.add(l);
    }

    /**
     * Adds an object that wants to receive key modifier changed events.
     * @param l listener to add
     */
    public synchronized void addModifierListener(ModifierListener l) {
        modifierListeners.add(l);
    }

    /**
     * Removes the listener.
     * @param l listener to remove
     */
    public synchronized void removeKeyListener(KeyPressReleaseListener l) {
        keyListeners.remove(l);
    }

    /**
     * Removes the key modifier listener.
     * @param l listener to remove
     */
    public synchronized void removeModifierListener(ModifierListener l) {
        modifierListeners.remove(l);
    }

    /**
     * Register this object as AWTEventListener
     */
    public void register() {
        try {
            Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK);
        } catch (SecurityException ex) {
            Main.warn(ex);
        }
        timer = new Timer(0, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timer.stop();
                if (set.remove(releaseEvent.getKeyCode()) && enabled) {
                    synchronized (AdvancedKeyPressDetector.this) {
                        if (isFocusInMainWindow()) {
                            for (KeyPressReleaseListener q: keyListeners) {
                                q.doKeyReleased(releaseEvent);
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     * Unregister this object as AWTEventListener
     * lists of listeners are not cleared!
     */
    public void unregister() {
        timer.stop();
        set.clear();
        synchronized (this) {
            if (!keyListeners.isEmpty()) {
                Main.warn(tr("Some of the key listeners forgot to remove themselves: {0}"), keyListeners.toString());
            }
            if (!modifierListeners.isEmpty()) {
                Main.warn(tr("Some of the key modifier listeners forgot to remove themselves: {0}"), modifierListeners.toString());
            }
        }
        try {
            Toolkit.getDefaultToolkit().removeAWTEventListener(this);
        } catch (SecurityException ex) {
            Main.warn(ex);
        }
    }

    private void processKeyEvent(KeyEvent e) {
        if (e.getID() == KeyEvent.KEY_PRESSED) {
            if (timer.isRunning()) {
                timer.stop();
            } else if (set.add(e.getKeyCode()) && enabled) {
                synchronized (this) {
                    if (isFocusInMainWindow()) {
                        for (KeyPressReleaseListener q: keyListeners) {
                            q.doKeyPressed(e);
                        }
                    }
                }
            }
        } else if (e.getID() == KeyEvent.KEY_RELEASED) {
            if (timer.isRunning()) {
                timer.stop();
                if (set.remove(e.getKeyCode()) && enabled) {
                    synchronized (this) {
                        if (isFocusInMainWindow()) {
                            for (KeyPressReleaseListener q: keyListeners) {
                                q.doKeyReleased(e);
                            }
                        }
                    }
                }
            } else {
                releaseEvent = e;
                timer.restart();
            }
        }
    }

    @Override
    public void eventDispatched(AWTEvent e) {
        if (!(e instanceof KeyEvent)) {
            return;
        }
        KeyEvent ke = (KeyEvent) e;

        // check if ctrl, alt, shift modifiers are changed
        int modif = ke.getModifiers();
        if (previousModifiers != modif) {
            previousModifiers = modif;
            synchronized (this) {
                for (ModifierListener m: modifierListeners) {
                    m.modifiersChanged(modif);
                }
            }
        }

        processKeyEvent(ke);
    }

    /**
     * Allows to determine if the key with specific code is pressed now
     * @param keyCode the key code, for example KeyEvent.VK_ENTER
     * @return true if the key is pressed now
     */
    public boolean isKeyPressed(int keyCode) {
        return set.contains(keyCode);
    }

    /**
     * Sets the enabled state of the key detector. We need to disable it when text fields that disable
     * shortcuts gain focus.
     * @param enabled if {@code true}, enables this key detector. If {@code false}, disables it
     * @since 7539
     */
    public final void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private boolean isFocusInMainWindow() {
        Component focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        return focused != null && SwingUtilities.getWindowAncestor(focused) instanceof JFrame;
    }
}
