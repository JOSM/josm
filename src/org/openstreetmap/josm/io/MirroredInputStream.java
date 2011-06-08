// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.openstreetmap.josm.Main;

/**
 * Mirrors a file to a local file.
 * <p>
 * The file mirrored is only downloaded if it has been more than 7 days since last download
 */
public class MirroredInputStream extends InputStream {
    InputStream fs = null;
    File file = null;

    public MirroredInputStream(String name) throws IOException {
        this(name, null, -1L);
    }

    public MirroredInputStream(String name, long maxTime) throws IOException {
        this(name, null, maxTime);
    }

    public MirroredInputStream(String name, String destDir) throws IOException {
        this(name, destDir, -1L);
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
                if(Main.applet) {
                    URLConnection conn = url.openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    fs = new BufferedInputStream(conn.getInputStream());
                    file = new File(url.getFile());
                } else {
                    file = checkLocal(url, destDir, maxTime);
                }
            }
        } catch (java.net.MalformedURLException e) {
            if(name.startsWith("resource://")) {
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
     * Replies an input stream for a file in a ZIP-file. Replies a file in the top
     * level directory of the ZIP file which has an extension <code>extension</code>. If more
     * than one files have this extension, the last file whose name includes <code>namepart</code>
     * is opened.
     *
     * @param extension  the extension of the file we're looking for
     * @param namepart the name part
     * @return an input stream. Null if this mirrored input stream doesn't represent a zip file or if
     * there was no matching file in the ZIP file
     */
    public InputStream getZipEntry(String extension, String namepart) {
        if (file == null)
            return null;
        InputStream res = null;
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
                res = zipFile.getInputStream(resentry);
            } else {
                zipFile.close();
            }
        } catch (Exception e) {
            if(file.getName().endsWith(".zip")) {
                System.err.println(tr("Warning: failed to open file with extension ''{2}'' and namepart ''{3}'' in zip file ''{0}''. Exception was: {1}",
                        file.getName(), e.toString(), extension, namepart));
            }
        }
        return res;
    }

    public File getFile()
    {
        return file;
    }

    static public void cleanup(String name)
    {
        cleanup(name, null);
    }
    static public void cleanup(String name, String destDir)
    {
        URL url;
        try {
            url = new URL(name);
            if (!url.getProtocol().equals("file"))
            {
                String prefKey = getPrefKey(url, destDir);
                // FIXME: replace with normal getCollection after july 2011
                Collection<String> localPath = Main.pref.getCollectionOld(prefKey, ";");
                if(localPath.size() == 2) {
                    String[] lp = (String[]) localPath.toArray();
                    File lfile = new File(lp[1]);
                    if(lfile.exists()) {
                        lfile.delete();
                    }
                }
                Main.pref.put(prefKey, null);
            }
        } catch (java.net.MalformedURLException e) {}
    }

    /**
     * get preference key to store the location and age of the cached file.
     * 2 resources that point to the same url, but that are to be stored in different
     * directories will not share a cache file.
     */
    private static String getPrefKey(URL url, String destDir) {
        StringBuilder prefKey = new StringBuilder("mirror.");
        if (destDir != null) {
            String prefDir = Main.pref.getPreferencesDir();
            if (destDir.startsWith(prefDir)) {
                destDir = destDir.substring(prefDir.length());
            }
            prefKey.append(destDir);
            prefKey.append(".");
        }
        prefKey.append(url.toString());
        return prefKey.toString().replaceAll("=","_");
    }

    private File checkLocal(URL url, String destDir, long maxTime) throws IOException {
        String prefKey = getPrefKey(url, destDir);
        long age = 0L;
        File file = null;
        // FIXME: replace with normal getCollection after july 2011
        Collection<String> localPathEntry = Main.pref.getCollectionOld(prefKey, ";");
        if(localPathEntry.size() == 2) {
            String[] lp = (String[]) localPathEntry.toArray();
            file = new File(lp[1]);
            if(!file.exists())
                file = null;
            else {
                if (maxTime <= 0) {
                    maxTime = Main.pref.getInteger("mirror.maxtime", 7*24*60*60);
                }
                age = System.currentTimeMillis() - Long.parseLong(lp[0]);
                if (age < maxTime*1000) {
                    return file;
                }
            }
        }
        if(destDir == null) {
            destDir = Main.pref.getPreferencesDir();
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
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            bis = new BufferedInputStream(conn.getInputStream());
            bos = new BufferedOutputStream( new FileOutputStream(destDirFile));
            byte[] buffer = new byte[4096];
            int length;
            while ((length = bis.read(buffer)) > -1) {
                bos.write(buffer, 0, length);
            }
            bos.close();
            bos = null;
            file = new File(destDir, localPath);
            destDirFile.renameTo(file);
            Main.pref.putCollection(prefKey, Arrays.asList(new String[]
            {Long.toString(System.currentTimeMillis()), file.toString()}));
        } catch (IOException e) {
            if (age > maxTime*1000 && age < maxTime*1000*2) {
                System.out.println(tr("Failed to load {0}, use cached file and retry next time: {1}",
                url, e));
                return file;
            } else {
                throw e;
            }
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return file;
    }
    @Override
    public int available() throws IOException
    { return fs.available(); }
    @Override
    public void close() throws IOException
    { fs.close(); }
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
