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

import org.openstreetmap.josm.Main;

/**
 * Mirrors a file to a local file.
 * <p>
 * The file mirrored is only downloaded if it has been more than one day since last download
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
                file = checkLocal(url, destDir, maxTime);
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
                String localPath = Main.pref.get("mirror." + url);
                if (localPath != null && localPath.length() > 0)
                {
                    String[] lp = localPath.split(";");
                    File lfile = new File(lp[1]);
                    if(lfile.exists()) {
                        lfile.delete();
                    }
                }
                Main.pref.put("mirror." + url, null);
            }
        } catch (java.net.MalformedURLException e) {}
    }

    private File checkLocal(URL url, String destDir, long maxTime) {
        String localPath = Main.pref.get("mirror." + url);
        File file = null;
        if (localPath != null && localPath.length() > 0) {
            String[] lp = localPath.split(";");
            file = new File(lp[1]);
            if (maxTime <= 0) {
                maxTime = Main.pref.getInteger("mirror.maxtime", 7*24*60*60);
            }
            if (System.currentTimeMillis() - Long.parseLong(lp[0]) < maxTime*1000) {
                if(file.exists())
                    return file;
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
        localPath = "mirror_" + a;
        destDirFile = new File(destDir, localPath + ".tmp");
        BufferedOutputStream bos = null;
        BufferedInputStream bis = null;
        try {
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(5000);
            bis = new BufferedInputStream(conn.getInputStream());
            bos = new BufferedOutputStream( new FileOutputStream(destDirFile));
            byte[] buffer = new byte[4096];
            int length;
            while ((length = bis.read(buffer)) > -1) {
                bos.write(buffer, 0, length);
            }
        } catch(IOException ioe) {
            if (file != null)
                return file;
            return null;
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
            file = new File(destDir, localPath);
            destDirFile.renameTo(file);
            Main.pref.put("mirror." + url, System.currentTimeMillis() + ";" + file);
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
