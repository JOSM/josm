// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

/**
 * see PlatformHook.java
 *
 * BTW: THIS IS A STUB. See comments below for details.
 *
 * Don't write (Main.platform instanceof PlatformHookUnixoid) because other platform
 * hooks are subclasses of this class.
 */
public class PlatformHookUnixoid implements PlatformHook {

    private String osDescription;

    @Override
    public void preStartupHook(){
    }

    @Override
    public void startupHook() {
    }

    @Override
    public void openUrl(String url) throws IOException {
        String[] programs = {"gnome-open", "kfmclient openURL", "firefox"};
        for (String program : programs) {
            try {
                Runtime.getRuntime().exec(program+" "+url);
                return;
            } catch (IOException e) {
            }
        }
    }

    @Override
    public void initSystemShortcuts() {
        // TODO: Insert system shortcuts here. See Windows and especially OSX to see how to.
        for(int i = KeyEvent.VK_F1; i <= KeyEvent.VK_F12; ++i)
            Shortcut.registerSystemShortcut("screen:toogle"+i, tr("reserved"), i, KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK).setAutomatic();
        Shortcut.registerSystemShortcut("system:reset", tr("reserved"), KeyEvent.VK_DELETE, KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK).setAutomatic();
        Shortcut.registerSystemShortcut("system:resetX", tr("reserved"), KeyEvent.VK_BACK_SPACE, KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK).setAutomatic();
    }
    /**
     * This should work for all platforms. Yeah, should.
     * See PlatformHook.java for a list of reasons why
     * this is implemented here...
     */
    @Override
    public String makeTooltip(String name, Shortcut sc) {
        String result = "";
        result += "<html>";
        result += name;
        if (sc != null && sc.getKeyText().length() != 0) {
            result += " ";
            result += "<font size='-2'>";
            result += "("+sc.getKeyText()+")";
            result += "</font>";
        }
        result += "&nbsp;</html>";
        return result;
    }

    @Override
    public String getDefaultStyle() {
        return "javax.swing.plaf.metal.MetalLookAndFeel";
    }

    @Override
    public boolean canFullscreen()
    {
        return GraphicsEnvironment.getLocalGraphicsEnvironment()
        .getDefaultScreenDevice().isFullScreenSupported();
    }

    @Override
    public boolean rename(File from, File to)
    {
        return from.renameTo(to);
    }

    /**
     * Get the Java package name including detailed version.
     *
     * Some Java bugs are specific to a certain security update, so in addition
     * to the Java version, we also need the exact package version.
     *
     * This was originally written for #8921, so only Debian based distributions
     * are covered at the moment. This can be extended to other distributions
     * if needed.
     *
     * @return The package name and package version if it can be identified, null
     * otherwise
     */
    public String getJavaPackageDetails() {
        try {
            String dist = Utils.execOutput(Arrays.asList("lsb_release", "-i", "-s"));
            if ("Debian".equalsIgnoreCase(dist) || "Ubuntu".equalsIgnoreCase(dist)) {
                String javaHome = System.getProperty("java.home");
                if ("/usr/lib/jvm/java-6-openjdk-amd64/jre".equals(javaHome) ||
                        "/usr/lib/jvm/java-6-openjdk-i386/jre".equals(javaHome) ||
                        "/usr/lib/jvm/java-6-openjdk/jre".equals(javaHome)) {
                    String version = Utils.execOutput(Arrays.asList("dpkg-query", "--show", "--showformat", "${Architecture}-${Version}", "openjdk-6-jre"));
                    return "openjdk-6-jre:" + version;
                }
                if ("/usr/lib/jvm/java-7-openjdk-amd64/jre".equals(javaHome) ||
                        "/usr/lib/jvm/java-7-openjdk-i386/jre".equals(javaHome)) {
                    String version = Utils.execOutput(Arrays.asList("dpkg-query", "--show", "--showformat", "${Architecture}-${Version}", "openjdk-7-jre"));
                    return "openjdk-7-jre:" + version;
                }
            }
        } catch (IOException e) {
        }
        return null;
    }

    protected String buildOSDescription() {
        String osName = System.getProperty("os.name");
        if ("Linux".equalsIgnoreCase(osName)) {
            try {
                // Try lsb_release (only available on LSB-compliant Linux systems, see https://www.linuxbase.org/lsb-cert/productdir.php?by_prod )
                Process p = Runtime.getRuntime().exec("lsb_release -ds");
                BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = Utils.strip(input.readLine());
                Utils.close(input);
                if (line != null && !line.isEmpty()) {
                    line = line.replaceAll("\"+","");
                    line = line.replaceAll("NAME=",""); // strange code for some Gentoo's
                    if(line.startsWith("Linux ")) // e.g. Linux Mint
                        return line;
                    else if(!line.isEmpty())
                        return "Linux " + line;
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
                        new LinuxReleaseInfo("/etc/redhat-release")
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
                    ", idField=" + idField + ", releaseField=" + releaseField + "]";
        }

        /**
         * Extracts OS detailed information from a Linux release file (/etc/xxx-release)
         * @return The OS detailed information, or {@code null}
         */
        public String extractDescription() {
            String result = null;
            if (path != null) {
                File file = new File(path);
                if (file.exists()) {
                    BufferedReader reader = null;
                    try {
                        reader = new BufferedReader(new FileReader(file));
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
                            result = id + " " + release;
                        }
                    } catch (IOException e) {
                        // Ignore
                    } finally {
                        Utils.close(reader);
                    }
                }
            }
            // Append prefix if any
            if (result != null && !result.isEmpty() && prefix != null && !prefix.isEmpty()) {
                result = prefix + result;
            }
            if(result != null)
                result = result.replaceAll("\"+","");
            return result;
        }
    }
}
