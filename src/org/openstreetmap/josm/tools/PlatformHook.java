// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.openstreetmap.josm.data.projection.datum.NTV2Proj4DirGridShiftFileSource;
import org.openstreetmap.josm.io.CertificateAmendment.NativeCertAmend;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * This interface allows platform (operating system) dependent code
 * to be bundled into self-contained classes.
 * @since 1023
 */
public interface PlatformHook {

    /**
     * Visitor to construct a PlatformHook from a given {@link Platform} object.
     */
    PlatformVisitor<PlatformHook> CONSTRUCT_FROM_PLATFORM = new PlatformVisitor<>() {
        @Override
        public PlatformHook visitUnixoid() {
            return new PlatformHookUnixoid();
        }

        @Override
        public PlatformHook visitWindows() {
            return new PlatformHookWindows();
        }

        @Override
        public PlatformHook visitOsx() {
            return new PlatformHookOsx();
        }
    };

    /**
     * Get the platform corresponding to this platform hook.
     * @return the platform corresponding to this platform hook
     */
    Platform getPlatform();

    /**
      * The preStartupHook will be called extremely early. It is
      * guaranteed to be called before the GUI setup has started.
      * <p>
      * Reason: On OSX we need to inform the Swing libraries
      * that we want to be integrated with the OS before we setup our GUI.
      */
    default void preStartupHook() {
        // Do nothing
    }

    /**
      * The afterPrefStartupHook will be called early, but after
      * the preferences have been loaded and basic processing of
      * command line arguments is finished.
      * It is guaranteed to be called before the GUI setup has started.
      */
    default void afterPrefStartupHook() {
        // Do nothing
    }

    /**
     * The startupHook will be called early, but after the GUI
     * setup has started.
     * <p>
     * Reason: On OSX we need to register some callbacks with the
     * OS, so we'll receive events from the system menu.
     * @param javaCallback Java expiration callback, providing GUI feedback
     * @param sanityCheckCallback Sanity check callback, providing GUI feedback
     * @since 18985
     */
    default void startupHook(JavaExpirationCallback javaCallback, SanityCheckCallback sanityCheckCallback) {
        startupSanityChecks(sanityCheckCallback);
    }

    /**
      * The openURL hook will be used to open a URL in the
      * default web browser.
     * @param url The URL to open
     * @throws IOException if any I/O error occurs
      */
    void openUrl(String url) throws IOException;

    /**
      * The initSystemShortcuts hook will be called by the
      * Shortcut class after the modifier groups have been read
      * from the config, but before any shortcuts are read from
      * it or registered from within the application.
      * <p>
      * Please note that you are not allowed to register any
      * shortcuts from this hook, but only "systemCuts"!
      * <p>
      * BTW: SystemCuts should be named "system:&lt;whatever&gt;",
      * and it'd be best if you'd recycle the names already used
      * by the Windows and OSX hooks. Especially the latter has
      * really many of them.
      * <p>
      * You should also register any and all shortcuts that the
      * operating system handles itself to block JOSM from trying
      * to use them---as that would just not work. Call setAutomatic
      * on them to prevent the keyboard preferences from allowing the
      * user to change them.
      */
    void initSystemShortcuts();

    /**
     * Returns the default LAF to be used on this platform to look almost as a native application.
     * @return The default native LAF for this platform
     */
    String getDefaultStyle();

    /**
     * Determines if the platform allows full-screen.
     * @return {@code true} if full screen is allowed, {@code false} otherwise
     */
    default boolean canFullscreen() {
        return !GraphicsEnvironment.isHeadless() &&
                GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().isFullScreenSupported();
    }

    /**
     * Renames a file.
     * @param from Source file
     * @param to Target file
     * @return {@code true} if the file has been renamed, {@code false} otherwise
     */
    default boolean rename(File from, File to) {
        return from.renameTo(to);
    }

    /**
     * Returns a detailed OS description (at least family + version).
     * @return A detailed OS description.
     * @since 5850
     */
    String getOSDescription();

    /**
     * Returns OS build number.
     * @return OS build number.
     * @since 12217
     */
    default String getOSBuildNumber() {
        return "";
    }

    /**
     * Returns the {@code X509Certificate} matching the given certificate amendment information.
     * @param certAmend certificate amendment
     * @return the {@code X509Certificate} matching the given certificate amendment information, or {@code null}
     * @throws KeyStoreException in case of error
     * @throws IOException in case of error
     * @throws CertificateException in case of error
     * @throws NoSuchAlgorithmException in case of error
     * @since 13450
     */
    default X509Certificate getX509Certificate(NativeCertAmend certAmend)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        return null;
    }

    /**
     * Executes a native command and returns the first line of standard output.
     * @param command array containing the command to call and its arguments.
     * @return first stripped line of standard output
     * @throws IOException if an I/O error occurs
     * @since 12217
     */
    default String exec(String... command) throws IOException {
        Process p = Runtime.getRuntime().exec(command);
        try (BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            return Utils.strip(input.readLine());
        }
    }

    /**
     * Returns the platform-dependent default cache directory.
     * @return the platform-dependent default cache directory
     * @since 7829
     */
    File getDefaultCacheDirectory();

    /**
     * Returns the platform-dependent default preferences directory.
     * @return the platform-dependent default preferences directory
     * @since 7831
     */
    File getDefaultPrefDirectory();

    /**
     * Returns the platform-dependent default user data directory.
     * @return the platform-dependent default user data directory
     * @since 7834
     */
    File getDefaultUserDataDirectory();

    /**
     * Returns the list of platform-dependent default datum shifting directories for the PROJ.4 library.
     * @return the list of platform-dependent default datum shifting directories for the PROJ.4 library
     * @since 11642
     */
    default List<File> getDefaultProj4NadshiftDirectories() {
        return getPlatform().accept(NTV2Proj4DirGridShiftFileSource.getInstance());
    }

    /**
     * Determines if the JVM is OpenJDK-based.
     * @return {@code true} if {@code java.home} contains "openjdk", {@code false} otherwise
     * @since 12219
     */
    default boolean isOpenJDK() {
        String javaHome = Utils.getSystemProperty("java.home");
        return javaHome != null && javaHome.contains("openjdk");
    }

    /**
     * Determines if HTML rendering is supported in menu tooltips.
     * @return {@code true} if HTML rendering is supported in menu tooltips
     * @since 18116
     */
    default boolean isHtmlSupportedInMenuTooltips() {
        return true;
    }

    /**
     * Returns extended modifier key used as the appropriate accelerator key for menu shortcuts.
     * It was advised everywhere to use {@link Toolkit#getMenuShortcutKeyMask()} to get the cross-platform modifier, but:
     * <ul>
     * <li>it returns KeyEvent.CTRL_MASK instead of KeyEvent.CTRL_DOWN_MASK. We used the extended
     *    modifier for years, and Oracle recommends to use it instead, so it's best to keep it</li>
     * <li>the method throws a HeadlessException ! So we would need to handle it for unit tests anyway</li>
     * </ul>
     * @return extended modifier key used as the appropriate accelerator key for menu shortcuts
     * @since 12748 (as a replacement to {@code GuiHelper.getMenuShortcutKeyMaskEx()})
     */
    default int getMenuShortcutKeyMaskEx() {
        if (!GraphicsEnvironment.isHeadless()) {
            return Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        }
        return InputEvent.CTRL_DOWN_MASK;
    }

    /**
     * Called when an outdated version of Java is detected at startup.
     * @since 12270
     */
    @FunctionalInterface
    interface JavaExpirationCallback {
        /**
         * Asks user to update its version of Java.
         * @param updVersion target update version
         * @param url download URL
         * @param major true for a migration towards a major version of Java (8:9), false otherwise
         * @param eolDate the EOL/expiration date
         */
        void askUpdateJava(String updVersion, String url, String eolDate, boolean major);
    }

    /**
     * Inform the user that a sanity check or checks failed
     */
    @FunctionalInterface
    interface SanityCheckCallback {
        /**
         * Tells the user that a sanity check failed
         * @param title The title of the message to show
         * @param canContinue {@code true} if the failed sanity check(s) will not instantly kill JOSM when the user edits
         * @param message The message parts to show the user (as a list)
         */
        void sanityCheckFailed(String title, boolean canContinue, String... message);
    }

    /**
     * Checks if the running version of Java has expired, proposes to user to update it if needed.
     * @param callback Java expiration callback
     * @since 12270 (signature)
     * @since 12219
     */
    default void checkExpiredJava(JavaExpirationCallback callback) {
        Date expiration = Utils.getJavaExpirationDate();
        if (expiration != null && expiration.before(new Date())) {
            String latestVersion = Utils.getJavaLatestVersion();
            String currentVersion = Utils.getSystemProperty("java.version");
            // #17831 WebStart may be launched with an expired JRE but then launching JOSM with up-to-date JRE
            if (latestVersion == null || !latestVersion.equalsIgnoreCase(currentVersion)) {
                callback.askUpdateJava(latestVersion != null ? latestVersion : "latest",
                        Config.getPref().get("java.update.url", getJavaUrl()),
                        DateUtils.getDateFormat(DateFormat.MEDIUM).format(expiration), false);
            }
        }
    }

    /**
     * Checks if we will soon not be supporting the running version of Java
     * @param callback Java expiration callback
     * @since 18580
     */
    default void warnSoonToBeUnsupportedJava(JavaExpirationCallback callback) {
        // Java 17 is our next minimum version, and OpenWebStart should be replacing Oracle WebStart
        if (Utils.getJavaVersion() < 17 && !Utils.isRunningWebStart()) {
            String latestVersion = Utils.getJavaLatestVersion();
            String currentVersion = Utils.getSystemProperty("java.version");
            // #17831 WebStart may be launched with an expired JRE but then launching JOSM with up-to-date JRE
            if (latestVersion == null || !latestVersion.equalsIgnoreCase(currentVersion)) {
                callback.askUpdateJava(latestVersion != null ? latestVersion : "latest",
                        Config.getPref().get("java.update.url", getJavaUrl()),
                        null, Utils.getJavaVersion() < 17);
            }
        }
    }

    /**
     * Get the Java download URL (really shouldn't be used outside of JOSM startup checks)
     * @return The download URL to use.
     * @since 18580
     */
    default String getJavaUrl() {
        StringBuilder defaultDownloadUrl = new StringBuilder("https://www.azul.com/downloads/?version=java-21-lts");
        if (PlatformManager.isPlatformWindows()) {
            defaultDownloadUrl.append("&os=windows");
        } else if (PlatformManager.isPlatformOsx()) {
            defaultDownloadUrl.append("&os=macos");
        } // else probably `linux`, but they should be using a package manager.
        // For available architectures, see
        // https://github.com/openjdk/jdk/blob/master/src/jdk.hotspot.agent/share/classes/sun/jvm/hotspot/utilities/PlatformInfo.java#L53
        String osArch = System.getProperty("os.arch");
        if (osArch != null) {
            // See https://learn.microsoft.com/en-us/windows/win32/winprog64/wow64-implementation-details#environment-variables
            // for PROCESSOR_ARCHITEW6432
            if ("x86_64".equals(osArch) || "amd64".equals(osArch)
                    || "AMD64".equalsIgnoreCase(System.getenv("PROCESSOR_ARCHITEW6432"))) {
                defaultDownloadUrl.append("&architecture=x86-64-bit").append("&package=jdk-fx"); // jdk-fx has an installer
            } else if ("aarch64".equals(osArch)) {
                defaultDownloadUrl.append("&architecture=arm-64-bit").append("&package=jdk-fx"); // jdk-fx has an installer
            } else if ("x86".equals(osArch)) {
                // Honestly, just about everyone should be on x86_64 at this point. But just in case someone
                // is running JOSM on a 10-year-old computer. They'd probably be better off running a RPi.
                defaultDownloadUrl.append("&architecture=x86-32-bit").append("&package=jdk"); // jdk has an installer
            } // else user will have to figure it out themselves.
        }
        defaultDownloadUrl.append("#zulu"); // Scrolls to download section
        return defaultDownloadUrl.toString();
    }

    /**
     * Check startup preconditions
     * @param sanityCheckCallback The callback to inform the user about failed checks
     */
    default void startupSanityChecks(SanityCheckCallback sanityCheckCallback) {
        final String arch = System.getProperty("os.arch");
        final List<String> messages = new ArrayList<>();
        final String jvmArch = System.getProperty("sun.arch.data.model");
        boolean canContinue = true;
        if (Utils.getJavaVersion() < 11) {
            canContinue = false;
            messages.add(tr("You must update Java to Java {0} or later in order to run this version of JOSM", 17));
            // Reset webstart/java update prompts
            Config.getPref().put("askUpdateWebStart", null);
            Config.getPref().put("askUpdateJava" + Utils.getJavaLatestVersion(), null);
            Config.getPref().put("askUpdateJavalatest", null);
        }
        if (!"x86".equals(arch) && "32".equals(jvmArch)) {
            messages.add(tr("Please use a 64 bit version of Java -- this will avoid out of memory errors"));
        }
        // Note: these might be able to be removed with the appropriate module-info.java settings.
        final String[] expectedJvmArguments = {
                "--add-exports=java.base/sun.security.action=ALL-UNNAMED",
                "--add-exports=java.desktop/com.sun.imageio.plugins.jpeg=ALL-UNNAMED",
                "--add-exports=java.desktop/com.sun.imageio.spi=ALL-UNNAMED"
        };
        final List<String> vmArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
        final StringBuilder missingArguments = new StringBuilder();
        for (String arg : expectedJvmArguments) {
            if (vmArguments.stream().noneMatch(s -> s.contains(arg))) {
                if (missingArguments.length() > 0) {
                    missingArguments.append("<br>");
                }
                missingArguments.append(arg);
            }
        }
        if (missingArguments.length() > 0) {
            final String args = missingArguments.toString();
            messages.add(tr("Missing JVM Arguments:<br>{0}<br>" +
                    "These arguments should be added in the command line or start script before the -jar parameter.", args));
        }
        if (!messages.isEmpty()) {
            if (canContinue) {
                sanityCheckCallback.sanityCheckFailed(tr("JOSM may work improperly"), true,
                        messages.toArray(new String[0]));
            } else {
                sanityCheckCallback.sanityCheckFailed(tr("JOSM will be unable to work properly and will exit"), false,
                        messages.toArray(new String[0]));
            }
        }
    }

    /**
     * Called when interfacing with native OS functions. Currently only used with macOS.
     * The callback must perform all GUI-related tasks associated to an OS request.
     * The non-GUI, platform-specific tasks, are usually performed by the {@code PlatformHook}.
     * @since 12695
     */
    interface NativeOsCallback {
        /**
         * macOS: Called when JOSM is asked to open a list of files.
         * @param files list of files to open
         */
        void openFiles(List<File> files);

        /**
         * macOS: Invoked when JOSM is asked to quit.
         * @return {@code true} if JOSM has been closed, {@code false} if the user has cancelled the operation.
         */
        boolean handleQuitRequest();

        /**
         * macOS: Called when JOSM is asked to show it's about dialog.
         */
        void handleAbout();

        /**
         * macOS: Called when JOSM is asked to show it's preferences UI.
         */
        void handlePreferences();
    }

    /**
     * Registers the native OS callback. Currently only needed for macOS.
     * @param callback the native OS callback
     * @since 12695
     */
    default void setNativeOsCallback(NativeOsCallback callback) {
        // To be implemented if needed
    }

    /**
     * Resolves a file link to its destination file.
     * @param file file (link or regular file)
     * @return destination file in case of a file link, file if regular
     * @since 13691
     */
    default File resolveFileLink(File file) {
        // Override if needed
        return file;
    }

    /**
     * Returns a set of possible platform specific directories where resources could be stored.
     * @return A set of possible platform specific directories where resources could be stored.
     * @since 14144
     */
    default Collection<String> getPossiblePreferenceDirs() {
        return Collections.emptyList();
    }
}
