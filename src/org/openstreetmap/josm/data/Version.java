// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.LanguageInfo;

/**
 * Provides basic information about the currently used JOSM build.
 *
 */
public class Version {
    /** constant to indicate that the currnt build isn't assigned a JOSM version number */
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
        BufferedReader in;
        String s = null;
        try {
            in = new BufferedReader(new InputStreamReader(resource.openStream()));
            StringBuffer sb = new StringBuffer();
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                sb.append(line).append("\n");
            }
            s = sb.toString();
        } catch (IOException e) {
            System.err.println(tr("Failed to load resource ''{0}'', error is {1}.", resource.toString(), e.toString()));
            e.printStackTrace();
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
    private String revision;
    private String time;
    private boolean isLocalBuild;

    protected HashMap<String, String> parseManifestStyleFormattedString(String content) {
        HashMap<String, String> properties = new HashMap<String, String>();
        if (content == null) return properties;
        Pattern p = Pattern.compile("^([^:]+):(.*)$");
        for (String line: content.split("\n")) {
            if (line == null || line.trim().equals("")) continue;
            if (line.matches("^\\s*#.*$")) continue;
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
            this.revision = tr("UNKNOWN");
            this.version = JOSM_UNKNOWN_VERSION;
            this.time = null;
            return;
        }

        HashMap<String, String> properties = parseManifestStyleFormattedString(revisionInfo);
        String value = properties.get("Revision");
        if (value != null) {
            value = value.trim();
            try {
                version = Integer.parseInt(value);
            } catch(NumberFormatException e) {
                version = 0;
                System.err.println(tr("Warning: unexpected JOSM version number in revison file, value is ''{0}''", value));
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
        if (value != null && value.trim().toLowerCase().equals("true"))  {
            isLocalBuild = true;
        }

        // the revision info
        //
        StringBuffer sb = new StringBuffer();
        for(Entry<String,String> property: properties.entrySet()) {
            sb.append(property.getKey()).append(":").append(property.getValue()).append("\n");
        }
        revision = sb.toString();
    }

    public void init() {
        URL u = Main.class.getResource("/REVISION");
        if (u == null) {
            System.err.println(tr("Warning: the revision file ''/REVISION'' is missing."));
            version = 0;
            revision = "";
            return;
        }
        initFromRevisionInfo(loadResourceFile(u));
        System.out.println(revision);
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
        return revision;
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
     * Replies the JOSM version. Replies {@see #JOSM_UNKNOWN_VERSION} if the version isn't known.
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

    public String getAgentString() {
        int v = getVersion();
        String s = (v == JOSM_UNKNOWN_VERSION) ? "UNKNOWN" : Integer.toString(v);
        if (isLocalBuild() && v != JOSM_UNKNOWN_VERSION) {
            s += " SVN";
        }
        return "JOSM/1.5 ("+ s+" "+LanguageInfo.getJOSMLocaleCode()+")";
    }
}
