// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.Utils.getSystemProperty;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.io.CertificateAmendment.NativeCertAmend;

/**
 * {@code PlatformHook} implementation for Apple Mac OS X systems.
 * @since 1023
 */
public class PlatformHookOsx implements PlatformHook, InvocationHandler {

    private String oSBuildNumber;

    private NativeOsCallback osCallback;

    @Override
    public Platform getPlatform() {
        return Platform.OSX;
    }

    @Override
    public void preStartupHook() {
        // This will merge our MenuBar into the system menu.
        // MUST be set before Swing is initialized!
        // And will not work when one of the system independent LAFs is used.
        // They just insist on painting themselves...
        Utils.updateSystemProperty("apple.laf.useScreenMenuBar", "true");
        Utils.updateSystemProperty("apple.awt.application.name", "JOSM");
    }

    @Override
    public void startupHook(JavaExpirationCallback callback) {
        // Here we register callbacks for the menu entries in the system menu and file opening through double-click
        // https://openjdk.java.net/jeps/272
        // https://bugs.openjdk.java.net/browse/JDK-8048731
        // https://cr.openjdk.java.net/~azvegint/jdk/9/8143227/10/jdk/
        // This method must be cleaned up after we switch to Java 9
        try {
            Class<?> eawtApplication = Class.forName("com.apple.eawt.Application");
            Class<?> quitHandler = findHandlerClass("QuitHandler");
            Class<?> aboutHandler = findHandlerClass("AboutHandler");
            Class<?> openFilesHandler = findHandlerClass("OpenFilesHandler");
            Class<?> preferencesHandler = findHandlerClass("PreferencesHandler");
            Object proxy = Proxy.newProxyInstance(PlatformHookOsx.class.getClassLoader(), new Class<?>[] {
                quitHandler, aboutHandler, openFilesHandler, preferencesHandler}, this);
            Object appli = eawtApplication.getConstructor((Class[]) null).newInstance((Object[]) null);
            if (Utils.getJavaVersion() < 9) {
                setHandlers(eawtApplication, quitHandler, aboutHandler, openFilesHandler, preferencesHandler, proxy, appli);
                // this method has been deprecated, but without replacement. To remove with Java 9 migration
                eawtApplication.getDeclaredMethod("setEnabledPreferencesMenu", boolean.class).invoke(appli, Boolean.TRUE);
            } else if (!GraphicsEnvironment.isHeadless()) {
                setHandlers(Desktop.class, quitHandler, aboutHandler, openFilesHandler, preferencesHandler, proxy, Desktop.getDesktop());
            }
            // setup the dock icon. It is automatically set with application bundle and Web start but we need
            // to do it manually if run with `java -jar``
            eawtApplication.getDeclaredMethod("setDockIconImage", Image.class).invoke(appli, ImageProvider.get("logo").getImage());
            // enable full screen
            enableOSXFullscreen(MainApplication.getMainFrame());
        } catch (ReflectiveOperationException | SecurityException | IllegalArgumentException ex) {
            // We'll just ignore this for now. The user will still be able to close JOSM by closing all its windows.
            Logging.warn("Failed to register with OSX: " + ex);
        }
        checkExpiredJava(callback);
    }

    @Override
    public int getMenuShortcutKeyMaskEx() {
        return KeyEvent.META_DOWN_MASK;
    }

    /**
     * Registers Apple handlers.
     * @param appClass application class
     * @param quitHandler quit handler class
     * @param aboutHandler about handler class
     * @param openFilesHandler open file handler class
     * @param preferencesHandler preferences handler class
     * @param proxy proxy
     * @param appInstance application instance (instance of {@code appClass})
     * @throws IllegalAccessException in case of reflection error
     * @throws InvocationTargetException in case of reflection error
     * @throws NoSuchMethodException if any {@code set*Handler} method cannot be found
     */
    protected void setHandlers(Class<?> appClass, Class<?> quitHandler, Class<?> aboutHandler,
            Class<?> openFilesHandler, Class<?> preferencesHandler, Object proxy, Object appInstance)
                    throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        appClass.getDeclaredMethod("setQuitHandler", quitHandler).invoke(appInstance, proxy);
        appClass.getDeclaredMethod("setAboutHandler", aboutHandler).invoke(appInstance, proxy);
        appClass.getDeclaredMethod("setOpenFileHandler", openFilesHandler).invoke(appInstance, proxy);
        appClass.getDeclaredMethod("setPreferencesHandler", preferencesHandler).invoke(appInstance, proxy);
    }

    /**
     * Find Apple handler class in {@code com.apple.eawt} or {@code java.awt.desktop} packages.
     * @param className simple class name
     * @return class
     * @throws ClassNotFoundException if the handler class cannot be found
     */
    protected Class<?> findHandlerClass(String className) throws ClassNotFoundException {
        try {
            // Java 8 handlers
            return Class.forName("com.apple.eawt."+className);
        } catch (ClassNotFoundException e) {
            Logging.trace(e);
            // Java 9 handlers
            return Class.forName("java.awt.desktop."+className);
        }
    }

    /**
     * Enables fullscreen support for the given window.
     * @param window The window for which full screen will be available
     * @since 7482
     */
    public static void enableOSXFullscreen(Window window) {
        CheckParameterUtil.ensureParameterNotNull(window, "window");
        try {
            // http://stackoverflow.com/a/8693890/2257172
            Class<?> eawtFullScreenUtilities = Class.forName("com.apple.eawt.FullScreenUtilities");
            eawtFullScreenUtilities.getDeclaredMethod("setWindowCanFullScreen",
                    Window.class, boolean.class).invoke(eawtFullScreenUtilities, window, Boolean.TRUE);
        } catch (ReflectiveOperationException | SecurityException | IllegalArgumentException e) {
            Logging.warn("Failed to register with OSX: " + e);
        }
    }

    @Override
    public void setNativeOsCallback(NativeOsCallback callback) {
        osCallback = Objects.requireNonNull(callback);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (Logging.isDebugEnabled()) {
            Logging.debug("OSX handler: {0} - {1}", method.getName(), Arrays.toString(args));
        }
        switch (method.getName()) {
        case "openFiles":
            if (args[0] != null) {
                try {
                    Object oFiles = args[0].getClass().getMethod("getFiles").invoke(args[0]);
                    if (oFiles instanceof List) {
                        osCallback.openFiles((List<File>) oFiles);
                    }
                } catch (ReflectiveOperationException | SecurityException | IllegalArgumentException ex) {
                    Logging.warn("Failed to access open files event: " + ex);
                }
            }
            break;
        case "handleQuitRequestWith":
            boolean closed = osCallback.handleQuitRequest();
            if (args[1] != null) {
                try {
                    args[1].getClass().getDeclaredMethod(closed ? "performQuit" : "cancelQuit").invoke(args[1]);
                } catch (IllegalAccessException e) {
                    Logging.debug(e);
                    // with Java 9, module java.desktop does not export com.apple.eawt, use new Desktop API instead
                    Class.forName("java.awt.desktop.QuitResponse").getMethod(closed ? "performQuit" : "cancelQuit").invoke(args[1]);
                }
            }
            break;
        case "handleAbout":
            osCallback.handleAbout();
            break;
        case "handlePreferences":
            osCallback.handlePreferences();
            break;
        default:
            Logging.warn("OSX unsupported method: "+method.getName());
        }
        return null;
    }

    @Override
    public void openUrl(String url) throws IOException {
        Runtime.getRuntime().exec(new String[]{"open", url});
    }

    @Override
    public void initSystemShortcuts() {
        // CHECKSTYLE.OFF: LineLength
        auto(Shortcut.registerSystemShortcut("apple-reserved-01", tr("reserved"), KeyEvent.VK_SPACE, KeyEvent.META_DOWN_MASK)); // Show or hide the Spotlight search field (when multiple languages are installed, may rotate through enabled script systems).
        auto(Shortcut.registerSystemShortcut("apple-reserved-02", tr("reserved"), KeyEvent.VK_SPACE, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)); // Apple reserved.
        auto(Shortcut.registerSystemShortcut("apple-reserved-03", tr("reserved"), KeyEvent.VK_SPACE, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)); // Show the Spotlight search results window (when multiple languages are installed, may rotate through keyboard layouts and input methods within a script).
        auto(Shortcut.registerSystemShortcut("apple-reserved-04", tr("reserved"), KeyEvent.VK_SPACE, KeyEvent.META_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK)); //  | Apple reserved.
        auto(Shortcut.registerSystemShortcut("apple-reserved-05", tr("reserved"), KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK)); // Navigate through controls in a reverse direction. See "Keyboard Focus and Navigation."
        auto(Shortcut.registerSystemShortcut("apple-reserved-06", tr("reserved"), KeyEvent.VK_TAB, KeyEvent.META_DOWN_MASK)); // Move forward to the next most recently used application in a list of open applications.
        auto(Shortcut.registerSystemShortcut("apple-reserved-07", tr("reserved"), KeyEvent.VK_TAB, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)); // Move backward through a list of open applications (sorted by recent use).
        auto(Shortcut.registerSystemShortcut("apple-reserved-08", tr("reserved"), KeyEvent.VK_TAB, KeyEvent.CTRL_DOWN_MASK)); // Move focus to the next grouping of controls in a dialog or the next table (when Tab moves to the next cell). See Accessibility Overview.
        auto(Shortcut.registerSystemShortcut("apple-reserved-09", tr("reserved"), KeyEvent.VK_TAB, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)); // Move focus to the previous grouping of controls. See Accessibility Overview.
        auto(Shortcut.registerSystemShortcut("apple-reserved-10", tr("reserved"), KeyEvent.VK_ESCAPE, KeyEvent.META_DOWN_MASK)); // Open Front Row.
        auto(Shortcut.registerSystemShortcut("apple-reserved-11", tr("reserved"), KeyEvent.VK_ESCAPE, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)); // Open the Force Quit dialog.
        auto(Shortcut.registerSystemShortcut("apple-reserved-12", tr("reserved"), KeyEvent.VK_F1, KeyEvent.CTRL_DOWN_MASK)); // Toggle full keyboard access on or off. See Accessibility Overview.
        auto(Shortcut.registerSystemShortcut("apple-reserved-13", tr("reserved"), KeyEvent.VK_F2, KeyEvent.CTRL_DOWN_MASK)); // Move focus to the menu bar. See Accessibility Overview.
        auto(Shortcut.registerSystemShortcut("apple-reserved-14", tr("reserved"), KeyEvent.VK_F3, KeyEvent.CTRL_DOWN_MASK)); // Move focus to the Dock. See Accessibility Overview.
        auto(Shortcut.registerSystemShortcut("apple-reserved-15", tr("reserved"), KeyEvent.VK_F4, KeyEvent.CTRL_DOWN_MASK)); // Move focus to the active (or next) window. See Accessibility Overview.
        auto(Shortcut.registerSystemShortcut("apple-reserved-16", tr("reserved"), KeyEvent.VK_F4, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)); // Move focus to the previously active window. See Accessibility Overview.
        auto(Shortcut.registerSystemShortcut("apple-reserved-17", tr("reserved"), KeyEvent.VK_F5, KeyEvent.CTRL_DOWN_MASK)); // Move focus to the toolbar. See Accessibility Overview.
        auto(Shortcut.registerSystemShortcut("apple-reserved-18", tr("reserved"), KeyEvent.VK_F5, KeyEvent.META_DOWN_MASK)); // Turn VoiceOver on or off. See Accessibility Overview.
        auto(Shortcut.registerSystemShortcut("apple-reserved-19", tr("reserved"), KeyEvent.VK_F6, KeyEvent.CTRL_DOWN_MASK)); // Move focus to the first (or next) panel. See Accessibility Overview.
        auto(Shortcut.registerSystemShortcut("apple-reserved-20", tr("reserved"), KeyEvent.VK_F6, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)); // Move focus to the previous panel. See Accessibility Overview.
        auto(Shortcut.registerSystemShortcut("apple-reserved-21", tr("reserved"), KeyEvent.VK_F7, KeyEvent.CTRL_DOWN_MASK)); // Temporarily override the current keyboard access mode in windows and dialogs. See Accessibility Overview.
        //auto(Shortcut.registerSystemShortcut("apple-reserved-22", tr("reserved"), KeyEvent.VK_F9, 0)); // Tile or untile all open windows.
        //auto(Shortcut.registerSystemShortcut("apple-reserved-23", tr("reserved"), KeyEvent.VK_F10, 0)); // Tile or untile all open windows in the currently active application.
        //auto(Shortcut.registerSystemShortcut("apple-reserved-24", tr("reserved"), KeyEvent.VK_F11, 0)); // Hide or show all open windows.
        //auto(Shortcut.registerSystemShortcut("apple-reserved-25", tr("reserved"), KeyEvent.VK_F12, 0)); // Hide or display Dashboard.
        auto(Shortcut.registerSystemShortcut("apple-reserved-26", tr("reserved"), KeyEvent.VK_DEAD_GRAVE, KeyEvent.META_DOWN_MASK)); // Activate the next open window in the frontmost application. See "Window Layering."
        auto(Shortcut.registerSystemShortcut("apple-reserved-27", tr("reserved"), KeyEvent.VK_DEAD_GRAVE, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)); // Activate the previous open window in the frontmost application. See "Window Layering."
        auto(Shortcut.registerSystemShortcut("apple-reserved-28", tr("reserved"), KeyEvent.VK_DEAD_GRAVE, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)); // Move focus to the window drawer.
        //auto(Shortcut.registerSystemShortcut("apple-reserved-29", tr("reserved"), KeyEvent.VK_MINUS, KeyEvent.META_DOWN_MASK)); // Decrease the size of the selected item (equivalent to the Smaller command). See "The Format Menu."
        auto(Shortcut.registerSystemShortcut("apple-reserved-30", tr("reserved"), KeyEvent.VK_MINUS, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)); // Zoom out when screen zooming is on. See Accessibility Overview.

        //Shortcut.registerSystemShortcut("system:align-left", tr("reserved"), KeyEvent.VK_OPEN_BRACKET, KeyEvent.META_DOWN_MASK); // Left-align a selection (equivalent to the Align Left command). See "The Format Menu."
        //Shortcut.registerSystemShortcut("system:align-right",tr("reserved"), KeyEvent.VK_CLOSE_BRACKET, KeyEvent.META_DOWN_MASK); // Right-align a selection (equivalent to the Align Right command). See "The Format Menu."
        // I found no KeyEvent for |
        //Shortcut.registerSystemCut("system:align-center", tr("reserved"), '|', KeyEvent.META_DOWN_MASK); // Center-align a selection (equivalent to the Align Center command). See "The Format Menu."
        //Shortcut.registerSystemShortcut("system:spelling", tr("reserved"), KeyEvent.VK_COLON, KeyEvent.META_DOWN_MASK); // Display the Spelling window (equivalent to the Spelling command). See "The Edit Menu."
        //Shortcut.registerSystemShortcut("system:spellcheck", tr("reserved"), KeyEvent.VK_SEMICOLON, KeyEvent.META_DOWN_MASK); // Find misspelled words in the document (equivalent to the Check Spelling command). See "The Edit Menu."
        auto(Shortcut.registerSystemShortcut("system:preferences", tr("reserved"), KeyEvent.VK_COMMA, KeyEvent.META_DOWN_MASK)); // Open the application's preferences window (equivalent to the Preferences command). See "The Application Menu."

        auto(Shortcut.registerSystemShortcut("apple-reserved-31", tr("reserved"), KeyEvent.VK_COMMA, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)); // Decrease screen contrast. See Accessibility Overview.
        auto(Shortcut.registerSystemShortcut("apple-reserved-32", tr("reserved"), KeyEvent.VK_PERIOD, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)); // Increase screen contrast. See Accessibility Overview.

        // I found no KeyEvent for ?
        //auto(Shortcut.registerSystemCut("system:help", tr("reserved"), '?', KeyEvent.META_DOWN_MASK)); // Open the application's help in Help Viewer. See "The Help Menu."

        auto(Shortcut.registerSystemShortcut("apple-reserved-33", tr("reserved"), KeyEvent.VK_SLASH, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)); // Turn font smoothing on or off.
        auto(Shortcut.registerSystemShortcut("apple-reserved-34", tr("reserved"), KeyEvent.VK_EQUALS, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)); // Increase the size of the selected item (equivalent to the Bigger command). See "The Format Menu."
        auto(Shortcut.registerSystemShortcut("apple-reserved-35", tr("reserved"), KeyEvent.VK_EQUALS, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)); // Zoom in when screen zooming is on. See Accessibility Overview.
        auto(Shortcut.registerSystemShortcut("apple-reserved-36", tr("reserved"), KeyEvent.VK_3, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)); // Capture the screen to a file.
        auto(Shortcut.registerSystemShortcut("apple-reserved-37", tr("reserved"), KeyEvent.VK_3, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK)); // Capture the screen to the Clipboard.
        auto(Shortcut.registerSystemShortcut("apple-reserved-38", tr("reserved"), KeyEvent.VK_4, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)); // Capture a selection to a file.
        auto(Shortcut.registerSystemShortcut("apple-reserved-39", tr("reserved"), KeyEvent.VK_4, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK)); // Capture a selection to the Clipboard.
        auto(Shortcut.registerSystemShortcut("apple-reserved-40", tr("reserved"), KeyEvent.VK_8, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)); // Turn screen zooming on or off. See Accessibility Overview.
        auto(Shortcut.registerSystemShortcut("apple-reserved-41", tr("reserved"), KeyEvent.VK_8, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK)); // Invert the screen colors. See Accessibility Overview.

        Shortcut.registerSystemShortcut("system:selectall", tr("reserved"), KeyEvent.VK_A, KeyEvent.META_DOWN_MASK); // Highlight every item in a document or window, or all characters in a text field (equivalent to the Select All command). See "The Edit Menu."
        //Shortcut.registerSystemShortcut("system:bold", tr("reserved"), KeyEvent.VK_B, KeyEvent.META_DOWN_MASK); // Boldface the selected text or toggle boldfaced text on and off (equivalent to the Bold command). See "The Edit Menu."
        Shortcut.registerSystemShortcut("system:copy", tr("reserved"), KeyEvent.VK_C, KeyEvent.META_DOWN_MASK); // Duplicate the selected data and store on the Clipboard (equivalent to the Copy command). See "The Edit Menu."
        //Shortcut.registerSystemShortcut("system:colors", tr("reserved"), KeyEvent.VK_C, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK); // Display the Colors window (equivalent to the Show Colors command). See "The Format Menu."
        //Shortcut.registerSystemShortcut("system:copystyle", tr("reserved"), KeyEvent.VK_C, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK); // Copy the style of the selected text (equivalent to the Copy Style command). See "The Format Menu."
        //Shortcut.registerSystemShortcut("system:copyformat", tr("reserved"), KeyEvent.VK_C, KeyEvent.META_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK)); // Copy the formatting settings of the selected item and store on the Clipboard (equivalent to the Copy Ruler command). See "The Format Menu."

        auto(Shortcut.registerSystemShortcut("apple-reserved-42", tr("reserved"), KeyEvent.VK_D, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)); // Show or hide the Dock. See "The Dock."

        Shortcut.registerSystemShortcut("system:dictionarylookup", tr("reserved"), KeyEvent.VK_D, KeyEvent.META_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK); // Display the definition of the selected word in the Dictionary application.
        //Shortcut.registerSystemShortcut("system:findselected", tr("reserved"), KeyEvent.VK_E, KeyEvent.META_DOWN_MASK); // Use the selection for a find operation. See "Find Windows."
        Shortcut.registerSystemShortcut("system:find", tr("reserved"), KeyEvent.VK_F, KeyEvent.META_DOWN_MASK); // Open a Find window (equivalent to the Find command). See "The Edit Menu."
        Shortcut.registerSystemShortcut("system:search", tr("reserved"), KeyEvent.VK_F, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK); // Jump to the search field control. See "Search Fields."
        //Shortcut.registerSystemShortcut("system:findnext", tr("reserved"), KeyEvent.VK_G, KeyEvent.META_DOWN_MASK); // Find the next occurrence of the selection (equivalent to the Find Next command). See "The Edit Menu."
        //Shortcut.registerSystemShortcut("system:findprev", tr("reserved"), KeyEvent.VK_G, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK); // Find the previous occurrence of the selection (equivalent to the Find Previous command). See "The Edit Menu."
        auto(Shortcut.registerSystemShortcut("system:hide", tr("reserved"), KeyEvent.VK_H, KeyEvent.META_DOWN_MASK)); // Hide the windows of the currently running application (equivalent to the Hide ApplicationName command). See "The Application Menu."
        auto(Shortcut.registerSystemShortcut("system:hideothers", tr("reserved"), KeyEvent.VK_H, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)); // Hide the windows of all other running applications (equivalent to the Hide Others command). See "The Application Menu."
        // What about applications that have italic text AND info windows?
        //Shortcut.registerSystemCut("system:italic", tr("reserved"), KeyEvent.VK_I, KeyEvent.META_DOWN_MASK); // Italicize the selected text or toggle italic text on or off (equivalent to the Italic command). See "The Format Menu."
        //Shortcut.registerSystemShortcut("system:info", tr("reserved"), KeyEvent.VK_I, KeyEvent.META_DOWN_MASK); // Display an Info window. See "Inspector Windows."
        //Shortcut.registerSystemShortcut("system:inspector", tr("reserved"), KeyEvent.VK_I, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK); // Display an inspector window. See "Inspector Windows."
        //Shortcut.registerSystemShortcut("system:toselection", tr("reserved"), KeyEvent.VK_J, KeyEvent.META_DOWN_MASK); // Scroll to a selection.
        //Shortcut.registerSystemShortcut("system:minimize", tr("reserved"), KeyEvent.VK_M, KeyEvent.META_DOWN_MASK); // Minimize the active window to the Dock (equivalent to the Minimize command). See "The Window Menu."
        //Shortcut.registerSystemShortcut("system:minimizeall", tr("reserved"), KeyEvent.VK_M, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK); // Minimize all windows of the active application to the Dock (equivalent to the Minimize All command). See "The Window Menu."
        Shortcut.registerSystemShortcut("system:new", tr("reserved"), KeyEvent.VK_N, KeyEvent.META_DOWN_MASK); // Open a new document (equivalent to the New command). See "The File Menu."
        Shortcut.registerSystemShortcut("system:open", tr("reserved"), KeyEvent.VK_O, KeyEvent.META_DOWN_MASK); // Display a dialog for choosing a document to open (equivalent to the Open command). See "The File Menu."
        Shortcut.registerSystemShortcut("system:print", tr("reserved"), KeyEvent.VK_P, KeyEvent.META_DOWN_MASK); // Display the Print dialog (equivalent to the Print command). See "The File Menu."
        //Shortcut.registerSystemShortcut("system:printsetup", tr("reserved"), KeyEvent.VK_P, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK); // Display a dialog for specifying printing parameters (equivalent to the Page Setup command). See "The File Menu."
        auto(Shortcut.registerSystemShortcut("system:menuexit", tr("reserved"), KeyEvent.VK_Q, KeyEvent.META_DOWN_MASK)); // Quit the application (equivalent to the Quit command). See "The Application Menu."

        auto(Shortcut.registerSystemShortcut("apple-reserved-43", tr("reserved"), KeyEvent.VK_Q, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)); // Log out the current user (equivalent to the Log Out command).
        auto(Shortcut.registerSystemShortcut("apple-reserved-44", tr("reserved"), KeyEvent.VK_Q, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)); // Log out the current user without confirmation.

        Shortcut.registerSystemShortcut("system:save", tr("reserved"), KeyEvent.VK_S, KeyEvent.META_DOWN_MASK); // Save the active document (equivalent to the Save command). See "The File Menu."
        Shortcut.registerSystemShortcut("system:saveas", tr("reserved"), KeyEvent.VK_S, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK); // Display the Save dialog (equivalent to the Save As command). See "The File Menu."
        //Shortcut.registerSystemShortcut("system:fonts", tr("reserved"), KeyEvent.VK_T, KeyEvent.META_DOWN_MASK); // Display the Fonts window (equivalent to the Show Fonts command). See "The Format Menu."
        Shortcut.registerSystemShortcut("system:toggletoolbar", tr("reserved"), KeyEvent.VK_T, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK); // Show or hide a toolbar (equivalent to the Show/Hide Toolbar command). See "The View Menu" and "Toolbars."
        //Shortcut.registerSystemShortcut("system:underline", tr("reserved"), KeyEvent.VK_U, KeyEvent.META_DOWN_MASK); // Underline the selected text or turn underlining on or off (equivalent to the Underline command). See "The Format Menu."
        Shortcut.registerSystemShortcut("system:paste", tr("reserved"), KeyEvent.VK_V, KeyEvent.META_DOWN_MASK); // Insert the Clipboard contents at the insertion point (equivalent to the Paste command). See "The File Menu."
        //Shortcut.registerSystemShortcut("system:pastestyle", tr("reserved"), KeyEvent.VK_V, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK); // Apply the style of one object to the selected object (equivalent to the Paste Style command). See "The Format Menu."
        //Shortcut.registerSystemShortcut("system:pastemwithoutstyle", tr("reserved"), KeyEvent.VK_V, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK | KeyEvent.ALT_DOWN_MASK); // Apply the style of the surrounding text to the inserted object (equivalent to the Paste and Match Style command). See "The Edit Menu."
        //Shortcut.registerSystemShortcut("system:pasteformatting", tr("reserved"), KeyEvent.VK_V, KeyEvent.META_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK); // Apply formatting settings to the selected object (equivalent to the Paste Ruler command). See "The Format Menu."
        //Shortcut.registerSystemShortcut("system:closewindow", tr("reserved"), KeyEvent.VK_W, KeyEvent.META_DOWN_MASK); // Close the active window (equivalent to the Close command). See "The File Menu."
        Shortcut.registerSystemShortcut("system:closefile", tr("reserved"), KeyEvent.VK_W, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK); // Close a file and its associated windows (equivalent to the Close File command). See "The File Menu."
        Shortcut.registerSystemShortcut("system:closeallwindows", tr("reserved"), KeyEvent.VK_W, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK); // Close all windows in the application (equivalent to the Close All command). See "The File Menu."
        Shortcut.registerSystemShortcut("system:cut", tr("reserved"), KeyEvent.VK_X, KeyEvent.META_DOWN_MASK); // Remove the selection and store on the Clipboard (equivalent to the Cut command). See "The Edit Menu."
        Shortcut.registerSystemShortcut("system:undo", tr("reserved"), KeyEvent.VK_Z, KeyEvent.META_DOWN_MASK); // Reverse the effect of the user's previous operation (equivalent to the Undo command). See "The Edit Menu."
        Shortcut.registerSystemShortcut("system:redo", tr("reserved"), KeyEvent.VK_Z, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK); // Reverse the effect of the last Undo command (equivalent to the Redo command). See "The Edit Menu."

        auto(Shortcut.registerSystemShortcut("apple-reserved-45", tr("reserved"), KeyEvent.VK_RIGHT, KeyEvent.META_DOWN_MASK)); // Change the keyboard layout to current layout of Roman script.
        //auto(Shortcut.registerSystemCut("apple-reserved-46", tr("reserved"), KeyEvent.VK_RIGHT, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)); // Extend selection to the next semantic unit, typically the end of the current line.
        //auto(Shortcut.registerSystemCut("apple-reserved-47", tr("reserved"), KeyEvent.VK_RIGHT, KeyEvent.SHIFT_DOWN_MASK)); // Extend selection one character to the right.
        //auto(Shortcut.registerSystemCut("apple-reserved-48", tr("reserved"), KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)); // Extend selection to the end of the current word, then to the end of the next word.

        Shortcut.registerSystemShortcut("system:movefocusright", tr("reserved"), KeyEvent.VK_RIGHT, KeyEvent.CTRL_DOWN_MASK); // Move focus to another value or cell within a view, such as a table. See Accessibility Overview.

        auto(Shortcut.registerSystemShortcut("apple-reserved-49", tr("reserved"), KeyEvent.VK_LEFT, KeyEvent.META_DOWN_MASK)); // Change the keyboard layout to current layout of system script.
        //auto(Shortcut.registerSystemCut("apple-reserved-50", tr("reserved"), KeyEvent.VK_LEFT, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)); // Extend selection to the previous semantic unit, typically the beginning of the current line.
        //auto(Shortcut.registerSystemCut("apple-reserved-51", tr("reserved"), KeyEvent.VK_LEFT, KeyEvent.SHIFT_DOWN_MASK)); // Extend selection one character to the left.
        //auto(Shortcut.registerSystemCut("apple-reserved-52", tr("reserved"), KeyEvent.VK_LEFT, KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)); // Extend selection to the beginning of the current word, then to the beginning of the previous word.

        Shortcut.registerSystemShortcut("system:movefocusleft", tr("reserved"), KeyEvent.VK_LEFT, KeyEvent.CTRL_DOWN_MASK); // Move focus to another value or cell within a view, such as a table. See Accessibility Overview.

        //auto(Shortcut.registerSystemCut("apple-reserved-53", tr("reserved"), KeyEvent.VK_UP, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)); // Extend selection upward in the next semantic unit, typically the beginning of the document.
        //auto(Shortcut.registerSystemCut("apple-reserved-54", tr("reserved"), KeyEvent.VK_UP, KeyEvent.SHIFT_DOWN_MASK)); // Extend selection to the line above, to the nearest character boundary at the same horizontal location.
        //auto(Shortcut.registerSystemCut("apple-reserved-55", tr("reserved"), KeyEvent.VK_UP, KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)); // Extend selection to the beginning of the current paragraph, then to the beginning of the next paragraph.

        Shortcut.registerSystemShortcut("system:movefocusup", tr("reserved"), KeyEvent.VK_UP, KeyEvent.CTRL_DOWN_MASK); // Move focus to another value or cell within a view, such as a table. See Accessibility Overview.

        //auto(Shortcut.registerSystemCut("apple-reserved-56", tr("reserved"), KeyEvent.VK_DOWN, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)); // Extend selection downward in the next semantic unit, typically the end of the document.
        //auto(Shortcut.registerSystemCut("apple-reserved-57", tr("reserved"), KeyEvent.VK_DOWN, KeyEvent.SHIFT_DOWN_MASK)); // Extend selection to the line below, to the nearest character boundary at the same horizontal location.
        //auto(Shortcut.registerSystemCut("apple-reserved-58", tr("reserved"), KeyEvent.VK_DOWN, KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)); // Extend selection to the end of the current paragraph, then to the end of the next paragraph (include the blank line between paragraphs in cut, copy, and paste operations).

        Shortcut.registerSystemShortcut("system:movefocusdown", tr("reserved"), KeyEvent.VK_DOWN, KeyEvent.CTRL_DOWN_MASK); // Move focus to another value or cell within a view, such as a table. See Accessibility Overview.

        auto(Shortcut.registerSystemShortcut("system:about", tr("reserved"), 0, -1)); // About

        Shortcut.registerSystemShortcut("view:zoomin", tr("reserved"), KeyEvent.VK_ADD, KeyEvent.META_DOWN_MASK); // Zoom in
        Shortcut.registerSystemShortcut("view:zoomout", tr("reserved"), KeyEvent.VK_SUBTRACT, KeyEvent.META_DOWN_MASK); // Zoom out
        // CHECKSTYLE.ON: LineLength
    }

    private static void auto(Shortcut sc) {
        if (sc != null) {
            sc.setAutomatic();
        }
    }

    @Override
    public String getDefaultStyle() {
        return "com.apple.laf.AquaLookAndFeel";
    }

    @Override
    public boolean canFullscreen() {
        // OS X provides native full screen support registered at initialization, no need for custom action
        return false;
    }

    @Override
    public String getOSDescription() {
        return getSystemProperty("os.name") + ' ' + getSystemProperty("os.version");
    }

    private String buildOSBuildNumber() {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append(exec("sw_vers", "-productName"))
              .append(' ')
              .append(exec("sw_vers", "-productVersion"))
              .append(" (")
              .append(exec("sw_vers", "-buildVersion"))
              .append(')');
        } catch (IOException e) {
            Logging.error(e);
        }
        return sb.toString();
    }

    @Override
    public String getOSBuildNumber() {
        if (oSBuildNumber == null) {
            oSBuildNumber = buildOSBuildNumber();
        }
        return oSBuildNumber;
    }

    @Override
    public File getDefaultCacheDirectory() {
        return new File(getSystemProperty("user.home")+"/Library/Caches",
                Preferences.getJOSMDirectoryBaseName());
    }

    @Override
    public File getDefaultPrefDirectory() {
        return new File(getSystemProperty("user.home")+"/Library/Preferences",
                Preferences.getJOSMDirectoryBaseName());
    }

    @Override
    public File getDefaultUserDataDirectory() {
        return new File(getSystemProperty("user.home")+"/Library",
                Preferences.getJOSMDirectoryBaseName());
    }

    @Override
    public X509Certificate getX509Certificate(NativeCertAmend certAmend)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        for (String macAlias : certAmend.getNativeAliases()) {
            try {
                // Get platform certificate in PEM format
                String pem = Utils.execOutput(Arrays.asList("security", "find-certificate",
                        "-c", macAlias, "-p", "/System/Library/Keychains/SystemRootCertificates.keychain"));
                Logging.debug(pem);
                return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(
                        new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)));
            } catch (ExecutionException | InterruptedException | IllegalArgumentException | CertificateException e) {
                Logging.debug(e);
            }
        }
        return null;
    }
}
