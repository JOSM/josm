// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.swing.UIManager;

import org.openstreetmap.josm.Main;

/**
 * see PlatformHook.java
 */
public class PlatformHookOsx extends PlatformHookUnixoid implements PlatformHook, InvocationHandler {
    private static PlatformHookOsx ivhandler = new PlatformHookOsx();
    @Override
    public void preStartupHook(){
        // This will merge our MenuBar into the system menu.
        // MUST be set before Swing is initialized!
        // And will not work when one of the system independet LAFs is used.
        // They just insist on painting themselves...
        System.setProperty("apple.laf.useScreenMenuBar", "true");
    }
    @Override
    public void startupHook() {
        // Here we register callbacks for the menu entries in the system menu
        try {
            Class Ccom_apple_eawt_Application = Class.forName("com.apple.eawt.Application");
            Object Ocom_apple_eawt_Application = Ccom_apple_eawt_Application.getConstructor((Class[])null).newInstance((Object[])null);
            Class Ccom_apple_eawt_ApplicationListener = Class.forName("com.apple.eawt.ApplicationListener");
            Method MaddApplicationListener = Ccom_apple_eawt_Application.getDeclaredMethod("addApplicationListener", new Class[] { Ccom_apple_eawt_ApplicationListener });
            Object Oproxy = Proxy.newProxyInstance(PlatformHookOsx.class.getClassLoader(), new Class[] { Ccom_apple_eawt_ApplicationListener }, ivhandler);
            MaddApplicationListener.invoke(Ocom_apple_eawt_Application, new Object[] { Oproxy });
            Method MsetEnabledPreferencesMenu = Ccom_apple_eawt_Application.getDeclaredMethod("setEnabledPreferencesMenu", new Class[] { boolean.class });
            MsetEnabledPreferencesMenu.invoke(Ocom_apple_eawt_Application, new Object[] { Boolean.TRUE });
        } catch (Exception ex) {
            // Oops, what now?
            // We'll just ignore this for now. The user will still be able to close JOSM
            // by closing all its windows.
            System.out.println("Failed to register with OSX: " + ex);
        }
    }
    public Object invoke (Object proxy, Method method, Object[] args) throws Throwable {
        Boolean handled = Boolean.TRUE;
        //System.out.println("Going to handle method "+method+" (short: "+method.getName()+") with event "+args[0]);
        if (method.getName().equals("handleQuit")) {
            handled = !Main.breakBecauseUnsavedChanges();
        } else if (method.getName().equals("handleAbout")) {
            Main.main.menu.about.actionPerformed(null);
        } else if (method.getName().equals("handlePreferences")) {
            Main.main.menu.preferences.actionPerformed(null);
        } else
            return null;
        if (args[0] != null) {
            try {
                args[0].getClass().getDeclaredMethod("setHandled", new Class[] { boolean.class }).invoke(args[0], new Object[] { handled });
            } catch (Exception ex) {
                System.out.println("Failed to report handled event: " + ex);
            }
        }
        return null;
    }
    @Override
    public void openUrl(String url) throws IOException {
        // Ain't that KISS?
        Runtime.getRuntime().exec("open " + url);
    }
    @Override
    public void initShortcutGroups() {
        // Everything but Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_MENU is guesswork.
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_NONE),    Integer.toString(-1));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_HOTKEY),  Integer.toString(KeyEvent.CTRL_DOWN_MASK));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_MENU),    Integer.toString(KeyEvent.META_DOWN_MASK));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_EDIT),    Integer.toString(0));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_LAYER),   Integer.toString(KeyEvent.ALT_DOWN_MASK));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_DIRECT),  Integer.toString(0));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_MNEMONIC),Integer.toString(KeyEvent.ALT_DOWN_MASK));

        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT1+Shortcut.GROUP_NONE),       Integer.toString(-1));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT1+Shortcut.GROUP_HOTKEY),     Integer.toString(KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT1+Shortcut.GROUP_MENU),       Integer.toString(KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT1+Shortcut.GROUP_EDIT),       Integer.toString(KeyEvent.SHIFT_DOWN_MASK));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT1+Shortcut.GROUP_LAYER),      Integer.toString(KeyEvent.ALT_DOWN_MASK  | KeyEvent.SHIFT_DOWN_MASK));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT1+Shortcut.GROUP_DIRECT),     Integer.toString(KeyEvent.SHIFT_DOWN_MASK));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT1+Shortcut.GROUP_MNEMONIC),   Integer.toString(KeyEvent.ALT_DOWN_MASK));

        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT2+Shortcut.GROUP_NONE),       Integer.toString(-1));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT2+Shortcut.GROUP_HOTKEY),     Integer.toString(KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT2+Shortcut.GROUP_MENU),       Integer.toString(KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT2+Shortcut.GROUP_EDIT),       Integer.toString(KeyEvent.ALT_DOWN_MASK  | KeyEvent.SHIFT_DOWN_MASK));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT2+Shortcut.GROUP_LAYER),      Integer.toString(KeyEvent.ALT_DOWN_MASK));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT2+Shortcut.GROUP_DIRECT),     Integer.toString(KeyEvent.SHIFT_DOWN_MASK | KeyEvent.ALT_DOWN_MASK));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT2+Shortcut.GROUP_MNEMONIC),   Integer.toString(KeyEvent.ALT_DOWN_MASK));
    }
    @Override
    public void initSystemShortcuts() {
        // Yeah, it's a long, long list. And people always complain that OSX has no shortcuts.
        Shortcut.registerSystemShortcut("apple-reserved-01", "reserved", KeyEvent.VK_SPACE, KeyEvent.META_DOWN_MASK).setAutomatic(); // Show or hide the Spotlight search field (when multiple languages are installed, may rotate through enabled script systems).
        Shortcut.registerSystemShortcut("apple-reserved-02", "reserved", KeyEvent.VK_SPACE, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK).setAutomatic(); // Apple reserved.
        Shortcut.registerSystemShortcut("apple-reserved-03", "reserved", KeyEvent.VK_SPACE, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK).setAutomatic(); // Show the Spotlight search results window (when multiple languages are installed, may rotate through keyboard layouts and input methods within a script).
        Shortcut.registerSystemShortcut("apple-reserved-04", "reserved", KeyEvent.VK_SPACE, KeyEvent.META_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK).setAutomatic(); //  | Apple reserved.
        Shortcut.registerSystemShortcut("apple-reserved-05", "reserved", KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK).setAutomatic(); // Navigate through controls in a reverse direction. See "Keyboard Focus and Navigation."
        Shortcut.registerSystemShortcut("apple-reserved-06", "reserved", KeyEvent.VK_TAB, KeyEvent.META_DOWN_MASK).setAutomatic(); // Move forward to the next most recently used application in a list of open applications.
        Shortcut.registerSystemShortcut("apple-reserved-07", "reserved", KeyEvent.VK_TAB, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK).setAutomatic(); // Move backward through a list of open applications (sorted by recent use).
        Shortcut.registerSystemShortcut("apple-reserved-08", "reserved", KeyEvent.VK_TAB, KeyEvent.CTRL_DOWN_MASK).setAutomatic(); // Move focus to the next grouping of controls in a dialog or the next table (when Tab moves to the next cell). See Accessibility Overview.
        Shortcut.registerSystemShortcut("apple-reserved-09", "reserved", KeyEvent.VK_TAB, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK).setAutomatic(); // Move focus to the previous grouping of controls. See Accessibility Overview.
        Shortcut.registerSystemShortcut("apple-reserved-10", "reserved", KeyEvent.VK_ESCAPE, KeyEvent.META_DOWN_MASK).setAutomatic(); // Open Front Row.
        Shortcut.registerSystemShortcut("apple-reserved-11", "reserved", KeyEvent.VK_ESCAPE, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK).setAutomatic(); // Open the Force Quit dialog.
        Shortcut.registerSystemShortcut("apple-reserved-12", "reserved", KeyEvent.VK_F1, KeyEvent.CTRL_DOWN_MASK).setAutomatic(); // Toggle full keyboard access on or off. See Accessibility Overview.
        Shortcut.registerSystemShortcut("apple-reserved-13", "reserved", KeyEvent.VK_F2, KeyEvent.CTRL_DOWN_MASK).setAutomatic(); // Move focus to the menu bar. See Accessibility Overview.
        Shortcut.registerSystemShortcut("apple-reserved-14", "reserved", KeyEvent.VK_F3, KeyEvent.CTRL_DOWN_MASK).setAutomatic(); // Move focus to the Dock. See Accessibility Overview.
        Shortcut.registerSystemShortcut("apple-reserved-15", "reserved", KeyEvent.VK_F4, KeyEvent.CTRL_DOWN_MASK).setAutomatic(); // Move focus to the active (or next) window. See Accessibility Overview.
        Shortcut.registerSystemShortcut("apple-reserved-16", "reserved", KeyEvent.VK_F4, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK).setAutomatic(); // Move focus to the previously active window. See Accessibility Overview.
        Shortcut.registerSystemShortcut("apple-reserved-17", "reserved", KeyEvent.VK_F5, KeyEvent.CTRL_DOWN_MASK).setAutomatic(); // Move focus to the toolbar. See Accessibility Overview.
        Shortcut.registerSystemShortcut("apple-reserved-18", "reserved", KeyEvent.VK_F5, KeyEvent.META_DOWN_MASK).setAutomatic(); // Turn VoiceOver on or off. See Accessibility Overview.
        Shortcut.registerSystemShortcut("apple-reserved-19", "reserved", KeyEvent.VK_F6, KeyEvent.CTRL_DOWN_MASK).setAutomatic(); // Move focus to the first (or next) panel. See Accessibility Overview.
        Shortcut.registerSystemShortcut("apple-reserved-20", "reserved", KeyEvent.VK_F6, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK).setAutomatic(); // Move focus to the previous panel. See Accessibility Overview.
        Shortcut.registerSystemShortcut("apple-reserved-21", "reserved", KeyEvent.VK_F7, KeyEvent.CTRL_DOWN_MASK).setAutomatic(); // Temporarily override the current keyboard access mode in windows and dialogs. See Accessibility Overview.
        Shortcut.registerSystemShortcut("apple-reserved-22", "reserved", KeyEvent.VK_F9, 0).setAutomatic(); // Tile or untile all open windows.
        Shortcut.registerSystemShortcut("apple-reserved-23", "reserved", KeyEvent.VK_F10, 0).setAutomatic(); // Tile or untile all open windows in the currently active application.
        Shortcut.registerSystemShortcut("apple-reserved-24", "reserved", KeyEvent.VK_F11, 0).setAutomatic(); // Hide or show all open windows.
        Shortcut.registerSystemShortcut("apple-reserved-25", "reserved", KeyEvent.VK_F12, 0).setAutomatic(); // Hide or display Dashboard.
        Shortcut.registerSystemShortcut("apple-reserved-26", "reserved", KeyEvent.VK_DEAD_GRAVE, KeyEvent.META_DOWN_MASK).setAutomatic(); // Activate the next open window in the frontmost application. See "Window Layering."
        Shortcut.registerSystemShortcut("apple-reserved-27", "reserved", KeyEvent.VK_DEAD_GRAVE, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK).setAutomatic(); // Activate the previous open window in the frontmost application. See "Window Layering."
        Shortcut.registerSystemShortcut("apple-reserved-28", "reserved", KeyEvent.VK_DEAD_GRAVE, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK).setAutomatic(); // Move focus to the window drawer.
        Shortcut.registerSystemShortcut("apple-reserved-29", "reserved", KeyEvent.VK_MINUS, KeyEvent.META_DOWN_MASK).setAutomatic(); // Decrease the size of the selected item (equivalent to the Smaller command). See "The Format Menu."
        Shortcut.registerSystemShortcut("apple-reserved-30", "reserved", KeyEvent.VK_MINUS, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK).setAutomatic(); // Zoom out when screen zooming is on. See Accessibility Overview.

        Shortcut.registerSystemShortcut("system:align-left", "reserved", KeyEvent.VK_OPEN_BRACKET, KeyEvent.META_DOWN_MASK); // Left-align a selection (equivalent to the Align Left command). See "The Format Menu."
        Shortcut.registerSystemShortcut("system:align-right","reserved", KeyEvent.VK_CLOSE_BRACKET, KeyEvent.META_DOWN_MASK); // Right-align a selection (equivalent to the Align Right command). See "The Format Menu."
        // I found no KeyEvent for |
        //Shortcut.registerSystemCut("system:align-center", "reserved", '|', KeyEvent.META_DOWN_MASK); // Center-align a selection (equivalent to the Align Center command). See "The Format Menu."
        Shortcut.registerSystemShortcut("system:spelling", "reserved", KeyEvent.VK_COLON, KeyEvent.META_DOWN_MASK); // Display the Spelling window (equivalent to the Spelling command). See "The Edit Menu."
        Shortcut.registerSystemShortcut("system:spellcheck", "reserved", KeyEvent.VK_SEMICOLON, KeyEvent.META_DOWN_MASK); // Find misspelled words in the document (equivalent to the Check Spelling command). See "The Edit Menu."
        Shortcut.registerSystemShortcut("system:preferences", "reserved", KeyEvent.VK_COMMA, KeyEvent.META_DOWN_MASK).setAutomatic(); // Open the application's preferences window (equivalent to the Preferences command). See "The Application Menu."

        Shortcut.registerSystemShortcut("apple-reserved-31", "reserved", KeyEvent.VK_COMMA, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK | KeyEvent.ALT_DOWN_MASK).setAutomatic(); // Decrease screen contrast. See Accessibility Overview.
        Shortcut.registerSystemShortcut("apple-reserved-32", "reserved", KeyEvent.VK_PERIOD, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK | KeyEvent.ALT_DOWN_MASK).setAutomatic(); // Increase screen contrast. See Accessibility Overview.

        // I found no KeyEvent for ?
        //Shortcut.registerSystemCut("system:help", "reserved", '?', KeyEvent.META_DOWN_MASK).setAutomatic(); // Open the application's help in Help Viewer. See "The Help Menu."

        Shortcut.registerSystemShortcut("apple-reserved-33", "reserved", KeyEvent.VK_SLASH, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK).setAutomatic(); // Turn font smoothing on or off.
        Shortcut.registerSystemShortcut("apple-reserved-34", "reserved", KeyEvent.VK_EQUALS, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK).setAutomatic(); // Increase the size of the selected item (equivalent to the Bigger command). See "The Format Menu."
        Shortcut.registerSystemShortcut("apple-reserved-35", "reserved", KeyEvent.VK_EQUALS, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK).setAutomatic(); // Zoom in when screen zooming is on. See Accessibility Overview.
        Shortcut.registerSystemShortcut("apple-reserved-36", "reserved", KeyEvent.VK_3, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK).setAutomatic(); // Capture the screen to a file.
        Shortcut.registerSystemShortcut("apple-reserved-37", "reserved", KeyEvent.VK_3, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK).setAutomatic(); // Capture the screen to the Clipboard.
        Shortcut.registerSystemShortcut("apple-reserved-38", "reserved", KeyEvent.VK_4, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK).setAutomatic(); // Capture a selection to a file.
        Shortcut.registerSystemShortcut("apple-reserved-39", "reserved", KeyEvent.VK_4, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK).setAutomatic(); // Capture a selection to the Clipboard.
        Shortcut.registerSystemShortcut("apple-reserved-40", "reserved", KeyEvent.VK_8, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK).setAutomatic(); // Turn screen zooming on or off. See Accessibility Overview.
        Shortcut.registerSystemShortcut("apple-reserved-41", "reserved", KeyEvent.VK_8, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK).setAutomatic(); // Invert the screen colors. See Accessibility Overview.

        Shortcut.registerSystemShortcut("system:selectall", "reserved", KeyEvent.VK_A, KeyEvent.META_DOWN_MASK); // Highlight every item in a document or window, or all characters in a text field (equivalent to the Select All command). See "The Edit Menu."
        Shortcut.registerSystemShortcut("system:bold", "reserved", KeyEvent.VK_B, KeyEvent.META_DOWN_MASK); // Boldface the selected text or toggle boldfaced text on and off (equivalent to the Bold command). See "The Edit Menu."
        Shortcut.registerSystemShortcut("system:copy", "reserved", KeyEvent.VK_C, KeyEvent.META_DOWN_MASK); // Duplicate the selected data and store on the Clipboard (equivalent to the Copy command). See "The Edit Menu."
        Shortcut.registerSystemShortcut("system:colors", "reserved", KeyEvent.VK_C, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK); // Display the Colors window (equivalent to the Show Colors command). See "The Format Menu."
        Shortcut.registerSystemShortcut("system:copystyle", "reserved", KeyEvent.VK_C, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK); // Copy the style of the selected text (equivalent to the Copy Style command). See "The Format Menu."
        Shortcut.registerSystemShortcut("system:copyformat", "reserved", KeyEvent.VK_C, KeyEvent.META_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK).setAutomatic(); // Copy the formatting settings of the selected item and store on the Clipboard (equivalent to the Copy Ruler command). See "The Format Menu."

        Shortcut.registerSystemShortcut("apple-reserved-42", "reserved", KeyEvent.VK_D, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK).setAutomatic(); // Show or hide the Dock. See "The Dock."

        Shortcut.registerSystemShortcut("system:dictionarylookup", "reserved", KeyEvent.VK_D, KeyEvent.META_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK); // Display the definition of the selected word in the Dictionary application.
        Shortcut.registerSystemShortcut("system:findselected", "reserved", KeyEvent.VK_E, KeyEvent.META_DOWN_MASK); // Use the selection for a find operation. See "Find Windows."
        Shortcut.registerSystemShortcut("system:find", "reserved", KeyEvent.VK_F, KeyEvent.META_DOWN_MASK); // Open a Find window (equivalent to the Find command). See "The Edit Menu."
        Shortcut.registerSystemShortcut("system:search", "reserved", KeyEvent.VK_F, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK); // Jump to the search field control. See "Search Fields."
        Shortcut.registerSystemShortcut("system:findnext", "reserved", KeyEvent.VK_G, KeyEvent.META_DOWN_MASK); // Find the next occurrence of the selection (equivalent to the Find Next command). See "The Edit Menu."
        Shortcut.registerSystemShortcut("system:findprev", "reserved", KeyEvent.VK_G, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK); // Find the previous occurrence of the selection (equivalent to the Find Previous command). See "The Edit Menu."
        Shortcut.registerSystemShortcut("system:hide", "reserved", KeyEvent.VK_H, KeyEvent.META_DOWN_MASK).setAutomatic(); // Hide the windows of the currently running application (equivalent to the Hide ApplicationName command). See "The Application Menu."
        Shortcut.registerSystemShortcut("system:hideothers", "reserved", KeyEvent.VK_H, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK).setAutomatic(); // Hide the windows of all other running applications (equivalent to the Hide Others command). See "The Application Menu."
        // What about applications that have italic text AND info windows?
        //Shortcut.registerSystemCut("system:italic", "reserved", KeyEvent.VK_I, KeyEvent.META_DOWN_MASK); // Italicize the selected text or toggle italic text on or off (equivalent to the Italic command). See "The Format Menu."
        Shortcut.registerSystemShortcut("system:info", "reserved", KeyEvent.VK_I, KeyEvent.META_DOWN_MASK); // Display an Info window. See "Inspector Windows."
        Shortcut.registerSystemShortcut("system:inspector", "reserved", KeyEvent.VK_I, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK); // Display an inspector window. See "Inspector Windows."
        Shortcut.registerSystemShortcut("system:toselection", "reserved", KeyEvent.VK_J, KeyEvent.META_DOWN_MASK); // Scroll to a selection.
        Shortcut.registerSystemShortcut("system:minimize", "reserved", KeyEvent.VK_M, KeyEvent.META_DOWN_MASK); // Minimize the active window to the Dock (equivalent to the Minimize command). See "The Window Menu."
        Shortcut.registerSystemShortcut("system:minimizeall", "reserved", KeyEvent.VK_M, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK); // Minimize all windows of the active application to the Dock (equivalent to the Minimize All command). See "The Window Menu."
        Shortcut.registerSystemShortcut("system:new", "reserved", KeyEvent.VK_N, KeyEvent.META_DOWN_MASK); // Open a new document (equivalent to the New command). See "The File Menu."
        Shortcut.registerSystemShortcut("system:open", "reserved", KeyEvent.VK_O, KeyEvent.META_DOWN_MASK); // Display a dialog for choosing a document to open (equivalent to the Open command). See "The File Menu."
        Shortcut.registerSystemShortcut("system:print", "reserved", KeyEvent.VK_P, KeyEvent.META_DOWN_MASK); // Display the Print dialog (equivalent to the Print command). See "The File Menu."
        Shortcut.registerSystemShortcut("system:printsetup", "reserved", KeyEvent.VK_P, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK); // Display a dialog for specifying printing parameters (equivalent to the Page Setup command). See "The File Menu."
        Shortcut.registerSystemShortcut("system:menuexit", "reserved", KeyEvent.VK_Q, KeyEvent.META_DOWN_MASK).setAutomatic(); // Quit the application (equivalent to the Quit command). See "The Application Menu."

        Shortcut.registerSystemShortcut("apple-reserved-43", "reserved", KeyEvent.VK_Q, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK).setAutomatic(); // Log out the current user (equivalent to the Log Out command).
        Shortcut.registerSystemShortcut("apple-reserved-44", "reserved", KeyEvent.VK_Q, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK | KeyEvent.ALT_DOWN_MASK).setAutomatic(); // Log out the current user without confirmation.

        Shortcut.registerSystemShortcut("system:save", "reserved", KeyEvent.VK_S, KeyEvent.META_DOWN_MASK); // Save the active document (equivalent to the Save command). See "The File Menu."
        Shortcut.registerSystemShortcut("system:saveas", "reserved", KeyEvent.VK_S, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK); // Display the Save dialog (equivalent to the Save As command). See "The File Menu."
        Shortcut.registerSystemShortcut("system:fonts", "reserved", KeyEvent.VK_T, KeyEvent.META_DOWN_MASK); // Display the Fonts window (equivalent to the Show Fonts command). See "The Format Menu."
        Shortcut.registerSystemShortcut("system:toggletoolbar", "reserved", KeyEvent.VK_T, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK); // Show or hide a toolbar (equivalent to the Show/Hide Toolbar command). See "The View Menu" and "Toolbars."
        Shortcut.registerSystemShortcut("system:underline", "reserved", KeyEvent.VK_U, KeyEvent.META_DOWN_MASK); // Underline the selected text or turn underlining on or off (equivalent to the Underline command). See "The Format Menu."
        Shortcut.registerSystemShortcut("system:paste", "reserved", KeyEvent.VK_V, KeyEvent.META_DOWN_MASK); // Insert the Clipboard contents at the insertion point (equivalent to the Paste command). See "The File Menu."
        Shortcut.registerSystemShortcut("system:pastestyle", "reserved", KeyEvent.VK_V, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK); // Apply the style of one object to the selected object (equivalent to the Paste Style command). See "The Format Menu."
        Shortcut.registerSystemShortcut("system:pastemwithoutstyle", "reserved", KeyEvent.VK_V, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK | KeyEvent.ALT_DOWN_MASK); // Apply the style of the surrounding text to the inserted object (equivalent to the Paste and Match Style command). See "The Edit Menu."
        Shortcut.registerSystemShortcut("system:pasteformatting", "reserved", KeyEvent.VK_V, KeyEvent.META_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK); // Apply formatting settings to the selected object (equivalent to the Paste Ruler command). See "The Format Menu."
        Shortcut.registerSystemShortcut("system:closewindow", "reserved", KeyEvent.VK_W, KeyEvent.META_DOWN_MASK); // Close the active window (equivalent to the Close command). See "The File Menu."
        Shortcut.registerSystemShortcut("system:closefile", "reserved", KeyEvent.VK_W, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK); // Close a file and its associated windows (equivalent to the Close File command). See "The File Menu."
        Shortcut.registerSystemShortcut("system:closeallwindows", "reserved", KeyEvent.VK_W, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK); // Close all windows in the application (equivalent to the Close All command). See "The File Menu."
        Shortcut.registerSystemShortcut("system:cut", "reserved", KeyEvent.VK_X, KeyEvent.META_DOWN_MASK); // Remove the selection and store on the Clipboard (equivalent to the Cut command). See "The Edit Menu."
        Shortcut.registerSystemShortcut("system:undo", "reserved", KeyEvent.VK_Z, KeyEvent.META_DOWN_MASK); // Reverse the effect of the user's previous operation (equivalent to the Undo command). See "The Edit Menu."
        Shortcut.registerSystemShortcut("system:redo", "reserved", KeyEvent.VK_Z, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK); // Reverse the effect of the last Undo command (equivalent to the Redo command). See "The Edit Menu."

        Shortcut.registerSystemShortcut("apple-reserved-45", "reserved", KeyEvent.VK_RIGHT, KeyEvent.META_DOWN_MASK).setAutomatic(); // Change the keyboard layout to current layout of Roman script.
        //Shortcut.registerSystemCut("apple-reserved-46", "reserved", KeyEvent.VK_RIGHT, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK).setAutomatic(); // Extend selection to the next semantic unit, typically the end of the current line.
        //Shortcut.registerSystemCut("apple-reserved-47", "reserved", KeyEvent.VK_RIGHT, KeyEvent.SHIFT_DOWN_MASK).setAutomatic(); // Extend selection one character to the right.
        //Shortcut.registerSystemCut("apple-reserved-48", "reserved", KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK).setAutomatic(); // Extend selection to the end of the current word, then to the end of the next word.

        Shortcut.registerSystemShortcut("system:movefocusright", "reserved", KeyEvent.VK_RIGHT, KeyEvent.CTRL_DOWN_MASK); // Move focus to another value or cell within a view, such as a table. See Accessibility Overview.

        Shortcut.registerSystemShortcut("apple-reserved-49", "reserved", KeyEvent.VK_LEFT, KeyEvent.META_DOWN_MASK).setAutomatic(); // Change the keyboard layout to current layout of system script.
        //Shortcut.registerSystemCut("apple-reserved-50", "reserved", KeyEvent.VK_LEFT, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK).setAutomatic(); // Extend selection to the previous semantic unit, typically the beginning of the current line.
        //Shortcut.registerSystemCut("apple-reserved-51", "reserved", KeyEvent.VK_LEFT, KeyEvent.SHIFT_DOWN_MASK).setAutomatic(); // Extend selection one character to the left.
        //Shortcut.registerSystemCut("apple-reserved-52", "reserved", KeyEvent.VK_LEFT, KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK).setAutomatic(); // Extend selection to the beginning of the current word, then to the beginning of the previous word.

        Shortcut.registerSystemShortcut("system:movefocusleft", "reserved", KeyEvent.VK_LEFT, KeyEvent.CTRL_DOWN_MASK); // Move focus to another value or cell within a view, such as a table. See Accessibility Overview.

        //Shortcut.registerSystemCut("apple-reserved-53", "reserved", KeyEvent.VK_UP, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK).setAutomatic(); // Extend selection upward in the next semantic unit, typically the beginning of the document.
        //Shortcut.registerSystemCut("apple-reserved-54", "reserved", KeyEvent.VK_UP, KeyEvent.SHIFT_DOWN_MASK).setAutomatic(); // Extend selection to the line above, to the nearest character boundary at the same horizontal location.
        //Shortcut.registerSystemCut("apple-reserved-55", "reserved", KeyEvent.VK_UP, KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK).setAutomatic(); // Extend selection to the beginning of the current paragraph, then to the beginning of the next paragraph.

        Shortcut.registerSystemShortcut("system:movefocusup", "reserved", KeyEvent.VK_UP, KeyEvent.CTRL_DOWN_MASK); // Move focus to another value or cell within a view, such as a table. See Accessibility Overview.

        //Shortcut.registerSystemCut("apple-reserved-56", "reserved", KeyEvent.VK_DOWN, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK).setAutomatic(); // Extend selection downward in the next semantic unit, typically the end of the document.
        //Shortcut.registerSystemCut("apple-reserved-57", "reserved", KeyEvent.VK_DOWN, KeyEvent.SHIFT_DOWN_MASK).setAutomatic(); // Extend selection to the line below, to the nearest character boundary at the same horizontal location.
        //Shortcut.registerSystemCut("apple-reserved-58", "reserved", KeyEvent.VK_DOWN, KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK).setAutomatic(); // Extend selection to the end of the current paragraph, then to the end of the next paragraph (include the blank line between paragraphs in cut, copy, and paste operations).

        Shortcut.registerSystemShortcut("system:movefocusdown", "reserved", KeyEvent.VK_DOWN, KeyEvent.CTRL_DOWN_MASK); // Move focus to another value or cell within a view, such as a table. See Accessibility Overview.

        Shortcut.registerSystemShortcut("system:about", "reserved", 0, -1).setAutomatic(); // About

        Shortcut.registerSystemShortcut("view:zoomin", "reserved", KeyEvent.VK_ADD, KeyEvent.META_DOWN_MASK); // Zoom in
        Shortcut.registerSystemShortcut("view:zoomout", "reserved", KeyEvent.VK_SUBTRACT, KeyEvent.META_DOWN_MASK); // Zoom out

    }
    @Override
    public String makeTooltip(String name, Shortcut sc) {
        String lafid = UIManager.getLookAndFeel().getID();
        boolean canHtml = true;
        // "Mac" is the native LAF, "Aqua" is Quaqua. Both use native menus with native tooltips.
        if (lafid.contains("Mac") || lafid.contains("Aqua")) {
            canHtml = false;
        }
        String result = "";
        if (canHtml) {
            result += "<html>";
        }
        result += name;
        if (sc != null && sc.getKeyText().length() != 0) {
            result += " ";
            if (canHtml) {
                result += "<font size='-2'>";
            }
            result += "("+sc.getKeyText()+")";
            if (canHtml) {
                result += "</font>";
            }
        }
        if (canHtml) {
            result += "&nbsp;</html>";
        }
        return result;
    }
}