// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Platform;
import org.openstreetmap.josm.tools.PlatformManager;
import org.openstreetmap.josm.tools.Utils;

/**
 * Encapsulate general information about a plugin. This information is available
 * without the need of loading any class from the plugin jar file.
 *
 * @author imi
 * @since 153
 */
public class PluginInformation {

    /** The plugin jar file. */
    public File file;
    /** The plugin name. */
    public String name;
    /** The lowest JOSM version required by this plugin (from plugin list). **/
    public int mainversion;
    /** The lowest JOSM version required by this plugin (from locally available jar). **/
    public int localmainversion;
    /** The lowest Java version required by this plugin (from plugin list). **/
    public int minjavaversion;
    /** The lowest Java version required by this plugin (from locally available jar). **/
    public int localminjavaversion;
    /** The plugin class name. */
    public String className;
    /** Determines if the plugin is an old version loaded for incompatibility with latest JOSM (from plugin list) */
    public boolean oldmode;
    /** The list of required plugins, separated by ';' (from plugin list). */
    public String requires;
    /** The list of required plugins, separated by ';' (from locally available jar). */
    public String localrequires;
    /** The plugin platform on which it is meant to run (windows, osx, unixoid). */
    public String platform;
    /** The virtual plugin provided by this plugin, if native for a given platform. */
    public String provides;
    /** The plugin link (for documentation). */
    public String link;
    /** The plugin description. */
    public String description;
    /** Determines if the plugin must be loaded early or not. */
    public boolean early;
    /** The plugin author. */
    public String author;
    /** The plugin stage, determining the loading sequence order of plugins. */
    public int stage = 50;
    /** The plugin version (from plugin list). **/
    public String version;
    /** The plugin version (from locally available jar). **/
    public String localversion;
    /** The plugin download link. */
    public String downloadlink;
    /** The plugin icon path inside jar. */
    public String iconPath;
    /** The plugin icon. */
    private ImageProvider icon;
    /** Plugin can be loaded at any time and not just at start. */
    public boolean canloadatruntime;
    /** The libraries referenced in Class-Path manifest attribute. */
    public List<URL> libraries = new LinkedList<>();
    /** All manifest attributes. */
    public final Map<String, String> attr = new TreeMap<>();
    /** Invalid manifest entries */
    final List<String> invalidManifestEntries = new ArrayList<>();
    /** Empty icon for these plugins which have none */
    private static final ImageIcon emptyIcon = ImageProvider.getEmpty(ImageProvider.ImageSizes.LARGEICON);

    /**
     * Creates a plugin information object by reading the plugin information from
     * the manifest in the plugin jar.
     *
     * The plugin name is derived from the file name.
     *
     * @param file the plugin jar file
     * @throws PluginException if reading the manifest fails
     */
    public PluginInformation(File file) throws PluginException {
        this(file, file.getName().substring(0, file.getName().length()-4));
    }

    /**
     * Creates a plugin information object for the plugin with name {@code name}.
     * Information about the plugin is extracted from the manifest file in the plugin jar
     * {@code file}.
     * @param file the plugin jar
     * @param name the plugin name
     * @throws PluginException if reading the manifest file fails
     */
    public PluginInformation(File file, String name) throws PluginException {
        if (!PluginHandler.isValidJar(file)) {
            throw new PluginException(tr("Invalid jar file ''{0}''", file));
        }
        this.name = name;
        this.file = file;
        try (
            InputStream fis = Files.newInputStream(file.toPath());
            JarInputStream jar = new JarInputStream(fis)
        ) {
            Manifest manifest = jar.getManifest();
            if (manifest == null)
                throw new PluginException(tr("The plugin file ''{0}'' does not include a Manifest.", file.toString()));
            scanManifest(manifest, false);
            libraries.add(0, Utils.fileToURL(file));
        } catch (IOException | InvalidPathException e) {
            throw new PluginException(name, e);
        }
    }

    /**
     * Creates a plugin information object by reading plugin information in Manifest format
     * from the input stream {@code manifestStream}.
     *
     * @param manifestStream the stream to read the manifest from
     * @param name the plugin name
     * @param url the download URL for the plugin
     * @throws PluginException if the plugin information can't be read from the input stream
     */
    public PluginInformation(InputStream manifestStream, String name, String url) throws PluginException {
        this.name = name;
        try {
            Manifest manifest = new Manifest();
            manifest.read(manifestStream);
            if (url != null) {
                downloadlink = url;
            }
            scanManifest(manifest, url != null);
        } catch (IOException e) {
            throw new PluginException(name, e);
        }
    }

    /**
     * Updates the plugin information of this plugin information object with the
     * plugin information in a plugin information object retrieved from a plugin
     * update site.
     *
     * @param other the plugin information object retrieved from the update site
     */
    public void updateFromPluginSite(PluginInformation other) {
        this.mainversion = other.mainversion;
        this.minjavaversion = other.minjavaversion;
        this.className = other.className;
        this.requires = other.requires;
        this.provides = other.provides;
        this.platform = other.platform;
        this.link = other.link;
        this.description = other.description;
        this.early = other.early;
        this.author = other.author;
        this.stage = other.stage;
        this.version = other.version;
        this.downloadlink = other.downloadlink;
        this.icon = other.icon;
        this.iconPath = other.iconPath;
        this.canloadatruntime = other.canloadatruntime;
        this.libraries = other.libraries;
        this.attr.clear();
        this.attr.putAll(other.attr);
        this.invalidManifestEntries.clear();
        this.invalidManifestEntries.addAll(other.invalidManifestEntries);
    }

    /**
     * Updates the plugin information of this plugin information object with the
     * plugin information in a plugin information object retrieved from a plugin jar.
     *
     * @param other the plugin information object retrieved from the jar file
     * @since 5601
     */
    public void updateFromJar(PluginInformation other) {
        updateLocalInfo(other);
        if (other.icon != null) {
            this.icon = other.icon;
        }
        this.early = other.early;
        this.className = other.className;
        this.canloadatruntime = other.canloadatruntime;
        this.libraries = other.libraries;
        this.stage = other.stage;
        this.file = other.file;
    }

    private void scanManifest(Manifest manifest, boolean oldcheck) {
        String lang = LanguageInfo.getLanguageCodeManifest();
        Attributes attr = manifest.getMainAttributes();
        className = attr.getValue("Plugin-Class");
        String s = Optional.ofNullable(attr.getValue(lang+"Plugin-Link")).orElseGet(() -> attr.getValue("Plugin-Link"));
        if (s != null && !Utils.isValidUrl(s)) {
            Logging.info(tr("Invalid URL ''{0}'' in plugin {1}", s, name));
            s = null;
        }
        link = s;
        platform = attr.getValue("Plugin-Platform");
        provides = attr.getValue("Plugin-Provides");
        requires = attr.getValue("Plugin-Requires");
        s = attr.getValue(lang+"Plugin-Description");
        if (s == null) {
            s = attr.getValue("Plugin-Description");
            if (s != null) {
                try {
                    s = tr(s);
                } catch (IllegalArgumentException e) {
                    Logging.debug(e);
                    Logging.info(tr("Invalid plugin description ''{0}'' in plugin {1}", s, name));
                }
            }
        } else {
            s = MessageFormat.format(s, (Object[]) null);
        }
        description = s;
        early = Boolean.parseBoolean(attr.getValue("Plugin-Early"));
        String stageStr = attr.getValue("Plugin-Stage");
        stage = stageStr == null ? 50 : Integer.parseInt(stageStr);
        version = attr.getValue("Plugin-Version");
        if (version != null && !version.isEmpty() && version.charAt(0) == '$') {
            invalidManifestEntries.add("Plugin-Version");
        }
        s = attr.getValue("Plugin-Mainversion");
        if (s != null) {
            try {
                mainversion = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                Logging.warn(tr("Invalid plugin main version ''{0}'' in plugin {1}", s, name));
                Logging.trace(e);
            }
        } else {
            Logging.warn(tr("Missing plugin main version in plugin {0}", name));
        }
        s = attr.getValue("Plugin-Minimum-Java-Version");
        if (s != null) {
            try {
                minjavaversion = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                Logging.warn(tr("Invalid Java version ''{0}'' in plugin {1}", s, name));
                Logging.trace(e);
            }
        }
        author = attr.getValue("Author");
        iconPath = attr.getValue("Plugin-Icon");
        if (iconPath != null) {
            if (file != null) {
                // extract icon from the plugin jar file
                icon = new ImageProvider(iconPath).setArchive(file).setMaxSize(ImageProvider.ImageSizes.LARGEICON).setOptional(true);
            } else if (iconPath.startsWith("data:")) {
                icon = new ImageProvider(iconPath).setMaxSize(ImageProvider.ImageSizes.LARGEICON).setOptional(true);
            }
        }
        canloadatruntime = Boolean.parseBoolean(attr.getValue("Plugin-Canloadatruntime"));
        int myv = Version.getInstance().getVersion();
        for (Map.Entry<Object, Object> entry : attr.entrySet()) {
            String key = ((Attributes.Name) entry.getKey()).toString();
            if (key.endsWith("_Plugin-Url")) {
                try {
                    int mv = Integer.parseInt(key.substring(0, key.length()-11));
                    String v = (String) entry.getValue();
                    int i = v.indexOf(';');
                    if (i <= 0) {
                        invalidManifestEntries.add(key);
                    } else if (oldcheck &&
                        mv <= myv && (mv > mainversion || mainversion > myv)) {
                        downloadlink = v.substring(i+1);
                        mainversion = mv;
                        version = v.substring(0, i);
                        oldmode = true;
                    }
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    invalidManifestEntries.add(key);
                    Logging.error(e);
                }
            }
        }

        String classPath = attr.getValue(Attributes.Name.CLASS_PATH);
        if (classPath != null) {
            for (String entry : classPath.split(" ")) {
                File entryFile;
                if (new File(entry).isAbsolute() || file == null) {
                    entryFile = new File(entry);
                } else {
                    entryFile = new File(file.getParent(), entry);
                }

                libraries.add(Utils.fileToURL(entryFile));
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
        StringBuilder sb = new StringBuilder(128);
        sb.append("<html><body>")
          .append(description == null ? tr("no description available") : Utils.escapeReservedCharactersHTML(description));
        if (link != null) {
            sb.append(" <a href=\"").append(link).append("\">").append(tr("More info...")).append("</a>");
        }
        if (downloadlink != null
                && !downloadlink.startsWith("http://svn.openstreetmap.org/applications/editors/josm/dist/")
                && !downloadlink.startsWith("https://svn.openstreetmap.org/applications/editors/josm/dist/")
                && !downloadlink.startsWith("http://trac.openstreetmap.org/browser/applications/editors/josm/dist/")
                && !downloadlink.startsWith("https://github.com/JOSM/")) {
            sb.append("<p>&nbsp;</p><p>").append(tr("<b>Plugin provided by an external source:</b> {0}", downloadlink)).append("</p>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * Loads and instantiates the plugin.
     *
     * @param klass the plugin class
     * @param classLoader the class loader for the plugin
     * @return the instantiated and initialized plugin
     * @throws PluginException if the plugin cannot be loaded or instanciated
     * @since 12322
     */
    public PluginProxy load(Class<?> klass, PluginClassLoader classLoader) throws PluginException {
        try {
            Constructor<?> c = klass.getConstructor(PluginInformation.class);
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);
            try {
                return new PluginProxy(c.newInstance(this), this, classLoader);
            } finally {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
        } catch (ReflectiveOperationException e) {
            throw new PluginException(name, e);
        }
    }

    /**
     * Loads the class of the plugin.
     *
     * @param classLoader the class loader to use
     * @return the loaded class
     * @throws PluginException if the class cannot be loaded
     */
    public Class<?> loadClass(ClassLoader classLoader) throws PluginException {
        if (className == null)
            return null;
        try {
            return Class.forName(className, true, classLoader);
        } catch (NoClassDefFoundError | ClassNotFoundException | ClassCastException e) {
            Logging.logWithStackTrace(Level.SEVERE, e,
                    "Unable to load class {0} from plugin {1} using classloader {2}", className, name, classLoader);
            throw new PluginException(name, e);
        }
    }

    /**
     * Try to find a plugin after some criteria. Extract the plugin-information
     * from the plugin and return it. The plugin is searched in the following way:
     *<ol>
     *<li>first look after an MANIFEST.MF in the package org.openstreetmap.josm.plugins.&lt;plugin name&gt;
     *    (After removing all fancy characters from the plugin name).
     *    If found, the plugin is loaded using the bootstrap classloader.</li>
     *<li>If not found, look for a jar file in the user specific plugin directory
     *    (~/.josm/plugins/&lt;plugin name&gt;.jar)</li>
     *<li>If not found and the environment variable JOSM_RESOURCES + "/plugins/" exist, look there.</li>
     *<li>Try for the java property josm.resources + "/plugins/" (set via java -Djosm.plugins.path=...)</li>
     *<li>If the environment variable ALLUSERSPROFILE and APPDATA exist, look in
     *    ALLUSERSPROFILE/&lt;the last stuff from APPDATA&gt;/JOSM/plugins.
     *    (*sic* There is no easy way under Windows to get the All User's application
     *    directory)</li>
     *<li>Finally, look in some typical unix paths:<ul>
     *    <li>/usr/local/share/josm/plugins/</li>
     *    <li>/usr/local/lib/josm/plugins/</li>
     *    <li>/usr/share/josm/plugins/</li>
     *    <li>/usr/lib/josm/plugins/</li></ul></li>
     *</ol>
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
        try (InputStream manifestStream = Utils.getResourceAsStream(
                PluginInformation.class, "/org/openstreetmap/josm/plugins/"+name+"/MANIFEST.MF")) {
            if (manifestStream != null) {
                return new PluginInformation(manifestStream, pluginName, null);
            }
        } catch (IOException e) {
            Logging.warn(e);
        }

        Collection<String> locations = getPluginLocations();

        String[] nameCandidates = {
                pluginName,
                pluginName + "-" + PlatformManager.getPlatform().getPlatform().name().toLowerCase(Locale.ENGLISH)};
        for (String s : locations) {
            for (String nameCandidate: nameCandidates) {
                File pluginFile = new File(s, nameCandidate + ".jar");
                if (pluginFile.exists()) {
                    return new PluginInformation(pluginFile);
                }
            }
        }
        return null;
    }

    /**
     * Returns all possible plugin locations.
     * @return all possible plugin locations.
     */
    public static Collection<String> getPluginLocations() {
        Collection<String> locations = Preferences.getAllPossiblePreferenceDirs();
        Collection<String> all = new ArrayList<>(locations.size());
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
        if (this.downloadlink == null) return false;
        if (this.version == null && referenceVersion != null)
            return true;
        return this.version != null && !this.version.equals(referenceVersion);
    }

    /**
     * Replies true if this this plugin should be updated/downloaded because either
     * it is not available locally (its local version is null) or its local version is
     * older than the available version on the server.
     *
     * @return true if the plugin should be updated
     */
    public boolean isUpdateRequired() {
        if (this.downloadlink == null) return false;
        if (this.localversion == null) return true;
        return isUpdateRequired(this.localversion);
    }

    protected boolean matches(String filter, String value) {
        if (filter == null) return true;
        if (value == null) return false;
        return value.toLowerCase(Locale.ENGLISH).contains(filter.toLowerCase(Locale.ENGLISH));
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
        String[] words = filter.split("\\s+");
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
     * Replies the name of the plugin.
     * @return The plugin name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name
     * @param name Plugin name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Replies the plugin icon, scaled to LARGE_ICON size.
     * @return the plugin icon, scaled to LARGE_ICON size.
     */
    public ImageIcon getScaledIcon() {
        ImageIcon img = (icon != null) ? icon.get() : null;
        if (img == null)
            return emptyIcon;
        return img;
    }

    @Override
    public final String toString() {
        return getName();
    }

    private static List<String> getRequiredPlugins(String pluginList) {
        List<String> requiredPlugins = new ArrayList<>();
        if (pluginList != null) {
            for (String s : pluginList.split(";")) {
                String plugin = s.trim();
                if (!plugin.isEmpty()) {
                    requiredPlugins.add(plugin);
                }
            }
        }
        return requiredPlugins;
    }

    /**
     * Replies the list of plugins required by the up-to-date version of this plugin.
     * @return List of plugins required. Empty if no plugin is required.
     * @since 5601
     */
    public List<String> getRequiredPlugins() {
        return getRequiredPlugins(requires);
    }

    /**
     * Replies the list of plugins required by the local instance of this plugin.
     * @return List of plugins required. Empty if no plugin is required.
     * @since 5601
     */
    public List<String> getLocalRequiredPlugins() {
        return getRequiredPlugins(localrequires);
    }

    /**
     * Updates the local fields
     * ({@link #localversion}, {@link #localmainversion}, {@link #localminjavaversion}, {@link #localrequires})
     * to values contained in the up-to-date fields
     * ({@link #version}, {@link #mainversion}, {@link #minjavaversion}, {@link #requires})
     * of the given PluginInformation.
     * @param info The plugin information to get the data from.
     * @since 5601
     */
    public void updateLocalInfo(PluginInformation info) {
        if (info != null) {
            this.localversion = info.version;
            this.localmainversion = info.mainversion;
            this.localminjavaversion = info.minjavaversion;
            this.localrequires = info.requires;
        }
    }

    /**
     * Determines if this plugin can be run on the current platform.
     * @return {@code true} if this plugin can be run on the current platform
     * @since 14384
     */
    public boolean isForCurrentPlatform() {
        try {
            return platform == null || PlatformManager.getPlatform().getPlatform() == Platform.valueOf(platform.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            Logging.warn(e);
            return true;
        }
    }
}
