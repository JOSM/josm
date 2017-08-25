// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences.sources;

import java.io.File;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * A source entry primarily used to save the user's selection of mappaint styles,
 * but also for preset sources or validator rules.
 * @since 12649 (moved from gui.preferences package)
 * @since 3796
 */
public class SourceEntry {

    /**
     *  A URL can be anything that CachedFile understands, i.e.
     *  a local file, http://, or a file from the current jar
     */
    public String url;

    /**
     * Indicates, that {@link #url} is a zip file and the resource is
     * inside the zip file.
     */
    public boolean isZip;

    /**
     * If {@link #isZip} is true, denotes the path inside the zip file.
     */
    public String zipEntryPath;

    /**
     *  Name is used as a namespace for color preferences and (currently) only
     *  one file with a name can be loaded at a time. Additional styles must
     *  either have the same name as the main style or no name at all.
     *  If no name is provided, it will be set to the default value "standard".
     *  The name can also be given in the xml file as attribute for the rules tag.
     *  (This overrides the name given in the preferences, otherwise both
     *  methods are equivalent.)
     */
    public String name;

    /**
     * A title that can be used as menu entry.
     */
    public String title;

    /**
     * active is a boolean flag that can be used to turn the source on or off at runtime.
     */
    public boolean active;

    /**
     * Constructs a new {@code SourceEntry}.
     * @param url URL that {@link org.openstreetmap.josm.io.CachedFile} understands
     * @param isZip if url is a zip file and the resource is inside the zip file
     * @param zipEntryPath If {@code isZip} is {@code true}, denotes the path inside the zip file
     * @param name Source name
     * @param title title that can be used as menu entry
     * @param active boolean flag that can be used to turn the source on or off at runtime
     * @see #url
     * @see #isZip
     * @see #zipEntryPath
     * @see #name
     * @see #title
     * @see #active
     */
    public SourceEntry(String url, boolean isZip, String zipEntryPath, String name, String title, boolean active) {
        this.url = url;
        this.isZip = isZip;
        this.zipEntryPath = "".equals(zipEntryPath) ? null : zipEntryPath;
        this.name = "".equals(name) ? null : name;
        this.title = "".equals(title) ? null : title;
        this.active = active;
    }

    /**
     * Constructs a new {@code SourceEntry}.
     * @param url URL that {@link org.openstreetmap.josm.io.CachedFile} understands
     * @param name Source name
     * @param title title that can be used as menu entry
     * @param active boolean flag that can be used to turn the source on or off at runtime
     * @see #url
     * @see #name
     * @see #title
     * @see #active
     */
    public SourceEntry(String url, String name, String title, boolean active) {
        this(url, false, null, name, title, active);
    }

    /**
     * Constructs a new {@code SourceEntry}.
     * @param e existing source entry to copy
     */
    public SourceEntry(SourceEntry e) {
        this.url = e.url;
        this.isZip = e.isZip;
        this.zipEntryPath = e.zipEntryPath;
        this.name = e.name;
        this.title = e.title;
        this.active = e.active;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SourceEntry that = (SourceEntry) obj;
        return isZip == that.isZip &&
                active == that.active &&
                Objects.equals(url, that.url) &&
                Objects.equals(zipEntryPath, that.zipEntryPath) &&
                Objects.equals(name, that.name) &&
                Objects.equals(title, that.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, isZip, zipEntryPath, name, title, active);
    }

    @Override
    public String toString() {
        return title != null ? title : url;
    }

    /**
     * String to show in menus and error messages.
     * @return Usually the shortdescription, but can be the file name
     * if no shortdescription is available.
     */
    public String getDisplayString() {
        if (title != null)
            return title;
        else
            return getFileNamePart();
    }

    /**
     * Extracts file part from url, e.g.:
     * <code>http://www.test.com/file.xml?format=text --&gt; file.xml</code>
     * @return The filename part of the URL
     */
    public String getFileNamePart() {
        Pattern p = Pattern.compile("([^/\\\\]*?)([?].*)?$");
        Matcher m = p.matcher(url);
        if (m.find()) {
            return m.group(1);
        } else {
            Logging.warn("Unexpected URL format: "+url);
            return url;
        }
    }

    /**
     * the name / identifier that should be used to save custom color values
     * and similar stuff to the preference file
     * @return the identifier; never null. Usually the result is "standard"
     */
    public String getPrefName() {
        return name == null ? "standard" : name;
    }

    /**
     * Determines if this source denotes a file on a local filesystem.
     * @return {@code true} if the source is a local file
     */
    public boolean isLocal() {
        return Utils.isLocalUrl(url);
    }

    /**
     * Return the source directory, only for local files.
     * @return The source directory, or {@code null} if this file isn't local, or does not have a parent
     * @since 7276
     */
    public File getLocalSourceDir() {
        if (!isLocal())
            return null;
        return new File(url).getParentFile();
    }

    /**
     * Returns the parent directory of the resource inside the zip file.
     *
     * @return the parent directory of the resource inside the zip file,
     * "." if zipEntryPath is a top level file; null, if zipEntryPath is null
     */
    public String getZipEntryDirName() {
        if (zipEntryPath == null) return null;
        File file = new File(zipEntryPath);
        File dir = file.getParentFile();
        if (dir == null) return ".";
        String path = dir.getPath();
        if (!"/".equals(File.separator)) {
            path = path.replace(File.separator, "/");
        }
        return path;
    }
}
