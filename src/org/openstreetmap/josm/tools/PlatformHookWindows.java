// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_A;
import static java.awt.event.KeyEvent.VK_C;
import static java.awt.event.KeyEvent.VK_D;
import static java.awt.event.KeyEvent.VK_DELETE;
import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_ENTER;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.awt.event.KeyEvent.VK_F10;
import static java.awt.event.KeyEvent.VK_F4;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_NUM_LOCK;
import static java.awt.event.KeyEvent.VK_PRINTSCREEN;
import static java.awt.event.KeyEvent.VK_RIGHT;
import static java.awt.event.KeyEvent.VK_SHIFT;
import static java.awt.event.KeyEvent.VK_SPACE;
import static java.awt.event.KeyEvent.VK_TAB;
import static java.awt.event.KeyEvent.VK_UP;
import static java.awt.event.KeyEvent.VK_V;
import static java.awt.event.KeyEvent.VK_X;
import static java.awt.event.KeyEvent.VK_Y;
import static java.awt.event.KeyEvent.VK_Z;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.Utils.getSystemEnv;
import static org.openstreetmap.josm.tools.Utils.getSystemProperty;
import static org.openstreetmap.josm.tools.WinRegistry.HKEY_LOCAL_MACHINE;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.StructUtils;
import org.openstreetmap.josm.data.StructUtils.StructEntry;
import org.openstreetmap.josm.data.StructUtils.WriteExplicitly;
import org.openstreetmap.josm.io.CertificateAmendment.NativeCertAmend;
import org.openstreetmap.josm.io.NetworkManager;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * {@code PlatformHook} implementation for Microsoft Windows systems.
 * @since 1023
 */
public class PlatformHookWindows implements PlatformHook {

    /**
     * Pattern of Microsoft .NET and Powershell version numbers in registry.
     */
    private static final Pattern MS_VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)(\\.\\d+.*)?");

    /**
     * Simple data class to hold information about a font.
     *
     * Used for fontconfig.properties files.
     */
    public static class FontEntry {
        /**
         * The character subset. Basically a free identifier, but should be unique.
         */
        @StructEntry
        public String charset;

        /**
         * Platform font name.
         */
        @StructEntry
        @WriteExplicitly
        public String name = "";

        /**
         * File name.
         */
        @StructEntry
        @WriteExplicitly
        public String file = "";

        /**
         * Constructs a new {@code FontEntry}.
         */
        public FontEntry() {
            // Default constructor needed for construction by reflection
        }

        /**
         * Constructs a new {@code FontEntry}.
         * @param charset The character subset. Basically a free identifier, but should be unique
         * @param name Platform font name
         * @param file File name
         */
        public FontEntry(String charset, String name, String file) {
            this.charset = charset;
            this.name = name;
            this.file = file;
        }
    }

    private static final String WINDOWS_ROOT = "Windows-ROOT";

    private static final String CURRENT_VERSION = "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion";

    private String oSBuildNumber;

    @Override
    public Platform getPlatform() {
        return Platform.WINDOWS;
    }

    @Override
    public void afterPrefStartupHook() {
        extendFontconfig("fontconfig.properties.src");
    }

    @Override
    public void startupHook(JavaExpirationCallback callback) {
        checkExpiredJava(callback);
    }

    @Override
    public void openUrl(String url) throws IOException {
        if (!url.startsWith("file:/")) {
            final String customBrowser = Config.getPref().get("browser.windows", "");
            if (!customBrowser.isEmpty()) {
                Runtime.getRuntime().exec(new String[]{customBrowser, url});
                return;
            }
        }
        try {
            // Desktop API works fine under Windows
            Desktop.getDesktop().browse(Utils.urlToURI(url));
        } catch (IOException | URISyntaxException e) {
            Logging.log(Logging.LEVEL_WARN, "Desktop class failed. Platform dependent fall back for open url in browser.", e);
            Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
        }
    }

    @Override
    public void initSystemShortcuts() {
        // CHECKSTYLE.OFF: LineLength
        //Shortcut.registerSystemCut("system:menuexit", tr("reserved"), VK_Q, CTRL_DOWN_MASK);
        Shortcut.registerSystemShortcut("system:duplicate", tr("reserved"), VK_D, CTRL_DOWN_MASK); // not really system, but to avoid odd results

        // Windows 7 shortcuts: http://windows.microsoft.com/en-US/windows7/Keyboard-shortcuts

        // Shortcuts with setAutomatic(): items with automatic shortcuts will not be added to the menu bar at all

        // Don't know why Ctrl-Alt-Del isn't even listed on official Microsoft support page
        Shortcut.registerSystemShortcut("system:reset", tr("reserved"), VK_DELETE, CTRL_DOWN_MASK | ALT_DOWN_MASK).setAutomatic();

        // Ease of Access keyboard shortcuts
        Shortcut.registerSystemShortcut("microsoft-reserved-01", tr("reserved"), VK_PRINTSCREEN, ALT_DOWN_MASK | SHIFT_DOWN_MASK).setAutomatic(); // Turn High Contrast on or off
        Shortcut.registerSystemShortcut("microsoft-reserved-02", tr("reserved"), VK_NUM_LOCK, ALT_DOWN_MASK | SHIFT_DOWN_MASK).setAutomatic(); // Turn Mouse Keys on or off
        //Shortcut.registerSystemCut("microsoft-reserved-03", tr("reserved"), VK_U, );// Open the Ease of Access Center (TODO: Windows-U, how to handle it in Java ?)

        // General keyboard shortcuts
        //Shortcut.registerSystemShortcut("system:help", tr("reserved"), VK_F1, 0);                            // Display Help
        Shortcut.registerSystemShortcut("system:copy", tr("reserved"), VK_C, CTRL_DOWN_MASK);                // Copy the selected item
        Shortcut.registerSystemShortcut("system:cut", tr("reserved"), VK_X, CTRL_DOWN_MASK);                 // Cut the selected item
        Shortcut.registerSystemShortcut("system:paste", tr("reserved"), VK_V, CTRL_DOWN_MASK);               // Paste the selected item
        Shortcut.registerSystemShortcut("system:undo", tr("reserved"), VK_Z, CTRL_DOWN_MASK);                // Undo an action
        Shortcut.registerSystemShortcut("system:redo", tr("reserved"), VK_Y, CTRL_DOWN_MASK);                // Redo an action
        //Shortcut.registerSystemCut("microsoft-reserved-10", tr("reserved"), VK_DELETE, 0);                  // Delete the selected item and move it to the Recycle Bin
        //Shortcut.registerSystemCut("microsoft-reserved-11", tr("reserved"), VK_DELETE, SHIFT_DOWN_MASK);    // Delete the selected item without moving it to the Recycle Bin first
        //Shortcut.registerSystemCut("system:rename", tr("reserved"), VK_F2, 0);                          // Rename the selected item
        Shortcut.registerSystemShortcut("system:movefocusright", tr("reserved"), VK_RIGHT, CTRL_DOWN_MASK);  // Move the cursor to the beginning of the next word
        Shortcut.registerSystemShortcut("system:movefocusleft", tr("reserved"), VK_LEFT, CTRL_DOWN_MASK);    // Move the cursor to the beginning of the previous word
        Shortcut.registerSystemShortcut("system:movefocusdown", tr("reserved"), VK_DOWN, CTRL_DOWN_MASK);    // Move the cursor to the beginning of the next paragraph
        Shortcut.registerSystemShortcut("system:movefocusup", tr("reserved"), VK_UP, CTRL_DOWN_MASK);        // Move the cursor to the beginning of the previous paragraph
        //Shortcut.registerSystemCut("microsoft-reserved-17", tr("reserved"), VK_RIGHT, CTRL_DOWN_MASK | SHIFT_DOWN_MASK); // Select a block of text
        //Shortcut.registerSystemCut("microsoft-reserved-18", tr("reserved"), VK_LEFT, CTRL_DOWN_MASK | SHIFT_DOWN_MASK);  // Select a block of text
        //Shortcut.registerSystemCut("microsoft-reserved-19", tr("reserved"), VK_DOWN, CTRL_DOWN_MASK | SHIFT_DOWN_MASK);  // Select a block of text
        //Shortcut.registerSystemCut("microsoft-reserved-20", tr("reserved"), VK_UP, CTRL_DOWN_MASK | SHIFT_DOWN_MASK);    // Select a block of text
        //Shortcut.registerSystemCut("microsoft-reserved-21", tr("reserved"), VK_RIGHT, SHIFT_DOWN_MASK); // Select more than one item in a window or on the desktop, or select text within a document
        //Shortcut.registerSystemCut("microsoft-reserved-22", tr("reserved"), VK_LEFT, SHIFT_DOWN_MASK);  // Select more than one item in a window or on the desktop, or select text within a document
        //Shortcut.registerSystemCut("microsoft-reserved-23", tr("reserved"), VK_DOWN, SHIFT_DOWN_MASK);  // Select more than one item in a window or on the desktop, or select text within a document
        //Shortcut.registerSystemCut("microsoft-reserved-24", tr("reserved"), VK_UP, SHIFT_DOWN_MASK);    // Select more than one item in a window or on the desktop, or select text within a document
        //Shortcut.registerSystemCut("microsoft-reserved-25", tr("reserved"), VK_RIGHT+, CTRL_DOWN_MASK); // Select multiple individual items in a window or on the desktop (TODO: ctrl+arrow+spacebar, how to handle it in Java ?)
        //Shortcut.registerSystemCut("microsoft-reserved-26", tr("reserved"), VK_LEFT+, CTRL_DOWN_MASK);  // Select multiple individual items in a window or on the desktop (TODO: ctrl+arrow+spacebar, how to handle it in Java ?)
        //Shortcut.registerSystemCut("microsoft-reserved-27", tr("reserved"), VK_DOWN+, CTRL_DOWN_MASK);  // Select multiple individual items in a window or on the desktop (TODO: ctrl+arrow+spacebar, how to handle it in Java ?)
        //Shortcut.registerSystemCut("microsoft-reserved-28", tr("reserved"), VK_UP+, CTRL_DOWN_MASK);    // Select multiple individual items in a window or on the desktop (TODO: ctrl+arrow+spacebar, how to handle it in Java ?)
        Shortcut.registerSystemShortcut("system:selectall", tr("reserved"), VK_A, CTRL_DOWN_MASK);           // Select all items in a document or window
        //Shortcut.registerSystemCut("system:search", tr("reserved"), VK_F3, 0);                          // Search for a file or folder
        Shortcut.registerSystemShortcut("microsoft-reserved-31", tr("reserved"), VK_ENTER, ALT_DOWN_MASK).setAutomatic();   // Display properties for the selected item
        Shortcut.registerSystemShortcut("system:exit", tr("reserved"), VK_F4, ALT_DOWN_MASK).setAutomatic(); // Close the active item, or exit the active program
        Shortcut.registerSystemShortcut("microsoft-reserved-33", tr("reserved"), VK_SPACE, ALT_DOWN_MASK).setAutomatic();   // Open the shortcut menu for the active window
        //Shortcut.registerSystemCut("microsoft-reserved-34", tr("reserved"), VK_F4, CTRL_DOWN_MASK);     // Close the active document (in programs that allow you to have multiple documents open simultaneously)
        Shortcut.registerSystemShortcut("microsoft-reserved-35", tr("reserved"), VK_TAB, ALT_DOWN_MASK).setAutomatic();     // Switch between open items
        Shortcut.registerSystemShortcut("microsoft-reserved-36", tr("reserved"), VK_TAB, CTRL_DOWN_MASK | ALT_DOWN_MASK).setAutomatic(); // Use the arrow keys to switch between open items
        //Shortcut.registerSystemCut("microsoft-reserved-37", tr("reserved"), VK_TAB, ); // Cycle through programs on the taskbar by using Aero Flip 3-D (TODO: Windows-Tab, how to handle it in Java ?)
        //Shortcut.registerSystemCut("microsoft-reserved-38", tr("reserved"), VK_TAB, CTRL_DOWN_MASK | ); // Use the arrow keys to cycle through programs on the taskbar by using Aero Flip 3-D (TODO: Ctrl-Windows-Tab, how to handle it in Java ?)
        Shortcut.registerSystemShortcut("microsoft-reserved-39", tr("reserved"), VK_ESCAPE, ALT_DOWN_MASK).setAutomatic();  // Cycle through items in the order in which they were opened
        //Shortcut.registerSystemCut("microsoft-reserved-40", tr("reserved"), VK_F6, 0);                  // Cycle through screen elements in a window or on the desktop
        //Shortcut.registerSystemCut("microsoft-reserved-41", tr("reserved"), VK_F4, 0);                  // Display the address bar list in Windows Explorer
        Shortcut.registerSystemShortcut("microsoft-reserved-42", tr("reserved"), VK_F10, SHIFT_DOWN_MASK);   // Display the shortcut menu for the selected item
        Shortcut.registerSystemShortcut("microsoft-reserved-43", tr("reserved"), VK_ESCAPE, CTRL_DOWN_MASK).setAutomatic(); // Open the Start menu
        //Shortcut.registerSystemShortcut("microsoft-reserved-44", tr("reserved"), VK_F10, 0);                 // Activate the menu bar in the active program
        //Shortcut.registerSystemCut("microsoft-reserved-45", tr("reserved"), VK_RIGHT, 0);               // Open the next menu to the right, or open a submenu
        //Shortcut.registerSystemCut("microsoft-reserved-46", tr("reserved"), VK_LEFT, 0);                // Open the next menu to the left, or close a submenu
        //Shortcut.registerSystemCut("microsoft-reserved-47", tr("reserved"), VK_F5, 0);                  // Refresh the active window
        //Shortcut.registerSystemCut("microsoft-reserved-48", tr("reserved"), VK_UP, ALT_DOWN_MASK);      // View the folder one level up in Windows Explorer
        //Shortcut.registerSystemCut("microsoft-reserved-49", tr("reserved"), VK_ESCAPE, 0);              // Cancel the current task
        Shortcut.registerSystemShortcut("microsoft-reserved-50", tr("reserved"), VK_ESCAPE, CTRL_DOWN_MASK | SHIFT_DOWN_MASK).setAutomatic(); // Open Task Manager
        Shortcut.registerSystemShortcut("microsoft-reserved-51", tr("reserved"), VK_SHIFT, ALT_DOWN_MASK).setAutomatic();   // Switch the input language when multiple input languages are enabled
        Shortcut.registerSystemShortcut("microsoft-reserved-52", tr("reserved"), VK_SHIFT, CTRL_DOWN_MASK).setAutomatic();  // Switch the keyboard layout when multiple keyboard layouts are enabled
        //Shortcut.registerSystemCut("microsoft-reserved-53", tr("reserved"), ); // Change the reading direction of text in right-to-left reading languages (TODO: unclear)
        // CHECKSTYLE.ON: LineLength
    }

    @Override
    public String getDefaultStyle() {
        return "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
    }

    @Override
    public boolean rename(File from, File to) {
        if (to.exists())
            Utils.deleteFile(to);
        return from.renameTo(to);
    }

    @Override
    public String getOSDescription() {
        return Utils.strip(getSystemProperty("os.name")) + ' ' +
                ((getSystemEnv("ProgramFiles(x86)") == null) ? "32" : "64") + "-Bit";
    }

    /**
     * Returns the Windows product name from registry (example: "Windows 10 Pro")
     * @return the Windows product name from registry
     * @throws IllegalAccessException if Java language access control is enforced and the underlying method is inaccessible
     * @throws InvocationTargetException if the underlying method throws an exception
     * @since 12744
     */
    public static String getProductName() throws IllegalAccessException, InvocationTargetException {
        return WinRegistry.readString(HKEY_LOCAL_MACHINE, CURRENT_VERSION, "ProductName");
    }

    /**
     * Returns the Windows release identifier from registry (example: "1703")
     * @return the Windows release identifier from registry
     * @throws IllegalAccessException if Java language access control is enforced and the underlying method is inaccessible
     * @throws InvocationTargetException if the underlying method throws an exception
     * @since 12744
     */
    public static String getReleaseId() throws IllegalAccessException, InvocationTargetException {
        return WinRegistry.readString(HKEY_LOCAL_MACHINE, CURRENT_VERSION, "ReleaseId");
    }

    /**
     * Returns the Windows current build number from registry (example: "15063")
     * @return the Windows current build number from registry
     * @throws IllegalAccessException if Java language access control is enforced and the underlying method is inaccessible
     * @throws InvocationTargetException if the underlying method throws an exception
     * @since 12744
     */
    public static String getCurrentBuild() throws IllegalAccessException, InvocationTargetException {
        return WinRegistry.readString(HKEY_LOCAL_MACHINE, CURRENT_VERSION, "CurrentBuild");
    }

    private static String buildOSBuildNumber() {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append(getProductName());
            String releaseId = getReleaseId();
            if (releaseId != null) {
                sb.append(' ').append(releaseId);
            }
            sb.append(" (").append(getCurrentBuild()).append(')');
        } catch (ReflectiveOperationException | JosmRuntimeException | NoClassDefFoundError e) {
            Logging.log(Logging.LEVEL_ERROR, "Unable to get Windows build number", e);
            Logging.debug(e);
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

    /**
     * Loads Windows-ROOT keystore.
     * @return Windows-ROOT keystore
     * @throws NoSuchAlgorithmException if the algorithm used to check the integrity of the keystore cannot be found
     * @throws CertificateException if any of the certificates in the keystore could not be loaded
     * @throws IOException if there is an I/O or format problem with the keystore data, if a password is required but not given
     * @throws KeyStoreException if no Provider supports a KeyStore implementation for the type "Windows-ROOT"
     * @since 7343
     */
    public static KeyStore getRootKeystore() throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
        KeyStore ks = KeyStore.getInstance(WINDOWS_ROOT);
        ks.load(null, null);
        return ks;
    }

    @Override
    public X509Certificate getX509Certificate(NativeCertAmend certAmend)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        // Get Windows Trust Root Store
        KeyStore ks = getRootKeystore();
        // Search by alias (fast)
        for (String winAlias : certAmend.getNativeAliases()) {
            Certificate result = ks.getCertificate(winAlias);
            // Check for SHA-256 signature, as sometimes Microsoft can ship several certificates with the same alias, for example:
            // AC RAIZ FNMT-RCM: EBC5570C29018C4D67B1AA127BAF12F703B4611EBC17B7DAB5573894179B93FA (SHA256)
            // AC RAIZ FNMT-RCM: 4D9EBB28825C9643AB15D54E5F9614F13CB3E95DE3CF4EAC971301F320F9226E (SHA1)
            if (!sha256matches(result, certAmend, md)) {
                Logging.trace("Ignoring {0} as SHA-256 signature does not match", result);
                result = null;
            }
            if (result == null && !NetworkManager.isOffline(OnlineResource.CERTIFICATES)) {
                // Make a web request to target site to force Windows to update if needed its trust root store from its certificate trust list
                // A better, but a lot more complex method might be to get certificate list from Windows Registry with PowerShell
                // using (Get-ItemProperty -Path 'HKLM:\\SOFTWARE\\Microsoft\\SystemCertificates\\AuthRoot\\AutoUpdate').EncodedCtl)
                // then decode it using CertUtil -dump or calling CertCreateCTLContext API using JNI, and finally find and decode the certificate
                Logging.trace(webRequest(certAmend.getWebSite()));
                // Reload Windows Trust Root Store and search again by alias (fast)
                ks = getRootKeystore();
                result = ks.getCertificate(winAlias);
            }
            if (result instanceof X509Certificate) {
                return (X509Certificate) result;
            }
        }
        // If not found, search by SHA-256 (slower)
        for (Enumeration<String> aliases = ks.aliases(); aliases.hasMoreElements();) {
            String alias = aliases.nextElement();
            Certificate result = ks.getCertificate(alias);
            if (sha256matches(result, certAmend, md)) {
                Logging.warn("Certificate not found for alias ''{0}'' but found for alias ''{1}''", certAmend.getNativeAliases(), alias);
                return (X509Certificate) result;
            }
        }
        // Not found
        return null;
    }

    private static boolean sha256matches(Certificate result, NativeCertAmend certAmend, MessageDigest md) throws CertificateEncodingException {
        return result instanceof X509Certificate
                && certAmend.getSha256().equalsIgnoreCase(Utils.toHexString(md.digest(result.getEncoded())));
    }

    @Override
    public File getDefaultCacheDirectory() {
        String p = getSystemEnv("LOCALAPPDATA");
        if (p == null || p.isEmpty()) {
            // Fallback for Windows OS earlier than Windows Vista, where the variable is not defined
            p = getSystemEnv("APPDATA");
        }
        return new File(new File(p, Preferences.getJOSMDirectoryBaseName()), "cache");
    }

    @Override
    public File getDefaultPrefDirectory() {
        return new File(getSystemEnv("APPDATA"), Preferences.getJOSMDirectoryBaseName());
    }

    @Override
    public File getDefaultUserDataDirectory() {
        // Use preferences directory by default
        return Config.getDirs().getPreferencesDirectory(false);
    }

    /**
     * <p>Add more fallback fonts to the Java runtime, in order to get
     * support for more scripts.</p>
     *
     * <p>The font configuration in Java doesn't include some Indic scripts,
     * even though MS Windows ships with fonts that cover these unicode ranges.</p>
     *
     * <p>To fix this, the fontconfig.properties template is copied to the JOSM
     * cache folder. Then, the additional entries are added to the font
     * configuration. Finally the system property "sun.awt.fontconfig" is set
     * to the customized fontconfig.properties file.</p>
     *
     * <p>This is a crude hack, but better than no font display at all for these languages.
     * There is no guarantee, that the template file
     * ($JAVA_HOME/lib/fontconfig.properties.src) matches the default
     * configuration (which is in a binary format).
     * Furthermore, the system property "sun.awt.fontconfig" is undocumented and
     * may no longer work in future versions of Java.</p>
     *
     * <p>Related Java bug: <a href="https://bugs.openjdk.java.net/browse/JDK-8008572">JDK-8008572</a></p>
     *
     * @param templateFileName file name of the fontconfig.properties template file
     */
    protected void extendFontconfig(String templateFileName) {
        String customFontconfigFile = Config.getPref().get("fontconfig.properties", null);
        if (customFontconfigFile != null) {
            Utils.updateSystemProperty("sun.awt.fontconfig", customFontconfigFile);
            return;
        }
        if (!Config.getPref().getBoolean("font.extended-unicode", true))
            return;

        String javaLibPath = getSystemProperty("java.home") + File.separator + "lib";
        Path templateFile = FileSystems.getDefault().getPath(javaLibPath, templateFileName);
        String templatePath = templateFile.toString();
        if (templatePath.startsWith("null") || !Files.isReadable(templateFile)) {
            Logging.warn("extended font config - unable to find font config template file {0}", templatePath);
            return;
        }
        try (InputStream fis = Files.newInputStream(templateFile)) {
            Properties props = new Properties();
            props.load(fis);
            byte[] content = Files.readAllBytes(templateFile);
            File cachePath = Config.getDirs().getCacheDirectory(true);
            Path fontconfigFile = cachePath.toPath().resolve("fontconfig.properties");
            OutputStream os = Files.newOutputStream(fontconfigFile); // NOPMD
            os.write(content);
            try (Writer w = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
                Collection<FontEntry> extrasPref = StructUtils.getListOfStructs(Config.getPref(),
                        "font.extended-unicode.extra-items", getAdditionalFonts(), FontEntry.class);
                Collection<FontEntry> extras = new ArrayList<>();
                w.append("\n\n# Added by JOSM to extend unicode coverage of Java font support:\n\n");
                List<String> allCharSubsets = new ArrayList<>();
                for (FontEntry entry: extrasPref) {
                    Collection<String> fontsAvail = getInstalledFonts();
                    if (fontsAvail != null && fontsAvail.contains(entry.file.toUpperCase(Locale.ENGLISH))) {
                        if (!allCharSubsets.contains(entry.charset)) {
                            allCharSubsets.add(entry.charset);
                            extras.add(entry);
                        } else {
                            Logging.trace("extended font config - already registered font for charset ''{0}'' - skipping ''{1}''",
                                    entry.charset, entry.name);
                        }
                    } else {
                        Logging.trace("extended font config - Font ''{0}'' not found on system - skipping", entry.name);
                    }
                }
                for (FontEntry entry: extras) {
                    allCharSubsets.add(entry.charset);
                    if ("".equals(entry.name)) {
                        continue;
                    }
                    String key = "allfonts." + entry.charset;
                    String value = entry.name;
                    String prevValue = props.getProperty(key);
                    if (prevValue != null && !prevValue.equals(value)) {
                        Logging.warn("extended font config - overriding ''{0}={1}'' with ''{2}''", key, prevValue, value);
                    }
                    w.append(key + '=' + value + '\n');
                }
                w.append('\n');
                for (FontEntry entry: extras) {
                    if ("".equals(entry.name) || "".equals(entry.file)) {
                        continue;
                    }
                    String key = "filename." + entry.name.replace(' ', '_');
                    String value = entry.file;
                    String prevValue = props.getProperty(key);
                    if (prevValue != null && !prevValue.equals(value)) {
                        Logging.warn("extended font config - overriding ''{0}={1}'' with ''{2}''", key, prevValue, value);
                    }
                    w.append(key + '=' + value + '\n');
                }
                w.append('\n');
                w.append("sequence.fallback=");
                String fallback = props.getProperty("sequence.fallback");
                if (fallback != null) {
                    w.append(fallback).append(",");
                }
                w.append(String.join(",", allCharSubsets)).append("\n");
            }
            Utils.updateSystemProperty("sun.awt.fontconfig", fontconfigFile.toString());
        } catch (IOException | InvalidPathException ex) {
            Logging.error(ex);
        }
    }

    /**
     * Get a list of fonts that are installed on the system.
     *
     * Must be done without triggering the Java Font initialization.
     * (See {@link #extendFontconfig(java.lang.String)}, have to set system
     * property first, which is then read by sun.awt.FontConfiguration upon initialization.)
     *
     * @return list of file names
     */
    protected Collection<String> getInstalledFonts() {
        // Cannot use GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()
        // because we have to set the system property before Java initializes its fonts.
        // Use more low-level method to find the installed fonts.
        List<String> fontsAvail = new ArrayList<>();
        Path fontPath = FileSystems.getDefault().getPath(getSystemEnv("SYSTEMROOT"), "Fonts");
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(fontPath)) {
            for (Path p : ds) {
                Path filename = p.getFileName();
                if (filename != null) {
                    fontsAvail.add(filename.toString().toUpperCase(Locale.ENGLISH));
                }
            }
            fontsAvail.add(""); // for devanagari
        } catch (IOException | DirectoryIteratorException ex) {
            Logging.log(Logging.LEVEL_ERROR, ex);
            Logging.warn("extended font config - failed to load available Fonts");
            fontsAvail = null;
        }
        return fontsAvail;
    }

    /**
     * Get default list of additional fonts to add to the configuration.
     *
     * Java will choose thee first font in the list that can render a certain character.
     *
     * @return list of FontEntry objects
     */
    protected Collection<FontEntry> getAdditionalFonts() {
        Collection<FontEntry> def = new ArrayList<>(33);
        def.add(new FontEntry("devanagari", "", "")); // just include in fallback list font already defined in template

        // Windows scripts: https://msdn.microsoft.com/en-us/goglobal/bb688099.aspx
        // IE default fonts: https://msdn.microsoft.com/en-us/library/ie/dn467844(v=vs.85).aspx

        // Windows 10 and later
        def.add(new FontEntry("historic", "Segoe UI Historic", "SEGUIHIS.TTF"));       // historic charsets

        // Windows 8/8.1 and later
        def.add(new FontEntry("javanese", "Javanese Text", "JAVATEXT.TTF"));           // ISO 639: jv
        def.add(new FontEntry("leelawadee", "Leelawadee", "LEELAWAD.TTF"));            // ISO 639: bug
        def.add(new FontEntry("malgun", "Malgun Gothic", "MALGUN.TTF"));               // ISO 639: ko
        def.add(new FontEntry("myanmar", "Myanmar Text", "MMRTEXT.TTF"));              // ISO 639: my
        def.add(new FontEntry("nirmala", "Nirmala UI", "NIRMALA.TTF"));                // ISO 639: sat,srb
        def.add(new FontEntry("segoeui", "Segoe UI", "SEGOEUI.TTF"));                  // ISO 639: lis
        def.add(new FontEntry("emoji", "Segoe UI Emoji", "SEGUIEMJ.TTF"));             // emoji symbol characters

        // Windows 7 and later
        def.add(new FontEntry("nko_tifinagh_vai_osmanya", "Ebrima", "EBRIMA.TTF"));    // ISO 639: ber. Nko only since Win 8
        def.add(new FontEntry("khmer1", "Khmer UI", "KHMERUI.TTF"));                   // ISO 639: km
        def.add(new FontEntry("lao1", "Lao UI", "LAOUI.TTF"));                         // ISO 639: lo
        def.add(new FontEntry("tai_le", "Microsoft Tai Le", "TAILE.TTF"));             // ISO 639: khb
        def.add(new FontEntry("new_tai_lue", "Microsoft New Tai Lue", "NTHAILU.TTF")); // ISO 639: khb

        // Windows Vista and later:
        def.add(new FontEntry("ethiopic", "Nyala", "NYALA.TTF"));                   // ISO 639: am,gez,ti
        def.add(new FontEntry("tibetan", "Microsoft Himalaya", "HIMALAYA.TTF"));    // ISO 639: bo,dz
        def.add(new FontEntry("cherokee", "Plantagenet Cherokee", "PLANTC.TTF"));   // ISO 639: chr
        def.add(new FontEntry("unified_canadian", "Euphemia", "EUPHEMIA.TTF"));     // ISO 639: cr,in
        def.add(new FontEntry("khmer2", "DaunPenh", "DAUNPENH.TTF"));               // ISO 639: km
        def.add(new FontEntry("khmer3", "MoolBoran", "MOOLBOR.TTF"));               // ISO 639: km
        def.add(new FontEntry("lao_thai", "DokChampa", "DOKCHAMP.TTF"));            // ISO 639: lo
        def.add(new FontEntry("mongolian", "Mongolian Baiti", "MONBAITI.TTF"));     // ISO 639: mn
        def.add(new FontEntry("oriya", "Kalinga", "KALINGA.TTF"));                  // ISO 639: or
        def.add(new FontEntry("sinhala", "Iskoola Pota", "ISKPOTA.TTF"));           // ISO 639: si
        def.add(new FontEntry("yi", "Yi Baiti", "MSYI.TTF"));                       // ISO 639: ii

        // Windows XP and later
        def.add(new FontEntry("gujarati", "Shruti", "SHRUTI.TTF"));
        def.add(new FontEntry("kannada", "Tunga", "TUNGA.TTF"));
        def.add(new FontEntry("gurmukhi", "Raavi", "RAAVI.TTF"));
        def.add(new FontEntry("telugu", "Gautami", "GAUTAMI.TTF"));
        def.add(new FontEntry("bengali", "Vrinda", "VRINDA.TTF"));                  // since XP SP2
        def.add(new FontEntry("syriac", "Estrangelo Edessa", "ESTRE.TTF"));         // ISO 639: arc
        def.add(new FontEntry("thaana", "MV Boli", "MVBOLI.TTF"));                  // ISO 639: dv
        def.add(new FontEntry("malayalam", "Kartika", "KARTIKA.TTF"));              // ISO 639: ml; since XP SP2

        // Windows 2000 and later
        def.add(new FontEntry("tamil", "Latha", "LATHA.TTF"));

        // Comes with MS Office & Outlook 2000. Good unicode coverage, so add if available.
        def.add(new FontEntry("arialuni", "Arial Unicode MS", "ARIALUNI.TTF"));

        return def;
    }

    /**
     * Determines if the .NET framework 4.5 (or later) is installed.
     * Windows 7 ships by default with an older version.
     * @return {@code true} if the .NET framework 4.5 (or later) is installed.
     * @since 13463
     */
    public static boolean isDotNet45Installed() {
        try {
            // https://docs.microsoft.com/en-us/dotnet/framework/migration-guide/how-to-determine-which-versions-are-installed#net_d
            // "The existence of the Release DWORD indicates that the .NET Framework 4.5 or later has been installed"
            // Great, but our WinRegistry only handles REG_SZ type, so we have to check the Version key
            String version = WinRegistry.readString(HKEY_LOCAL_MACHINE, "SOFTWARE\\Microsoft\\NET Framework Setup\\NDP\\v4\\Full", "Version");
            if (version != null) {
                Matcher m = MS_VERSION_PATTERN.matcher(version);
                if (m.matches()) {
                    int maj = Integer.parseInt(m.group(1));
                    int min = Integer.parseInt(m.group(2));
                    return (maj == 4 && min >= 5) || maj > 4;
                }
            }
        } catch (IllegalAccessException | InvocationTargetException | NumberFormatException e) {
            Logging.error(e);
        }
        return false;
    }

    /**
     * Returns the major version number of PowerShell.
     * @return the major version number of PowerShell. -1 in case of error
     * @since 13465
     */
    public static int getPowerShellVersion() {
        try {
            String version = WinRegistry.readString(
                    HKEY_LOCAL_MACHINE, "SOFTWARE\\Microsoft\\Powershell\\3\\PowershellEngine", "PowershellVersion");
            if (version != null) {
                Matcher m = MS_VERSION_PATTERN.matcher(version);
                if (m.matches()) {
                    return Integer.parseInt(m.group(1));
                }
            }
        } catch (NumberFormatException | IllegalAccessException | InvocationTargetException e) {
            Logging.error(e);
        }
        return -1;
    }

    /**
     * Performs a web request using Windows CryptoAPI (through PowerShell).
     * This is useful to ensure Windows trust store will contain a specific root CA.
     * @param uri the web URI to request
     * @return HTTP response from the given URI
     * @throws IOException if any I/O error occurs
     * @since 13458
     */
    public static String webRequest(String uri) throws IOException {
        // With PS 6.0 (not yet released in Windows) we could simply use:
        // Invoke-WebRequest -SSlProtocol Tsl12 $uri
        // .NET framework < 4.5 does not support TLS 1.2 (https://stackoverflow.com/a/43240673/2257172)
        if (isDotNet45Installed() && getPowerShellVersion() >= 3) {
            try {
                // The following works with PS 3.0 (Windows 8+), https://stackoverflow.com/a/41618979/2257172
                return Utils.execOutput(Arrays.asList("powershell", "-Command",
                        "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12;"+
                        "[System.Net.WebRequest]::Create('"+uri+"').GetResponse()"
                        ), 5, TimeUnit.SECONDS);
            } catch (ExecutionException | InterruptedException e) {
                Logging.warn("Unable to request certificate of " + uri);
                Logging.debug(e);
            }
        }
        return null;
    }

    @Override
    public File resolveFileLink(File file) {
        if (file.getName().endsWith(".lnk")) {
            try {
                return new File(new WindowsShortcut(file).getRealFilename());
            } catch (IOException | ParseException e) {
                Logging.error(e);
            }
        }
        return file;
    }

    @Override
    public Collection<String> getPossiblePreferenceDirs() {
        Set<String> locations = new HashSet<>();
        String appdata = getSystemEnv("APPDATA");
        if (appdata != null && getSystemEnv("ALLUSERSPROFILE") != null
                && appdata.lastIndexOf(File.separator) != -1) {
            appdata = appdata.substring(appdata.lastIndexOf(File.separator));
            locations.add(new File(new File(getSystemEnv("ALLUSERSPROFILE"), appdata), "JOSM").getPath());
        }
        return locations;
    }
}
