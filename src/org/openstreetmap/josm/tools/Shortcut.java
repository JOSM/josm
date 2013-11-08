//License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JMenu;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;

/**
 * Global shortcut class.
 *
 * Note: This class represents a single shortcut, contains the factory to obtain
 *       shortcut objects from, manages shortcuts and shortcut collisions, and
 *       finally manages loading and saving shortcuts to/from the preferences.
 *
 * Action authors: You only need the {@link #registerShortcut} factory. Ignore everything
 *                 else.
 *
 * All: Use only public methods that are also marked to be used. The others are
 *      public so the shortcut preferences can use them.
 *
 */
public final class Shortcut {
    private String shortText;        // the unique ID of the shortcut
    private String longText;         // a human readable description that will be shown in the preferences
    private final int requestedKey;  // the key, the caller requested
    private final int requestedGroup;// the group, the caller requested
    private int assignedKey;         // the key that actually is used
    private int assignedModifier;    // the modifiers that are used
    private boolean assignedDefault; // true if it got assigned what was requested. (Note: modifiers will be ignored in favour of group when loading it from the preferences then.)
    private boolean assignedUser;    // true if the user changed this shortcut
    private boolean automatic;       // true if the user cannot change this shortcut (Note: it also will not be saved into the preferences)
    private boolean reset;           // true if the user requested this shortcut to be set to its default value (will happen on next restart, as this shortcut will not be saved to the preferences)

    // simple constructor
    private Shortcut(String shortText, String longText, int requestedKey, int requestedGroup, int assignedKey, int assignedModifier, boolean assignedDefault, boolean assignedUser) {
        this.shortText = shortText;
        this.longText = longText;
        this.requestedKey = requestedKey;
        this.requestedGroup = requestedGroup;
        this.assignedKey = assignedKey;
        this.assignedModifier = assignedModifier;
        this.assignedDefault = assignedDefault;
        this.assignedUser = assignedUser;
        this.automatic = false;
        this.reset = false;
    }

    public String getShortText() {
        return shortText;
    }

    public String getLongText() {
        return longText;
    }

    // a shortcut will be renamed when it is handed out again, because the original name
    // may be a dummy
    private void setLongText(String longText) {
        this.longText = longText;
    }

    public int getAssignedKey() {
        return assignedKey;
    }

    public int getAssignedModifier() {
        return assignedModifier;
    }

    public boolean getAssignedDefault() {
        return assignedDefault;
    }

    public boolean getAssignedUser() {
        return assignedUser;
    }

    public boolean getAutomatic() {
        return automatic;
    }

    public boolean isChangeable() {
        return !automatic && !shortText.equals("core:none");
    }

    private boolean getReset() {
        return reset;
    }

    /**
     * FOR PREF PANE ONLY
     */
    public void setAutomatic() {
        automatic = true;
    }

    /**
     * FOR PREF PANE ONLY
     */
    public void setAssignedModifier(int assignedModifier) {
        this.assignedModifier = assignedModifier;
    }

    /**
     * FOR PREF PANE ONLY
     */
    public void setAssignedKey(int assignedKey) {
        this.assignedKey = assignedKey;
    }

    /**
     * FOR PREF PANE ONLY
     */
    public void setAssignedUser(boolean assignedUser) {
        this.reset = (this.assignedUser || reset) && !assignedUser;
        if (assignedUser) {
            assignedDefault = false;
        } else if (reset) {
            assignedKey = requestedKey;
            assignedModifier = findModifier(requestedGroup, null);
        }
        this.assignedUser = assignedUser;
    }

    /**
     * Use this to register the shortcut with Swing
     */
    public KeyStroke getKeyStroke() {
        if (assignedModifier != -1)
            return KeyStroke.getKeyStroke(assignedKey, assignedModifier);
        return null;
    }

    // create a shortcut object from an string as saved in the preferences
    private Shortcut(String prefString) {
        List<String> s = (new ArrayList<String>(Main.pref.getCollection(prefString)));
        this.shortText = prefString.substring(15);
        this.longText = s.get(0);
        this.requestedKey = Integer.parseInt(s.get(1));
        this.requestedGroup = Integer.parseInt(s.get(2));
        this.assignedKey = Integer.parseInt(s.get(3));
        this.assignedModifier = Integer.parseInt(s.get(4));
        this.assignedDefault = Boolean.parseBoolean(s.get(5));
        this.assignedUser = Boolean.parseBoolean(s.get(6));
    }

    private void saveDefault() {
        Main.pref.getCollection("shortcut.entry."+shortText, Arrays.asList(new String[]{longText,
        String.valueOf(requestedKey), String.valueOf(requestedGroup), String.valueOf(requestedKey),
        String.valueOf(getGroupModifier(requestedGroup)), String.valueOf(true), String.valueOf(false)}));
    }

    // get a string that can be put into the preferences
    private boolean save() {
        if (getAutomatic() || getReset() || !getAssignedUser()) {
            return Main.pref.putCollection("shortcut.entry."+shortText, null);
        } else {
            return Main.pref.putCollection("shortcut.entry."+shortText, Arrays.asList(new String[]{longText,
            String.valueOf(requestedKey), String.valueOf(requestedGroup), String.valueOf(assignedKey),
            String.valueOf(assignedModifier), String.valueOf(assignedDefault), String.valueOf(assignedUser)}));
        }
    }

    private boolean isSame(int isKey, int isModifier) {
        // an unassigned shortcut is different from any other shortcut
        return isKey == assignedKey && isModifier == assignedModifier && assignedModifier != getGroupModifier(NONE);
    }

    public boolean isEvent(KeyEvent e) {
        return getKeyStroke() != null && getKeyStroke().equals(
        KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiers()));
    }

    /**
     * use this to set a menu's mnemonic
     */
    public void setMnemonic(JMenu menu) {
        if (assignedModifier == getGroupModifier(MNEMONIC) && getKeyStroke() != null && KeyEvent.getKeyText(assignedKey).length() == 1) {
            menu.setMnemonic(KeyEvent.getKeyText(assignedKey).charAt(0)); //getKeyStroke().getKeyChar() seems not to work here
        }
    }
    /**
     * use this to set a buttons's mnemonic
     */
    public void setMnemonic(AbstractButton button) {
        if (assignedModifier == getGroupModifier(MNEMONIC)  && getKeyStroke() != null && KeyEvent.getKeyText(assignedKey).length() == 1) {
            button.setMnemonic(KeyEvent.getKeyText(assignedKey).charAt(0)); //getKeyStroke().getKeyChar() seems not to work here
        }
    }
    /**
     * use this to set a actions's accelerator
     */
    public void setAccelerator(AbstractAction action) {
        if (getKeyStroke() != null) {
            action.putValue(AbstractAction.ACCELERATOR_KEY, getKeyStroke());
        }
    }

    /**
     * use this to get a human readable text for your shortcut
     */
    public String getKeyText() {
        KeyStroke keyStroke = getKeyStroke();
        if (keyStroke == null) return "";
        String modifText = KeyEvent.getKeyModifiersText(keyStroke.getModifiers());
        if ("".equals (modifText)) return KeyEvent.getKeyText (keyStroke.getKeyCode ());
        return modifText + "+" + KeyEvent.getKeyText(keyStroke.getKeyCode ());
    }

    @Override
    public String toString() {
        return getKeyText();
    }

    ///////////////////////////////
    // everything's static below //
    ///////////////////////////////

    // here we store our shortcuts
    private static Map<String, Shortcut> shortcuts = new LinkedHashMap<String, Shortcut>();

    // and here our modifier groups
    private static Map<Integer, Integer> groups= new HashMap<Integer, Integer>();

    // check if something collides with an existing shortcut
    public static Shortcut findShortcut(int requestedKey, int modifier) {
        if (modifier == getGroupModifier(NONE))
            return null;
        for (Shortcut sc : shortcuts.values()) {
            if (sc.isSame(requestedKey, modifier))
                return sc;
        }
        return null;
    }

    /**
     * FOR PREF PANE ONLY
     */
    public static List<Shortcut> listAll() {
        List<Shortcut> l = new ArrayList<Shortcut>();
        for(Shortcut c : shortcuts.values())
        {
            if(!c.shortText.equals("core:none")) {
                l.add(c);
            }
        }
        return l;
    }

    public static final int NONE = 5000;
    public static final int MNEMONIC = 5001;
    public static final int RESERVED = 5002;
    public static final int DIRECT = 5003;
    public static final int ALT = 5004;
    public static final int SHIFT = 5005;
    public static final int CTRL = 5006;
    public static final int ALT_SHIFT = 5007;
    public static final int ALT_CTRL = 5008;
    public static final int CTRL_SHIFT = 5009;
    public static final int ALT_CTRL_SHIFT = 5010;

    /* for reassignment */
    private static int[] mods = {ALT_CTRL, ALT_SHIFT, CTRL_SHIFT, ALT_CTRL_SHIFT};
    private static int[] keys = {KeyEvent.VK_F1, KeyEvent.VK_F2, KeyEvent.VK_F3, KeyEvent.VK_F4,
                                 KeyEvent.VK_F5, KeyEvent.VK_F6, KeyEvent.VK_F7, KeyEvent.VK_F8,
                                 KeyEvent.VK_F9, KeyEvent.VK_F10, KeyEvent.VK_F11, KeyEvent.VK_F12};

    // bootstrap
    private static boolean initdone = false;
    private static void doInit() {
        if (initdone) return;
        initdone = true;
        groups.put(NONE, -1);
        groups.put(MNEMONIC, KeyEvent.ALT_DOWN_MASK);
        groups.put(DIRECT, 0);
        groups.put(ALT, KeyEvent.ALT_DOWN_MASK);
        groups.put(SHIFT, KeyEvent.SHIFT_DOWN_MASK);
        groups.put(CTRL, KeyEvent.CTRL_DOWN_MASK);
        groups.put(ALT_SHIFT, KeyEvent.ALT_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK);
        groups.put(ALT_CTRL, KeyEvent.ALT_DOWN_MASK|KeyEvent.CTRL_DOWN_MASK);
        groups.put(CTRL_SHIFT, KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK);
        groups.put(ALT_CTRL_SHIFT, KeyEvent.ALT_DOWN_MASK|KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK);

        // (1) System reserved shortcuts
        Main.platform.initSystemShortcuts();
        // (2) User defined shortcuts
        LinkedList<Shortcut> newshortcuts = new LinkedList<Shortcut>();
        for(String s : Main.pref.getAllPrefixCollectionKeys("shortcut.entry.")) {
            newshortcuts.add(new Shortcut(s));
        }

        for(Shortcut sc : newshortcuts) {
            if (sc.getAssignedUser()
            && findShortcut(sc.getAssignedKey(), sc.getAssignedModifier()) == null) {
                shortcuts.put(sc.getShortText(), sc);
            }
        }
        // Shortcuts at their default values
        for(Shortcut sc : newshortcuts) {
            if (!sc.getAssignedUser() && sc.getAssignedDefault()
            && findShortcut(sc.getAssignedKey(), sc.getAssignedModifier()) == null) {
                shortcuts.put(sc.getShortText(), sc);
            }
        }
        // Shortcuts that were automatically moved
        for(Shortcut sc : newshortcuts) {
            if (!sc.getAssignedUser() && !sc.getAssignedDefault()
            && findShortcut(sc.getAssignedKey(), sc.getAssignedModifier()) == null) {
                shortcuts.put(sc.getShortText(), sc);
            }
        }
    }

    private static int getGroupModifier(int group) {
        Integer m = groups.get(group);
        if(m == null)
            m = -1;
        return m;
    }

    private static int findModifier(int group, Integer modifier) {
        if(modifier == null) {
            modifier = getGroupModifier(group);
            if (modifier == null) { // garbage in, no shortcut out
                modifier = getGroupModifier(NONE);
            }
        }
        return modifier;
    }

    // shutdown handling
    public static boolean savePrefs() {
        boolean changed = false;
        for (Shortcut sc : shortcuts.values()) {
            changed = changed | sc.save();
        }
        return changed;
    }

    /**
     * FOR PLATFORMHOOK USE ONLY
     *
     * This registers a system shortcut. See PlatformHook for details.
     */
    public static Shortcut registerSystemShortcut(String shortText, String longText, int key, int modifier) {
        if (shortcuts.containsKey(shortText))
            return shortcuts.get(shortText);
        Shortcut potentialShortcut = findShortcut(key, modifier);
        if (potentialShortcut != null) {
            // this always is a logic error in the hook
            Main.error("CONFLICT WITH SYSTEM KEY "+shortText);
            return null;
        }
        potentialShortcut = new Shortcut(shortText, longText, key, RESERVED, key, modifier, true, false);
        shortcuts.put(shortText, potentialShortcut);
        return potentialShortcut;
    }

    /**
     * Register a shortcut.
     *
     * Here you get your shortcuts from. The parameters are:
     *
     * @param shortText an ID. re-use a {@code "system:*"} ID if possible, else use something unique.
     * {@code "menu:*"} is reserved for menu mnemonics, {@code "core:*"} is reserved for
     * actions that are part of JOSM's core. Use something like
     * {@code <pluginname>+":"+<actionname>}.
     * @param longText this will be displayed in the shortcut preferences dialog. Better
     * use something the user will recognize...
     * @param requestedKey the key you'd prefer. Use a {@link KeyEvent KeyEvent.VK_*} constant here.
     * @param requestedGroup the group this shortcut fits best. This will determine the
     * modifiers your shortcut will get assigned. Use the constants defined above.
     */
    public static Shortcut registerShortcut(String shortText, String longText, int requestedKey, int requestedGroup) {
        return registerShortcut(shortText, longText, requestedKey, requestedGroup, null);
    }

    // and now the workhorse. same parameters as above, just one more
    private static Shortcut registerShortcut(String shortText, String longText, int requestedKey, int requestedGroup, Integer modifier) {
        doInit();
        Integer defaultModifier = findModifier(requestedGroup, modifier);
        if (shortcuts.containsKey(shortText)) { // a re-register? maybe a sc already read from the preferences?
            Shortcut sc = shortcuts.get(shortText);
            sc.setLongText(longText); // or set by the platformHook, in this case the original longText doesn't match the real action
            sc.saveDefault();
            return sc;
        }
        Shortcut conflict = findShortcut(requestedKey, defaultModifier);
        if (conflict != null) {
            for (int m : mods) {
                for (int k : keys) {
                    int newmodifier = getGroupModifier(m);
                    if ( findShortcut(k, newmodifier) == null ) {
                        Shortcut newsc = new Shortcut(shortText, longText, requestedKey, m, k, newmodifier, false, false);
                        Main.info(tr("Silent shortcut conflict: ''{0}'' moved by ''{1}'' to ''{2}''.",
                            shortText, conflict.getShortText(), newsc.getKeyText()));
                        newsc.saveDefault();
                        shortcuts.put(shortText, newsc);
                        return newsc;
                    }
                }
            }
        } else {
            Shortcut newsc = new Shortcut(shortText, longText, requestedKey, requestedGroup, requestedKey, defaultModifier, true, false);
            newsc.saveDefault();
            shortcuts.put(shortText, newsc);
            return newsc;
        }

        return null;
    }

    /**
     * Replies the platform specific key stroke for the 'Copy' command, i.e.
     * 'Ctrl-C' on windows or 'Meta-C' on a Mac. null, if the platform specific
     * copy command isn't known.
     *
     * @return the platform specific key stroke for the  'Copy' command
     */
    static public KeyStroke getCopyKeyStroke() {
        Shortcut sc = shortcuts.get("system:copy");
        if (sc == null) return null;
        return sc.getKeyStroke();
    }

    /**
     * Replies the platform specific key stroke for the 'Paste' command, i.e.
     * 'Ctrl-V' on windows or 'Meta-V' on a Mac. null, if the platform specific
     * paste command isn't known.
     *
     * @return the platform specific key stroke for the 'Paste' command
     */
    static public KeyStroke getPasteKeyStroke() {
        Shortcut sc = shortcuts.get("system:paste");
        if (sc == null) return null;
        return sc.getKeyStroke();
    }

    /**
     * Replies the platform specific key stroke for the 'Cut' command, i.e.
     * 'Ctrl-X' on windows or 'Meta-X' on a Mac. null, if the platform specific
     * 'Cut' command isn't known.
     *
     * @return the platform specific key stroke for the 'Cut' command
     */
    static public KeyStroke getCutKeyStroke() {
        Shortcut sc = shortcuts.get("system:cut");
        if (sc == null) return null;
        return sc.getKeyStroke();
    }
}
