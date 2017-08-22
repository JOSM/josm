// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.Optional;

import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Read from an other reader and increment an progress counter while on the way.
 * @author Imi
 */
public class ProgressInputStream extends InputStream {

    private final StreamProgressUpdater updater;
    private final InputStream in;

    /**
     * Constructs a new {@code ProgressInputStream}.
     *
     * @param in the stream to monitor. Must not be null
     * @param size the total size which will be sent
     * @param progressMonitor the monitor to report to
     * @since 9172
     */
    public ProgressInputStream(InputStream in, long size, ProgressMonitor progressMonitor) {
        CheckParameterUtil.ensureParameterNotNull(in, "in");
        this.updater = new StreamProgressUpdater(size,
                Optional.ofNullable(progressMonitor).orElse(NullProgressMonitor.INSTANCE), tr("Downloading data..."));
        this.in = in;
    }

    /**
     * Constructs a new {@code ProgressInputStream}.
     *
     * Will call {@link URLConnection#getInputStream()} to obtain the stream to monitor.
     *
     * @param con the connection to monitor
     * @param progressMonitor the monitor to report to
     * @throws OsmTransferException if any I/O error occurs
     * @deprecated use {@link org.openstreetmap.josm.tools.HttpClient.Response#getContent}
     */
    @Deprecated
    public ProgressInputStream(URLConnection con, ProgressMonitor progressMonitor) throws OsmTransferException {
        if (progressMonitor == null) {
            progressMonitor = NullProgressMonitor.INSTANCE;
        }
        progressMonitor.beginTask(tr("Contacting OSM Server..."), 1);
        progressMonitor.indeterminateSubTask(null);

        try {
            this.in = con.getInputStream();
            this.updater = new StreamProgressUpdater(con.getContentLength(), progressMonitor, tr("Downloading data..."));
        } catch (IOException e) {
            progressMonitor.finishTask();
            if (con.getHeaderField("Error") != null)
                throw new OsmTransferException(tr(con.getHeaderField("Error")), e);
            throw new OsmTransferException(e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            in.close();
        } finally {
            updater.finishTask();
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = in.read(b, off, len);
        if (read != -1) {
            updater.advanceTicker(read);
        } else {
            updater.finishTask();
        }
        return read;
    }

    @Override
    public int read() throws IOException {
        int read = in.read();
        if (read != -1) {
            updater.advanceTicker(1);
        } else {
            updater.finishTask();
        }
        return read;
    }
}
