// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.HttpClient;

/**
 * Read from an other reader and increment an progress counter while on the way.
 * @author Imi
 */
public class ProgressInputStream extends InputStream {

    private final InputStream in;
    private final long size;
    private int readSoFar;
    private int lastDialogUpdate;
    private final ProgressMonitor progressMonitor;

    public ProgressInputStream(InputStream in, long size, ProgressMonitor progressMonitor) {
        if (progressMonitor == null) {
            progressMonitor = NullProgressMonitor.INSTANCE;
        }
        this.in = in;
        this.size = size;
        this.progressMonitor = progressMonitor;
        progressMonitor.beginTask(tr("Contacting OSM Server..."), 1);
        progressMonitor.indeterminateSubTask(null);
        initProgressMonitor();
    }

    public ProgressInputStream(HttpClient.Response response, ProgressMonitor progressMonitor) throws IOException {
        this(response.getContent(), response.getContentLength(), progressMonitor);
    }

    public ProgressInputStream(URLConnection con, ProgressMonitor progressMonitor) throws OsmTransferException {
        if (progressMonitor == null) {
            progressMonitor = NullProgressMonitor.INSTANCE;
        }
        this.progressMonitor = progressMonitor;
        progressMonitor.beginTask(tr("Contacting OSM Server..."), 1);
        progressMonitor.indeterminateSubTask(null);

        try {
            this.in = con.getInputStream();
            this.size = con.getContentLength();
        } catch (IOException e) {
            progressMonitor.finishTask();
            if (con.getHeaderField("Error") != null)
                throw new OsmTransferException(tr(con.getHeaderField("Error")), e);
            throw new OsmTransferException(e);
        }
        initProgressMonitor();
    }

    protected void initProgressMonitor() {
        if (size > 0) {
            progressMonitor.subTask(tr("Downloading OSM data..."));
            progressMonitor.setTicksCount((int) size);
        } else {
            progressMonitor.indeterminateSubTask(tr("Downloading OSM data..."));
        }
    }

    @Override public void close() throws IOException {
        try {
            in.close();
        } finally {
            progressMonitor.finishTask();
        }
    }

    @Override public int read(byte[] b, int off, int len) throws IOException {
        int read = in.read(b, off, len);
        if (read != -1) {
            advanceTicker(read);
        } else {
            progressMonitor.finishTask();
        }
        return read;
    }

    @Override public int read() throws IOException {
        int read = in.read();
        if (read != -1) {
            advanceTicker(1);
        } else {
            progressMonitor.finishTask();
        }
        return read;
    }

    /**
     * Increase ticker (progress counter and displayed text) by the given amount.
     * @param amount number of ticks
     */
    private void advanceTicker(int amount) {
        readSoFar += amount;

        if (readSoFar / 1024 != lastDialogUpdate) {
            lastDialogUpdate++;
            if (size > 0) {
                progressMonitor.setTicks(readSoFar);
            }
            progressMonitor.setExtraText(readSoFar/1024 + " KB");
        }
    }
}
