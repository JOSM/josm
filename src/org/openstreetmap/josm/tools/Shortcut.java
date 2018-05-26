// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JMenu;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * Global shortcut class.
 *
 * Note: This class represents a single shortcut, contains the factory to obtain
 *       shortcut objects from, manages shortcuts and shortcut collisions, and
 *       finally manages loading and saving shortcuts to/from the preferences.
 *
 * Action authors: You only need the {@link #registerShortcut} factory. Ignore everything else.
 *
 * All: Use only public methods that are also marked to be used. The others are
 *      public so the shortcut preferences can use them.
 * @since 1084
 */
public final class Shortcut {
    /** the unique ID of the shortcut */
    private final String shortText;
    /** a human readable description that will be shown in the preferences */
    private String longText;
    /** the key, the caller requested */
    private final int requestedKey;
    /** the group, the caller requested */
    private final int requestedGroup;
    /** the key that actually is used */
    private int assignedKey;
    /** the modifiers that are used */
    private int assignedModifier;
    /** true if it got assigned what was requested.
     * (Note: modifiers will be ignored in favour of group when loading it from the preferences then.) */
    private boolean assignedDefault;
    /** true if the user changed this shortcut */
    private boolean assignedUser;
    /** true if the user cannot change this shortcut (Note: it also will not be saved into the preferences) */
    private boolean automatic;
    /** true if the user requested this shortcut to be set to its default value
     * (will happen on next restart, as this shortcut will not be saved to the preferences) */
    private boolean reset;

    // simple constructor
    private Shortcut(String shortText, String longText, int requestedKey, int requestedGroup, int assignedKey, int assignedModifier,
            boolean assignedDefault, boolean assignedUser) {
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

    // a shortcut will be renamed when it is handed out again, because the original name may be a dummy
    private void setLongText(String longText) {
        this.longText = longText;
    }

    public int getAssignedKey() {
        return assignedKey;
    }

    public int getAssignedModifier() {
        return assignedModifier;
    }

    public boolean isAssignedDefault() {
        return assignedDefault;
    }

    public boolean isAssignedUser() {
        return assignedUser;
    }

    public boolean isAutomatic() {
        return automatic;
    }

    public boolean isChangeable() {
        return !automatic && !"core:none".equals(shortText);
    }

    private boolean isReset() {
        return reset;
    }

    /**
     * FOR PREF PANE ONLY
     */
    public void setAutomatic() {
        automatic = true;
    }

    /**
     * FOR PREF PANE ONLY.<p>
     * Sets the modifiers that are used.
     * @param assignedModifier assigned modifier
     */
    public void setAssignedModifier(int assignedModifier) {
        this.assignedModifier = assignedModifier;
    }

    /**
     * FOR PREF PANE ONLY.<p>
     * Sets the key that actually is used.
     * @param assignedKey assigned key
     */
    public void setAssignedKey(int assignedKey) {
        this.assignedKey = assignedKey;
    }

    /**
     * FOR PREF PANE ONLY.<p>
     * Sets whether the user has changed this shortcut.
     * @param assignedUser {@code true} if the user has changed this shortcut
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
     * @return the key stroke
     */
    public KeyStroke getKeyStroke() {
        if (assignedModifier != -1)
            return KeyStroke.getKeyStroke(assignedKey, assignedModifier);
        return null;
    }

    // create a shortcut object from an string as saved in the preferences
    private Shortcut(String prefString) {
        List<String> s = new ArrayList<>(Config.getPref().getList(prefString));
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
        Config.getPref().getList("shortcut.entry."+shortText, Arrays.asList(longText,
            String.valueOf(requestedKey), String.valueOf(requestedGroup), String.valueOf(requestedKey),
            String.valueOf(getGroupModifier(requestedGroup)), String.valueOf(true), String.valueOf(false)));
    }

    // get a string that can be put into the preferences
    private boolean save() {
        if (isAutomatic() || isReset() || !isAssignedUser()) {
            return Config.getPref().putList("shortcut.entry."+shortText, null);
        } else {
            return Config.getPref().putList("shortcut.entry."+shortText, Arrays.asList(longText,
                String.valueOf(requestedKey), String.valueOf(requestedGroup), String.valueOf(assignedKey),
                String.valueOf(assignedModifier), String.valueOf(assignedDefault), String.valueOf(assignedUser)));
        }
    }

    private boolean isSame(int isKey, int isModifier) {
        // an unassigned shortcut is different from any other shortcut
        return isKey == assignedKey && isModifier == assignedModifier && assignedModifier != getGroupModifier(NONE);
    }

    public boolean isEvent(KeyEvent e) {
        KeyStroke ks = getKeyStroke();
        return ks != null && ks.equals(KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiersEx()));
    }

    /**
     * use this to set a menu's mnemonic
     * @param menu menu
     */
    public void setMnemonic(JMenu menu) {
        if (assignedModifier == getGroupModifier(MNEMONIC) && getKeyStroke() != null && KeyEvent.getKeyText(assignedKey).length() == 1) {
            menu.setMnemonic(KeyEvent.getKeyText(assignedKey).charAt(0)); //getKeyStroke().getKeyChar() seems not to work here
        }
    }

    /**
     * use this to set a buttons's mnemonic
     * @param button button
     */
    public void setMnemonic(AbstractButton button) {
        if (assignedModifier == getGroupModifier(MNEMONIC) && getKeyStroke() != null && KeyEvent.getKeyText(assignedKey).length() == 1) {
            button.setMnemonic(KeyEvent.getKeyText(assignedKey).charAt(0)); //getKeyStroke().getKeyChar() seems not to work here
        }
    }

    /**
     * Sets the mnemonic key on a text component.
     * @param component component
     */
    public void setFocusAccelerator(JTextComponent component) {
        if (assignedModifier == getGroupModifier(MNEMONIC) && getKeyStroke() != null && KeyEvent.getKeyText(assignedKey).length() == 1) {
            component.setFocusAccelerator(KeyEvent.getKeyText(assignedKey).charAt(0));
        }
    }

    /**
     * use this to set a actions's accelerator
     * @param action action
     */
    public void setAccelerator(AbstractAction action) {
        if (getKeyStroke() != null) {
            action.putValue(AbstractAction.ACCELERATOR_KEY, getKeyStroke());
        }
    }

    /**
     * Returns a human readable text for the shortcut.
     * @return a human readable text for the shortcut
     */
    public String getKeyText() {
        return getKeyText(getKeyStroke());
    }

    /**
     * Returns a human readable text for the key stroke.
     * @param keyStroke key stroke to convert to human readable text
     * @return a human readable text for the key stroke
     * @since 12520
     */
    public static String getKeyText(KeyStroke keyStroke) {
        if (keyStroke == null) return "";
        String modifText = KeyEvent.getModifiersExText(keyStroke.getModifiers());
        if ("".equals(modifText)) return KeyEvent.getKeyText(keyStroke.getKeyCode());
        return modifText + '+' + KeyEvent.getKeyText(keyStroke.getKeyCode());
    }

    @Override
    public String toString() {
        return getKeyText();
    }

    ///////////////////////////////
    // everything's static below //
    ///////////////////////////////

    // here we store our shortcuts
    private static ShortcutCollection shortcuts = new ShortcutCollection();

    private static class ShortcutCollection extends CopyOnWriteArrayList<Shortcut> {
        private static final long serialVersionUID = 1L;
        @Override
        public boolean add(Shortcut shortcut) {
            // expensive consistency check only in debug mode
            if (Logging.isDebugEnabled()
                    && stream().map(Shortcut::getShortText).anyMatch(shortcut.getShortText()::equals)) {
                Logging.warn(new AssertionError(shortcut.getShortText() + " already added"));
            }
            return super.add(shortcut);
        }

        void replace(Shortcut newShortcut) {
            final Optional<Shortcut> existing = findShortcutByKeyOrShortText(-1, NONE, newShortcut.shortText);
            if (existing.isPresent()) {
                replaceAll(sc -> existing.get() == sc ? newShortcut : sc);
            } else {
                add(newShortcut);
            }
        }
    }

    // and here our modifier groups
    private static Map<Integer, Integer> groups = new HashMap<>();

    // check if something collides with an existing shortcut

    /**
     * Returns the registered shortcut fot the key and modifier
     * @param requestedKey the requested key
     * @param modifier the modifier
     * @return an {@link Optional} registered shortcut, never {@code null}
     */
    public static Optional<Shortcut> findShortcut(int requestedKey, int modifier) {
        return findShortcutByKeyOrShortText(requestedKey, modifier, null);
    }

    private static Optional<Shortcut> findShortcutByKeyOrShortText(int requestedKey, int modifier, String shortText) {
        final Predicate<Shortcut> sameKey = sc -> modifier != getGroupModifier(NONE) && sc.isSame(requestedKey, modifier);
        final Predicate<Shortcut> sameShortText = sc -> sc.getShortText().equals(shortText);
        return shortcuts.stream()
                .filter(sameKey.or(sameShortText))
                .sorted(Comparator.comparingInt(sc -> sameShortText.test(sc) ? 0 : 1))
                .findAny();
    }

    /**
     * Returns a list of all shortcuts.
     * @return a list of all shortcuts
     */
    public static List<Shortcut> listAll() {
        return shortcuts.stream()
                .filter(c -> !"core:none".equals(c.shortText))
                .collect(Collectors.toList());
    }

    /** None group: used with KeyEvent.CHAR_UNDEFINED if no shortcut is defined */
    public static final int NONE = 5000;
    public static final int MNEMONIC = 5001;
    /** Reserved group: for system shortcuts only */
    public static final int RESERVED = 5002;
    /** Direct group: no modifier */
    public static final int DIRECT = 5003;
    /** Alt group */
    public static final int ALT = 5004;
    /** Shift group */
    public static final int SHIFT = 5005;
    /** Command group. Matches CTRL modifier on Windows/Linux but META modifier on OS X */
    public static final int CTRL = 5006;
    /** Alt-Shift group */
    public static final int ALT_SHIFT = 5007;
    /** Alt-Command group. Matches ALT-CTRL modifier on Windows/Linux but ALT-META modifier on OS X */
    public static final int ALT_CTRL = 5008;
    /** Command-Shift group. Matches CTRL-SHIFT modifier on Windows/Linux but META-SHIFT modifier on OS X */
    public static final int CTRL_SHIFT = 5009;
    /** Alt-Command-Shift group. Matches ALT-CTRL-SHIFT modifier on Windows/Linux but ALT-META-SHIFT modifier on OS X */
    public static final int ALT_CTRL_SHIFT = 5010;

    /* for reassignment */
    private static int[] mods = {ALT_CTRL, ALT_SHIFT, CTRL_SHIFT, ALT_CTRL_SHIFT};
    private static int[] keys = {KeyEvent.VK_F1, KeyEvent.VK_F2, KeyEvent.VK_F3, KeyEvent.VK_F4,
                                 KeyEvent.VK_F5, KeyEvent.VK_F6, KeyEvent.VK_F7, KeyEvent.VK_F8,
                                 KeyEvent.VK_F9, KeyEvent.VK_F10, KeyEvent.VK_F11, KeyEvent.VK_F12};

    // bootstrap
    private static boolean initdone;
    private static void doInit() {
        if (initdone) return;
        initdone = true;
        int commandDownMask = Main.platform.getMenuShortcutKeyMaskEx();
        groups.put(NONE, -1);
        groups.put(MNEMONIC, KeyEvent.ALT_DOWN_MASK);
        groups.put(DIRECT, 0);
        groups.put(ALT, KeyEvent.ALT_DOWN_MASK);
        groups.put(SHIFT, KeyEvent.SHIFT_DOWN_MASK);
        groups.put(CTRL, commandDownMask);
        groups.put(ALT_SHIFT, KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK);
        groups.put(ALT_CTRL, KeyEvent.ALT_DOWN_MASK | commandDownMask);
        groups.put(CTRL_SHIFT, commandDownMask | KeyEvent.SHIFT_DOWN_MASK);
        groups.put(ALT_CTRL_SHIFT, KeyEvent.ALT_DOWN_MASK | commandDownMask | KeyEvent.SHIFT_DOWN_MASK);

        // (1) System reserved shortcuts
        Main.platform.initSystemShortcuts();
        // (2) User defined shortcuts
        Main.pref.getAllPrefixCollectionKeys("shortcut.entry.").stream()
                .map(Shortcut::new)
                .filter(sc -> !findShortcut(sc.getAssignedKey(), sc.getAssignedModifier()).isPresent())
                .sorted(Comparator.comparing(sc -> sc.isAssignedUser() ? 1 : sc.isAssignedDefault() ? 2 : 3))
                .forEachOrdered(shortcuts::replace);
    }

    private static int getGroupModifier(int group) {
        return Optional.ofNullable(groups.get(group)).orElse(-1);
    }

    private static int findModifier(int group, Integer modifier) {
        if (modifier == null) {
            modifier = getGroupModifier(group);
            if (modifier == null) { // garbage in, no shortcut out
                modifier = getGroupModifier(NONE);
            }
        }
        return modifier;
    }

    // shutdown handling
    public static boolean savePrefs() {
        return shortcuts.stream()
                .map(Shortcut::save)
                .reduce(Boolean.FALSE, Boolean::logicalOr); // has changed
    }

    /**
     * FOR PLATFORMHOOK USE ONLY.
     * <p>
     * This registers a system shortcut. See PlatformHook for details.
     * @param shortText an ID. re-use a {@code "system:*"} ID if possible, else use something unique.
     * @param longText this will be displayed in the shortcut preferences dialog. Better
     * use something the user will recognize...
     * @param key the key. Use a {@link KeyEvent KeyEvent.VK_*} constant here.
     * @param modifier the modifier. Use a {@link KeyEvent KeyEvent.*_MASK} constant here.
     * @return the system shortcut
     */
    public static Shortcut registerSystemShortcut(String shortText, String longText, int key, int modifier) {
        final Optional<Shortcut> existing = findShortcutByKeyOrShortText(key, modifier, shortText);
        if (existing.isPresent() && shortText.equals(existing.get().getShortText())) {
            return existing.get();
        } else if (existing.isPresent()) {
            // this always is a logic error in the hook
            Logging.error("CONFLICT WITH SYSTEM KEY " + shortText + ": " + existing.get());
            return null;
        }
        final Shortcut shortcut = new Shortcut(shortText, longText, key, RESERVED, key, modifier, true, false);
        shortcuts.add(shortcut);
        return shortcut;
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
     * @return the shortcut
     */
    public static Shortcut registerShortcut(String shortText, String longText, int requestedKey, int requestedGroup) {
        return registerShortcut(shortText, longText, requestedKey, requestedGroup, null);
    }

    // and now the workhorse. same parameters as above, just one more
    private static Shortcut registerShortcut(String shortText, String longText, int requestedKey, int requestedGroup, Integer modifier) {
        doInit();
        Integer defaultModifier = findModifier(requestedGroup, modifier);
        final Optional<Shortcut> existing = findShortcutByKeyOrShortText(requestedKey, defaultModifier, shortText);
        if (existing.isPresent() && shortText.equals(existing.get().getShortText())) {
            // a re-register? maybe a sc already read from the preferences?
            final Shortcut sc = existing.get();
            sc.setLongText(longText); // or set by the platformHook, in this case the original longText doesn't match the real action
            sc.saveDefault();
            return sc;
        } else if (existing.isPresent()) {
            final Shortcut conflict = existing.get();
            if (Main.isPlatformOsx()) {
                // Try to reassign Meta to Ctrl
                int newmodifier = findNewOsxModifier(requestedGroup);
                if (!findShortcut(requestedKey, newmodifier).isPresent()) {
                    Logging.info("Reassigning OSX shortcut '" + shortText + "' from Meta to Ctrl because of conflict with " + conflict);
                    return reassignShortcut(shortText, longText, requestedKey, conflict, requestedGroup, requestedKey, newmodifier);
                }
            }
            for (int m : mods) {
                for (int k : keys) {
                    int newmodifier = getGroupModifier(m);
                    if (!findShortcut(k, newmodifier).isPresent()) {
                        Logging.info("Reassigning shortcut '" + shortText + "' from " + modifier + " to " + newmodifier +
                                " because of conflict with " + conflict);
                        return reassignShortcut(shortText, longText, requestedKey, conflict, m, k, newmodifier);
                    }
                }
            }
        } else {
            Shortcut newsc = new Shortcut(shortText, longText, requestedKey, requestedGroup, requestedKey, defaultModifier, true, false);
            newsc.saveDefault();
            shortcuts.add(newsc);
            return newsc;
        }

        return null;
    }

    private static int findNewOsxModifier(int requestedGroup) {
        switch (requestedGroup) {
            case CTRL: return KeyEvent.CTRL_DOWN_MASK;
            case ALT_CTRL: return KeyEvent.ALT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK;
            case CTRL_SHIFT: return KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK;
            case ALT_CTRL_SHIFT: return KeyEvent.ALT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK;
            default: return 0;
        }
    }

    private static Shortcut reassignShortcut(String shortText, String longText, int requestedKey, Shortcut conflict,
            int m, int k, int newmodifier) {
        Shortcut newsc = new Shortcut(shortText, longText, requestedKey, m, k, newmodifier, false, false);
        Logging.info(tr("Silent shortcut conflict: ''{0}'' moved by ''{1}'' to ''{2}''.",
            shortText, conflict.getShortText(), newsc.getKeyText()));
        newsc.saveDefault();
        shortcuts.add(newsc);
        return newsc;
    }

    /**
     * Replies the platform specific key stroke for the 'Copy' command, i.e.
     * 'Ctrl-C' on windows or 'Meta-C' on a Mac. null, if the platform specific
     * copy command isn't known.
     *
     * @return the platform specific key stroke for the  'Copy' command
     */
    public static KeyStroke getCopyKeyStroke() {
        return getKeyStrokeForShortKey("system:copy");
    }

    /**
     * Replies the platform specific key stroke for the 'Paste' command, i.e.
     * 'Ctrl-V' on windows or 'Meta-V' on a Mac. null, if the platform specific
     * paste command isn't known.
     *
     * @return the platform specific key stroke for the 'Paste' command
     */
    public static KeyStroke getPasteKeyStroke() {
        return getKeyStrokeForShortKey("system:paste");
    }

    /**
     * Replies the platform specific key stroke for the 'Cut' command, i.e.
     * 'Ctrl-X' on windows or 'Meta-X' on a Mac. null, if the platform specific
     * 'Cut' command isn't known.
     *
     * @return the platform specific key stroke for the 'Cut' command
     */
    public static KeyStroke getCutKeyStroke() {
        return getKeyStrokeForShortKey("system:cut");
    }

    private static KeyStroke getKeyStrokeForShortKey(String shortKey) {
        return shortcuts.stream()
                .filter(sc -> shortKey.equals(sc.getShortText()))
                .findAny()
                .map(Shortcut::getKeyStroke)
                .orElse(null);
    }
}
