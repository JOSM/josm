// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.plugins;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
    public String localversion = null;
    public String downloadlink = null;
    public List<URL> libraries = new LinkedList<URL>();

    public final Map<String, String> attr = new TreeMap<String, String>();

    /**
     * @param file the plugin jar file.
     */
    public PluginInformation(File file) throws PluginException{
        this(file, file.getName().substring(0, file.getName().length()-4));
    }

    public PluginInformation(File file, String name) throws PluginException{
        this.name = name;
        this.file = file;
        JarInputStream jar = null;
        try {
            jar = new JarInputStream(new FileInputStream(file));
            Manifest manifest = jar.getManifest();
            if (manifest == null)
                throw new PluginException(name, tr("The plugin file ''{0}'' does not include a Manifest.", file.toString()));
            scanManifest(manifest, false);
            libraries.add(0, fileToURL(file));
        } catch (IOException e) {
            throw new PluginException(name, e);
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch(IOException e) { /* ignore */ }
            }
        }
    }

    public PluginInformation(InputStream manifestStream, String name, String url) throws PluginException {
        this.name = name;
        try {
            Manifest manifest = new Manifest();
            manifest.read(manifestStream);
            if(url != null) {
                downloadlink = url;
            }
            scanManifest(manifest, url != null);
        } catch (IOException e) {
            throw new PluginException(name, e);
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

    /**
     * Replies the description as HTML document, including a link to a web page with
     * more information, provided such a link is available.
     * 
     * @return the description as HTML document
     */
    public String getDescriptionAsHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append(description == null ? tr("no description available") : description);
        if (link != null) {
            sb.append(" <a href=\"").append(link).append("\">").append(tr("More info...")).append("</a>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * Load and instantiate the plugin
     * 
     * @param the plugin class
     * @return the instantiated and initialized plugin
     */
    public PluginProxy load(Class<?> klass) throws PluginException{
        try {
            try {
                Constructor<?> c = klass.getConstructor(PluginInformation.class);
                Object plugin = c.newInstance(this);
                return new PluginProxy(plugin, this);
            } catch(NoSuchMethodException e) {
                // do nothing - try again with the noarg constructor for legacy support
            } catch(InstantiationException e) {
                // do nothing - try again with the noarg constructor for legacy support
            }
            // FIXME: This is legacy support. It is necessary because of a former ugly hack in the
            // plugin bootstrap procedure. Plugins should be migrated to the new required
            // constructor with PluginInformation as argument, new plugins should only use this
            // constructor. The following is legacy support and should be removed by Q2/2010.
            // Note that this is not fool proof because it isn't
            // completely equivalent with the former hack: plugins derived from the Plugin
            // class can't access their local "info" object any more from within the noarg-
            // constructor. It is only set *after* constructor invocation.
            //
            Constructor<?> c = klass.getConstructor();
            Object plugin = c.newInstance();
            if (plugin instanceof Plugin) {
                Method m = klass.getMethod("setPluginInformation", PluginInformation.class);
                m.invoke(plugin, this);
            }
            return new PluginProxy(plugin, this);
        } catch(NoSuchMethodException e) {
            throw new PluginException(name, e);
        } catch(IllegalAccessException e) {
            throw new PluginException(name, e);
        } catch (InstantiationException e) {
            throw new PluginException(name, e);
        } catch(InvocationTargetException e) {
            throw new PluginException(name, e);
        }
    }

    /**
     * Load the class of the plugin
     * 
     * @param classLoader the class loader to use
     * @return the loaded class
     */
    public Class<?> loadClass(ClassLoader classLoader) throws PluginException {
        if (className == null)
            return null;
        try{
            Class<?> realClass = Class.forName(className, true, classLoader);
            return realClass;
        } catch (ClassNotFoundException e) {
            throw new PluginException(name, e);
        } catch(ClassCastException e) {
            throw new PluginException(name, e);
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
     *<li>If not found and the environment variable JOSM_RESOURCES + "/plugins/" exist, look there.
     *<li>Try for the java property josm.resources + "/plugins/" (set via java -Djosm.plugins.path=...)
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

    /**
     * Replies true if the plugin with the given information is most likely outdated with
     * respect to the referenceVersion.
     * 
     * @param referenceVersion the reference version. Can be null if we don't know a
     * reference version
     * 
     * @return true, if the plugin needs to be updated; false, otherweise
     */
    public boolean isUpdateRequired(String referenceVersion) {
        if (this.version == null && referenceVersion!= null)
            return true;
        if (this.version != null && !this.version.equals(referenceVersion))
            return true;
        return false;
    }

    /**
     * Replies true if this this plugin should be updated/downloaded because either
     * it is not available locally (its local version is null) or its local version is
     * older than the available version on the server.
     * 
     * @return true if the plugin should be updated
     */
    public boolean isUpdateRequired() {
        if (this.localversion == null) return true;
        return isUpdateRequired(this.localversion);
    }

    protected boolean matches(String filter, String value) {
        if (filter == null) return true;
        if (value == null) return false;
        return value.toLowerCase().contains(filter.toLowerCase());
    }

    /**
     * Replies true if either the name, the description, or the version match (case insensitive)
     * one of the words in filter. Replies true if filter is null.
     * 
     * @param filter the filter expression
     * @return true if this plugin info matches with the filter
     */
    public boolean matches(String filter) {
        if (filter == null) return true;
        String words[] = filter.split("\\s+");
        for (String word: words) {
            if (matches(word, name)
                    || matches(word, description)
                    || matches(word, version)
                    || matches(word, localversion))
                return true;
        }
        return false;
    }

    /**
     * Replies the name of the plugin
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

}
