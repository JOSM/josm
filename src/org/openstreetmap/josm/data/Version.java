// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;

import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.PlatformManager;
import org.openstreetmap.josm.tools.Utils;

/**
 * Provides basic information about the currently used JOSM build.
 * @since 2358
 */
public class Version {
    /** constant to indicate that the current build isn't assigned a JOSM version number */
    public static final int JOSM_UNKNOWN_VERSION = 0;

    /** the unique instance */
    private static Version instance;

    /**
     * Replies the unique instance of the version information
     *
     * @return the unique instance of the version information
     */
    public static synchronized Version getInstance() {
        if (instance == null) {
            instance = new Version();
            instance.init();
        }
        return instance;
    }

    private int version;
    private String releaseDescription;
    private String time;
    private String buildName;
    private boolean isLocalBuild;

    /**
     * Initializes the version infos from the revision resource file
     *
     * @param revisionInfo the revision info from a revision resource file as InputStream
     */
    protected void initFromRevisionInfo(InputStream revisionInfo) {
        if (revisionInfo == null) {
            this.releaseDescription = tr("UNKNOWN");
            this.version = JOSM_UNKNOWN_VERSION;
            this.time = null;
            return;
        }

        Properties properties = new Properties();
        try {
            properties.load(revisionInfo);
        } catch (IOException e) {
            Logging.log(Logging.LEVEL_WARN, tr("Error reading revision info from revision file: {0}", e.getMessage()), e);
        }
        String value = Optional.ofNullable(properties.getProperty("Revision")).orElse("").trim();
        if (!value.isEmpty()) {
            try {
                version = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                version = 0;
                Logging.warn(tr("Unexpected JOSM version number in revision file, value is ''{0}''", value));
            }
        } else {
            version = JOSM_UNKNOWN_VERSION;
        }

        // the last changed data
        //
        time = properties.getProperty("Last Changed Date");
        if (time == null) {
            time = properties.getProperty("Build-Date");
        }

        // is this a local build ?
        //
        isLocalBuild = "true".equalsIgnoreCase(
                Optional.ofNullable(properties.getProperty("Is-Local-Build")).orElse("").trim());

        // is this a specific build ?
        //
        buildName = Optional.ofNullable(properties.getProperty("Build-Name")).orElse("").trim();

        // the revision info
        //
        StringBuilder sb = new StringBuilder();
        for (Entry<Object, Object> property: properties.entrySet()) {
            sb.append(property.getKey()).append(':').append(property.getValue()).append('\n');
        }
        releaseDescription = sb.toString();
    }

    /**
     * Initializes version info
     */
    public void init() {
        try (InputStream stream = openRevisionStream("/REVISION")) {
            if (stream == null) {
                Logging.warn(tr("The revision file ''/REVISION'' is missing."));
                version = 0;
                releaseDescription = "";
                return;
            }
            initFromRevisionInfo(stream);
        } catch (IOException e) {
            Logging.warn(e);
        }
    }

    private static InputStream openRevisionStream(String path) throws IOException {
        try {
            return Version.class.getResourceAsStream(path);
        } catch (InvalidPathException e) {
            Logging.error("Cannot open {0}: {1}", path, e.getMessage());
            URL betterUrl = Utils.betterJarUrl(Version.class.getResource(path));
            if (betterUrl != null) {
                return betterUrl.openStream();
            }
            return null;
        }
    }

    /**
     * Replies the version string. Either the SVN revision "1234" (as string) or the
     * the I18n equivalent of "UNKNOWN".
     *
     * @return the JOSM version
     */
    public String getVersionString() {
        return version == 0 ? tr("UNKNOWN") : Integer.toString(version);
    }

    /**
     * Replies a text with the release attributes
     *
     * @return a text with the release attributes
     */
    public String getReleaseAttributes() {
        return releaseDescription;
    }

    /**
     * Replies the build date as string
     *
     * @return the build date as string
     */
    public String getTime() {
        return time;
    }

    /**
     * Replies the JOSM version. Replies {@link #JOSM_UNKNOWN_VERSION} if the version isn't known.
     * @return the JOSM version
     */
    public int getVersion() {
        return version;
    }

    /**
     * Replies true if this is a local build, i.e. an unofficial development build.
     *
     * @return true if this is a local build, i.e. an unofficial development build.
     */
    public boolean isLocalBuild() {
        return isLocalBuild;
    }

    /**
     * Returns the User-Agent string
     * @return The User-Agent
     */
    public String getAgentString() {
        return getAgentString(true);
    }

    /**
     * Returns the User-Agent string, with or without OS details
     * @param includeOsDetails Append Operating System details at the end of the User-Agent
     * @return The User-Agent
     * @since 5956
     */
    public String getAgentString(boolean includeOsDetails) {
        int v = getVersion();
        String s = (v == JOSM_UNKNOWN_VERSION) ? "UNKNOWN" : Integer.toString(v);
        if (buildName != null && !buildName.isEmpty()) {
            s += ' ' + buildName;
        }
        if (isLocalBuild() && v != JOSM_UNKNOWN_VERSION) {
            s += " SVN";
        }
        String result = "JOSM/1.5 ("+ s+' '+LanguageInfo.getJOSMLocaleCode()+')';
        if (includeOsDetails) {
            result += ' ' + PlatformManager.getPlatform().getOSDescription();
        }
        return result;
    }

    /**
     * Returns the full User-Agent string
     * @return The User-Agent
     * @since 5868
     */
    public String getFullAgentString() {
        return getAgentString() + " Java/"+Utils.getSystemProperty("java.version");
    }
}
