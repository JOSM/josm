// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.openstreetmap.josm.gui.PleaseWaitDialog;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.SAXException;

/**
 * Asynchronous task for downloading and unpacking arbitrary file lists
 * Shows progress bar when downloading
 */
public class DownloadFileTask extends PleaseWaitRunnable {
    private final String address;
    private final File file;
    private final boolean mkdir;
    private final boolean unpack;

    /**
     * Creates the download task
     *
     * @param parent the parent component relative to which the {@link PleaseWaitDialog} is displayed
     * @param address the URL to download
     * @param file The destination file
     * @param mkdir {@code true} if the destination directory must be created, {@code false} otherwise
     * @param unpack {@code true} if zip archives must be unpacked recursively, {@code false} otherwise
     * @throws IllegalArgumentException if {@code parent} is null
     */
    public DownloadFileTask(Component parent, String address, File file, boolean mkdir, boolean unpack) {
        super(parent, tr("Downloading file"), false);
        this.address = address;
        this.file = file;
        this.mkdir = mkdir;
        this.unpack = unpack;
    }

    private static class DownloadException extends Exception {
        /**
         * Constructs a new {@code DownloadException}.
         * @param message the detail message. The detail message is saved for
         *          later retrieval by the {@link #getMessage()} method.
         * @param  cause the cause (which is saved for later retrieval by the
         *         {@link #getCause()} method).  (A <tt>null</tt> value is
         *         permitted, and indicates that the cause is nonexistent or unknown.)
         */
        DownloadException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private boolean canceled;
    private HttpClient downloadConnection;

    private synchronized void closeConnectionIfNeeded() {
        if (downloadConnection != null) {
            downloadConnection.disconnect();
        }
        downloadConnection = null;
    }

    @Override
    protected void cancel() {
        this.canceled = true;
        closeConnectionIfNeeded();
    }

    @Override
    protected void finish() {
        // Do nothing
    }

    /**
     * Performs download.
     * @throws DownloadException if the URL is invalid or if any I/O error occurs.
     */
    public void download() throws DownloadException {
        try {
            if (mkdir) {
                File newDir = file.getParentFile();
                if (!newDir.exists()) {
                    Utils.mkDirs(newDir);
                }
            }

            URL url = new URL(address);
            long size;
            synchronized (this) {
                downloadConnection = HttpClient.create(url).useCache(false);
                downloadConnection.connect();
                size = downloadConnection.getResponse().getContentLength();
            }

            progressMonitor.setTicksCount(100);
            progressMonitor.subTask(tr("Downloading File {0}: {1} bytes...", file.getName(), size));

            try (
                InputStream in = downloadConnection.getResponse().getContent();
                OutputStream out = Files.newOutputStream(file.toPath())
            ) {
                byte[] buffer = new byte[32_768];
                int count = 0;
                long p1 = 0;
                long p2;
                for (int read = in.read(buffer); read != -1; read = in.read(buffer)) {
                    out.write(buffer, 0, read);
                    count += read;
                    if (canceled) break;
                    p2 = 100L * count / size;
                    if (p2 != p1) {
                        progressMonitor.setTicks((int) p2);
                        p1 = p2;
                    }
                }
            }
            if (!canceled) {
                Logging.info(tr("Download finished"));
                if (unpack) {
                    Logging.info(tr("Unpacking {0} into {1}", file.getAbsolutePath(), file.getParent()));
                    unzipFileRecursively(file, file.getParent());
                    Utils.deleteFile(file);
                }
            }
        } catch (MalformedURLException e) {
            String msg = tr("Cannot download file ''{0}''. Its download link ''{1}'' is not a valid URL. Skipping download.",
                    file.getName(), address);
            Logging.warn(msg);
            throw new DownloadException(msg, e);
        } catch (IOException | InvalidPathException e) {
            if (canceled)
                return;
            throw new DownloadException(e.getMessage(), e);
        } finally {
            closeConnectionIfNeeded();
        }
    }

    @Override
    protected void realRun() throws SAXException, IOException {
        if (canceled) return;
        try {
            download();
        } catch (DownloadException e) {
            Logging.error(e);
        }
    }

    /**
     * Replies true if the task was canceled by the user
     *
     * @return {@code true} if the task was canceled by the user, {@code false} otherwise
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Recursive unzipping function
     * TODO: May be placed somewhere else - Tools.Utils?
     * @param file zip file
     * @param dir output directory
     * @throws IOException if any I/O error occurs
     */
    public static void unzipFileRecursively(File file, String dir) throws IOException {
        try (ZipFile zf = new ZipFile(file, StandardCharsets.UTF_8)) {
            Enumeration<? extends ZipEntry> es = zf.entries();
            while (es.hasMoreElements()) {
                ZipEntry ze = es.nextElement();
                File newFile = new File(dir, ze.getName());
                if (ze.isDirectory()) {
                    Utils.mkDirs(newFile);
                } else try (InputStream is = zf.getInputStream(ze)) {
                    Files.copy(is, newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
