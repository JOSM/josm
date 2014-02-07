// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.Utils;

/**
 * Provides basic information about the currently used JOSM build.
 *
 */
public class Version {
    /** constant to indicate that the current build isn't assigned a JOSM version number */
    static public final int JOSM_UNKNOWN_VERSION = 0;

    /** the unique instance */
    private static Version instance;

    /**
     * Load the specified resource as string.
     *
     * @param resource the resource url to load
     * @return  the content of the resource file; null, if an error occurred
     */
    static public String loadResourceFile(URL resource) {
        if (resource == null) return null;
        String s = null;
        try {
            BufferedReader in = Utils.openURLReader(resource);
            StringBuilder sb = new StringBuilder();
            try {
                for (String line = in.readLine(); line != null; line = in.readLine()) {
                    sb.append(line).append("\n");
                }
            } finally {
                Utils.close(in);
            }
            s = sb.toString();
        } catch (IOException e) {
            Main.error(tr("Failed to load resource ''{0}'', error is {1}.", resource.toString(), e.toString()));
            Main.error(e);
        }
        return s;
    }

    /**
     * Replies the unique instance of the version information
     *
     * @return the unique instance of the version information
     */

    static public Version getInstance() {
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

    protected Map<String, String> parseManifestStyleFormattedString(String content) {
        Map<String, String> properties = new HashMap<String, String>();
        if (content == null) return properties;
        Pattern p = Pattern.compile("^([^:]+):(.*)$");
        for (String line: content.split("\n")) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            if (line.matches("^\\s*#.*$")) {
                continue;
            }
            Matcher m = p.matcher(line);
            if (m.matches()) {
                properties.put(m.group(1), m.group(2));
            }
        }
        return properties;
    }

    /**
     * Initializes the version infos from the revision resource file
     *
     * @param revisionInfo the revision info loaded from a revision resource file
     */
    protected void initFromRevisionInfo(String revisionInfo) {
        if (revisionInfo == null) {
            this.releaseDescription = tr("UNKNOWN");
            this.version = JOSM_UNKNOWN_VERSION;
            this.time = null;
            return;
        }

        Map<String, String> properties = parseManifestStyleFormattedString(revisionInfo);
        String value = properties.get("Revision");
        if (value != null) {
            value = value.trim();
            try {
                version = Integer.parseInt(value);
            } catch(NumberFormatException e) {
                version = 0;
                Main.warn(tr("Unexpected JOSM version number in revision file, value is ''{0}''", value));
            }
        } else {
            version = JOSM_UNKNOWN_VERSION;
        }

        // the last changed data
        //
        time = properties.get("Last Changed Date");
        if (time == null) {
            time = properties.get("Build-Date");
        }

        // is this a local build ?
        //
        isLocalBuild = false;
        value = properties.get("Is-Local-Build");
        if (value != null && value.trim().equalsIgnoreCase("true"))  {
            isLocalBuild = true;
        }

        // is this a specific build ?
        //
        buildName = null;
        value = properties.get("Build-Name");
        if (value != null && !value.trim().isEmpty())  {
            buildName = value.trim();
        }

        // the revision info
        //
        StringBuilder sb = new StringBuilder();
        for(Entry<String,String> property: properties.entrySet()) {
            sb.append(property.getKey()).append(":").append(property.getValue()).append("\n");
        }
        releaseDescription = sb.toString();
    }

    /**
     * Initializes version info
     */
    public void init() {
        URL u = Main.class.getResource("/REVISION");
        if (u == null) {
            Main.warn(tr("The revision file ''/REVISION'' is missing."));
            version = 0;
            releaseDescription = "";
            return;
        }
        initFromRevisionInfo(loadResourceFile(u));
    }

    /**
     * Replies the version string. Either the SVN revision "1234" (as string) or the
     * the I18n equivalent of "UNKNOWN".
     *
     * @return the JOSM version
     */
    public String getVersionString() {
        return  version == 0 ? tr("UNKNOWN") : Integer.toString(version);
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
     * Replies true if this is a local build, i.e. an inofficial development build.
     *
     * @return true if this is a local build, i.e. an inofficial development build.
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
        if (buildName != null) {
            s += " " + buildName;
        }
        if (isLocalBuild() && v != JOSM_UNKNOWN_VERSION) {
            s += " SVN";
        }
        String result = "JOSM/1.5 ("+ s+" "+LanguageInfo.getJOSMLocaleCode()+")";
        if (includeOsDetails && Main.platform != null) {
            result += " " + Main.platform.getOSDescription();
        }
        return result;
    }

    /**
     * Returns the full User-Agent string
     * @return The User-Agent
     * @since 5868
     */
    public String getFullAgentString() {
        return getAgentString() + " Java/"+System.getProperty("java.version");
    }
}
