// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.PlatformManager;
import org.openstreetmap.josm.tools.Utils;

/**
 * Downloads a file and caches it on disk in order to reduce network load.
 *
 * Supports URLs, local files, and a custom scheme (<code>resource:</code>) to get
 * resources from the current *.jar file. (Local caching is only done for URLs.)
 * <p>
 * The mirrored file is only downloaded if it has been more than 7 days since
 * last download. (Time can be configured.)
 * <p>
 * The file content is normally accessed with {@link #getInputStream()}, but
 * you can also get the mirrored copy with {@link #getFile()}.
 */
public class CachedFile implements Closeable {

    /**
     * Caching strategy.
     */
    public enum CachingStrategy {
        /**
         * If cached file on disk is older than a certain time (7 days by default),
         * consider the cache stale and try to download the file again.
         */
        MaxAge,
        /**
         * Similar to MaxAge, considers the cache stale when a certain age is
         * exceeded. In addition, a If-Modified-Since HTTP header is added.
         * When the server replies "304 Not Modified", this is considered the same
         * as a full download.
         */
        IfModifiedSince
    }

    protected String name;
    protected long maxAge;
    protected String destDir;
    protected String httpAccept;
    protected CachingStrategy cachingStrategy;

    private boolean fastFail;
    private HttpClient activeConnection;
    protected File cacheFile;
    protected boolean initialized;
    protected String parameter;

    public static final long DEFAULT_MAXTIME = -1L;
    public static final long DAYS = TimeUnit.DAYS.toSeconds(1); // factor to get caching time in days

    private final Map<String, String> httpHeaders = new ConcurrentHashMap<>();

    /**
     * Constructs a CachedFile object from a given filename, URL or internal resource.
     *
     * @param name can be:<ul>
     *  <li>relative or absolute file name</li>
     *  <li>{@code file:///SOME/FILE} the same as above</li>
     *  <li>{@code http://...} a URL. It will be cached on disk.</li>
     *  <li>{@code resource://SOME/FILE} file from the classpath (usually in the current *.jar)</li>
     *  <li>{@code josmdir://SOME/FILE} file inside josm user data directory (since r7058)</li>
     *  <li>{@code josmplugindir://SOME/FILE} file inside josm plugin directory (since r7834)</li></ul>
     */
    public CachedFile(String name) {
        this.name = name;
    }

    /**
     * Set the name of the resource.
     * @param name can be:<ul>
     *  <li>relative or absolute file name</li>
     *  <li>{@code file:///SOME/FILE} the same as above</li>
     *  <li>{@code http://...} a URL. It will be cached on disk.</li>
     *  <li>{@code resource://SOME/FILE} file from the classpath (usually in the current *.jar)</li>
     *  <li>{@code josmdir://SOME/FILE} file inside josm user data directory (since r7058)</li>
     *  <li>{@code josmplugindir://SOME/FILE} file inside josm plugin directory (since r7834)</li></ul>
     * @return this object
     */
    public CachedFile setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Set maximum age of cache file. Only applies to URLs.
     * When this time has passed after the last download of the file, the
     * cache is considered stale and a new download will be attempted.
     * @param maxAge the maximum cache age in seconds
     * @return this object
     */
    public CachedFile setMaxAge(long maxAge) {
        this.maxAge = maxAge;
        return this;
    }

    /**
     * Set the destination directory for the cache file. Only applies to URLs.
     * @param destDir the destination directory
     * @return this object
     */
    public CachedFile setDestDir(String destDir) {
        this.destDir = destDir;
        return this;
    }

    /**
     * Set the accepted MIME types sent in the HTTP Accept header. Only applies to URLs.
     * @param httpAccept the accepted MIME types
     * @return this object
     */
    public CachedFile setHttpAccept(String httpAccept) {
        this.httpAccept = httpAccept;
        return this;
    }

    /**
     * Set the caching strategy. Only applies to URLs.
     * @param cachingStrategy caching strategy
     * @return this object
     */
    public CachedFile setCachingStrategy(CachingStrategy cachingStrategy) {
        this.cachingStrategy = cachingStrategy;
        return this;
    }

    /**
     * Sets the http headers. Only applies to URL pointing to http or https resources
     * @param headers that should be sent together with request
     * @return this object
     */
    public CachedFile setHttpHeaders(Map<String, String> headers) {
        this.httpHeaders.putAll(headers);
        return this;
    }

    /**
     * Sets whether opening HTTP connections should fail fast, i.e., whether a
     * {@link HttpClient#setConnectTimeout(int) low connect timeout} should be used.
     * @param fastFail whether opening HTTP connections should fail fast
     */
    public void setFastFail(boolean fastFail) {
        this.fastFail = fastFail;
    }

    /**
     * Sets additional URL parameter (used e.g. for maps)
     * @param parameter the URL parameter
     * @since 13536
     */
    public void setParam(String parameter) {
        this.parameter = parameter;
    }

    public String getName() {
        if (parameter != null)
            return name.replaceAll("%<(.*)>", "");
        return name;
    }

    /**
     * Returns maximum age of cache file. Only applies to URLs.
     * When this time has passed after the last download of the file, the
     * cache is considered stale and a new download will be attempted.
     * @return the maximum cache age in seconds
     */
    public long getMaxAge() {
        return maxAge;
    }

    public String getDestDir() {
        return destDir;
    }

    public String getHttpAccept() {
        return httpAccept;
    }

    public CachingStrategy getCachingStrategy() {
        return cachingStrategy;
    }

    /**
     * Get InputStream to the requested resource.
     * @return the InputStream
     * @throws IOException when the resource with the given name could not be retrieved
     * @throws InvalidPathException if a Path object cannot be constructed from the inner file path
     */
    public InputStream getInputStream() throws IOException {
        File file = getFile();
        if (file == null) {
            if (name != null && name.startsWith("resource://")) {
                String resourceName = name.substring("resource:/".length());
                InputStream is = Utils.getResourceAsStream(getClass(), resourceName);
                if (is == null) {
                    throw new IOException(tr("Failed to open input stream for resource ''{0}''", name));
                }
                return is;
            } else {
                throw new IOException("No file found for: "+name);
            }
        }
        return Files.newInputStream(file.toPath());
    }

    /**
     * Get the full content of the requested resource as a byte array.
     * @return the full content of the requested resource as byte array
     * @throws IOException in case of an I/O error
     */
    public byte[] getByteContent() throws IOException {
        return Utils.readBytesFromStream(getInputStream());
    }

    /**
     * Returns {@link #getInputStream()} wrapped in a buffered reader.
     * <p>
     * Detects Unicode charset in use utilizing {@link UTFInputStreamReader}.
     *
     * @return buffered reader
     * @throws IOException if any I/O error occurs
     * @since 9411
     */
    public BufferedReader getContentReader() throws IOException {
        return new BufferedReader(UTFInputStreamReader.create(getInputStream()));
    }

    /**
     * Get local file for the requested resource.
     * @return The local cache file for URLs. If the resource is a local file,
     * returns just that file.
     * @throws IOException when the resource with the given name could not be retrieved
     */
    public synchronized File getFile() throws IOException {
        if (initialized)
            return cacheFile;
        initialized = true;
        URL url;
        try {
            url = new URL(name);
            if ("file".equals(url.getProtocol())) {
                cacheFile = new File(name.substring("file:/".length() - 1));
                if (!cacheFile.exists()) {
                    cacheFile = new File(name.substring("file://".length() - 1));
                }
            } else {
                try {
                    cacheFile = checkLocal(url);
                } catch (SecurityException e) {
                    throw new IOException(e);
                }
            }
        } catch (MalformedURLException e) {
            if (name == null || name.startsWith("resource://")) {
                return null;
            } else if (name.startsWith("josmdir://")) {
                cacheFile = new File(Config.getDirs().getUserDataDirectory(false), name.substring("josmdir://".length()));
            } else if (name.startsWith("josmplugindir://")) {
                cacheFile = new File(Preferences.main().getPluginsDirectory(), name.substring("josmplugindir://".length()));
            } else {
                cacheFile = new File(name);
            }
        }
        if (cacheFile == null)
            throw new IOException("Unable to get cache file for "+getName());
        return cacheFile;
    }

    /**
     * Looks for a certain entry inside a zip file and returns the entry path.
     *
     * Replies a file in the top level directory of the ZIP file which has an
     * extension <code>extension</code>. If more than one files have this
     * extension, the last file whose name includes <code>namepart</code>
     * is opened.
     *
     * @param extension  the extension of the file we're looking for
     * @param namepart the name part
     * @return The zip entry path of the matching file. <code>null</code> if this cached file
     * doesn't represent a zip file or if there was no matching
     * file in the ZIP file.
     */
    public String findZipEntryPath(String extension, String namepart) {
        Pair<String, InputStream> ze = findZipEntryImpl(extension, namepart);
        if (ze == null) return null;
        return ze.a;
    }

    /**
     * Like {@link #findZipEntryPath}, but returns the corresponding InputStream.
     * @param extension  the extension of the file we're looking for
     * @param namepart the name part
     * @return InputStream to the matching file. <code>null</code> if this cached file
     * doesn't represent a zip file or if there was no matching
     * file in the ZIP file.
     * @since 6148
     */
    public InputStream findZipEntryInputStream(String extension, String namepart) {
        Pair<String, InputStream> ze = findZipEntryImpl(extension, namepart);
        if (ze == null) return null;
        return ze.b;
    }

    private Pair<String, InputStream> findZipEntryImpl(String extension, String namepart) {
        File file = null;
        try {
            file = getFile();
        } catch (IOException ex) {
            Logging.log(Logging.LEVEL_WARN, ex);
        }
        if (file == null)
            return null;
        Pair<String, InputStream> res = null;
        try {
            ZipFile zipFile = new ZipFile(file, StandardCharsets.UTF_8);
            ZipEntry resentry = null;
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                // choose any file with correct extension. When more than one file, prefer the one which matches namepart
                if (entry.getName().endsWith('.' + extension) && (resentry == null || entry.getName().indexOf(namepart) >= 0)) {
                    resentry = entry;
                }
            }
            if (resentry != null) {
                InputStream is = zipFile.getInputStream(resentry);
                res = Pair.create(resentry.getName(), is);
            } else {
                Utils.close(zipFile);
            }
        } catch (IOException e) {
            if (file.getName().endsWith(".zip")) {
                Logging.log(Logging.LEVEL_WARN,
                        tr("Failed to open file with extension ''{2}'' and namepart ''{3}'' in zip file ''{0}''. Exception was: {1}",
                        file.getName(), e.toString(), extension, namepart), e);
            }
        }
        return res;
    }

    /**
     * Clear the cache for the given resource.
     * This forces a fresh download.
     * @param name the URL
     */
    public static void cleanup(String name) {
        cleanup(name, null);
    }

    /**
     * Clear the cache for the given resource.
     * This forces a fresh download.
     * @param name the URL
     * @param destDir the destination directory (see {@link #setDestDir(java.lang.String)})
     */
    public static void cleanup(String name, String destDir) {
        URL url;
        try {
            url = new URL(name);
            if (!"file".equals(url.getProtocol())) {
                String prefKey = getPrefKey(url, destDir);
                List<String> localPath = new ArrayList<>(Config.getPref().getList(prefKey));
                if (localPath.size() == 2) {
                    File lfile = new File(localPath.get(1));
                    if (lfile.exists()) {
                        Utils.deleteFile(lfile);
                    }
                }
                Config.getPref().putList(prefKey, null);
            }
        } catch (MalformedURLException e) {
            Logging.warn(e);
        }
    }

    /**
     * Get preference key to store the location and age of the cached file.
     * 2 resources that point to the same url, but that are to be stored in different
     * directories will not share a cache file.
     * @param url URL
     * @param destDir destination directory
     * @return Preference key
     */
    private static String getPrefKey(URL url, String destDir) {
        StringBuilder prefKey = new StringBuilder("mirror.");
        if (destDir != null) {
            prefKey.append(destDir).append('.');
        }
        prefKey.append(url.toString().replaceAll("%<(.*)>", ""));
        return prefKey.toString().replaceAll("=", "_");
    }

    private File checkLocal(URL url) throws IOException {
        String prefKey = getPrefKey(url, destDir);
        String urlStr = url.toExternalForm();
        if (parameter != null)
            urlStr = urlStr.replaceAll("%<(.*)>", "");
        long age = 0L;
        long maxAgeMillis = TimeUnit.SECONDS.toMillis(maxAge);
        Long ifModifiedSince = null;
        File localFile = null;
        List<String> localPathEntry = new ArrayList<>(Config.getPref().getList(prefKey));
        boolean offline = false;
        try {
            checkOfflineAccess(urlStr);
        } catch (OfflineAccessException e) {
            Logging.trace(e);
            offline = true;
        }
        if (localPathEntry.size() == 2) {
            localFile = new File(localPathEntry.get(1));
            if (!localFile.exists()) {
                localFile = null;
            } else {
                if (maxAge == DEFAULT_MAXTIME
                        || maxAge <= 0 // arbitrary value <= 0 is deprecated
                ) {
                    maxAgeMillis = TimeUnit.SECONDS.toMillis(Config.getPref().getLong("mirror.maxtime", TimeUnit.DAYS.toSeconds(7)));
                }
                age = System.currentTimeMillis() - Long.parseLong(localPathEntry.get(0));
                if (offline || age < maxAgeMillis) {
                    return localFile;
                }
                if (cachingStrategy == CachingStrategy.IfModifiedSince) {
                    ifModifiedSince = Long.valueOf(localPathEntry.get(0));
                }
            }
        }
        if (destDir == null) {
            destDir = Config.getDirs().getCacheDirectory(true).getPath();
        }

        File destDirFile = new File(destDir);
        if (!destDirFile.exists()) {
            Utils.mkDirs(destDirFile);
        }

        // No local file + offline => nothing to do
        if (offline) {
            return null;
        }

        if (parameter != null) {
            String u = url.toExternalForm();
            String uc;
            if (parameter.isEmpty()) {
                uc = u.replaceAll("%<(.*)>", "");
            } else {
                uc = u.replaceAll("%<(.*)>", "$1" + Utils.encodeUrl(parameter));
            }
            if (!uc.equals(u))
                url = new URL(uc);
        }

        String a = urlStr.replaceAll("[^A-Za-z0-9_.-]", "_");
        String localPath = "mirror_" + a;
        localPath = truncatePath(destDir, localPath);
        destDirFile = new File(destDir, localPath + ".tmp");
        try {
            activeConnection = HttpClient.create(url)
                    .setAccept(httpAccept)
                    .setIfModifiedSince(ifModifiedSince == null ? 0L : ifModifiedSince)
                    .setHeaders(httpHeaders);
            if (fastFail) {
                activeConnection.setReadTimeout(1000);
            }
            final HttpClient.Response con = activeConnection.connect();
            if (ifModifiedSince != null && con.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                Logging.debug("304 Not Modified ({0})", urlStr);
                if (localFile == null)
                    throw new AssertionError();
                Config.getPref().putList(prefKey,
                        Arrays.asList(Long.toString(System.currentTimeMillis()), localPathEntry.get(1)));
                return localFile;
            } else if (con.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                throw new IOException(tr("The requested URL {0} was not found", urlStr));
            }
            try (InputStream is = con.getContent()) {
                Files.copy(is, destDirFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            activeConnection = null;
            localFile = new File(destDir, localPath);
            if (PlatformManager.getPlatform().rename(destDirFile, localFile)) {
                Config.getPref().putList(prefKey,
                        Arrays.asList(Long.toString(System.currentTimeMillis()), localFile.toString()));
            } else {
                Logging.warn(tr("Failed to rename file {0} to {1}.",
                destDirFile.getPath(), localFile.getPath()));
            }
        } catch (IOException e) {
            if (age >= maxAgeMillis && age < maxAgeMillis*2) {
                Logging.warn(tr("Failed to load {0}, use cached file and retry next time: {1}", urlStr, e));
                return localFile;
            } else {
                throw e;
            }
        }

        return localFile;
    }

    private static void checkOfflineAccess(String urlString) {
        OnlineResource.JOSM_WEBSITE.checkOfflineAccess(urlString, Config.getUrls().getJOSMWebsite());
        OnlineResource.OSM_API.checkOfflineAccess(urlString, OsmApi.getOsmApi().getServerUrl());
    }

    private static String truncatePath(String directory, String fileName) {
        if (directory.length() + fileName.length() > 255) {
            // Windows doesn't support paths longer than 260, leave 5 chars as safe buffer, 4 will be used by ".tmp"
            // TODO: what about filename size on other systems? 255?
            if (directory.length() > 191 && PlatformManager.isPlatformWindows()) {
                // digest length + name prefix == 64
                // 255 - 64 = 191
                // TODO: use this check only on Windows?
                throw new IllegalArgumentException("Path " + directory + " too long to cached files");
            }

            MessageDigest md;
            try {
                md = MessageDigest.getInstance("SHA-256");
                md.update(fileName.getBytes(StandardCharsets.UTF_8));
                String digest = String.format("%064x", new BigInteger(1, md.digest()));
                return fileName.substring(0, Math.min(fileName.length(), 32)) + digest.substring(0, 32);
            } catch (NoSuchAlgorithmException e) {
                Logging.error(e);
                // TODO: what better can we do here?
                throw new IllegalArgumentException("Missing digest algorithm SHA-256", e);
            }
        }
        return fileName;
    }

    /**
     * Attempts to disconnect an URL connection.
     * @see HttpClient#disconnect()
     * @since 9411
     */
    @Override
    public void close() {
        if (activeConnection != null) {
            activeConnection.disconnect();
        }
    }

    /**
     * Clears the cached file
     * @throws IOException if any I/O error occurs
     * @since 10993
     */
    public void clear() throws IOException {
        URL url;
        try {
            url = new URL(name);
            if ("file".equals(url.getProtocol())) {
                return; // this is local file - do not delete it
            }
        } catch (MalformedURLException e) {
            return; // if it's not a URL, then it still might be a local file - better not to delete
        }
        File f = getFile();
        if (f != null && f.exists()) {
            Utils.deleteFile(f);
        }
    }
}
