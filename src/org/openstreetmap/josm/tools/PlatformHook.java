// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.io.CertificateAmendment.CertAmend;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * This interface allows platform (operating system) dependent code
 * to be bundled into self-contained classes.
 * @since 1023
 */
public interface PlatformHook {

    /**
      * The preStartupHook will be called extremly early. It is
      * guaranteed to be called before the GUI setup has started.
      *
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
      *
      * Reason: On OSX we need to register some callbacks with the
      * OS, so we'll receive events from the system menu.
      */
    default void startupHook() {
        // Do nothing
    }

    /**
      * The openURL hook will be used to open an URL in the
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
      *
      * Please note that you are not allowed to register any
      * shortuts from this hook, but only "systemCuts"!
      *
      * BTW: SystemCuts should be named "system:&lt;whatever&gt;",
      * and it'd be best if sou'd recycle the names already used
      * by the Windows and OSX hooks. Especially the later has
      * really many of them.
      *
      * You should also register any and all shortcuts that the
      * operation system handles itself to block JOSM from trying
      * to use them---as that would just not work. Call setAutomatic
      * on them to prevent the keyboard preferences from allowing the
      * user to change them.
      */
    void initSystemShortcuts();

    /**
      * The makeTooltip hook will be called whenever a tooltip for
      * a menu or button is created.
      *
      * Tooltips are usually not system dependent, unless the
      * JVM is too dumb to provide correct names for all the keys.
      *
      * Some LAFs don't understand HTML, such as the OSX LAFs.
      *
     * @param name Tooltip text to display
     * @param sc Shortcut associated (to display accelerator between parenthesis)
     * @return Full tooltip text (name + accelerator)
      */
    default String makeTooltip(String name, Shortcut sc) {
        StringBuilder result = new StringBuilder();
        result.append("<html>").append(name);
        if (sc != null && !sc.getKeyText().isEmpty()) {
            result.append(" <font size='-2'>(")
                  .append(sc.getKeyText())
                  .append(")</font>");
        }
        return result.append("&nbsp;</html>").toString();
    }

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
     * Setup system keystore to add JOSM HTTPS certificate (for remote control).
     * @param entryAlias The entry alias to use
     * @param trustedCert the JOSM certificate for localhost
     * @return {@code true} if something has changed as a result of the call (certificate installation, etc.)
     * @throws KeyStoreException in case of error
     * @throws IOException in case of error
     * @throws CertificateException in case of error
     * @throws NoSuchAlgorithmException in case of error
     * @since 7343
     */
    default boolean setupHttpsCertificate(String entryAlias, KeyStore.TrustedCertificateEntry trustedCert)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        // TODO setup HTTPS certificate on Unix and OS X systems
        return false;
    }

    /**
     * Returns the {@code X509Certificate} matching the given certificate amendment information.
     * @param certAmend certificate amendment
     * @return the {@code X509Certificate} matching the given certificate amendment information, or {@code null}
     * @throws KeyStoreException in case of error
     * @throws IOException in case of error
     * @throws CertificateException in case of error
     * @throws NoSuchAlgorithmException in case of error
     * @since 11943
     */
    default X509Certificate getX509Certificate(CertAmend certAmend)
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
    List<File> getDefaultProj4NadshiftDirectories();

    /**
     * Determines if the JVM is OpenJDK-based.
     * @return {@code true} if {@code java.home} contains "openjdk", {@code false} otherwise
     * @since 12219
     */
    default boolean isOpenJDK() {
        String javaHome = System.getProperty("java.home");
        return javaHome != null && javaHome.contains("openjdk");
    }

    /**
     * Asks user to update its version of Java.
     * @param updVersion target update version
     * @param url download URL
     * @param major true for a migration towards a major version of Java (8:9), false otherwise
     * @param eolDate the EOL/expiration date
     * @since 12219
     */
    default void askUpdateJava(String updVersion, String url, String eolDate, boolean major) {
        ExtendedDialog ed = new ExtendedDialog(
                Main.parent,
                tr("Outdated Java version"),
                tr("OK"), tr("Update Java"), tr("Cancel"));
        // Check if the dialog has not already been permanently hidden by user
        if (!ed.toggleEnable("askUpdateJava"+updVersion).toggleCheckState()) {
            ed.setButtonIcons("ok", "java", "cancel").setCancelButton(3);
            ed.setMinimumSize(new Dimension(480, 300));
            ed.setIcon(JOptionPane.WARNING_MESSAGE);
            StringBuilder content = new StringBuilder(tr("You are running version {0} of Java.",
                    "<b>"+System.getProperty("java.version")+"</b>")).append("<br><br>");
            if ("Sun Microsystems Inc.".equals(System.getProperty("java.vendor")) && !isOpenJDK()) {
                content.append("<b>").append(tr("This version is no longer supported by {0} since {1} and is not recommended for use.",
                        "Oracle", eolDate)).append("</b><br><br>");
            }
            content.append("<b>")
                   .append(major ?
                        tr("JOSM will soon stop working with this version; we highly recommend you to update to Java {0}.", updVersion) :
                        tr("You may face critical Java bugs; we highly recommend you to update to Java {0}.", updVersion))
                   .append("</b><br><br>")
                   .append(tr("Would you like to update now ?"));
            ed.setContent(content.toString());

            if (ed.showDialog().getValue() == 2) {
                try {
                    openUrl(url);
                } catch (IOException e) {
                    Logging.warn(e);
                }
            }
        }
    }

    /**
     * Checks if the running version of Java has expired, proposes to user to update it if needed.
     * @since 12219
     */
    default void checkExpiredJava() {
        Date expiration = Utils.getJavaExpirationDate();
        if (expiration != null && expiration.before(new Date())) {
            String version = Utils.getJavaLatestVersion();
            askUpdateJava(version != null ? version : "latest",
                    Main.pref.get("java.update.url", "https://www.java.com/download"),
                    DateUtils.getDateFormat(DateFormat.MEDIUM).format(expiration), false);
        }
    }
}
