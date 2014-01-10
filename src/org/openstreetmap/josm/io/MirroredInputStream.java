// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

/**
 * Mirrors a file to a local file.
 * <p>
 * The file mirrored is only downloaded if it has been more than 7 days since last download
 */
public class MirroredInputStream extends InputStream {
    InputStream fs = null;
    File file = null;

    public final static long DEFAULT_MAXTIME = -1L;

    public MirroredInputStream(String name) throws IOException {
        this(name, null, DEFAULT_MAXTIME);
    }

    public MirroredInputStream(String name, long maxTime) throws IOException {
        this(name, null, maxTime);
    }

    public MirroredInputStream(String name, String destDir) throws IOException {
        this(name, destDir, DEFAULT_MAXTIME);
    }

    /**
     * Get an inputstream from a given filename, url or internal resource.
     * @param name can be
     *  - relative or absolute file name
     *  - file:///SOME/FILE the same as above
     *  - resource://SOME/FILE file from the classpath (usually in the current *.jar)
     *  - http://... a url. It will be cached on disk.
     * @param destDir the destination directory for the cache file. only applies for urls.
     * @param maxTime the maximum age of the cache file (in seconds)
     * @throws IOException when the resource with the given name could not be retrieved
     */
    public MirroredInputStream(String name, String destDir, long maxTime) throws IOException {
        URL url;
        try {
            url = new URL(name);
            if (url.getProtocol().equals("file")) {
                file = new File(name.substring("file:/".length()));
                if (!file.exists()) {
                    file = new File(name.substring("file://".length()));
                }
            } else {
                if (Main.applet) {
                    fs = new BufferedInputStream(Utils.openURL(url));
                    file = new File(url.getFile());
                } else {
                    file = checkLocal(url, destDir, maxTime);
                }
            }
        } catch (java.net.MalformedURLException e) {
            if (name.startsWith("resource://")) {
                fs = getClass().getResourceAsStream(
                        name.substring("resource:/".length()));
                if (fs == null)
                    throw new IOException(tr("Failed to open input stream for resource ''{0}''", name));
                return;
            }
            file = new File(name);
        }
        if (file == null)
            throw new IOException();
        fs = new FileInputStream(file);
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
     * @return The zip entry path of the matching file. Null if this mirrored
     * input stream doesn't represent a zip file or if there was no matching
     * file in the ZIP file.
     */
    public String findZipEntryPath(String extension, String namepart) {
        Pair<String, InputStream> ze = findZipEntryImpl(extension, namepart);
        if (ze == null) return null;
        return ze.a;
    }

    /**
     * Like {@link #findZipEntryPath}, but returns the corresponding InputStream.
     */
    public InputStream findZipEntryInputStream(String extension, String namepart) {
        Pair<String, InputStream> ze = findZipEntryImpl(extension, namepart);
        if (ze == null) return null;
        return ze.b;
    }

    @Deprecated // use findZipEntryInputStream
    public InputStream getZipEntry(String extension, String namepart) {
        return findZipEntryInputStream(extension, namepart);
    }

    private Pair<String, InputStream> findZipEntryImpl(String extension, String namepart) {
        if (file == null)
            return null;
        Pair<String, InputStream> res = null;
        try {
            ZipFile zipFile = new ZipFile(file);
            ZipEntry resentry = null;
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().endsWith("." + extension)) {
                    /* choose any file with correct extension. When more than
                        one file, prefer the one which matches namepart */
                    if (resentry == null || entry.getName().indexOf(namepart) >= 0) {
                        resentry = entry;
                    }
                }
            }
            if (resentry != null) {
                InputStream is = zipFile.getInputStream(resentry);
                res = Pair.create(resentry.getName(), is);
            } else {
                Utils.close(zipFile);
            }
        } catch (Exception e) {
            if (file.getName().endsWith(".zip")) {
                Main.warn(tr("Failed to open file with extension ''{2}'' and namepart ''{3}'' in zip file ''{0}''. Exception was: {1}",
                        file.getName(), e.toString(), extension, namepart));
            }
        }
        return res;
    }

    public File getFile() {
        return file;
    }

    public static void cleanup(String name) {
        cleanup(name, null);
    }
    
    public static void cleanup(String name, String destDir) {
        URL url;
        try {
            url = new URL(name);
            if (!url.getProtocol().equals("file")) {
                String prefKey = getPrefKey(url, destDir);
                List<String> localPath = new ArrayList<String>(Main.pref.getCollection(prefKey));
                if (localPath.size() == 2) {
                    File lfile = new File(localPath.get(1));
                    if(lfile.exists()) {
                        lfile.delete();
                    }
                }
                Main.pref.putCollection(prefKey, null);
            }
        } catch (MalformedURLException e) {
            Main.warn(e);
        }
    }

    /**
     * get preference key to store the location and age of the cached file.
     * 2 resources that point to the same url, but that are to be stored in different
     * directories will not share a cache file.
     */
    private static String getPrefKey(URL url, String destDir) {
        StringBuilder prefKey = new StringBuilder("mirror.");
        if (destDir != null) {
            prefKey.append(destDir);
            prefKey.append(".");
        }
        prefKey.append(url.toString());
        return prefKey.toString().replaceAll("=","_");
    }

    private File checkLocal(URL url, String destDir, long maxTime) throws IOException {
        String prefKey = getPrefKey(url, destDir);
        long age = 0L;
        File localFile = null;
        List<String> localPathEntry = new ArrayList<String>(Main.pref.getCollection(prefKey));
        if (localPathEntry.size() == 2) {
            localFile = new File(localPathEntry.get(1));
            if(!localFile.exists())
                localFile = null;
            else {
                if ( maxTime == DEFAULT_MAXTIME
                        || maxTime <= 0 // arbitrary value <= 0 is deprecated
                ) {
                    maxTime = Main.pref.getInteger("mirror.maxtime", 7*24*60*60); // one week
                }
                age = System.currentTimeMillis() - Long.parseLong(localPathEntry.get(0));
                if (age < maxTime*1000) {
                    return localFile;
                }
            }
        }
        if (destDir == null) {
            destDir = Main.pref.getCacheDirectory().getPath();
        }

        File destDirFile = new File(destDir);
        if (!destDirFile.exists()) {
            destDirFile.mkdirs();
        }

        String a = url.toString().replaceAll("[^A-Za-z0-9_.-]", "_");
        String localPath = "mirror_" + a;
        destDirFile = new File(destDir, localPath + ".tmp");
        BufferedOutputStream bos = null;
        BufferedInputStream bis = null;
        try {
            HttpURLConnection con = connectFollowingRedirect(url);
            bis = new BufferedInputStream(con.getInputStream());
            FileOutputStream fos = new FileOutputStream(destDirFile);
            bos = new BufferedOutputStream(fos);
            byte[] buffer = new byte[4096];
            int length;
            while ((length = bis.read(buffer)) > -1) {
                bos.write(buffer, 0, length);
            }
            Utils.close(bos);
            bos = null;
            /* close fos as well to be sure! */
            Utils.close(fos);
            fos = null;
            localFile = new File(destDir, localPath);
            if(Main.platform.rename(destDirFile, localFile)) {
                Main.pref.putCollection(prefKey, Arrays.asList(new String[]
                {Long.toString(System.currentTimeMillis()), localFile.toString()}));
            } else {
                Main.warn(tr("Failed to rename file {0} to {1}.",
                destDirFile.getPath(), localFile.getPath()));
            }
        } catch (IOException e) {
            if (age >= maxTime*1000 && age < maxTime*1000*2) {
                Main.warn(tr("Failed to load {0}, use cached file and retry next time: {1}", url, e));
                return localFile;
            } else {
                throw e;
            }
        } finally {
            Utils.close(bis);
            Utils.close(bos);
        }

        return localFile;
    }

    /**
     * Opens a connection for downloading a resource.
     * <p>
     * Manually follows redirects because
     * {@link HttpURLConnection#setFollowRedirects(boolean)} fails if the redirect
     * is going from a http to a https URL, see <a href="https://bugs.openjdk.java.net/browse/JDK-4620571">bug report</a>.
     * <p>
     * This can causes problems when downloading from certain GitHub URLs.
     * 
     * @param downloadUrl The resource URL to download
     * @return The HTTP connection effectively linked to the resource, after all potential redirections
     * @throws MalformedURLException If a redirected URL is wrong
     * @throws IOException If any I/O operation goes wrong
     * @since 6073
     */
    public static HttpURLConnection connectFollowingRedirect(URL downloadUrl) throws MalformedURLException, IOException {
        HttpURLConnection con = null;
        int numRedirects = 0;
        while(true) {
            con = Utils.openHttpConnection(downloadUrl);
            con.setInstanceFollowRedirects(false);
            con.setConnectTimeout(Main.pref.getInteger("socket.timeout.connect",15)*1000);
            con.setReadTimeout(Main.pref.getInteger("socket.timeout.read",30)*1000);
            try {
                con.connect();
            } catch (IOException e) {
                Main.addNetworkError(downloadUrl, Utils.getRootCause(e));
                throw e;
            }
            switch(con.getResponseCode()) {
            case HttpURLConnection.HTTP_OK:
                return con;
            case HttpURLConnection.HTTP_MOVED_PERM:
            case HttpURLConnection.HTTP_MOVED_TEMP:
            case HttpURLConnection.HTTP_SEE_OTHER:
                String redirectLocation = con.getHeaderField("Location");
                if (downloadUrl == null) {
                    /* I18n: argument is HTTP response code */ String msg = tr("Unexpected response from HTTP server. Got {0} response without ''Location'' header. Can''t redirect. Aborting.", con.getResponseCode());
                    throw new IOException(msg);
                }
                downloadUrl = new URL(redirectLocation);
                // keep track of redirect attempts to break a redirect loops if it happens
                // to occur for whatever reason
                numRedirects++;
                if (numRedirects >= Main.pref.getInteger("socket.maxredirects", 5)) {
                    String msg = tr("Too many redirects to the download URL detected. Aborting.");
                    throw new IOException(msg);
                }
                Main.info(tr("Download redirected to ''{0}''", downloadUrl));
                break;
            default:
                String msg = tr("Failed to read from ''{0}''. Server responded with status code {1}.", downloadUrl, con.getResponseCode());
                throw new IOException(msg);
            }
        }
    }

    @Override
    public int available() throws IOException
    { return fs.available(); }
    @Override
    public void close() throws IOException
    { Utils.close(fs); }
    @Override
    public int read() throws IOException
    { return fs.read(); }
    @Override
    public int read(byte[] b) throws IOException
    { return fs.read(b); }
    @Override
    public int read(byte[] b, int off, int len) throws IOException
    { return fs.read(b,off, len); }
    @Override
    public long skip(long n) throws IOException
    { return fs.skip(n); }
}
