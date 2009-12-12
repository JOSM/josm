// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.plugins;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.tools.LanguageInfo;

/**
 * Encapsulate general information about a plugin. This information is available
 * without the need of loading any class from the plugin jar file.
 *
 * @author imi
 */
public class PluginInformation {
    public File file = null;
    public String name = null;
    public int mainversion = 0;
    public String className = null;
    public boolean oldmode = false;
    public String requires = null;
    public String link = null;
    public String description = null;
    public boolean early = false;
    public String author = null;
    public int stage = 50;
    public String version = null;
    public String downloadlink = null;
    public List<URL> libraries = new LinkedList<URL>();

    public final Map<String, String> attr = new TreeMap<String, String>();

    /**
     * Used in the Plugin constructor to make the information of the plugin
     * that is currently initializing available.
     *
     * If you think this is hacky, you are probably right. But it is
     * convinient anyway ;-)
     */
    static PluginInformation currentPluginInitialization = null;

    /**
     * @param file the plugin jar file.
     */
    public PluginInformation(File file) {
        this(file, file.getName().substring(0, file.getName().length()-4));
    }

    public PluginInformation(File file, String name) {
        this.name = name;
        this.file = file;
        try {
            JarInputStream jar = new JarInputStream(new FileInputStream(file));
            Manifest manifest = jar.getManifest();
            if (manifest == null)
                throw new IOException(file+" contains no manifest.");
            scanManifest(manifest, false);
            libraries.add(0, fileToURL(file));
            jar.close();
        } catch (IOException e) {
            throw new PluginException(null, name, e);
        }
    }

    public PluginInformation(InputStream manifestStream, String name, String url) {
        this.name = name;
        try {
            Manifest manifest = new Manifest();
            manifest.read(manifestStream);
            if(url != null) {
                downloadlink = url;
            }
            scanManifest(manifest, url != null);
        } catch (IOException e) {
            throw new PluginException(null, name, e);
        }
    }

    private void scanManifest(Manifest manifest, boolean oldcheck)
    {
        String lang = LanguageInfo.getLanguageCodeManifest();
        Attributes attr = manifest.getMainAttributes();
        className = attr.getValue("Plugin-Class");
        String s = attr.getValue(lang+"Plugin-Link");
        if(s == null) {
            s = attr.getValue("Plugin-Link");
        }
        link = s;
        requires = attr.getValue("Plugin-Requires");
        s = attr.getValue(lang+"Plugin-Description");
        if(s == null)
        {
            s = attr.getValue("Plugin-Description");
            if(s != null) {
                s = tr(s);
            }
        }
        description = s;
        early = Boolean.parseBoolean(attr.getValue("Plugin-Early"));
        String stageStr = attr.getValue("Plugin-Stage");
        stage = stageStr == null ? 50 : Integer.parseInt(stageStr);
        version = attr.getValue("Plugin-Version");
        try { mainversion = Integer.parseInt(attr.getValue("Plugin-Mainversion")); }
        catch(NumberFormatException e) {}
        author = attr.getValue("Author");
        if(oldcheck && mainversion > Version.getInstance().getVersion())
        {
            int myv = Version.getInstance().getVersion();
            for(Map.Entry<Object, Object> entry : attr.entrySet())
            {
                try {
                    String key = ((Attributes.Name)entry.getKey()).toString();
                    if(key.endsWith("_Plugin-Url"))
                    {
                        int mv = Integer.parseInt(key.substring(0,key.length()-11));
                        if(mv <= myv && (mv > mainversion || mainversion > myv))
                        {
                            String v = (String)entry.getValue();
                            int i = v.indexOf(";");
                            if(i > 0)
                            {
                                downloadlink = v.substring(i+1);
                                mainversion = mv;
                                version = v.substring(0,i);
                                oldmode = true;
                            }
                        }
                    }
                }
                catch(Exception e) { e.printStackTrace(); }
            }
        }

        String classPath = attr.getValue(Attributes.Name.CLASS_PATH);
        if (classPath != null) {
            for (String entry : classPath.split(" ")) {
                File entryFile;
                if (new File(entry).isAbsolute()) {
                    entryFile = new File(entry);
                } else {
                    entryFile = new File(file.getParent(), entry);
                }

                libraries.add(fileToURL(entryFile));
            }
        }
        for (Object o : attr.keySet()) {
            this.attr.put(o.toString(), attr.getValue(o.toString()));
        }
    }

    public String getLinkDescription()
    {
        String d = description == null ? tr("no description available") : description;
        if(link != null) {
            d += " <A HREF=\""+link+"\">"+tr("More details")+"</A>";
        }
        return d;
    }

    /**
     * Load and instantiate the plugin
     */
    public PluginProxy load(Class<?> klass) {
        try {
            currentPluginInitialization = this;
            return new PluginProxy(klass.newInstance(), this);
        } catch (Exception e) {
            throw new PluginException(null, name, e);
        }
    }

    /**
     * Load the class of the plugin
     */
    public Class<?> loadClass(ClassLoader classLoader) {
        if (className == null)
            return null;
        try {
            Class<?> realClass = Class.forName(className, true, classLoader);
            return realClass;
        } catch (Exception e) {
            throw new PluginException(null, name, e);
        }
    }

    public static URL fileToURL(File f) {
        try {
            return f.toURI().toURL();
        } catch (MalformedURLException ex) {
            return null;
        }
    }

    /**
     * Try to find a plugin after some criterias. Extract the plugin-information
     * from the plugin and return it. The plugin is searched in the following way:
     *
     *<li>first look after an MANIFEST.MF in the package org.openstreetmap.josm.plugins.<plugin name>
     *    (After removing all fancy characters from the plugin name).
     *    If found, the plugin is loaded using the bootstrap classloader.
     *<li>If not found, look for a jar file in the user specific plugin directory
     *    (~/.josm/plugins/<plugin name>.jar)
     *<li>If not found and the environment variable JOSM_RESSOURCES + "/plugins/" exist, look there.
     *<li>Try for the java property josm.ressources + "/plugins/" (set via java -Djosm.plugins.path=...)
     *<li>If the environment variable ALLUSERSPROFILE and APPDATA exist, look in
     *    ALLUSERSPROFILE/<the last stuff from APPDATA>/JOSM/plugins.
     *    (*sic* There is no easy way under Windows to get the All User's application
     *    directory)
     *<li>Finally, look in some typical unix paths:<ul>
     *    <li>/usr/local/share/josm/plugins/
     *    <li>/usr/local/lib/josm/plugins/
     *    <li>/usr/share/josm/plugins/
     *    <li>/usr/lib/josm/plugins/
     *
     * If a plugin class or jar file is found earlier in the list but seem not to
     * be working, an PluginException is thrown rather than continuing the search.
     * This is so JOSM can detect broken user-provided plugins and do not go silently
     * ignore them.
     *
     * The plugin is not initialized. If the plugin is a .jar file, it is not loaded
     * (only the manifest is extracted). In the classloader-case, the class is
     * bootstraped (e.g. static {} - declarations will run. However, nothing else is done.
     *
     * @param pluginName The name of the plugin (in all lowercase). E.g. "lang-de"
     * @return Information about the plugin or <code>null</code>, if the plugin
     *         was nowhere to be found.
     * @throws PluginException In case of broken plugins.
     */
    public static PluginInformation findPlugin(String pluginName) throws PluginException {
        String name = pluginName;
        name = name.replaceAll("[-. ]", "");
        InputStream manifestStream = PluginInformation.class.getResourceAsStream("/org/openstreetmap/josm/plugins/"+name+"/MANIFEST.MF");
        if (manifestStream != null)
            return new PluginInformation(manifestStream, pluginName, null);

        Collection<String> locations = getPluginLocations();

        for (String s : locations) {
            File pluginFile = new File(s, pluginName + ".jar");
            if (pluginFile.exists()) {
                PluginInformation info = new PluginInformation(pluginFile);
                return info;
            }
        }
        return null;
    }

    public static Collection<String> getPluginLocations() {
        Collection<String> locations = Main.pref.getAllPossiblePreferenceDirs();
        Collection<String> all = new ArrayList<String>(locations.size());
        for (String s : locations) {
            all.add(s+"plugins");
        }
        return all;
    }
}
