// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences.pref;
import org.openstreetmap.josm.data.Preferences.writeExplicitly;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.util.GuiHelper;

/**
 * {@code PlatformHook} base implementation.
 *
 * Don't write (Main.platform instanceof PlatformHookUnixoid) because other platform
 * hooks are subclasses of this class.
 */
public class PlatformHookUnixoid implements PlatformHook {

    /**
     * Simple data class to hold information about a font.
     *
     * Used for fontconfig.properties files.
     */
    public static class FontEntry {
        /**
         * The character subset. Basically a free identifier, but should be unique.
         */
        @pref
        public String charset;

        /**
         * Platform font name.
         */
        @pref @writeExplicitly
        public String name = "";

        /**
         * File name.
         */
        @pref @writeExplicitly
        public String file = "";

        /**
         * Constructs a new {@code FontEntry}.
         */
        public FontEntry() {
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

    private String osDescription;

    @Override
    public void preStartupHook() {
    }

    @Override
    public void afterPrefStartupHook() {
    }

    @Override
    public void startupHook() {
    }

    @Override
    public void openUrl(String url) throws IOException {
        for (String program : Main.pref.getCollection("browser.unix",
                Arrays.asList("xdg-open", "#DESKTOP#", "$BROWSER", "gnome-open", "kfmclient openURL", "firefox"))) {
            try {
                if ("#DESKTOP#".equals(program)) {
                    Desktop.getDesktop().browse(new URI(url));
                } else if (program.startsWith("$")) {
                    program = System.getenv().get(program.substring(1));
                    Runtime.getRuntime().exec(new String[]{program, url});
                } else {
                    Runtime.getRuntime().exec(new String[]{program, url});
                }
                return;
            } catch (IOException | URISyntaxException e) {
                Main.warn(e);
            }
        }
    }

    @Override
    public void initSystemShortcuts() {
        // CHECKSTYLE.OFF: LineLength
        // TODO: Insert system shortcuts here. See Windows and especially OSX to see how to.
        for (int i = KeyEvent.VK_F1; i <= KeyEvent.VK_F12; ++i) {
            Shortcut.registerSystemShortcut("screen:toogle"+i, tr("reserved"), i, KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)
                .setAutomatic();
        }
        Shortcut.registerSystemShortcut("system:reset", tr("reserved"), KeyEvent.VK_DELETE, KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)
            .setAutomatic();
        Shortcut.registerSystemShortcut("system:resetX", tr("reserved"), KeyEvent.VK_BACK_SPACE, KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)
            .setAutomatic();
        // CHECKSTYLE.ON: LineLength
    }

    /**
     * This should work for all platforms. Yeah, should.
     * See PlatformHook.java for a list of reasons why this is implemented here...
     */
    @Override
    public String makeTooltip(String name, Shortcut sc) {
        StringBuilder result = new StringBuilder();
        result.append("<html>").append(name);
        if (sc != null && !sc.getKeyText().isEmpty()) {
            result.append(' ')
                  .append("<font size='-2'>")
                  .append('(').append(sc.getKeyText()).append(')')
                  .append("</font>");
        }
        return result.append("&nbsp;</html>").toString();
    }

    @Override
    public String getDefaultStyle() {
        return "javax.swing.plaf.metal.MetalLookAndFeel";
    }

    @Override
    public boolean canFullscreen() {
        return !GraphicsEnvironment.isHeadless() &&
                GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().isFullScreenSupported();
    }

    @Override
    public boolean rename(File from, File to) {
        return from.renameTo(to);
    }

    /**
     * Determines if the JVM is OpenJDK-based.
     * @return {@code true} if {@code java.home} contains "openjdk", {@code false} otherwise
     * @since 6951
     */
    public static boolean isOpenJDK() {
        String javaHome = System.getProperty("java.home");
        return javaHome != null && javaHome.contains("openjdk");
    }

    /**
     * Get the package name including detailed version.
     * @param packageNames The possible package names (when a package can have different names on different distributions)
     * @return The package name and package version if it can be identified, null otherwise
     * @since 7314
     */
    public static String getPackageDetails(String ... packageNames) {
        try {
            boolean dpkg = Files.exists(Paths.get("/usr/bin/dpkg-query"));
            boolean eque = Files.exists(Paths.get("/usr/bin/equery"));
            boolean rpm  = Files.exists(Paths.get("/bin/rpm"));
            if (dpkg || rpm || eque) {
                for (String packageName : packageNames) {
                    String[] args = null;
                    if (dpkg) {
                        args = new String[] {"dpkg-query", "--show", "--showformat", "${Architecture}-${Version}", packageName};
                    } else if (eque) {
                        args = new String[] {"equery", "-q", "list", "-e", "--format=$fullversion", packageName};
                    } else {
                        args = new String[] {"rpm", "-q", "--qf", "%{arch}-%{version}", packageName};
                    }
                    String version = Utils.execOutput(Arrays.asList(args));
                    if (version != null && !version.contains("not installed")) {
                        return packageName + ':' + version;
                    }
                }
            }
        } catch (IOException e) {
            Main.warn(e);
        }
        return null;
    }

    /**
     * Get the Java package name including detailed version.
     *
     * Some Java bugs are specific to a certain security update, so in addition
     * to the Java version, we also need the exact package version.
     *
     * @return The package name and package version if it can be identified, null otherwise
     */
    public String getJavaPackageDetails() {
        String home = System.getProperty("java.home");
        if (home.contains("java-7-openjdk") || home.contains("java-1.7.0-openjdk")) {
            return getPackageDetails("openjdk-7-jre", "java-1_7_0-openjdk", "java-1.7.0-openjdk");
        } else if (home.contains("icedtea")) {
            return getPackageDetails("icedtea-bin");
        } else if (home.contains("oracle")) {
            return getPackageDetails("oracle-jdk-bin", "oracle-jre-bin");
        }
        return null;
    }

    /**
     * Get the Web Start package name including detailed version.
     *
     * OpenJDK packages are shipped with icedtea-web package,
     * but its version generally does not match main java package version.
     *
     * Simply return {@code null} if there's no separate package for Java WebStart.
     *
     * @return The package name and package version if it can be identified, null otherwise
     */
    public String getWebStartPackageDetails() {
        if (isOpenJDK()) {
            return getPackageDetails("icedtea-netx", "icedtea-web");
        }
        return null;
    }

    protected String buildOSDescription() {
        String osName = System.getProperty("os.name");
        if ("Linux".equalsIgnoreCase(osName)) {
            try {
                // Try lsb_release (only available on LSB-compliant Linux systems,
                // see https://www.linuxbase.org/lsb-cert/productdir.php?by_prod )
                Process p = Runtime.getRuntime().exec("lsb_release -ds");
                try (BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String line = Utils.strip(input.readLine());
                    if (line != null && !line.isEmpty()) {
                        line = line.replaceAll("\"+", "");
                        line = line.replaceAll("NAME=", ""); // strange code for some Gentoo's
                        if (line.startsWith("Linux ")) // e.g. Linux Mint
                            return line;
                        else if (!line.isEmpty())
                            return "Linux " + line;
                    }
                }
            } catch (IOException e) {
                // Non LSB-compliant Linux system. List of common fallback release files: http://linuxmafia.com/faq/Admin/release-files.html
                for (LinuxReleaseInfo info : new LinuxReleaseInfo[]{
                        new LinuxReleaseInfo("/etc/lsb-release", "DISTRIB_DESCRIPTION", "DISTRIB_ID", "DISTRIB_RELEASE"),
                        new LinuxReleaseInfo("/etc/os-release", "PRETTY_NAME", "NAME", "VERSION"),
                        new LinuxReleaseInfo("/etc/arch-release"),
                        new LinuxReleaseInfo("/etc/debian_version", "Debian GNU/Linux "),
                        new LinuxReleaseInfo("/etc/fedora-release"),
                        new LinuxReleaseInfo("/etc/gentoo-release"),
                        new LinuxReleaseInfo("/etc/redhat-release"),
                        new LinuxReleaseInfo("/etc/SuSE-release")
                }) {
                    String description = info.extractDescription();
                    if (description != null && !description.isEmpty()) {
                        return "Linux " + description;
                    }
                }
            }
        }
        return osName;
    }

    @Override
    public String getOSDescription() {
        if (osDescription == null) {
            osDescription = buildOSDescription();
        }
        return osDescription;
    }

    protected static class LinuxReleaseInfo {
        private final String path;
        private final String descriptionField;
        private final String idField;
        private final String releaseField;
        private final boolean plainText;
        private final String prefix;

        public LinuxReleaseInfo(String path, String descriptionField, String idField, String releaseField) {
            this(path, descriptionField, idField, releaseField, false, null);
        }

        public LinuxReleaseInfo(String path) {
            this(path, null, null, null, true, null);
        }

        public LinuxReleaseInfo(String path, String prefix) {
            this(path, null, null, null, true, prefix);
        }

        private LinuxReleaseInfo(String path, String descriptionField, String idField, String releaseField, boolean plainText, String prefix) {
            this.path = path;
            this.descriptionField = descriptionField;
            this.idField = idField;
            this.releaseField = releaseField;
            this.plainText = plainText;
            this.prefix = prefix;
        }

        @Override public String toString() {
            return "ReleaseInfo [path=" + path + ", descriptionField=" + descriptionField +
                    ", idField=" + idField + ", releaseField=" + releaseField + ']';
        }

        /**
         * Extracts OS detailed information from a Linux release file (/etc/xxx-release)
         * @return The OS detailed information, or {@code null}
         */
        public String extractDescription() {
            String result = null;
            if (path != null) {
                Path p = Paths.get(path);
                if (Files.exists(p)) {
                    try (BufferedReader reader = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                        String id = null;
                        String release = null;
                        String line;
                        while (result == null && (line = reader.readLine()) != null) {
                            if (line.contains("=")) {
                                String[] tokens = line.split("=");
                                if (tokens.length >= 2) {
                                    // Description, if available, contains exactly what we need
                                    if (descriptionField != null && descriptionField.equalsIgnoreCase(tokens[0])) {
                                        result = Utils.strip(tokens[1]);
                                    } else if (idField != null && idField.equalsIgnoreCase(tokens[0])) {
                                        id = Utils.strip(tokens[1]);
                                    } else if (releaseField != null && releaseField.equalsIgnoreCase(tokens[0])) {
                                        release = Utils.strip(tokens[1]);
                                    }
                                }
                            } else if (plainText && !line.isEmpty()) {
                                // Files composed of a single line
                                result = Utils.strip(line);
                            }
                        }
                        // If no description has been found, try to rebuild it with "id" + "release" (i.e. "name" + "version")
                        if (result == null && id != null && release != null) {
                            result = id + ' ' + release;
                        }
                    } catch (IOException e) {
                        // Ignore
                        if (Main.isTraceEnabled()) {
                            Main.trace(e.getMessage());
                        }
                    }
                }
            }
            // Append prefix if any
            if (result != null && !result.isEmpty() && prefix != null && !prefix.isEmpty()) {
                result = prefix + result;
            }
            if (result != null)
                result = result.replaceAll("\"+", "");
            return result;
        }
    }

    protected void askUpdateJava(String version) {
        askUpdateJava(version, "https://www.java.com/download");
    }

    // Method kept because strings have already been translated. To enable for Java 8 migration somewhere in 2016
    protected void askUpdateJava(final String version, final String url) {
        GuiHelper.runInEDTAndWait(new Runnable() {
            @Override
            public void run() {
                ExtendedDialog ed = new ExtendedDialog(
                        Main.parent,
                        tr("Outdated Java version"),
                        new String[]{tr("Update Java"), tr("Cancel")});
                // Check if the dialog has not already been permanently hidden by user
                if (!ed.toggleEnable("askUpdateJava8").toggleCheckState()) {
                    ed.setButtonIcons(new String[]{"java", "cancel"}).setCancelButton(2);
                    ed.setMinimumSize(new Dimension(480, 300));
                    ed.setIcon(JOptionPane.WARNING_MESSAGE);
                    String content = tr("You are running version {0} of Java.", "<b>"+version+"</b>")+"<br><br>";
                    if ("Sun Microsystems Inc.".equals(System.getProperty("java.vendor")) && !isOpenJDK()) {
                        content += "<b>"+tr("This version is no longer supported by {0} since {1} and is not recommended for use.",
                                "Oracle", tr("April 2015"))+"</b><br><br>";
                    }
                    content += "<b>" +
                            tr("JOSM will soon stop working with this version; we highly recommend you to update to Java {0}.", "8")
                            + "</b><br><br>" +
                            tr("Would you like to update now ?");
                    ed.setContent(content);

                    if (ed.showDialog().getValue() == 1) {
                        try {
                            openUrl(url);
                        } catch (IOException e) {
                            Main.warn(e);
                        }
                    }
                }
            }
        });
    }

    @Override
    public boolean setupHttpsCertificate(String entryAlias, KeyStore.TrustedCertificateEntry trustedCert)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        // TODO setup HTTPS certificate on Unix systems
        return false;
    }

    @Override
    public File getDefaultCacheDirectory() {
        return new File(Main.pref.getUserDataDirectory(), "cache");
    }

    @Override
    public File getDefaultPrefDirectory() {
        return new File(System.getProperty("user.home"), ".josm");
    }

    @Override
    public File getDefaultUserDataDirectory() {
        // Use preferences directory by default
        return Main.pref.getPreferencesDirectory();
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
        String customFontconfigFile = Main.pref.get("fontconfig.properties", null);
        if (customFontconfigFile != null) {
            Utils.updateSystemProperty("sun.awt.fontconfig", customFontconfigFile);
            return;
        }
        if (!Main.pref.getBoolean("font.extended-unicode", true))
            return;

        String javaLibPath = System.getProperty("java.home") + File.separator + "lib";
        Path templateFile = FileSystems.getDefault().getPath(javaLibPath, templateFileName);
        if (!Files.isReadable(templateFile)) {
            Main.warn("extended font config - unable to find font config template file "+templateFile.toString());
            return;
        }
        try (FileInputStream fis = new FileInputStream(templateFile.toFile())) {
            Properties props = new Properties();
            props.load(fis);
            byte[] content = Files.readAllBytes(templateFile);
            File cachePath = Main.pref.getCacheDirectory();
            Path fontconfigFile = cachePath.toPath().resolve("fontconfig.properties");
            OutputStream os = Files.newOutputStream(fontconfigFile);
            os.write(content);
            try (Writer w = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
                Collection<FontEntry> extrasPref = Main.pref.getListOfStructs(
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
                            Main.trace("extended font config - already registered font for charset ''{0}'' - skipping ''{1}''",
                                    entry.charset, entry.name);
                        }
                    } else {
                        Main.trace("extended font config - Font ''{0}'' not found on system - skipping", entry.name);
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
                        Main.warn("extended font config - overriding ''{0}={1}'' with ''{2}''", key, prevValue, value);
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
                        Main.warn("extended font config - overriding ''{0}={1}'' with ''{2}''", key, prevValue, value);
                    }
                    w.append(key + '=' + value + '\n');
                }
                w.append('\n');
                String fallback = props.getProperty("sequence.fallback");
                if (fallback != null) {
                    w.append("sequence.fallback=" + fallback + ',' + Utils.join(",", allCharSubsets) + '\n');
                } else {
                    w.append("sequence.fallback=" + Utils.join(",", allCharSubsets) + '\n');
                }
            }
            Utils.updateSystemProperty("sun.awt.fontconfig", fontconfigFile.toString());
        } catch (IOException ex) {
            Main.error(ex);
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
    public Collection<String> getInstalledFonts() {
        throw new UnsupportedOperationException();
    }

    /**
     * Get default list of additional fonts to add to the configuration.
     *
     * Java will choose thee first font in the list that can render a certain character.
     *
     * @return list of FontEntry objects
     */
    public Collection<FontEntry> getAdditionalFonts() {
        throw new UnsupportedOperationException();
    }
}
