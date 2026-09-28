// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.Utils.getSystemEnv;
import static org.openstreetmap.josm.tools.Utils.getSystemProperty;

import java.awt.Desktop;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.io.CertificateAmendment.NativeCertAmend;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * {@code PlatformHook} implementation for Unix systems.
 * @since 1023
 */
public class PlatformHookUnixoid implements PlatformHook {

    private String osDescription;

    @Override
    public Platform getPlatform() {
        return Platform.UNIXOID;
    }

    @Override
    public void preStartupHook() {
        // See #12022, #16666 - Disable GNOME ATK Java wrapper as it causes a lot of serious trouble
        if (isDebianOrUbuntu()) {
            // TODO: find a way to disable ATK wrapper on Java >= 9
            // We should probably be able to do that by embedding a no-op AccessibilityProvider in our jar
            // so that it is loaded by ServiceLoader without error
            // But this require to compile at least one class with Java 9
        }
    }

    @Override
    public void startupHook(JavaExpirationCallback javaCallback, SanityCheckCallback sanityCheckCallback) {
        PlatformHook.super.startupHook(javaCallback, sanityCheckCallback);
    }

    @Override
    public void openUrl(String url) throws IOException {
        // Note: xdg-open may not always give "expected" results, see #23804.
        // At the time, it appeared to be primarily a Brave browser problem.
        for (String program : Config.getPref().getList("browser.unix",
                Arrays.asList("#DESKTOP#", "xdg-open", "$BROWSER", "gnome-open", "kfmclient openURL", "firefox"))) {
            try {
                if ("#DESKTOP#".equals(program)) {
                    Desktop.getDesktop().browse(Utils.urlToURI(url));
                } else {
                    if (program.startsWith("$")) {
                        program = System.getenv().get(program.substring(1));
                    }
                    Runtime.getRuntime().exec(new String[]{program, url});
                }
                return;
            } catch (IOException | UnsupportedOperationException | URISyntaxException e) {
                Logging.warn(e);
            }
        }
    }

    @Override
    @SuppressWarnings("squid:S103") // NOSONAR LineLength
    public void initSystemShortcuts() {
        final String reserved = marktr("reserved");
        // CHECKSTYLE.OFF: LineLength
        // TODO: Insert system shortcuts here. See Windows and especially OSX to see how to.
        /* POSSIBLE SHORTCUTS: F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12 */
        for (int i = KeyEvent.VK_F1; i <= KeyEvent.VK_F12; ++i) {
            Shortcut.registerSystemShortcut("screen:toggle"+i, tr(reserved), i, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK)
                .setAutomatic();
        }
        Shortcut.registerSystemShortcut("system:reset", tr(reserved), KeyEvent.VK_DELETE, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK)
            .setAutomatic();
        Shortcut.registerSystemShortcut("system:resetX", tr(reserved), KeyEvent.VK_BACK_SPACE, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK)
            .setAutomatic();
        // CHECKSTYLE.ON: LineLength
    }

    @Override
    public String getDefaultStyle() {
        return "javax.swing.plaf.metal.MetalLookAndFeel";
    }

    /**
     * Returns desktop environment based on the environment variable {@code XDG_CURRENT_DESKTOP}.
     * @return desktop environment.
     */
    public Optional<String> getDesktopEnvironment() {
        return Optional.ofNullable(getSystemEnv("XDG_CURRENT_DESKTOP")).filter(s -> !s.isEmpty());
    }

    /**
     * Determines if the distribution is Debian or Ubuntu, or a derivative.
     * @return {@code true} if the distribution is Debian, Ubuntu or Mint, {@code false} otherwise
     */
    public static boolean isDebianOrUbuntu() {
        try {
            String dist = Utils.execOutput(Arrays.asList("lsb_release", "-i", "-s"));
            return "Debian".equalsIgnoreCase(dist) || "Ubuntu".equalsIgnoreCase(dist) || "Mint".equalsIgnoreCase(dist);
        } catch (IOException | ExecutionException | InterruptedException e) {
            // lsb_release is not available on all Linux systems, so don't log at warning level
            Logging.debug(e);
            return false;
        }
    }

    /**
     * Get the package name including detailed version.
     * @param packageNames The possible package names (when a package can have different names on different distributions)
     * @return The package name and package version if it can be identified, null otherwise
     * @since 7314
     */
    public static String getPackageDetails(String... packageNames) {
        try {
            // CHECKSTYLE.OFF: SingleSpaceSeparator
            boolean dpkg = Paths.get("/usr/bin/dpkg-query").toFile().exists();
            boolean eque = Paths.get("/usr/bin/equery").toFile().exists();
            boolean rpm  = Paths.get("/bin/rpm").toFile().exists();
            // CHECKSTYLE.ON: SingleSpaceSeparator
            if (dpkg || rpm || eque) {
                for (String packageName : packageNames) {
                    String[] args;
                    if (dpkg) {
                        args = new String[] {"dpkg-query", "--show", "--showformat", "${Architecture}-${Version}", packageName};
                    } else if (eque) {
                        args = new String[] {"equery", "-q", "list", "-e", "--format=$fullversion", packageName};
                    } else {
                        args = new String[] {"rpm", "-q", "--qf", "%{arch}-%{version}", packageName};
                    }
                    try {
                        String version = Utils.execOutput(Arrays.asList(args));
                        if (!Utils.isEmpty(version)) {
                            return packageName + ':' + version;
                        }
                    } catch (ExecutionException e) {
                        // Package does not exist, continue
                        Logging.trace(e);
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            Logging.warn(e);
        }
        return null;
    }

    /**
     * Get the Java package name including detailed version.
     * <p>
     * Some Java bugs are specific to a certain security update, so in addition
     * to the Java version, we also need the exact package version.
     *
     * @return The package name and package version if it can be identified, null otherwise
     */
    public String getJavaPackageDetails() {
        String home = getSystemProperty("java.home");
        if (home == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("java-(\\d+)-openjdk").matcher(home);
        if (matcher.find()) {
            String version = matcher.group(1);
            return getPackageDetails("openjdk-" + version + "-jre", "java-" + version + "-openjdk");
        } else if (home.contains("java-openjdk")) {
            return getPackageDetails("java-openjdk");
        } else if (home.contains("icedtea")) {
            return getPackageDetails("icedtea-bin");
        } else if (home.contains("oracle")) {
            return getPackageDetails("oracle-jdk-bin", "oracle-jre-bin");
        }
        return null;
    }

    /**
     * Get the Web Start package name including detailed version.
     * <p>
     * OpenJDK packages are shipped with icedtea-web package,
     * but its version generally does not match main java package version.
     * <p>
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

    /**
     * Get the Gnome ATK wrapper package name including detailed version.
     * <p>
     * Debian and Ubuntu derivatives come with a pre-enabled accessibility software
     * completely buggy that makes Swing crash in a lot of different ways.
     * <p>
     * Simply return {@code null} if it's not found.
     *
     * @return The package name and package version if it can be identified, null otherwise
     */
    public String getAtkWrapperPackageDetails() {
        if (isOpenJDK() && isDebianOrUbuntu()) {
            return getPackageDetails("libatk-wrapper-java");
        }
        return null;
    }

    private String buildOSDescription() {
        String osName = getSystemProperty("os.name");
        if ("Linux".equalsIgnoreCase(osName)) {
            try {
                // Try lsb_release (only available on LSB-compliant Linux systems,
                // see https://www.linuxbase.org/lsb-cert/productdir.php?by_prod )
                String line = exec("lsb_release", "-ds");
                if (!Utils.isEmpty(line)) {
                    line = line.replaceAll("\"+", "");
                    line = line.replace("NAME=", ""); // strange code for some Gentoo's
                    if (line.startsWith("Linux ")) // e.g. Linux Mint
                        return line;
                    else if (!line.isEmpty())
                        return "Linux " + line;
                }
            } catch (IOException e) {
                Logging.debug(e);
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
                    if (!Utils.isEmpty(description)) {
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

    private static class LinuxReleaseInfo {
        private final String path;
        private final String descriptionField;
        private final String idField;
        private final String releaseField;
        private final boolean plainText;
        private final String prefix;

        LinuxReleaseInfo(String path, String descriptionField, String idField, String releaseField) {
            this(path, descriptionField, idField, releaseField, false, null);
        }

        LinuxReleaseInfo(String path) {
            this(path, null, null, null, true, null);
        }

        LinuxReleaseInfo(String path, String prefix) {
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

        @Override
        public String toString() {
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
                if (p.toFile().exists()) {
                    try (BufferedReader reader = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                        String id = null;
                        String release = null;
                        String line;
                        while (result == null && (line = reader.readLine()) != null) {
                            if (line.contains("=")) {
                                String[] tokens = line.split("=", -1);
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
                        Logging.trace(e);
                    }
                }
            }
            // Append prefix if any
            if (!Utils.isEmpty(result) && !Utils.isEmpty(prefix)) {
                result = prefix + result;
            }
            if (result != null)
                result = result.replaceAll("\"+", "");
            return result;
        }
    }

    /**
     * Get the dot directory <code>~/.josm</code>.
     * @return the dot directory
     */
    private static File getDotDirectory() {
        String dirName = "." + Preferences.getJOSMDirectoryBaseName().toLowerCase(Locale.ENGLISH);
        return new File(getSystemProperty("user.home"), dirName);
    }

    /**
     * Returns true if the dot directory should be used for storing preferences,
     * cache and user data.
     * Currently this is the case, if the dot directory already exists.
     * @return true if the dot directory should be used
     */
    private static boolean useDotDirectory() {
        return getDotDirectory().exists();
    }

    @Override
    public File getDefaultCacheDirectory() {
        if (useDotDirectory()) {
            return new File(getDotDirectory(), "cache");
        } else {
            String xdgCacheDir = getSystemEnv("XDG_CACHE_HOME");
            if (!Utils.isEmpty(xdgCacheDir)) {
                return new File(xdgCacheDir, Preferences.getJOSMDirectoryBaseName());
            } else {
                return new File(getSystemProperty("user.home") + File.separator +
                        ".cache" + File.separator + Preferences.getJOSMDirectoryBaseName());
            }
        }
    }

    @Override
    public File getDefaultPrefDirectory() {
        if (useDotDirectory()) {
            return getDotDirectory();
        } else {
            String xdgConfigDir = getSystemEnv("XDG_CONFIG_HOME");
            if (!Utils.isEmpty(xdgConfigDir)) {
                return new File(xdgConfigDir, Preferences.getJOSMDirectoryBaseName());
            } else {
                return new File(getSystemProperty("user.home") + File.separator +
                        ".config" + File.separator + Preferences.getJOSMDirectoryBaseName());
            }
        }
    }

    @Override
    public File getDefaultUserDataDirectory() {
        if (useDotDirectory()) {
            return getDotDirectory();
        } else {
            String xdgDataDir = getSystemEnv("XDG_DATA_HOME");
            if (!Utils.isEmpty(xdgDataDir)) {
                return new File(xdgDataDir, Preferences.getJOSMDirectoryBaseName());
            } else {
                return new File(getSystemProperty("user.home") + File.separator +
                        ".local" + File.separator + "share" + File.separator + Preferences.getJOSMDirectoryBaseName());
            }
        }
    }

    @Override
    public X509Certificate getX509Certificate(NativeCertAmend certAmend)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        for (String dir : new String[] {"/etc/ssl/certs", "/usr/share/ca-certificates/mozilla"}) {
            File f = new File(dir, certAmend.getFilename());
            if (f.exists()) {
                CertificateFactory fact = CertificateFactory.getInstance("X.509");
                try (InputStream is = Files.newInputStream(f.toPath())) {
                    return (X509Certificate) fact.generateCertificate(is);
                }
            }
        }
        return null;
    }

    @Override
    public Collection<String> getPossiblePreferenceDirs() {
        Set<String> locations = new HashSet<>();
        locations.add("/usr/local/share/josm/");
        locations.add("/usr/local/lib/josm/");
        locations.add("/usr/share/josm/");
        locations.add("/usr/lib/josm/");
        return locations;
    }
}
